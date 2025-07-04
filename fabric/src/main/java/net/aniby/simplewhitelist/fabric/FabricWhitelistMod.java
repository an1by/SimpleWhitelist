package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class FabricWhitelistMod implements DedicatedServerModInitializer, WhitelistPlugin {
    private volatile MinecraftServerAudiences adventure;
    private WhitelistConfiguration configuration;

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getGameDir().resolve("/config/SimpleWhitelist");
        this.configuration = new WhitelistConfiguration(
                path.resolve("config.yml"),
                path.resolve("whitelist.txt")
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            new WhitelistCommand<>(new FabricCommandSourceExecutor(), this).register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                this.adventure = MinecraftServerAudiences.of(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                this.adventure = null);
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            String name = player.getName().getString();
            if (this.configuration.getSettings().isEnabled()
                    && !this.configuration.getWhitelist().contains(name)) {
                player.connection.disconnect(this.adventure.asNative(
                        this.configuration.getMessage("notInWhitelist")
                ));
            }
        });
    }
}
