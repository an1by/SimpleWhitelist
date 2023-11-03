package net.aniby.simplewhitelist.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Path;

public class SimpleWhitelist {
    private final ProxyServer proxy;
    private final @DataDirectory Path pluginFolder;

    @Inject
    public SimpleWhitelist(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            SimpleCore.init(pluginFolder);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("simplewhitelist")
                .aliases("simplewl", "swl")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new WhitelistCommand());
    }

    @Subscribe
    public void onLogin(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        if (!SimpleCore.getConfiguration().whitelistContains(player.getUsername()))
            player.disconnect(Component.text(
                    SimpleCore.getConfiguration().getMessages().get("kick")
            ));
    }
}
