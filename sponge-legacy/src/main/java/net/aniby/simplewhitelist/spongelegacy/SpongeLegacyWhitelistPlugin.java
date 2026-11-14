package net.aniby.simplewhitelist.spongelegacy;

import com.google.inject.Inject;
import net.aniby.simplewhitelist.BuildConstants;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin(
        id = "simplewhitelist",
        name = "SimpleWhitelist",
        version = BuildConstants.MOD_VERSION,
        description = "Simple whitelist system for your server",
        url = "https://modrinth.com/plugin/simplewhitelist",
        authors = {"An1by"}
)
public class SpongeLegacyWhitelistPlugin implements WhitelistPlugin {
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirectory;

    private WhitelistConfiguration configuration;

    @Override
    public WhitelistConfiguration getConfiguration() {
        return this.configuration;
    }

    @Listener
    public void onServerStarting(GameStartingServerEvent event) {
        this.configuration = new WhitelistConfiguration(
                this.configDirectory.resolve("config.yml"),
                this.configDirectory.resolve("whitelist.txt")
        );

        final RawCommandRunner<CommandSource> runner = new RawCommandRunner<>(
                new WhitelistCommandSourceExecutor<CommandSource>() {
                    @Override
                    public boolean hasPermission(CommandSource source, String permission) {
                        return source.hasPermission(permission);
                    }

                    @Override
                    public void sendMessage(CommandSource source, Component message) {
                        source.sendMessage(toSpongeText(message));
                    }
                },
                this
        );

        CommandCallable callable = new CommandCallable() {
            @Override
            public CommandResult process(CommandSource source, String arguments) {
                return runner.execute(source, arguments) ? CommandResult.success() : CommandResult.empty();
            }

            @Override
            public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) {
                return runner.suggest(source, arguments);
            }

            @Override
            public boolean testPermission(CommandSource source) {
                return runner.hasPermission(source);
            }

            @Override
            public Optional<Text> getShortDescription(CommandSource source) {
                return Optional.of(Text.of("Manage SimpleWhitelist"));
            }

            @Override
            public Optional<Text> getHelp(CommandSource source) {
                return Optional.of(toSpongeText(configuration.getCommandMessage("help")));
            }

            @Override
            public Text getUsage(CommandSource source) {
                return Text.of("/simplewhitelist <add|remove|list|enable|disable|reload|help>");
            }
        };

        Sponge.getCommandManager().register(this, callable, "simplewhitelist", "swl", "simplewl");
    }

    @Listener
    public void onClientAuth(ClientConnectionEvent.Auth event) {
        if (this.configuration == null || !this.configuration.getSettings().isEnabled()) {
            return;
        }
        String name = event.getProfile().getName().orElse(null);
        if (name == null || !this.configuration.getWhitelist().contains(name)) {
            this.handleBlockedJoin(name);
            event.setMessage(toSpongeText(this.getNotInWhitelistMessage(name)));
            event.setMessageCancelled(false);
            event.setCancelled(true);
        }
    }

    @Override
    public void logConsoleMessage(Component message) {
        if (message != null && Sponge.isServerAvailable()) {
            Sponge.getServer().getConsole().sendMessage(toSpongeText(message));
        }
    }

    @Override
    public void executeConsoleCommand(final String command) {
        if (command == null || command.trim().isEmpty() || !Sponge.isServerAvailable()) {
            return;
        }
        Runnable task = () -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), command);
        if (Sponge.getServer().isMainThread()) {
            task.run();
        } else {
            Task.builder().execute(task).submit(this);
        }
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        if (!Sponge.isServerAvailable()) {
            return java.util.Collections.emptyList();
        }
        return Sponge.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void kickUnwhitelistedPlayers() {
        if (!Sponge.isServerAvailable() || this.configuration == null) {
            return;
        }
        Runnable task = () -> {
            for (Player player : Sponge.getServer().getOnlinePlayers()) {
                String name = player.getName();
                if (!this.configuration.getWhitelist().contains(name)) {
                    player.kick(toSpongeText(this.getNotInWhitelistMessage(name)));
                }
            }
        };
        if (Sponge.getServer().isMainThread()) {
            task.run();
        } else {
            Task.builder().execute(task).submit(this);
        }
    }

    public static Text toSpongeText(Component component) {
        try {
            String json = GsonComponentSerializer.gson().serialize(component);
            return TextSerializers.JSON.deserialize(json);
        } catch (Throwable ignored) {
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);
            return TextSerializers.FORMATTING_CODE.deserialize(legacy);
        }
    }
}
