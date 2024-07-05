package net.aniby.simplewhitelist.bungee;

import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Path;

public final class SimpleWhitelist extends Plugin implements Listener {
    private PluginWhitelist whitelist;
    private PluginConfiguration configuration;

    public PluginWhitelist whitelist() {
        return whitelist;
    }

    public PluginConfiguration configuration() {
        return configuration;
    }

    @Override
    public void onEnable() {
        Path path = getDataFolder().toPath();

        whitelist = new PluginWhitelist(path);
        configuration = new PluginConfiguration(path);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new WhitelistCommand(this));
    }

    @EventHandler(priority = 5)
    public void onLogin(PreLoginEvent event) {
        if (!whitelist.isWhitelisted(event.getConnection().getName())) {
            event.setCancelReason(new ComponentBuilder(
                    configuration.getMessage("not_in_whitelist")
            ).create());
            event.setCancelled(true);
        }
    }
}