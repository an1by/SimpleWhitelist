package net.aniby.simplewhitelist;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
        id = VelocityWhitelistPlugin.MOD_ID,
        name = "SimpleWhitelist",
        version = WhitelistProxy.MOD_VERSION,
        authors = {"An1by"},
        url = "https://github.com/an1by/SimpleWhitelist",
        description = "Simple whitelist system for your server"
)
public class VelocityWhitelistPlugin implements WhitelistProxy {
    public static final String MOD_ID = "simplewhitelist";

    @Inject public Logger logger;

    private PluginWhitelist whitelist;
    private PluginConfiguration configuration;

    @Override
    public PluginWhitelist getWhitelist() {
        return this.whitelist;
    }

    @Override
    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }

    private final ProxyServer proxy;
    private final @DataDirectory Path pluginFolder;

    @Inject
    public VelocityWhitelistPlugin(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.whitelist = new PluginWhitelist(this.pluginFolder.resolve("whitelist.txt"));
        this.configuration = new PluginConfiguration(this.pluginFolder.resolve("config.json"));

        CommandManager commandManager = this.proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("simplewhitelist")
                .aliases("simplewl", "swl")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new VelocityWhitelistCommand(this));
    }

    @Subscribe(priority = 1)
    public void onPreLogin(PreLoginEvent event) {
        if (this.configuration.isEnabled() && !this.whitelist.isWhitelisted(event.getUsername())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    LegacyComponentSerializer.legacySection().deserialize(
                            this.configuration.getMessage("not_in_whitelist")
                    )
            ));
        }
    }
}
