package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Path;

public final class BungeeWhitelistPlugin extends Plugin implements Listener, WhitelistProxy {
    private PluginWhitelist whitelist;
    private PluginConfiguration configuration;

    @Override
    public PluginWhitelist getWhitelist() {
        return this.whitelist;
    }

    @Override
    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void onEnable() {
        Path path = this.getDataFolder().toPath();

        this.whitelist = new PluginWhitelist(path.resolve("whitelist.txt"));
        this.configuration = new PluginConfiguration(path.resolve("config.json"));

        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerCommand(this, new BungeeWhitelistCommand(this));
    }

    @EventHandler(priority = 5)
    public void onLogin(PreLoginEvent event) {
        if (this.configuration.isEnabled() && !this.whitelist.isWhitelisted(event.getConnection().getName())) {
            event.setReason(new ComponentBuilder(
                    this.configuration.getMessage("not_in_whitelist")
            ).build());
            event.setCancelled(true);
        }
    }
}
