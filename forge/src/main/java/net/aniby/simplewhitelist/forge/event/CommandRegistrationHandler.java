package net.aniby.simplewhitelist.forge.event;

import net.aniby.simplewhitelist.forge.SimpleWhitelist;
import net.aniby.simplewhitelist.forge.WhitelistCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandRegistrationHandler {
    private final SimpleWhitelist plugin;
    public CommandRegistrationHandler(SimpleWhitelist plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        WhitelistCommand.register(event.getDispatcher(), plugin);
    }
}