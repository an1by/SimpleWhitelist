package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.WhitelistCommand;
import net.aniby.simplewhitelist.WhitelistMod;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class FabricWhitelistMod extends WhitelistMod implements DedicatedServerModInitializer {
    private PluginWhitelist whitelist;

    private PluginConfiguration configuration;

    @Override
    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public PluginWhitelist getWhitelist() {
        return this.whitelist;
    }

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getGameDir().resolve("/config/SimpleWhitelist");

        System.out.println("created");
        this.whitelist = new PluginWhitelist(path.resolve("whitelist.txt"));
        this.configuration = new PluginConfiguration(path.resolve("config.json"));

        System.out.println(this.getWhitelist());
        System.out.println(this.getConfiguration());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        WhitelistCommand.register(dispatcher, this)
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            String name = player.getName().getString();
            if (this.configuration.isEnabled() && !this.whitelist.isWhitelisted(name)) {
                player.connection.disconnect(Component.literal(
                        this.configuration.getMessage("not_in_whitelist")
                ));
            }
        });
    }
}
