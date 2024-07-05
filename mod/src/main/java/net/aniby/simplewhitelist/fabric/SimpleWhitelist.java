package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class SimpleWhitelist implements DedicatedServerModInitializer {
    private PluginWhitelist whitelist;
    private PluginConfiguration configuration;

    public PluginConfiguration configuration() {
        return configuration;
    }

    public PluginWhitelist whitelist() {
        return whitelist;
    }

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getGameDir().resolve("/config/SimpleWhitelist");
        whitelist = new PluginWhitelist(path);
        configuration = new PluginConfiguration(path);

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> WhitelistCommand.register(dispatcher, this)
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String name = player.getName().getString();
            if (!whitelist.isWhitelisted(name))
                player.networkHandler.disconnect(Text.of(
                        configuration.getCommandMessage("kick")
                ));
        });
    }
}