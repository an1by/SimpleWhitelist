package net.aniby.simplewhitelist;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;

import java.nio.file.Path;

@Plugin(
        id = VelocityWhitelistPlugin.MOD_ID,
        name = "SimpleWhitelist",
        version = BuildConstants.MOD_VERSION,
        authors = {"An1by"},
        url = "https://github.com/an1by/SimpleWhitelist",
        description = "Simple whitelist system for your server"
)
public class VelocityWhitelistPlugin implements WhitelistPlugin {
    public static final String MOD_ID = "simplewhitelist";

    private WhitelistConfiguration configuration;

    @Override
    public WhitelistConfiguration getConfiguration() {
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
        this.configuration = new WhitelistConfiguration(
                this.pluginFolder.resolve("config.yml"),
                this.pluginFolder.resolve("whitelist.txt")
        );

        CommandManager commandManager = this.proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("simplewhitelist")
                .aliases("simplewl", "swl")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new BrigadierCommand(
                new WhitelistCommand<>(
                        new VelocityCommandSourceExecutor(), this
                ).builder()
        ));
    }

    @Subscribe(priority = 1)
    public void onPreLogin(PreLoginEvent event) {
        if (this.configuration.getSettings().isEnabled()
                && !this.configuration.getWhitelist().contains(event.getUsername())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    this.configuration.getMessage("notInWhitelist")
            ));
        }
    }
}
