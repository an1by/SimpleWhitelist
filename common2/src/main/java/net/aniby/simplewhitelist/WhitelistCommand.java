package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class WhitelistCommand {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Predicate<CommandSourceStack> hasPermission;
    private final WhitelistPlugin plugin;

    public WhitelistCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            Predicate<CommandSourceStack> hasPermission,
            WhitelistPlugin plugin
    ) {

        this.dispatcher = dispatcher;
        this.hasPermission = hasPermission;
        this.plugin = plugin;
    }

    public void register() {
        final LiteralArgumentBuilder<CommandSourceStack> builder =
                Commands.literal("simplewhitelist")
                        .requires(this.hasPermission)
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(this::add)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(this::remove)))
                        .then(Commands.literal("list").executes(this::list))
                        .then(Commands.literal("enable").executes(this::enable))
                        .then(Commands.literal("disable").executes(this::disable))
                        .then(Commands.literal("reload").executes(this::reload))
                        .then(Commands.literal("help").executes(this::help))
                        .executes(this::help);
        LiteralCommandNode<CommandSourceStack> command = this.dispatcher.register(builder);
        List.of("swl", "simplewl").forEach(c ->
                this.dispatcher.register(
                        Commands.literal(c)
                                .requires(source -> source.hasPermission(3))
                                .redirect(command))
        );
    }

    private int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        PluginWhitelist whitelist = configuration.getWhitelist();
        CommandSourceStack source = context.getSource();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) source;

        final String name = StringArgumentType.getString(context, "name");
        if (name == null) {
            adventureSource.sendFailure(
                    configuration.getMessage("need_player")
            );
            return 0;
        }

        if (whitelist.contains(name)) {
            adventureSource.sendFailure(
                    configuration.getMessage(
                            "already_whitelisted",
                            Placeholder.unparsed("name", name)
                    )
            );
            return 0;
        }

        whitelist.add(name);
        configuration.saveWhitelist();
        adventureSource.sendSuccess(
                configuration.getCommandMessage(
                        "add",
                        Placeholder.unparsed("name", name)
                ),
                true
        );
        return 1;
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        PluginWhitelist whitelist = configuration.getWhitelist();
        CommandSourceStack source = context.getSource();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) source;

        final String name = StringArgumentType.getString(context, "name");
        if (name == null) {
            adventureSource.sendFailure(
                    configuration.getMessage("need_player")
            );
            return 0;
        }
        whitelist.remove(name);
        configuration.saveWhitelist();

        Component message = configuration.getCommandMessage(
                "remove", Placeholder.unparsed("name", name)
        );

        adventureSource.sendSuccess(message, true);

        return 1;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) context.getSource();

        String list = configuration.getWhitelist().getAsString();
        TagResolver.Single placeholder = list.isEmpty()
                ? Placeholder.component("list", configuration.getMessage("empty"))
                : Placeholder.unparsed("list", list);
        adventureSource.sendSuccess(
                configuration.getCommandMessage("list", placeholder),
                false
        );
        return 1;
    }

    private int enable(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) context.getSource();

        configuration.getConfigurationFile().setEnabled(true);
        configuration.saveConfigurationFile();
        adventureSource.sendSuccess(
                configuration.getCommandMessage("enable"),
                true
        );
        return 1;
    }

    private int disable(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) context.getSource();

        configuration.getConfigurationFile().setEnabled(false);
        configuration.saveConfigurationFile();
        adventureSource.sendSuccess(
                configuration.getCommandMessage("disable"),
                true
        );
        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) context.getSource();

        configuration.reload();
        adventureSource.sendSuccess(
                configuration.getCommandMessage("reload"),
                true
        );
        return 1;
    }

    private int help(CommandContext<CommandSourceStack> context) {
        PluginConfiguration configuration = this.plugin.getConfiguration();
        AdventureCommandSourceStack adventureSource = (AdventureCommandSourceStack) context.getSource();

        adventureSource.sendSuccess(
                configuration.getCommandMessage("help"),
                false
        );
        return 1;
    }
}
