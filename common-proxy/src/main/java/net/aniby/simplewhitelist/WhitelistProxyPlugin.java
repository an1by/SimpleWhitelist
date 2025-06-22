package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;

public interface WhitelistProxyPlugin {
    String MOD_VERSION = BuildConstants.MOD_VERSION;

    PluginConfiguration getConfiguration();
}
