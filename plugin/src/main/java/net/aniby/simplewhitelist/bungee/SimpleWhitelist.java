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
        return this.whitelist;
    }

    public PluginConfiguration configuration() {
        return this.configuration;
    }

    @Override
    public void onEnable() {
        Path path = this.getDataFolder().toPath();

        this.whitelist = new PluginWhitelist(path);
        this.configuration = new PluginConfiguration(path);

        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerCommand(this, new WhitelistCommand(this));
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
