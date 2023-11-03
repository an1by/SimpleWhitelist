package net.aniby.simplewhitelist.bungee;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;

public final class SimpleWhitelist extends Plugin implements Listener {
    @Override
    public void onEnable() {
        try {
            SimpleCore.init(getDataFolder().toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new WhitelistCommand());
    }

    @EventHandler(priority = 5)
    public void onLogin(PreLoginEvent event) {
        if (!SimpleCore.getConfiguration().whitelistContains(event.getConnection().getName())) {
            event.setCancelReason(new ComponentBuilder(
                    SimpleCore.getConfiguration().getMessages().get("kick")
            ).create());
            event.setCancelled(true);
        }
    }
}