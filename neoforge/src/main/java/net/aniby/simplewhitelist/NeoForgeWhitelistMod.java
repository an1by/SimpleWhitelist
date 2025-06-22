package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.event.ServerPlayConnectionEvent;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.nio.file.Path;

@Mod(NeoForgeWhitelistMod.MOD_ID)
public class NeoForgeWhitelistMod implements WhitelistPlugin {
    public static final String MOD_ID = "simplewhitelist";

    private final PluginConfiguration configuration;
    private volatile MinecraftServerAudiences adventure;

    @Override
    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }

    public NeoForgeWhitelistMod(IEventBus eventBus) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        this.configuration = new PluginConfiguration(
                path.resolve("config.yml"),
                path.resolve("whitelist.txt")
        );

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    private void onConnect(ServerPlayConnectionEvent event) {
        String name = event.getPlayer().getName().getString();
        if (this.configuration.getConfigurationFile().isEnabled()
                && !this.configuration.getWhitelist().contains(name)) {
            event.getConnection().disconnect(this.adventure.asNative(
                    this.configuration.getMessage("not_in_whitelist")
            ));
        }
    }

    @SubscribeEvent
    private void onServerStarting(ServerStartingEvent event) {
        this.adventure = MinecraftServerAudiences.of(event.getServer());
    }

    @SubscribeEvent
    private void onServerStop(ServerStoppedEvent event) {
        this.adventure = null;
    }

    @SubscribeEvent
    private void onRegisterCommands(RegisterCommandsEvent event) {
        WhitelistCommand.register(event.getDispatcher(), this);
    }
}
