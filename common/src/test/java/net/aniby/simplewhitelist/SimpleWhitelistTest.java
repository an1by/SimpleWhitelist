package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.aniby.simplewhitelist.command.WhitelistPermission;
import net.aniby.simplewhitelist.configuration.Whitelist;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.aniby.simplewhitelist.mod.VanillaCommandSourceExecutor;
import net.aniby.simplewhitelist.mod.VanillaModBridge;
import net.aniby.simplewhitelist.mod.mixin.SimpleWhitelistMixinPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SimpleWhitelistTest {

    // === Mock multi-era classes for VanillaModBridge & ASM verification ===

    public record ObfuscatedNameAndId(UUID comp_4421, String comp_4422) {
    }

    public static class LegacyGameProfile {
        private final String name;

        public LegacyGameProfile(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static class MockVanillaComponent {
        private final String content;

        public MockVanillaComponent(String content) {
            this.content = content;
        }

        public static MockVanillaComponent literal(String text) {
            return new MockVanillaComponent(text);
        }

        public String content() {
            return this.content;
        }
    }

    public static class MockPacketListener {
        public MockVanillaComponent disconnectedReason;

        public void disconnect(MockVanillaComponent reason) {
            this.disconnectedReason = reason;
        }
    }

    public static class MockServerPlayer {
        private final LegacyGameProfile profile;
        public final MockPacketListener connection = new MockPacketListener();

        public MockServerPlayer(String name) {
            this.profile = new LegacyGameProfile(name);
        }

        public LegacyGameProfile getGameProfile() {
            return this.profile;
        }
    }

    public static class MockPlayerList {
        private final List<MockServerPlayer> players = new ArrayList<>();
        private final boolean simulateBan;

        public MockPlayerList() {
            this(false);
        }

        public MockPlayerList(boolean simulateBan) {
            this.simulateBan = simulateBan;
        }

        public List<MockServerPlayer> getPlayers() {
            return this.players;
        }

        public MockVanillaComponent canPlayerLogin(SocketAddress address, Object profile) {
            if (this.simulateBan) {
                return MockVanillaComponent.literal("Banned");
            }
            return null;
        }
    }

    public static class MockPermLevel {
        public final int level;

        public MockPermLevel(int level) {
            this.level = level;
        }
    }

    public static class MockModernSource {
        public final int permLevel;
        public final List<String> sent = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();

        public MockModernSource(int permLevel) {
            this.permLevel = permLevel;
        }

        public Object getEntity() {
            return new Object();
        }

        public void sendSuccess(Supplier<MockVanillaComponent> supplier, boolean broadcast) {
            this.sent.add(supplier.get().content());
        }

        public void sendFailure(MockVanillaComponent err) {
            this.errors.add(err.content());
        }
    }

    public static class MockCommands26 {
        public static final MockPermLevel LEVEL_ALL = new MockPermLevel(0);
        public static final MockPermLevel LEVEL_MODERATORS = new MockPermLevel(1);
        public static final MockPermLevel LEVEL_GAMEMASTERS = new MockPermLevel(2);
        public static final MockPermLevel LEVEL_ADMINS = new MockPermLevel(3);
        public static final MockPermLevel LEVEL_OWNERS = new MockPermLevel(4);

        private final CommandDispatcher<MockModernSource> dispatcher = new CommandDispatcher<>();

        public MockCommands26() {
        }

        public static Predicate<MockModernSource> hasPermission(MockPermLevel req) {
            return src -> src.permLevel >= req.level;
        }

        public CommandDispatcher<MockModernSource> getDispatcher() {
            return this.dispatcher;
        }
    }

    @Test
    void whitelistCaseInsensitiveAddContainsRemove() {
        Whitelist wl = new Whitelist(Set.of("An1by"), false);
        assertTrue(wl.contains("an1by"));
        assertTrue(wl.contains("AN1BY"));
        assertFalse(wl.add("an1by"));
        assertTrue(wl.remove("AN1BY"));
        assertFalse(wl.contains("An1by"));
        assertFalse(wl.remove("An1by"));
    }

    @Test
    void whitelistCaseSensitiveAddContainsRemove() {
        Whitelist wl = new Whitelist(Set.of("An1by"), true);
        assertTrue(wl.contains("An1by"));
        assertFalse(wl.contains("an1by"));
        assertFalse(wl.remove("an1by"));
        assertTrue(wl.remove("An1by"));
        assertFalse(wl.contains("An1by"));
    }

    @Test
    void configurationSaveSettingsAndReloadRoundTrip(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("config.yml");
        Path whitelistFile = tempDir.resolve("whitelist.txt");

        WhitelistConfiguration cfg = new WhitelistConfiguration(configFile, whitelistFile);
        assertTrue(cfg.getSettings().isEnabled());
        assertFalse(cfg.getSettings().isCaseSensitive());
        assertTrue(cfg.getSettings().isEnforceWhitelist());

        cfg.getSettings().setEnabled(false);
        cfg.saveSettings();

        String dumped = Files.readString(configFile, StandardCharsets.UTF_8);
        assertFalse(dumped.contains("!!"), "Dumped YAML must not contain global class tag");

        cfg.load();
        assertFalse(cfg.getSettings().isEnabled());

        // Block-style legacy !! tag (Issues #22 & #17): loads cleanly AND self-heals file on disk
        Files.writeString(configFile,
                "!!net.aniby.simplewhitelist.configuration.WhitelistSettings\nenabled: true\ncaseSensitive: true\n",
                StandardCharsets.UTF_8);
        cfg.load();
        assertTrue(cfg.getSettings().isEnabled());
        assertTrue(cfg.getSettings().isCaseSensitive());
        assertNotNull(cfg.getMessage("notWhitelisted"));
        assertFalse(Files.readString(configFile, StandardCharsets.UTF_8).contains("!!"),
                "Self-healed config.yml must strip legacy !! global tag");

        // Flow-style legacy !! tag: loads cleanly AND self-heals
        Files.writeString(configFile,
                "--- !!net.aniby.simplewhitelist.configuration.WhitelistSettings {enabled: true, caseSensitive: false, logBlockedJoins: false}\n",
                StandardCharsets.UTF_8);
        cfg.load();
        assertTrue(cfg.getSettings().isEnabled());
        assertFalse(cfg.getSettings().isCaseSensitive());
        assertFalse(cfg.getSettings().isLogBlockedJoins());
        assertFalse(Files.readString(configFile, StandardCharsets.UTF_8).contains("!!"));

        // Malformed YAML must fail-closed (never throw NPE/YAMLException or disable whitelist)
        Files.writeString(configFile, "enabled: [unclosed_yaml_syntax\n", StandardCharsets.UTF_8);
        assertDoesNotThrow(cfg::load);
        assertNotNull(cfg.getSettings());
        assertTrue(cfg.getSettings().isEnabled(), "Malformed config must preserve fail-closed enabled state");
    }

    @Test
    void bedrockPrefixesSpacesPermissionsAndBlockedJoinActions(@TempDir Path tempDir) throws Exception {
        WhitelistConfiguration cfg = new WhitelistConfiguration(
                tempDir.resolve("config.yml"),
                tempDir.resolve("whitelist.txt")
        );
        List<String> online = List.of("*LemonOx917136", ".Geyser_Player", "+Plus-User", "*Xbox Gamer Tag");
        List<String> consoleLogs = new ArrayList<>();
        List<String> executedCommands = new ArrayList<>();

        WhitelistPlugin plugin = new WhitelistPlugin() {
            @Override
            public WhitelistConfiguration getConfiguration() {
                return cfg;
            }

            @Override
            public Collection<String> getOnlinePlayerNames() {
                return online;
            }

            @Override
            public void logConsoleMessage(Component message) {
                consoleLogs.add(PlainTextComponentSerializer.plainText().serialize(message));
            }

            @Override
            public void executeConsoleCommand(String command) {
                executedCommands.add(command);
            }
        };

        List<String> sentMessages = new ArrayList<>();
        Set<String> perms = new HashSet<>();
        WhitelistCommandSourceExecutor<String> executor = new WhitelistCommandSourceExecutor<>() {
            @Override
            public boolean hasPermission(String source, String permission) {
                return perms.contains(permission);
            }

            @Override
            public void sendMessage(String source, Component message) {
                sentMessages.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        };

        CommandDispatcher<String> dispatcher = new CommandDispatcher<>();
        new WhitelistCommand<>(executor, plugin).register(dispatcher);
        RawCommandRunner<String> rawRunner = new RawCommandRunner<>(executor, plugin);

        // Issue #14: Moderator with only `add` permission gets noPermission (not invalidArguments) on /swl disable
        perms.add(WhitelistPermission.ADD.getPermission());
        assertEquals(List.of("add", "help"), rawRunner.suggest("mod", ""));
        assertEquals(List.of(), rawRunner.suggest("mod", "remove "));
        assertFalse(rawRunner.execute("mod", "disable"));
        assertEquals("Not enough permissions!", sentMessages.get(sentMessages.size() - 1));

        // Issues #21 & #17: Bedrock/Floodgate/Geyser prefixes (* . + -) and spaces work in Brigadier & RawCommandRunner
        perms.add(WhitelistPermission.REMOVE.getPermission());
        ParseResults<String> parseAdd = dispatcher.parse("swl add *", "mod");
        List<String> suggestions = dispatcher.getCompletionSuggestions(parseAdd).join()
                .getList().stream().map(Suggestion::getText).collect(Collectors.toList());
        assertEquals(List.of("*LemonOx917136", "*Xbox Gamer Tag"), suggestions);

        assertEquals(1, dispatcher.execute("swl add *LemonOx917136", "mod"));
        assertEquals(1, dispatcher.execute("swl add .Geyser_Player", "mod"));
        assertEquals(1, dispatcher.execute("swl add +Plus-User", "mod"));
        assertEquals(1, dispatcher.execute("swl add *Xbox Gamer Tag", "mod"));
        assertTrue(cfg.getWhitelist().contains("*lemonox917136"));
        assertTrue(cfg.getWhitelist().contains(".geyser_player"));
        assertTrue(cfg.getWhitelist().contains("+plus-user"));
        assertTrue(cfg.getWhitelist().contains("*xbox gamer tag"));

        assertTrue(rawRunner.execute("mod", "remove *lemonox917136"));
        assertFalse(cfg.getWhitelist().contains("*LemonOx917136"));

        // Issues #13 & #10: logBlockedJoins toggle + onBlockedJoinCommands + command injection guard
        cfg.getSettings().setLogBlockedJoins(true);
        cfg.getSettings().setOnBlockedJoinCommands(List.of(
                "/say Blocked <name>",
                "notify %player% %name% {name}"
        ));
        cfg.getSettings().getMessages().put("notInWhitelist", "<red><name> is not whitelisted!</red>");

        String kickText = PlainTextComponentSerializer.plainText()
                .serialize(plugin.getNotInWhitelistMessage("*LemonOx917136"));
        assertEquals("*LemonOx917136 is not whitelisted!", kickText);

        plugin.handleBlockedJoin("*LemonOx917136");
        assertEquals(1, consoleLogs.size());
        assertTrue(consoleLogs.get(0).contains("*LemonOx917136"));
        assertEquals(List.of(
                "say Blocked *LemonOx917136",
                "notify *LemonOx917136 *LemonOx917136 *LemonOx917136"
        ), executedCommands);

        // Toggle logBlockedJoins off (#13 separate setting toggle)
        consoleLogs.clear();
        executedCommands.clear();
        cfg.getSettings().setLogBlockedJoins(false);
        plugin.handleBlockedJoin("*Xbox Gamer Tag");
        assertTrue(consoleLogs.isEmpty(), "logBlockedJoins=false must suppress console log");
        assertEquals(List.of(
                "say Blocked *Xbox Gamer Tag",
                "notify *Xbox Gamer Tag *Xbox Gamer Tag *Xbox Gamer Tag"
        ), executedCommands);

        // Malicious pre-login username must NEVER execute console commands
        executedCommands.clear();
        assertFalse(WhitelistPlugin.isSafePlayerName("Evil; op Evil"));
        assertFalse(WhitelistPlugin.isSafePlayerName("Evil\nstop"));
        assertFalse(WhitelistPlugin.isSafePlayerName("Evil/op"));
        plugin.handleBlockedJoin("Evil; op Evil");
        assertTrue(executedCommands.isEmpty(), "Unsafe player names must not trigger console commands");
    }

    @Test
    void commandExecutionPermissionsSuggestionsAndRawRunner(@TempDir Path tempDir) throws Exception {
        WhitelistConfiguration cfg = new WhitelistConfiguration(
                tempDir.resolve("config.yml"),
                tempDir.resolve("whitelist.txt")
        );
        AtomicInteger kickCalls = new AtomicInteger();
        List<String> online = List.of("Steve", "Alex");

        WhitelistPlugin plugin = new WhitelistPlugin() {
            @Override
            public WhitelistConfiguration getConfiguration() {
                return cfg;
            }

            @Override
            public Collection<String> getOnlinePlayerNames() {
                return online;
            }

            @Override
            public void kickUnwhitelistedPlayers() {
                kickCalls.incrementAndGet();
            }
        };

        List<String> messages = new ArrayList<>();
        Set<String> perms = new HashSet<>();

        WhitelistCommandSourceExecutor<String> executor = new WhitelistCommandSourceExecutor<>() {
            @Override
            public boolean hasPermission(String source, String permission) {
                return perms.contains(permission);
            }

            @Override
            public void sendMessage(String source, Component message) {
                messages.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        };

        CommandDispatcher<String> dispatcher = new CommandDispatcher<>();
        new WhitelistCommand<>(executor, plugin).register(dispatcher);
        RawCommandRunner<String> rawRunner = new RawCommandRunner<>(executor, plugin);

        // Denied without permission
        assertFalse(rawRunner.execute("guest", "list"));
        assertEquals(List.of(), rawRunner.suggest("guest", ""));

        // Granular permission: only list allowed via alias /swl
        perms.add(WhitelistPermission.LIST.getPermission());
        assertTrue(WhitelistPermission.hasAnyPermission("user", executor));
        assertFalse(WhitelistPermission.ADD.hasPermission("user", executor));
        assertEquals(1, dispatcher.execute("swl list", "user"));
        assertTrue(messages.get(messages.size() - 1).contains("Empty"));

        // Grant wildcard permission
        perms.clear();
        perms.add(WhitelistPermission.ALL.getPermission());

        assertEquals(List.of("add", "disable", "enable", "help", "list", "reload", "remove"), rawRunner.suggest("admin", ""));
        assertEquals(List.of("Alex", "Steve"), rawRunner.suggest("admin", "add "));

        // Missing player argument produces needPlayer message
        assertFalse(rawRunner.execute("admin", new String[]{"add"}));

        ParseResults<String> addParse = dispatcher.parse("simplewhitelist add ", "admin");
        List<String> addSuggestions = dispatcher.getCompletionSuggestions(addParse).join()
                .getList().stream().map(Suggestion::getText).collect(Collectors.toList());
        assertEquals(List.of("Alex", "Steve"), addSuggestions);

        assertEquals(1, dispatcher.execute("swl add Steve", "admin"));
        assertEquals(0, dispatcher.execute("swl add steve", "admin"));

        assertEquals(List.of("Steve"), rawRunner.suggest("admin", "remove s"));

        assertEquals(1, dispatcher.execute("swl remove STEVE", "admin"));
        assertEquals(1, kickCalls.get());
        assertEquals(0, dispatcher.execute("swl remove STEVE", "admin"));
    }

    @Test
    void profileExtractionSupportsLegacyModernAndObfuscatedNameAndId() {
        assertEquals("An1by", VanillaModBridge.extractProfileName(new LegacyGameProfile("An1by")));
        assertEquals("Notch", VanillaModBridge.extractProfileName(new ObfuscatedNameAndId(UUID.randomUUID(), "Notch")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void asmTransformationAnd26xBridgeEndToEnd(@TempDir Path tempDir) throws Exception {
        // Re-initialize VanillaModBridge config in tempDir
        WhitelistConfiguration cfg = new WhitelistConfiguration(
                tempDir.resolve("config.yml"),
                tempDir.resolve("whitelist.txt")
        );
        java.lang.reflect.Field cfgField = VanillaModBridge.class.getDeclaredField("configuration");
        cfgField.setAccessible(true);
        cfgField.set(VanillaModBridge.INSTANCE, cfg);

        java.lang.reflect.Field compField = VanillaModBridge.class.getDeclaredField("vanillaComponentClass");
        compField.setAccessible(true);
        compField.set(VanillaModBridge.INSTANCE, null);

        java.lang.reflect.Field predField = VanillaModBridge.class.getDeclaredField("commandsAdminPredicate");
        predField.setAccessible(true);
        predField.set(VanillaModBridge.INSTANCE, null);

        // Transform both MockPlayerList and MockCommands26 via SimpleWhitelistMixinPlugin ASM
        byte[] transformedPlayerList = transformClass(MockPlayerList.class, true);
        byte[] transformedCommands = transformClass(MockCommands26.class, false);

        // Verify transformed bytecode strictly with ASM CheckClassAdapter
        CheckClassAdapter.verify(new ClassReader(transformedPlayerList), false, new java.io.PrintWriter(System.out));
        CheckClassAdapter.verify(new ClassReader(transformedCommands), false, new java.io.PrintWriter(System.out));

        ClassLoader testLoader = new ClassLoader(SimpleWhitelistTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(MockPlayerList.class.getName())) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = defineClass(name, transformedPlayerList, 0, transformedPlayerList.length);
                    }
                    if (resolve) resolveClass(c);
                    return c;
                }
                if (name.equals(MockCommands26.class.getName())) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = defineClass(name, transformedCommands, 0, transformedCommands.length);
                    }
                    if (resolve) resolveClass(c);
                    return c;
                }
                return super.loadClass(name, resolve);
            }
        };

        // Instantiate transformed PlayerList (triggers onPlayerListConstructed)
        Class<?> plClass = Class.forName(MockPlayerList.class.getName(), true, testLoader);
        Object plInstance = plClass.getConstructor().newInstance();
        assertSame(plInstance, VanillaModBridge.INSTANCE.getPlayerList());

        List<MockServerPlayer> onlineList = (List<MockServerPlayer>) plClass.getMethod("getPlayers").invoke(plInstance);
        MockServerPlayer intruder = new MockServerPlayer("Intruder");
        onlineList.add(intruder);
        assertEquals(List.of("Intruder"), VanillaModBridge.INSTANCE.getOnlinePlayerNames());

        // Unwhitelisted login with 1.21.9+/26.x obfuscated NameAndId must return non-null MockVanillaComponent
        Method canLogin = plClass.getMethod("canPlayerLogin", SocketAddress.class, Object.class);
        Object denyComp = canLogin.invoke(
                plInstance,
                new InetSocketAddress("127.0.0.1", 25565),
                new ObfuscatedNameAndId(UUID.randomUUID(), "Intruder")
        );
        assertNotNull(denyComp, "Unwhitelisted player must be denied login");
        assertTrue(denyComp instanceof MockVanillaComponent);

        // Instantiate transformed MockCommands26 (triggers onCommandsConstructed + caches 26.x LEVEL_ADMINS predicate)
        Class<?> cmdClass = Class.forName(MockCommands26.class.getName(), true, testLoader);
        Object cmdInstance = cmdClass.getConstructor().newInstance();
        CommandDispatcher<MockModernSource> dispatcher =
                (CommandDispatcher<MockModernSource>) cmdClass.getMethod("getDispatcher").invoke(cmdInstance);
        assertNotNull(dispatcher.getRoot().getChild("simplewhitelist"));
        assertNotNull(dispatcher.getRoot().getChild("swl"));

        // Level 0 player cannot execute /swl add, Level 3 admin (26.x LEVEL_ADMINS) CAN!
        VanillaCommandSourceExecutor modExec = new VanillaCommandSourceExecutor(VanillaModBridge.INSTANCE);
        MockModernSource lowSource = new MockModernSource(0);
        MockModernSource adminSource = new MockModernSource(3);
        assertFalse(modExec.hasPermission(lowSource, WhitelistPermission.ADD.getPermission()));
        assertTrue(modExec.hasPermission(adminSource, WhitelistPermission.ADD.getPermission()));

        // Execute /swl add AllowedUser via transformed Commands dispatcher
        assertEquals(1, dispatcher.execute("swl add AllowedUser", adminSource));
        assertFalse(adminSource.sent.isEmpty());

        // AllowedUser can now log in (returns null)!
        Object allowComp = canLogin.invoke(
                plInstance,
                new InetSocketAddress("127.0.0.1", 25565),
                new ObfuscatedNameAndId(UUID.randomUUID(), "AllowedUser")
        );
        assertNull(allowComp, "Whitelisted player must be allowed to log in");

        // Enabling whitelist kicks online unwhitelisted Intruder!
        assertEquals(1, dispatcher.execute("swl enable", adminSource));
        assertNotNull(intruder.connection.disconnectedReason, "Unwhitelisted online player must be disconnected");
    }

    private static byte[] transformClass(Class<?> clazz, boolean isPlayerList) throws Exception {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getResourceAsStream(resource)) {
            assertNotNull(in);
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            if (isPlayerList) {
                SimpleWhitelistMixinPlugin.transformPlayerList(node);
            } else {
                SimpleWhitelistMixinPlugin.transformCommands(node);
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }
    }
}
