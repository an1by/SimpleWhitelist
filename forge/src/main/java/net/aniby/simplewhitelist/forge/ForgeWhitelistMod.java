package net.aniby.simplewhitelist.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.aniby.simplewhitelist.mod.FmlPermissionBridge;
import net.aniby.simplewhitelist.mod.VanillaModBridge;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.nio.file.Path;

@Mod("simplewhitelist")
public class ForgeWhitelistMod {
    public ForgeWhitelistMod() {
        Path configDir = FmlPermissionBridge.resolveFmlConfigDir(
                "net.minecraftforge.fml.loading.FMLPaths",
                "simplewhitelist"
        );
        VanillaModBridge.INSTANCE.init(configDir);

        ClassLoader loader = ForgeWhitelistMod.class.getClassLoader();
        FmlPermissionBridge.installPermissionApi(
                "simplewhitelist",
                "net.minecraftforge.common.MinecraftForge",
                "net.minecraftforge.server.permission",
                loader
        );

        // Fallback for Forge 1.13.2 - 1.15.2 (JavaFML 25-31 where Sponge Mixin was not bundled)
        if (!VanillaModBridge.INSTANCE.isMixinApplied()) {
            registerLegacyJavaFmlFallback(loader);
        }
    }

    private static void registerLegacyJavaFmlFallback(ClassLoader loader) {
        Class<?> serverStartingClass = VanillaModBridge.tryLoadClass(
                "net.minecraftforge.fml.event.server.FMLServerStartingEvent", loader
        );
        if (serverStartingClass != null) {
            FmlPermissionBridge.registerEventListener(
                    "net.minecraftforge.common.MinecraftForge",
                    serverStartingClass,
                    event -> {
                        try {
                            Method getServer = event.getClass().getMethod("getServer");
                            Object server = getServer.invoke(event);
                            if (server != null) {
                                for (Method m : server.getClass().getMethods()) {
                                    if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("PlayerList")) {
                                        VanillaModBridge.onPlayerListConstructed(m.invoke(server));
                                        break;
                                    }
                                }
                            }
                            Method getDispatcher = event.getClass().getMethod("getCommandDispatcher");
                            Object disp = getDispatcher.invoke(event);
                            if (disp instanceof CommandDispatcher) {
                                VanillaModBridge.registerOnDispatcher((CommandDispatcher<?>) disp);
                            }
                        } catch (Throwable ignored) {
                        }
                    },
                    loader
            );
        }

        Class<?> loggedInClass = VanillaModBridge.tryLoadClass(
                "net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent", loader
        );
        if (loggedInClass != null) {
            FmlPermissionBridge.registerEventListener(
                    "net.minecraftforge.common.MinecraftForge",
                    loggedInClass,
                    event -> {
                        WhitelistConfiguration cfg = VanillaModBridge.INSTANCE.getConfiguration();
                        if (!cfg.getSettings().isEnabled()) {
                            return;
                        }
                        try {
                            Object player = null;
                            for (String getter : new String[]{"getEntity", "getPlayer"}) {
                                try {
                                    player = event.getClass().getMethod(getter).invoke(event);
                                    if (player != null) break;
                                } catch (NoSuchMethodException ignored) {
                                }
                            }
                            String name = VanillaModBridge.extractPlayerName(player);
                            if (name != null && !cfg.getWhitelist().contains(name)) {
                                VanillaModBridge.INSTANCE.handleBlockedJoin(name);
                                Class<?> compClass = VanillaModBridge.INSTANCE.resolveComponentClass(player);
                                if (compClass != null) {
                                    Object reason = VanillaModBridge.toVanillaComponent(
                                            VanillaModBridge.INSTANCE.getNotInWhitelistMessage(name), compClass, loader
                                    );
                                    VanillaModBridge.disconnectPlayer(player, reason, compClass);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    },
                    loader
            );
        }
    }
}
