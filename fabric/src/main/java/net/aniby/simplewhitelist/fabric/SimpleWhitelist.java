package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class SimpleWhitelist implements DedicatedServerModInitializer {
    private PluginWhitelist whitelist;

    private PluginConfiguration configuration;

    public PluginConfiguration configuration() {
        return this.configuration;
    }

    public PluginWhitelist whitelist() {
        return this.whitelist;
    }

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getGameDir().resolve("/config/SimpleWhitelist");
        this.whitelist = new PluginWhitelist(path);
        this.configuration = new PluginConfiguration(path);


        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> WhitelistCommand.register(dispatcher, this)
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            String name = player.getName().getString();
            if (!this.whitelist.isWhitelisted(name))
                player.connection.disconnect(Component.literal(
                        this.configuration.getMessage("not_in_whitelist")
                ));
        });
    }
}
