package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;

public interface WhitelistProxy {
    String MOD_VERSION = BuildConstants.MOD_VERSION;

    PluginWhitelist getWhitelist();

    PluginConfiguration getConfiguration();
}
