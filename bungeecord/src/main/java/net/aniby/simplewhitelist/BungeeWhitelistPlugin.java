package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Path;

public final class BungeeWhitelistPlugin extends Plugin implements Listener, WhitelistPlugin {
    private WhitelistConfiguration configuration;

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void onEnable() {
        Path path = this.getDataFolder().toPath();

        this.configuration = new WhitelistConfiguration(
                path.resolve("config.yml"),
                path.resolve("whitelist.txt")
        );


        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerCommand(this,
                new BungeeWhitelistCommand(this)
        );
    }

    @EventHandler(priority = 5)
    public void onLogin(PreLoginEvent event) {
        if (this.configuration.getSettings().isEnabled()
                && !this.configuration.getWhitelist().contains(event.getConnection().getName())) {
            event.setReason(new ComponentBuilder().append(BungeeComponentSerializer.get().serialize(
                    this.configuration.getMessage("notInWhitelist")
            )).build());
            event.setCancelled(true);
        }
    }
}
