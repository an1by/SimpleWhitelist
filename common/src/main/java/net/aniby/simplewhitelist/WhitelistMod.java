package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;

public abstract class WhitelistMod {
    public abstract PluginConfiguration getConfiguration();

    public abstract PluginWhitelist getWhitelist();
}
