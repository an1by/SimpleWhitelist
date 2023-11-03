package net.aniby.simplewhitelist.spigot;

import net.aniby.simplewhitelist.common.SimpleCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class SimpleWhitelist extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        try {
            SimpleCore.init(getDataFolder().toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginCommand("simplewhitelist").setExecutor(new WhitelistCommand());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (!SimpleCore.getConfiguration().whitelistContains(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, SimpleCore.getConfiguration().getMessages().get("kick"));
        }
    }
}