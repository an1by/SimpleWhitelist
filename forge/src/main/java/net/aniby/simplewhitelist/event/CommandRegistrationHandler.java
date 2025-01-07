package net.aniby.simplewhitelist.event;

import net.aniby.simplewhitelist.WhitelistCommand;
import net.aniby.simplewhitelist.ForgeWhitelistMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandRegistrationHandler {
    private final ForgeWhitelistMod plugin;

    public CommandRegistrationHandler(ForgeWhitelistMod plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        WhitelistCommand.register(event.getDispatcher(), this.plugin);
    }
}
