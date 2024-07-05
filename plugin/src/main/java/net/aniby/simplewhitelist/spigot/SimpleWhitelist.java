package net.aniby.simplewhitelist.spigot;

import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
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

        getServer().getPluginManager().registerEvents(this, this);

        WhitelistCommand whitelistCommand = new WhitelistCommand(this);
        PluginCommand command = getServer().getPluginCommand("simplewhitelist");
        assert command != null;
        command.setExecutor(whitelistCommand);
        command.setTabCompleter(whitelistCommand);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (!whitelist.isWhitelisted(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, configuration.getMessage("kick"));
        }
    }
}