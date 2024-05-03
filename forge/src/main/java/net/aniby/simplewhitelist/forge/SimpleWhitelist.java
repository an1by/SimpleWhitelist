package net.aniby.simplewhitelist.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

@Mod(SimpleWhitelist.MODID)
public class SimpleWhitelist {

    public static final String MODID = "simplewhitelist";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleWhitelist() {
        try {
            SimpleCore.init(FMLPaths.CONFIGDIR.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!SimpleCore.getConfiguration().whitelistContains(player.getName().getString())) {
            event.setCanceled(true);
            ((ServerPlayer) player).connection.disconnect(Component.literal(
                    SimpleCore.getConfiguration().getMessages().get("kick")
            ));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        WhitelistCommand.register(event.getDispatcher());
    }
}
