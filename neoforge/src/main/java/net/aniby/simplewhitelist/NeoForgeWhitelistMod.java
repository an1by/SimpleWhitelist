package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
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

    private final WhitelistConfiguration configuration;
    private volatile MinecraftServerAudiences adventure;

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.configuration;
    }

    public MinecraftServerAudiences getAdventure() {
        return this.adventure;
    }

    public NeoForgeWhitelistMod(IEventBus eventBus) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        this.configuration = new WhitelistConfiguration(
                path.resolve("config.yml"),
                path.resolve("whitelist.txt")
        );

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    private void onConnect(ServerPlayConnectionEvent event) {
        String name = event.getGameProfile().getName();
        if (this.configuration.getSettings().isEnabled()
                && !this.configuration.getWhitelist().contains(name)) {
            event.cancel(this.adventure.asNative(
                    this.configuration.getMessage("notInWhitelist")
            ));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.adventure = MinecraftServerAudiences.of(event.getServer());
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppedEvent event) {
        this.adventure = null;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        new WhitelistCommand<>(
                new NeoForgeCommandSourceExecutor(this), this
        ).register(event.getDispatcher());
    }
}
