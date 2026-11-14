package net.aniby.simplewhitelist.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class VanillaModBridge implements WhitelistPlugin {
    public static final VanillaModBridge INSTANCE = new VanillaModBridge();

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .build();

    private volatile WhitelistConfiguration configuration;
    private volatile Object playerList;
    private volatile Object commandsInstance;
    @SuppressWarnings("rawtypes")
    private volatile CommandDispatcher commandDispatcher;
    private volatile Class<?> vanillaComponentClass;
    private volatile Predicate<Object> commandsAdminPredicate;
    private volatile BiFunction<Object, String, Boolean> customPermissionChecker;
    private volatile boolean mixinApplied;

    private VanillaModBridge() {
    }

    public synchronized WhitelistConfiguration init(Path configDirectory) {
        if (this.configuration == null) {
            Path dir = configDirectory != null ? configDirectory : Paths.get("config", "simplewhitelist");
            this.configuration = new WhitelistConfiguration(
                    dir.resolve("config.yml"),
                    dir.resolve("whitelist.txt")
            );
        }
        return this.configuration;
    }

    public WhitelistConfiguration ensureConfiguration() {
        WhitelistConfiguration cfg = this.configuration;
        if (cfg == null) {
            cfg = this.init(Paths.get("config", "simplewhitelist"));
        }
        return cfg;
    }

    public void markMixinApplied() {
        this.mixinApplied = true;
    }

    public boolean isMixinApplied() {
        return this.mixinApplied;
    }

    public void setCustomPermissionChecker(BiFunction<Object, String, Boolean> checker) {
        this.customPermissionChecker = checker;
    }

    public BiFunction<Object, String, Boolean> getCustomPermissionChecker() {
        return this.customPermissionChecker;
    }

    public Predicate<Object> getCommandsAdminPredicate() {
        return this.commandsAdminPredicate;
    }

    public Object getPlayerList() {
        return this.playerList;
    }

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.ensureConfiguration();
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        Object pl = this.playerList;
        if (pl == null) {
            return Collections.emptyList();
        }
        List<?> players = getPlayersSnapshot(pl);
        List<String> names = new ArrayList<>(players.size());
        for (Object player : players) {
            String name = extractPlayerName(player);
            if (name != null && !name.trim().isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        final Object pl = this.playerList;
        if (pl == null) {
            return;
        }
        final WhitelistConfiguration cfg = this.ensureConfiguration();
        Runnable task = () -> {
            Class<?> compClass = resolveComponentClass(pl);
            if (compClass == null) {
                return;
            }
            for (Object player : getPlayersSnapshot(pl)) {
                String name = extractPlayerName(player);
                if (name != null && !cfg.getWhitelist().contains(name)) {
                    Object reason = toVanillaComponent(
                            this.getNotInWhitelistMessage(name),
                            compClass,
                            pl.getClass().getClassLoader()
                    );
                    if (reason != null) {
                        disconnectPlayer(player, reason, compClass);
                    }
                }
            }
        };

        runOnServerThread(pl, task);
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message == null) {
            return;
        }
        String plain = stripFormattingCodes(LEGACY_SECTION.serialize(message));
        try {
            Class<?> slf4jFactory = tryLoadClass("org.slf4j.LoggerFactory", VanillaModBridge.class.getClassLoader());
            if (slf4jFactory != null) {
                Object logger = slf4jFactory.getMethod("getLogger", String.class).invoke(null, "SimpleWhitelist");
                logger.getClass().getMethod("info", String.class).invoke(logger, plain);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> log4jManager = tryLoadClass("org.apache.logging.log4j.LogManager", VanillaModBridge.class.getClassLoader());
            if (log4jManager != null) {
                Object logger = log4jManager.getMethod("getLogger", String.class).invoke(null, "SimpleWhitelist");
                logger.getClass().getMethod("info", String.class).invoke(logger, plain);
                return;
            }
        } catch (Throwable ignored) {
        }
        java.util.logging.Logger.getLogger("SimpleWhitelist").info(plain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeConsoleCommand(final String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        final Object pl = this.playerList;
        runOnServerThread(pl, () -> {
            Object server = pl != null ? findMinecraftServer(pl) : null;
            Object consoleSource = server != null ? createConsoleCommandSource(server) : null;

            Object cmds = this.commandsInstance;
            if (cmds != null && consoleSource != null) {
                for (Method m : cmds.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 2 && p[0].isInstance(consoleSource) && p[1] == String.class) {
                        try {
                            m.invoke(cmds, consoleSource, command);
                            return;
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            CommandDispatcher<Object> disp = this.commandDispatcher;
            if (disp != null && consoleSource != null) {
                try {
                    disp.execute(command, consoleSource);
                    return;
                } catch (Throwable ignored) {
                }
            }

            // MC 1.12 fallback: server.getCommandManager().executeCommand(server, command)
            if (server != null) {
                for (String mgrGetter : new String[]{"func_71187_D", "getCommandManager"}) {
                    try {
                        Object mgr = server.getClass().getMethod(mgrGetter).invoke(server);
                        if (mgr != null) {
                            for (Method m : mgr.getClass().getMethods()) {
                                Class<?>[] p = m.getParameterTypes();
                                if (p.length == 2 && p[0].isInstance(server) && p[1] == String.class) {
                                    m.invoke(mgr, server, command);
                                    return;
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private static void runOnServerThread(Object playerListInstance, Runnable task) {
        Object server = playerListInstance != null ? findMinecraftServer(playerListInstance) : null;
        if (server instanceof Executor) {
            ((Executor) server).execute(task);
            return;
        }
        if (server != null) {
            try {
                for (Method m : server.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                        m.invoke(server, task);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        task.run();
    }

    private static Object createConsoleCommandSource(Object server) {
        for (String name : new String[]{"createCommandSourceStack", "method_29715", "func_195573_bab"}) {
            try {
                return server.getClass().getMethod(name).invoke(server);
            } catch (Throwable ignored) {
            }
        }
        for (Method m : server.getClass().getMethods()) {
            if (m.getParameterCount() == 0) {
                String retName = m.getReturnType().getName();
                if (retName.contains("CommandSource") || retName.contains("class_2168")) {
                    try {
                        return m.invoke(server);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static String stripFormattingCodes(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\u00A7' && i + 1 < input.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // === Static Hooks Invoked by ASM / Fallback Listeners ===

    public static void onPlayerListConstructed(Object playerListInstance) {
        if (playerListInstance == null) {
            return;
        }
        INSTANCE.playerList = playerListInstance;
        INSTANCE.resolveComponentClass(playerListInstance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onCommandsConstructed(Object commandsInstance) {
        if (commandsInstance == null) {
            return;
        }
        INSTANCE.commandsInstance = commandsInstance;
        INSTANCE.ensureConfiguration();
        INSTANCE.cacheAdminPredicateFromCommands(commandsInstance.getClass());
        CommandDispatcher dispatcher = findDispatcher(commandsInstance);
        if (dispatcher != null) {
            registerOnDispatcher(dispatcher);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerOnDispatcher(CommandDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        INSTANCE.commandDispatcher = dispatcher;
        if (dispatcher.getRoot().getChild("simplewhitelist") != null) {
            return;
        }
        INSTANCE.ensureConfiguration();
        WhitelistCommand command = new WhitelistCommand(new VanillaCommandSourceExecutor(INSTANCE), INSTANCE);
        command.register(dispatcher);
    }

    public static Object checkCanPlayerLogin(
            Object playerListInstance,
            SocketAddress socketAddress,
            Object profileOrNameAndId,
            String componentClassName
    ) {
        if (playerListInstance != null) {
            INSTANCE.playerList = playerListInstance;
        }
        WhitelistConfiguration cfg = INSTANCE.ensureConfiguration();
        if (!cfg.getSettings().isEnabled()) {
            return null;
        }
        String username = extractProfileName(profileOrNameAndId);
        if (username != null && cfg.getWhitelist().contains(username)) {
            return null;
        }

        INSTANCE.handleBlockedJoin(username);

        ClassLoader loader = playerListInstance != null
                ? playerListInstance.getClass().getClassLoader()
                : VanillaModBridge.class.getClassLoader();
        Class<?> compClass = INSTANCE.vanillaComponentClass;
        if (compClass == null && componentClassName != null && !componentClassName.isEmpty()) {
            compClass = tryLoadClass(componentClassName, loader);
            if (compClass != null) {
                INSTANCE.vanillaComponentClass = compClass;
            }
        }
        if (compClass == null && playerListInstance != null) {
            compClass = INSTANCE.resolveComponentClass(playerListInstance);
        }
        if (compClass == null) {
            return null;
        }
        return toVanillaComponent(INSTANCE.getNotInWhitelistMessage(username), compClass, loader);
    }

    // === Reflection Bridges ===

    public Class<?> resolveComponentClass(Object contextObj) {
        Class<?> cached = this.vanillaComponentClass;
        if (cached != null) {
            return cached;
        }
        ClassLoader loader = contextObj != null
                ? contextObj.getClass().getClassLoader()
                : VanillaModBridge.class.getClassLoader();
        if (contextObj != null) {
            for (Method m : contextObj.getClass().getMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 && SocketAddress.class.isAssignableFrom(params[0])
                        && m.getReturnType() != void.class && !m.getReturnType().isPrimitive()) {
                    this.vanillaComponentClass = m.getReturnType();
                    return this.vanillaComponentClass;
                }
            }
        }
        String[] candidates = {
                "net.minecraft.network.chat.Component",
                "net.minecraft.class_2561",
                "net.minecraft.util.text.ITextComponent"
        };
        for (String name : candidates) {
            Class<?> found = tryLoadClass(name, loader);
            if (found != null) {
                this.vanillaComponentClass = found;
                return found;
            }
        }
        return null;
    }

    public static Object toVanillaComponent(Component component, Class<?> targetComponentClass, ClassLoader loader) {
        if (component == null || targetComponentClass == null) {
            return null;
        }
        String json = GsonComponentSerializer.gson().serialize(component);

        // Tier 1: MC 1.20.5 - 26.3 ComponentSerialization.CODEC + JsonOps.INSTANCE
        Object viaCodec = tryCodecDeserialize(json, targetComponentClass, loader);
        if (viaCodec != null) {
            return viaCodec;
        }

        // Tier 2: MC 1.13.2 - 1.20.4 Serializer.fromJson(String)
        Object viaSerializer = trySerializerDeserialize(json, targetComponentClass);
        if (viaSerializer != null) {
            return viaSerializer;
        }

        // Tier 3: Universal Literal / TextComponentString fallback (1.12 - 26.3)
        String legacy = LEGACY_SECTION.serialize(component);
        return createLiteralComponent(legacy, targetComponentClass, loader);
    }

    private static Object tryCodecDeserialize(String json, Class<?> targetComponentClass, ClassLoader loader) {
        String[] codecHolders = {
                "net.minecraft.network.chat.ComponentSerialization",
                "net.minecraft.class_8824"
        };
        Class<?> jsonOpsClass = tryLoadClass("com.mojang.serialization.JsonOps", loader);
        Class<?> codecClass = tryLoadClass("com.mojang.serialization.Codec", loader);
        Class<?> dynamicOpsClass = tryLoadClass("com.mojang.serialization.DynamicOps", loader);
        Class<?> jsonParserClass = tryLoadClass("com.google.gson.JsonParser", loader);
        if (jsonOpsClass == null || codecClass == null || dynamicOpsClass == null || jsonParserClass == null) {
            return null;
        }
        try {
            Object jsonOps = jsonOpsClass.getField("INSTANCE").get(null);
            Object jsonElement = parseJsonElement(jsonParserClass, json);
            Method parseMethod = codecClass.getMethod("parse", dynamicOpsClass, Object.class);

            for (String holderName : codecHolders) {
                Class<?> holder = tryLoadClass(holderName, loader);
                if (holder == null) {
                    continue;
                }
                for (Field field : holder.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && codecClass.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object codec = field.get(null);
                        if (codec == null) {
                            continue;
                        }
                        Object dataResult = parseMethod.invoke(codec, jsonOps, jsonElement);
                        if (dataResult != null) {
                            Method resultMethod = dataResult.getClass().getMethod("result");
                            Object optionalObj = resultMethod.invoke(dataResult);
                            if (optionalObj instanceof Optional) {
                                Optional<?> opt = (Optional<?>) optionalObj;
                                if (opt.isPresent() && targetComponentClass.isInstance(opt.get())) {
                                    return opt.get();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object parseJsonElement(Class<?> jsonParserClass, String json) throws Exception {
        try {
            Method parseString = jsonParserClass.getMethod("parseString", String.class);
            return parseString.invoke(null, json);
        } catch (NoSuchMethodException ignored) {
            Object parser = jsonParserClass.getDeclaredConstructor().newInstance();
            Method parse = jsonParserClass.getMethod("parse", String.class);
            return parse.invoke(parser, json);
        }
    }

    private static Object trySerializerDeserialize(String json, Class<?> targetComponentClass) {
        for (Class<?> inner : targetComponentClass.getDeclaredClasses()) {
            for (Method method : inner.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == String.class
                        && targetComponentClass.isAssignableFrom(method.getReturnType())) {
                    try {
                        method.setAccessible(true);
                        Object parsed = method.invoke(null, json);
                        if (parsed != null) {
                            return parsed;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    public static Object createLiteralComponent(String text, Class<?> targetComponentClass, ClassLoader loader) {
        // 1.19 - 26.3 static factory Component.literal(String) / Text.method_43470(String)
        String[] preferredFactoryNames = {"literal", "method_43470"};
        for (String factoryName : preferredFactoryNames) {
            try {
                Method m = targetComponentClass.getMethod(factoryName, String.class);
                if (Modifier.isStatic(m.getModifiers()) && targetComponentClass.isAssignableFrom(m.getReturnType())) {
                    return m.invoke(null, text);
                }
            } catch (Throwable ignored) {
            }
        }

        // 1.12 - 1.18.2 concrete literal component classes
        String[] concreteClasses = {
                "net.minecraft.network.chat.TextComponent",
                "net.minecraft.class_2585",
                "net.minecraft.util.text.StringTextComponent",
                "net.minecraft.util.text.TextComponentString"
        };
        for (String clsName : concreteClasses) {
            Class<?> cls = tryLoadClass(clsName, loader);
            if (cls != null && targetComponentClass.isAssignableFrom(cls)) {
                try {
                    Constructor<?> ctor = cls.getConstructor(String.class);
                    return ctor.newInstance(text);
                } catch (Throwable ignored) {
                }
            }
        }

        // Generic static (String) -> Component fallback on targetComponentClass
        for (Method m : targetComponentClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == String.class
                    && targetComponentClass.isAssignableFrom(m.getReturnType())
                    && !m.getName().toLowerCase().contains("translat")
                    && !m.getName().toLowerCase().contains("keybind")) {
                try {
                    return m.invoke(null, text);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    public static String extractProfileName(Object profileOrNameAndId) {
        if (profileOrNameAndId == null) {
            return null;
        }
        if (profileOrNameAndId instanceof String) {
            return (String) profileOrNameAndId;
        }
        Class<?> cls = profileOrNameAndId.getClass();
        for (String methodName : new String[]{"name", "getName"}) {
            try {
                Method m = cls.getMethod(methodName);
                if (m.getReturnType() == String.class) {
                    Object res = m.invoke(profileOrNameAndId);
                    if (res instanceof String) {
                        return (String) res;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        // Fallback for obfuscated records such as Fabric Intermediary 1.21.9-1.21.11 NameAndId(UUID, String)
        Field singleStringField = null;
        int stringFieldCount = 0;
        for (Field f : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                singleStringField = f;
                stringFieldCount++;
            }
        }
        if (stringFieldCount == 1 && singleStringField != null) {
            try {
                singleStringField.setAccessible(true);
                Object val = singleStringField.get(profileOrNameAndId);
                if (val instanceof String) {
                    return (String) val;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static String extractPlayerName(Object serverPlayer) {
        if (serverPlayer == null) {
            return null;
        }
        // Prefer GameProfile accessor (getGameProfile / method_7334 / func_146103_bH)
        for (Method m : serverPlayer.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getName().endsWith("GameProfile")) {
                try {
                    Object profile = m.invoke(serverPlayer);
                    String name = extractProfileName(profile);
                    if (name != null) {
                        return name;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        for (String methodName : new String[]{"getScoreboardName", "getGameProfile", "getName"}) {
            try {
                Method m = serverPlayer.getClass().getMethod(methodName);
                Object res = m.invoke(serverPlayer);
                if (res instanceof String) {
                    return (String) res;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static List<?> getPlayersSnapshot(Object playerListInstance) {
        for (Method m : playerListInstance.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                try {
                    Object res = m.invoke(playerListInstance);
                    if (res instanceof List) {
                        return new ArrayList<>((List<?>) res);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        Class<?> current = playerListInstance.getClass();
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && List.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object res = f.get(playerListInstance);
                        if (res instanceof List) {
                            return new ArrayList<>((List<?>) res);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        return Collections.emptyList();
    }

    public static void disconnectPlayer(Object serverPlayer, Object vanillaComponent, Class<?> componentClass) {
        if (serverPlayer == null || vanillaComponent == null || componentClass == null) {
            return;
        }
        Class<?> current = serverPlayer.getClass();
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
                    continue;
                }
                Method disconnectMethod = findDisconnectMethod(f.getType(), componentClass);
                if (disconnectMethod != null) {
                    try {
                        f.setAccessible(true);
                        Object connection = f.get(serverPlayer);
                        if (connection != null) {
                            disconnectMethod.invoke(connection, vanillaComponent);
                            return;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
    }

    private static Method findDisconnectMethod(Class<?> connectionType, Class<?> componentClass) {
        for (String preferred : new String[]{"disconnect", "method_14367", "func_194028_b"}) {
            try {
                return connectionType.getMethod(preferred, componentClass);
            } catch (NoSuchMethodException ignored) {
            }
        }
        String typeName = connectionType.getSimpleName().toLowerCase();
        if (typeName.contains("packet") || typeName.contains("net") || typeName.contains("connection")
                || connectionType.getName().contains("class_3244") || connectionType.getName().contains("class_8609")) {
            for (Method m : connectionType.getMethods()) {
                if (m.getParameterCount() == 1 && m.getReturnType() == void.class
                        && m.getParameterTypes()[0] == componentClass) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Object findMinecraftServer(Object playerListInstance) {
        for (Method m : playerListInstance.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getName().contains("MinecraftServer")) {
                try {
                    return m.invoke(playerListInstance);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static CommandDispatcher<?> findDispatcher(Object commandsInstance) {
        for (Method m : commandsInstance.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && CommandDispatcher.class.isAssignableFrom(m.getReturnType())) {
                try {
                    Object d = m.invoke(commandsInstance);
                    if (d instanceof CommandDispatcher) {
                        return (CommandDispatcher<?>) d;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        Class<?> current = commandsInstance.getClass();
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && CommandDispatcher.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object d = f.get(commandsInstance);
                        if (d instanceof CommandDispatcher) {
                            return (CommandDispatcher<?>) d;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void cacheAdminPredicateFromCommands(Class<?> commandsClass) {
        if (commandsClass == null || this.commandsAdminPredicate != null) {
            return;
        }
        // MC 1.21.11 & 26.x: Commands.hasPermission(Commands.LEVEL_ADMINS) -> Predicate<CommandSourceStack>
        try {
            Field named = null;
            try {
                named = commandsClass.getField("LEVEL_ADMINS");
            } catch (NoSuchFieldException ignored) {
            }
            for (Method m : commandsClass.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1
                        || !Predicate.class.isAssignableFrom(m.getReturnType())) {
                    continue;
                }
                Class<?> paramType = m.getParameterTypes()[0];
                if (named != null && paramType.isAssignableFrom(named.getType())) {
                    Object pred = m.invoke(null, named.get(null));
                    if (pred instanceof Predicate) {
                        this.commandsAdminPredicate = (Predicate<Object>) pred;
                        return;
                    }
                }
                List<Field> matchingFields = new ArrayList<>();
                for (Field f : commandsClass.getFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
                            && paramType.isAssignableFrom(f.getType())) {
                        matchingFields.add(f);
                    }
                }
                // LEVEL_ALL(0), LEVEL_MODERATORS(1), LEVEL_GAMEMASTERS(2), LEVEL_ADMINS(3), LEVEL_OWNERS(4)
                if (matchingFields.size() >= 4) {
                    Object levelAdmins = matchingFields.get(3).get(null);
                    Object pred = m.invoke(null, levelAdmins);
                    if (pred instanceof Predicate) {
                        this.commandsAdminPredicate = (Predicate<Object>) pred;
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static Class<?> tryLoadClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable ignored) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }
}
