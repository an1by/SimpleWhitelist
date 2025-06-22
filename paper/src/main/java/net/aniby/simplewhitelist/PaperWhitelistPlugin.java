package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class PaperWhitelistPlugin extends JavaPlugin implements Listener, WhitelistPlugin {
    private PluginConfiguration configuration;

    @Override
    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void onEnable() {
        Path path = this.getDataFolder().toPath();
        this.configuration = new PluginConfiguration(
                path.resolve("config.json"),
                path.resolve("whitelist.txt")
        );

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (this.configuration.getConfigurationFile().isEnabled()
                && !this.configuration.getWhitelist().contains(event.getName())) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    this.configuration.getMessage("not_in_whitelist")
            );
        }
    }
}
