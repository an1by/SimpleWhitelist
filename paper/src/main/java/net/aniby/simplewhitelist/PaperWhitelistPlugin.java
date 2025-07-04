package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class PaperWhitelistPlugin extends JavaPlugin implements Listener, WhitelistPlugin {
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

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (this.configuration.getSettings().isEnabled()
                && !this.configuration.getWhitelist().contains(event.getName())) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    this.configuration.getMessage("notInWhitelist")
            );
        }
    }
}
