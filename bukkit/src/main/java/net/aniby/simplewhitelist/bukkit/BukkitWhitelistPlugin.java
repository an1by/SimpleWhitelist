package net.aniby.simplewhitelist.bukkit;

import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class BukkitWhitelistPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter, WhitelistPlugin {
    private volatile WhitelistConfiguration configuration;
    private BukkitAudiences audiences;
    private RawCommandRunner<CommandSender> commandRunner;

    private WhitelistConfiguration ensureConfiguration() {
        WhitelistConfiguration cfg = this.configuration;
        if (cfg == null) {
            synchronized (this) {
                cfg = this.configuration;
                if (cfg == null) {
                    Path dataPath = this.getDataFolder().toPath();
                    cfg = new WhitelistConfiguration(
                            dataPath.resolve("config.yml"),
                            dataPath.resolve("whitelist.txt")
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

    public BukkitAudiences getAudiences() {
        return this.audiences;
    }

    @Override
    public void onEnable() {
        this.ensureConfiguration();

        try {
            this.audiences = BukkitAudiences.create(this);
        } catch (Throwable ignored) {
            this.audiences = null;
        }

        this.commandRunner = new RawCommandRunner<>(new BukkitCommandSourceExecutor(this), this);

        PluginCommand pluginCommand = this.getCommand("simplewhitelist");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(this);
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (this.audiences != null) {
            try {
                this.audiences.close();
            } catch (Throwable ignored) {
            }
            this.audiences = null;
        }
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (!cfg.getWhitelist().contains(name)) {
                String reason = BukkitCommandSourceExecutor.toLegacyString(this.getNotInWhitelistMessage(name));
                kickPlayerSafely(player, reason);
            }
        }
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message == null) {
            return;
        }
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        BukkitAudiences aud = this.audiences;
        if (aud != null) {
            try {
                aud.sender(console).sendMessage(message);
                return;
            } catch (Throwable ignored) {
            }
        }
        console.sendMessage(BukkitCommandSourceExecutor.toLegacyString(message));
    }

    @Override
    public void executeConsoleCommand(final String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        final Runnable dispatch = () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Folia & Paper 1.20+ GlobalRegionScheduler support
        try {
            Method getGlobalScheduler = this.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Object globalScheduler = getGlobalScheduler.invoke(this.getServer());
            if (globalScheduler != null) {
                Method executeMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                executeMethod.invoke(globalScheduler, this, dispatch);
                return;
            }
        } catch (Throwable ignored) {
        }

        if (Bukkit.isPrimaryThread()) {
            dispatch.run();
        } else {
            Bukkit.getScheduler().runTask(this, dispatch);
        }
    }

    private void kickPlayerSafely(final Player player, final String reason) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(player);
            if (entityScheduler != null) {
                Method runMethod = entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                Consumer<Object> task = ignored -> player.kickPlayer(reason);
                runMethod.invoke(entityScheduler, this, task, null);
                return;
            }
        } catch (Throwable ignored) {
        }

        if (Bukkit.isPrimaryThread()) {
            player.kickPlayer(reason);
        } else {
            Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(reason));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        String username = event.getName();
        if (cfg.getSettings().isEnabled() && !cfg.getWhitelist().contains(username)) {
            this.handleBlockedJoin(username);
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    BukkitCommandSourceExecutor.toLegacyString(this.getNotInWhitelistMessage(username))
            );
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.commandRunner.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.commandRunner.suggest(sender, args);
    }
}
