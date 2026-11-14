package net.aniby.simplewhitelist.sponge;

import com.google.inject.Inject;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin("simplewhitelist")
public class SpongeWhitelistPlugin implements WhitelistPlugin {
    private final PluginContainer pluginContainer;
    private final Path configDirectory;
    private volatile WhitelistConfiguration configuration;

    @Inject
    public SpongeWhitelistPlugin(
            PluginContainer pluginContainer,
            @ConfigDir(sharedRoot = false) Path configDirectory
    ) {
        this.pluginContainer = pluginContainer;
        this.configDirectory = configDirectory;
    }

    private WhitelistConfiguration ensureConfiguration() {
        WhitelistConfiguration cfg = this.configuration;
        if (cfg == null) {
            synchronized (this) {
                cfg = this.configuration;
                if (cfg == null) {
                    cfg = new WhitelistConfiguration(
                            this.configDirectory.resolve("config.yml"),
                            this.configDirectory.resolve("whitelist.txt")
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

    @Listener
    public void onServerStarting(final StartingEngineEvent<Server> event) {
        this.ensureConfiguration();
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Raw> event) {
        this.ensureConfiguration();
        final RawCommandRunner<CommandCause> runner = new RawCommandRunner<>(
                new WhitelistCommandSourceExecutor<CommandCause>() {
                    @Override
                    public boolean hasPermission(CommandCause source, String permission) {
                        return source.hasPermission(permission);
                    }

                    @Override
                    public void sendMessage(CommandCause source, Component message) {
                        source.audience().sendMessage(message);
                    }
                },
                this
        );

        Command.Raw rawCommand = new Command.Raw() {
            @Override
            public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
                return runner.execute(cause, arguments.remaining())
                        ? CommandResult.success()
                        : CommandResult.builder().result(0).build();
            }

            @Override
            public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
                return runner.suggest(cause, arguments.remaining())
                        .stream()
                        .map(CommandCompletion::of)
                        .collect(Collectors.toList());
            }

            @Override
            public boolean canExecute(CommandCause cause) {
                return runner.hasPermission(cause);
            }

            @Override
            public Optional<Component> shortDescription(CommandCause cause) {
                return Optional.of(Component.text("Manage SimpleWhitelist"));
            }

            @Override
            public Optional<Component> extendedDescription(CommandCause cause) {
                return Optional.of(ensureConfiguration().getCommandMessage("help"));
            }

            @Override
            public Component usage(CommandCause cause) {
                return Component.text("/simplewhitelist <add|remove|list|enable|disable|reload|help>");
            }
        };

        event.register(this.pluginContainer, rawCommand, "simplewhitelist", "swl", "simplewl");
    }

    @Listener
    public void onAuth(final ServerSideConnectionEvent.Auth event) {
        WhitelistConfiguration cfg = this.ensureConfiguration();
        if (!cfg.getSettings().isEnabled()) {
            return;
        }
        String name = extractAuthProfileName(event);
        if (name == null || !cfg.getWhitelist().contains(name)) {
            this.handleBlockedJoin(name);
            event.setMessage(this.getNotInWhitelistMessage(name));
            event.setCancelled(true);
        }
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message != null) {
            try {
                Sponge.systemSubject().sendMessage(message);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void executeConsoleCommand(final String command) {
        if (command == null || command.trim().isEmpty() || !Sponge.isServerAvailable()) {
            return;
        }
        Runnable task = () -> {
            try {
                Sponge.server().commandManager().process(command);
            } catch (Throwable ignored) {
            }
        };
        if (Sponge.server().onMainThread()) {
            task.run();
        } else {
            Sponge.server().scheduler().submit(
                    Task.builder().plugin(this.pluginContainer).execute(task).build()
            );
        }
    }

    private static String extractAuthProfileName(ServerSideConnectionEvent.Auth event) {
        try {
            Method profileMethod = event.getClass().getMethod("profile");
            Object profileObj = profileMethod.invoke(event);
            if (profileObj instanceof GameProfile) {
                return ((GameProfile) profileObj).name().orElse(null);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        if (!Sponge.isServerAvailable()) {
            return Collections.emptyList();
        }
        return Sponge.server().onlinePlayers().stream()
                .map(ServerPlayer::name)
                .collect(Collectors.toList());
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        if (!Sponge.isServerAvailable()) {
            return;
        }
        final WhitelistConfiguration cfg = this.ensureConfiguration();
        Runnable task = () -> {
            for (ServerPlayer player : Sponge.server().onlinePlayers()) {
                String name = player.name();
                if (!cfg.getWhitelist().contains(name)) {
                    player.kick(this.getNotInWhitelistMessage(name));
                }
            }
        };
        if (Sponge.server().onMainThread()) {
            task.run();
        } else {
            Sponge.server().scheduler().submit(
                    Task.builder().plugin(this.pluginContainer).execute(task).build()
            );
        }
    }
}
