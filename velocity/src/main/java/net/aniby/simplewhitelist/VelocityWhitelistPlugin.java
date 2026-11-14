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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

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

    private volatile WhitelistConfiguration configuration;
    private final ProxyServer proxy;
    private final @DataDirectory Path pluginFolder;

    @Inject
    public VelocityWhitelistPlugin(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    private WhitelistConfiguration ensureConfiguration() {
        WhitelistConfiguration cfg = this.configuration;
        if (cfg == null) {
            synchronized (this) {
                cfg = this.configuration;
                if (cfg == null) {
                    cfg = new WhitelistConfiguration(
                            this.pluginFolder.resolve("config.yml"),
                            this.pluginFolder.resolve("whitelist.txt")
                    );
                    this.configuration = cfg;
                }
            }
        }
        return cfg;
    }

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.ensureConfiguration();
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return this.proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.toList());
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        for (Player player : this.proxy.getAllPlayers()) {
            String username = player.getUsername();
            if (!cfg.getWhitelist().contains(username)) {
                player.disconnect(this.getNotInWhitelistMessage(username));
            }
        }
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message != null) {
            this.proxy.getConsoleCommandSource().sendMessage(message);
        }
    }

    @Override
    public void executeConsoleCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            this.proxy.getCommandManager().executeAsync(this.proxy.getConsoleCommandSource(), command);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.ensureConfiguration();

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
        WhitelistConfiguration cfg = this.ensureConfiguration();
        String username = event.getUsername();
        if (cfg.getSettings().isEnabled() && !cfg.getWhitelist().contains(username)) {
            this.handleBlockedJoin(username);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    this.getNotInWhitelistMessage(username)
            ));
        }
    }
}
