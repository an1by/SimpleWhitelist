package net.aniby.simplewhitelist.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.Whitelist;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;
import java.util.function.Supplier;

public class WhitelistCommand<S> {
    public static final String PERMISSION = "simplewhitelist.command";
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
                .requires(source -> this.executor.hasPermission(source, PERMISSION))
                .then(this.commands.literal("add")
                        .then(this.commands.argument("name", StringArgumentType.word())
                                .executes(this::add)))
                .then(this.commands.literal("remove")
                        .then(this.commands.argument("name", StringArgumentType.word())
                                .executes(this::remove)))
                .then(this.commands.literal("list").executes(this::list))
                .then(this.commands.literal("enable").executes(this::enable))
                .then(this.commands.literal("disable").executes(this::disable))
                .then(this.commands.literal("reload").executes(this::reload))
                .then(this.commands.literal("help").executes(this::help))
                .executes(this::help);
    }

    public void register(CommandDispatcher<S> dispatcher) {
        Commands<S> commands = new Commands<>();
        var builder = this.builder();
        LiteralCommandNode<S> command = dispatcher.register(builder);

        List.of("swl", "simplewl").forEach(c ->
                dispatcher.register(
                        commands.literal(c)
                                .requires(source -> this.executor.hasPermission(source, PERMISSION))
                                .redirect(command))
        );
    }

    private int add(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        Whitelist whitelist = configuration.getWhitelist();
        S source = context.getSource();

        final String name = StringArgumentType.getString(context, "name");
        if (name == null) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage("needPlayer")
            );
            return 0;
        }

        if (whitelist.contains(name)) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage(
                            "alreadyWhitelisted",
                            Placeholder.unparsed("name", name)
                    )
            );
            return 0;
        }

        whitelist.add(name);
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

        final String name = StringArgumentType.getString(context, "name");
        if (name == null) {
            this.executor.sendFailure(
                    source,
                    configuration.getMessage("needPlayer")
            );
            return 0;
        }
        whitelist.remove(name);
        configuration.saveWhitelist();

        Component message = configuration.getCommandMessage(
                "remove", Placeholder.unparsed("name", name)
        );

        this.executor.sendSuccess(source, message, true);

        return 1;
    }

    private int list(CommandContext<S> context) {
        WhitelistConfiguration configuration = this.plugin().getConfiguration();
        S source = context.getSource();

        String list = String.join(", ", configuration.getWhitelist().getSet());
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
