package net.aniby.simplewhitelist.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.Whitelist;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class WhitelistCommand<S> {
    public static final String PERMISSION_BASE = WhitelistPermission.BASE.getPermission();
    private final Commands<S> commands = new Commands<>();

    private final WhitelistCommandSourceExecutor<S> executor;
    private WhitelistPlugin plugin;

    public WhitelistCommand(WhitelistCommandSourceExecutor<S> executor,
                            WhitelistPlugin plugin) {
        this.executor = executor;
        this.plugin = plugin;
    }

    public void plugin(WhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    private WhitelistPlugin plugin() {
        return this.plugin;
    }

    public LiteralArgumentBuilder<S> builder() {
        return this.commands.literal("simplewhitelist")
                .requires(source -> WhitelistPermission.hasAnyPermission(source, this.executor))
                .then(this.commands.literal("add")
                        .requires(source -> WhitelistPermission.ADD.hasPermission(source, this.executor))
                        .then(this.commands.argument("name", StringArgumentType.greedyString())
                                .suggests(this::suggestAdd)
                                .executes(this::add)))
                .then(this.commands.literal("remove")
                        .requires(source -> WhitelistPermission.REMOVE.hasPermission(source, this.executor))
                        .then(this.commands.argument("name", StringArgumentType.greedyString())
                                .suggests(this::suggestRemove)
                                .executes(this::remove)))
                .then(this.commands.literal("list")
                        .requires(source -> WhitelistPermission.LIST.hasPermission(source, this.executor))
                        .executes(this::list))
                .then(this.commands.literal("enable")
                        .requires(source -> WhitelistPermission.ENABLE.hasPermission(source, this.executor))
                        .executes(this::enable))
                .then(this.commands.literal("disable")
                        .requires(source -> WhitelistPermission.DISABLE.hasPermission(source, this.executor))
                        .executes(this::disable))
                .then(this.commands.literal("reload")
                        .requires(source -> WhitelistPermission.RELOAD.hasPermission(source, this.executor))
                        .executes(this::reload))
                .then(this.commands.literal("help")
                        .executes(this::help))
                .executes(this::help);
    }

    public void register(CommandDispatcher<S> dispatcher) {
        LiteralCommandNode<S> command = dispatcher.register(this.builder());

        for (String alias : Arrays.asList("swl", "simplewl")) {
            dispatcher.register(
                    this.commands.literal(alias)
                            .requires(source -> WhitelistPermission.hasAnyPermission(source, this.executor))
                            .executes(this::help)
                            .redirect(command)
            );
        }
    }

    private CompletableFuture<Suggestions> suggestAdd(
            CommandContext<S> context, SuggestionsBuilder builder) {
        WhitelistPlugin currentPlugin = this.plugin();
        if (currentPlugin != null && currentPlugin.getConfiguration() != null) {
            Whitelist whitelist = currentPlugin.getConfiguration().getWhitelist();
            String remaining = builder.getRemainingLowerCase();
            for (String online : currentPlugin.getOnlinePlayerNames()) {
                if (online != null && !whitelist.contains(online)
                        && online.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(online);
                }
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestRemove(
            CommandContext<S> context, SuggestionsBuilder builder) {
        WhitelistPlugin currentPlugin = this.plugin();
        if (currentPlugin != null && currentPlugin.getConfiguration() != null) {
            String remaining = builder.getRemainingLowerCase();
            List<String> sorted = new ArrayList<>(currentPlugin.getConfiguration().getWhitelist().getSet());
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            for (String entry : sorted) {
                if (entry.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(entry);
                }
            }
        }
        return builder.buildFuture();
    }

    private static String normalizePlayerArgument(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private int add(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        Whitelist whitelist = configuration.getWhitelist();
        S source = context.getSource();

        final String name = normalizePlayerArgument(StringArgumentType.getString(context, "name"));
        if (name.isEmpty()) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage("needPlayer")
            );
            return 0;
        }

        if (!whitelist.add(name)) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage(
                            "alreadyWhitelisted",
                            Placeholder.unparsed("name", name)
                    )
            );
            return 0;
        }

        configuration.saveWhitelist();
        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage(
                        "add",
                        Placeholder.unparsed("name", name)
                ),
                true
        );
        return 1;
    }

    private int remove(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        Whitelist whitelist = configuration.getWhitelist();
        S source = context.getSource();

        final String name = normalizePlayerArgument(StringArgumentType.getString(context, "name"));
        if (name.isEmpty()) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage("needPlayer")
            );
            return 0;
        }

        if (!whitelist.remove(name)) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage(
                            "notWhitelisted",
                            Placeholder.unparsed("name", name)
                    )
            );
            return 0;
        }

        configuration.saveWhitelist();

        Component message = configuration.getCommandMessage(
                "remove", Placeholder.unparsed("name", name)
        );
        this.executor.sendSuccess(source, message, true);

        if (configuration.getSettings().isEnabled() && configuration.getSettings().isEnforceWhitelist()) {
            this.plugin().kickUnwhitelistedPlayers();
        }
        return 1;
    }

    private int list(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        List<String> sorted = new ArrayList<>(configuration.getWhitelist().getSet());
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        String list = String.join(", ", sorted);
        TagResolver.Single placeholder = list.isEmpty()
                ? Placeholder.component("list", configuration.getMessage("empty"))
                : Placeholder.unparsed("list", list);
        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage("list", placeholder),
                false
        );
        return 1;
    }

    private int enable(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        configuration.getSettings().setEnabled(true);
        configuration.saveSettings();
        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage("enable"),
                true
        );
        if (configuration.getSettings().isEnforceWhitelist()) {
            this.plugin().kickUnwhitelistedPlayers();
        }
        return 1;
    }

    private int disable(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        configuration.getSettings().setEnabled(false);
        configuration.saveSettings();
        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage("disable"),
                true
        );
        return 1;
    }

    private int reload(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        configuration.load();
        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage("reload"),
                true
        );
        if (configuration.getSettings().isEnabled() && configuration.getSettings().isEnforceWhitelist()) {
            this.plugin().kickUnwhitelistedPlayers();
        }
        return 1;
    }

    private int help(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        this.executor.sendSuccess(
                source,
                configuration.getCommandMessage("help"),
                false
        );
        return 1;
    }
}
