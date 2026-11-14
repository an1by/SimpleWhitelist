package net.aniby.simplewhitelist.forgelegacy;

import net.aniby.simplewhitelist.BuildConstants;
import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.aniby.simplewhitelist.command.WhitelistPermission;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.aniby.simplewhitelist.mod.VanillaModBridge;
import net.kyori.adventure.text.Component;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Mod(
        modid = "simplewhitelist",
        name = "SimpleWhitelist",
        version = BuildConstants.MOD_VERSION,
        acceptableRemoteVersions = "*",
        serverSideOnly = true
)
public class ForgeLegacyWhitelistMod {

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Path configDir = event.getModConfigurationDirectory().toPath().resolve("simplewhitelist");
        VanillaModBridge.INSTANCE.init(configDir);
        registerForgeEventBus(this);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        registerPermissionNodes();

        MinecraftServer server = event.getServer();
        Object playerList = findPlayerList(server);
        if (playerList != null) {
            VanillaModBridge.onPlayerListConstructed(playerList);
        }

        ICommand commandProxy = createCommandProxy();
        event.registerServerCommand(commandProxy);
    }

    @SubscribeEvent
    public void onClientConnection(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        WhitelistConfiguration cfg = VanillaModBridge.INSTANCE.getConfiguration();
        if (!cfg.getSettings().isEnabled()) {
            return;
        }
        INetHandler handler = event.getHandler();
        if (handler == null) {
            return;
        }
        Object player = extractPlayerFromNetHandler(handler);
        String username = VanillaModBridge.extractPlayerName(player);
        if (username != null && !cfg.getWhitelist().contains(username)) {
            VanillaModBridge.INSTANCE.handleBlockedJoin(username);
            disconnectNetHandler(handler, VanillaModBridge.INSTANCE.getNotInWhitelistMessage(username));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        WhitelistConfiguration cfg = VanillaModBridge.INSTANCE.getConfiguration();
        if (!cfg.getSettings().isEnabled() || event.player == null) {
            return;
        }
        String username = VanillaModBridge.extractPlayerName(event.player);
        if (username != null && !cfg.getWhitelist().contains(username)) {
            ClassLoader loader = event.player.getClass().getClassLoader();
            Class<?> iTextClass = VanillaModBridge.tryLoadClass("net.minecraft.util.text.ITextComponent", loader);
            if (iTextClass != null) {
                Object reason = VanillaModBridge.toVanillaComponent(
                        VanillaModBridge.INSTANCE.getNotInWhitelistMessage(username),
                        iTextClass,
                        loader
                );
                VanillaModBridge.disconnectPlayer(event.player, reason, iTextClass);
            }
        }
    }

    private static ICommand createCommandProxy() {
        final ClassLoader loader = ICommand.class.getClassLoader();
        final Class<?> iTextClass = VanillaModBridge.tryLoadClass("net.minecraft.util.text.ITextComponent", loader);

        final RawCommandRunner<Object> runner = new RawCommandRunner<>(
                new WhitelistCommandSourceExecutor<Object>() {
                    @Override
                    public boolean hasPermission(Object sender, String permission) {
                        if (sender == null) {
                            return false;
                        }
                        if (sender instanceof EntityPlayer) {
                            Boolean permApi = checkForge12PermissionApi(sender, permission);
                            if (permApi != null) {
                                return permApi;
                            }
                        }
                        // ICommandSender.canUseCommand(int permLevel, String commandName) / func_70003_b
                        for (Method m : sender.getClass().getMethods()) {
                            Class<?>[] p = m.getParameterTypes();
                            if (p.length == 2 && p[0] == int.class && p[1] == String.class && m.getReturnType() == boolean.class) {
                                try {
                                    return (Boolean) m.invoke(sender, 3, "simplewhitelist");
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        return !(sender instanceof EntityPlayer);
                    }

                    @Override
                    public void sendMessage(Object sender, Component message) {
                        if (sender == null || iTextClass == null) {
                            return;
                        }
                        Object textComp = VanillaModBridge.toVanillaComponent(message, iTextClass, loader);
                        if (textComp == null) {
                            return;
                        }
                        for (String name : new String[]{"func_145747_a", "sendMessage"}) {
                            try {
                                Method m = sender.getClass().getMethod(name, iTextClass);
                                m.invoke(sender, textComp);
                                return;
                            } catch (Throwable ignored) {
                            }
                        }
                        for (Method m : sender.getClass().getMethods()) {
                            if (m.getParameterCount() == 1 && m.getReturnType() == void.class
                                    && m.getParameterTypes()[0] == iTextClass) {
                                try {
                                    m.invoke(sender, textComp);
                                    return;
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                },
                VanillaModBridge.INSTANCE
        );

        InvocationHandler handler = (proxy, method, args) -> {
            String mName = method.getName();
            Class<?>[] pTypes = method.getParameterTypes();

            if ("equals".equals(mName) && pTypes.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(mName) && pTypes.length == 0) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(mName) && pTypes.length == 0) {
                return "SimpleWhitelistCommand";
            }
            // compareTo(ICommand)
            if ("compareTo".equals(mName) && pTypes.length == 1) {
                String otherName = extractCommandName(args[0]);
                return "simplewhitelist".compareTo(otherName != null ? otherName : "");
            }
            // getName() / func_71517_b() -> String
            if (pTypes.length == 0 && method.getReturnType() == String.class) {
                return "simplewhitelist";
            }
            // getAliases() / func_71514_a() -> List<String>
            if (pTypes.length == 0 && List.class.isAssignableFrom(method.getReturnType())) {
                return Arrays.asList("swl", "simplewl");
            }
            // getUsage(ICommandSender) / func_71518_a -> String
            if (pTypes.length == 1 && method.getReturnType() == String.class) {
                return "/simplewhitelist <add|remove|list|enable|disable|reload|help>";
            }
            // checkPermission(MinecraftServer, ICommandSender) / func_184882_a -> boolean
            if (pTypes.length == 2 && method.getReturnType() == boolean.class && pTypes[1] != int.class) {
                return runner.hasPermission(args[1]);
            }
            // isUsernameIndex(String[], int) / func_82358_a -> boolean
            if (pTypes.length == 2 && pTypes[0] == String[].class && pTypes[1] == int.class) {
                String[] cmdArgs = (String[]) args[0];
                int index = (Integer) args[1];
                return index == 1 && cmdArgs != null && cmdArgs.length >= 1
                        && ("add".equalsIgnoreCase(cmdArgs[0]) || "remove".equalsIgnoreCase(cmdArgs[0]));
            }
            // execute(MinecraftServer, ICommandSender, String[]) / func_184881_a -> void
            if (pTypes.length == 3 && method.getReturnType() == void.class) {
                runner.execute(args[1], (String[]) args[2]);
                return null;
            }
            // getTabCompletions(MinecraftServer, ICommandSender, String[], BlockPos) / func_184883_a -> List<String>
            if (pTypes.length == 4 && List.class.isAssignableFrom(method.getReturnType())) {
                return runner.suggest(args[1], (String[]) args[2]);
            }
            return null;
        };

        return (ICommand) Proxy.newProxyInstance(loader, new Class<?>[]{ICommand.class}, handler);
    }

    private static String extractCommandName(Object otherCommand) {
        if (otherCommand == null) {
            return null;
        }
        for (Method m : otherCommand.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == String.class && !"toString".equals(m.getName())) {
                try {
                    return (String) m.invoke(otherCommand);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static void registerForgeEventBus(Object listener) {
        try {
            Class<?> mfClass = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object bus = mfClass.getField("EVENT_BUS").get(null);
            Method reg = bus.getClass().getMethod("register", Object.class);
            reg.invoke(bus, listener);
        } catch (Throwable ignored) {
        }
    }

    private static void registerPermissionNodes() {
        try {
            ClassLoader loader = ForgeLegacyWhitelistMod.class.getClassLoader();
            Class<?> permApi = VanillaModBridge.tryLoadClass("net.minecraftforge.server.permission.PermissionAPI", loader);
            Class<?> levelEnum = VanillaModBridge.tryLoadClass("net.minecraftforge.server.permission.DefaultPermissionLevel", loader);
            if (permApi == null || levelEnum == null) {
                return;
            }
            Object opLevel = null;
            for (Object constant : levelEnum.getEnumConstants()) {
                if ("OP".equals(constant.toString())) {
                    opLevel = constant;
                    break;
                }
            }
            if (opLevel == null) {
                return;
            }
            Method registerNode = permApi.getMethod("registerNode", String.class, levelEnum, String.class);
            for (WhitelistPermission wp : WhitelistPermission.values()) {
                if (wp == WhitelistPermission.ALL) {
                    continue;
                }
                registerNode.invoke(null, wp.getPermission(), opLevel, "SimpleWhitelist permission");
            }
        } catch (Throwable ignored) {
        }
    }

    private static Boolean checkForge12PermissionApi(Object player, String permission) {
        if (permission.endsWith(".*")) {
            return null;
        }
        try {
            ClassLoader loader = player.getClass().getClassLoader();
            Class<?> permApi = VanillaModBridge.tryLoadClass("net.minecraftforge.server.permission.PermissionAPI", loader);
            if (permApi == null) {
                return null;
            }
            Method hasPerm = permApi.getMethod("hasPermission", EntityPlayer.class, String.class);
            return (Boolean) hasPerm.invoke(null, player, permission);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object findPlayerList(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        for (String mName : new String[]{"func_184103_al", "getPlayerList"}) {
            try {
                Method m = server.getClass().getMethod(mName);
                return m.invoke(server);
            } catch (Throwable ignored) {
            }
        }
        for (Method m : server.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("PlayerList")) {
                try {
                    return m.invoke(server);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object extractPlayerFromNetHandler(Object handler) {
        Class<?> current = handler.getClass();
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (EntityPlayer.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return f.get(handler);
                    } catch (Throwable ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void disconnectNetHandler(Object handler, Component message) {
        ClassLoader loader = handler.getClass().getClassLoader();
        Class<?> iTextClass = VanillaModBridge.tryLoadClass("net.minecraft.util.text.ITextComponent", loader);
        if (iTextClass == null) {
            return;
        }
        Object reason = VanillaModBridge.toVanillaComponent(message, iTextClass, loader);
        if (reason == null) {
            return;
        }
        for (String name : new String[]{"func_194028_b", "disconnect"}) {
            try {
                Method m = handler.getClass().getMethod(name, iTextClass);
                m.invoke(handler, reason);
                return;
            } catch (Throwable ignored) {
            }
        }
    }
}
