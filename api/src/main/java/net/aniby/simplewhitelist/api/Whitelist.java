package net.aniby.simplewhitelist.api;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;

public interface Whitelist {
    PluginConfiguration getConfiguration();

    PluginWhitelist getWhitelist();
}
