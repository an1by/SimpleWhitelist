package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class SimpleWhitelist extends JavaPlugin implements Listener {
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

        this.whitelist = new PluginWhitelist(path.resolve("whitelist.txt"));
        this.configuration = new PluginConfiguration(path.resolve("config.json"));

        this.getServer().getPluginManager().registerEvents(this, this);

        WhitelistCommand whitelistCommand = new WhitelistCommand(this);
        PluginCommand command = this.getServer().getPluginCommand("simplewhitelist");
        assert command != null;
        command.setExecutor(whitelistCommand);
        command.setTabCompleter(whitelistCommand);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (this.configuration.isEnabled() && !this.whitelist.isWhitelisted(event.getName())) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    this.configuration.getMessage("not_in_whitelist")
            );
        }
    }
}
