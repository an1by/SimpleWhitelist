package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;

public final class SimpleWhitelist implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        try {
            SimpleCore.init(
                    FabricLoader.getInstance().getGameDir().resolve("/config/SimpleWhitelist")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> WhitelistCommand.register(dispatcher)
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String name = player.getName().getString();
            if (!SimpleCore.getConfiguration().whitelistContains(name))
                player.networkHandler.disconnect(Text.of(
                        SimpleCore.getConfiguration().getMessages().get("kick")
                ));
        });
    }
}