package net.aniby.simplewhitelist.plugin;

import net.aniby.simplewhitelist.api.plugin.ConfigurationObject;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;

public class ForgeConfigurationObject extends ConfigurationObject {
    public ForgeConfigurationObject(boolean enabled, Map<String, String> messages, Map<String, String> commandMessages) {
        super(enabled, messages, commandMessages);
    }

    @Override
    public boolean isEnabled() {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().isUsingWhitelist();
    }

    @Override
    public void setEnabled(boolean enabled) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().setUsingWhiteList(enabled);
    }
}
