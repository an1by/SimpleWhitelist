package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public final class BungeeWhitelistPlugin extends Plugin implements Listener, WhitelistPlugin {
    private volatile WhitelistConfiguration configuration;

    private WhitelistConfiguration ensureConfiguration() {
        WhitelistConfiguration cfg = this.configuration;
        if (cfg == null) {
            synchronized (this) {
                cfg = this.configuration;
                if (cfg == null) {
                    Path path = this.getDataFolder().toPath();
                    cfg = new WhitelistConfiguration(
                            path.resolve("config.yml"),
                            path.resolve("whitelist.txt")
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
        return this.getProxy().getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        for (ProxiedPlayer player : this.getProxy().getPlayers()) {
            String name = player.getName();
            if (!cfg.getWhitelist().contains(name)) {
                player.disconnect(BungeeCommandSourceExecutor.toBungee(this.getNotInWhitelistMessage(name)));
            }
        }
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message != null) {
            this.getProxy().getConsole().sendMessage(BungeeCommandSourceExecutor.toBungee(message));
        }
    }

    @Override
    public void executeConsoleCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            this.getProxy().getPluginManager().dispatchCommand(this.getProxy().getConsole(), command);
        }
    }

    @Override
    public void onEnable() {
        this.ensureConfiguration();

        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerCommand(this,
                new BungeeWhitelistCommand(this)
        );
    }

    @EventHandler(priority = 5)
    public void onLogin(PreLoginEvent event) {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        String username = event.getConnection().getName();
        if (cfg.getSettings().isEnabled() && !cfg.getWhitelist().contains(username)) {
            this.handleBlockedJoin(username);
            event.setCancelReason(BungeeCommandSourceExecutor.toBungee(
                    this.getNotInWhitelistMessage(username)
            ));
            event.setCancelled(true);
        }
    }
}
