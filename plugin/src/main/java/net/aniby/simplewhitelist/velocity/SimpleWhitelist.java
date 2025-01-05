package net.aniby.simplewhitelist.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;

public class SimpleWhitelist {
    private PluginWhitelist whitelist;
    private PluginConfiguration configuration;

    public PluginWhitelist whitelist() {
        return this.whitelist;
    }

    public PluginConfiguration configuration() {
        return this.configuration;
    }

    private final ProxyServer proxy;
    private final @DataDirectory Path pluginFolder;

    @Inject
    public SimpleWhitelist(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.whitelist = new PluginWhitelist(this.pluginFolder);
        this.configuration = new PluginConfiguration(this.pluginFolder);

        CommandManager commandManager = this.proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("simplewhitelist")
                .aliases("simplewl", "swl")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new WhitelistCommand(this));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        if (this.configuration.isEnabled() && !this.whitelist.isWhitelisted(event.getUsername())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text(
                            this.configuration.getMessage("not_in_whitelist")
                    )
            ));
        }
    }
}
