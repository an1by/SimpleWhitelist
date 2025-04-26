package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.api.Whitelist;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WhitelistCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, Whitelist plugin) {
        PluginConfiguration configuration = plugin.getConfiguration();
        PluginWhitelist whitelist = plugin.getWhitelist();

        final LiteralArgumentBuilder<CommandSourceStack> builder =
                Commands.literal("simplewhitelist")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();

                                            final String name = StringArgumentType.getString(context, "name");
                                            if (name == null) {
                                                source.sendSystemMessage(Component.literal(
                                                        configuration.getMessage("need_player")
                                                ));
                                                return 0;
                                            }

                                            boolean result = whitelist.isWhitelisted(name);
                                            String message;
                                            if (result) {
                                                message = configuration.getMessage("already_whitelisted");
                                            } else {
                                                whitelist.addWhitelist(name);
                                                whitelist.save();
                                                message = configuration.getCommandMessage("add");
                                            }
                                            message = message.replace("<name>", name);

                                            source.sendSystemMessage(Component.literal(message));

                                            return result ? 1 : 0;
                                        })
                                ))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();

                                            final String name = StringArgumentType.getString(context, "name");
                                            if (name == null) {
                                                source.sendSystemMessage(Component.literal(
                                                        configuration.getMessage("need_player")
                                                ));
                                                return 0;
                                            }
                                            whitelist.removeWhitelist(name);
                                            whitelist.save();

                                            String message = configuration.getCommandMessage("remove");
                                            message = message.replace("<name>", name);

                                            source.sendSystemMessage(Component.literal(message));

                                            return 1;
                                        })
                                ))
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    String list = whitelist.getWhitelistedAsString();
                                    if (list.isEmpty()) {
                                        list = configuration.getMessage("empty");
                                    }
                                    context.getSource().sendSystemMessage(Component.literal(
                                            configuration.getCommandMessage("list")
                                                    .replace("<list>", list)
                                    ));
                                    return 1;
                                }))
                        .then(Commands.literal("enable")
                                .executes(context -> {
                                    configuration.setEnabled(true);
                                    context.getSource().sendSystemMessage(Component.literal(
                                            configuration.getCommandMessage("enable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("disable")
                                .executes(context -> {
                                    configuration.setEnabled(false);
                                    context.getSource().sendSystemMessage(Component.literal(
                                            configuration.getCommandMessage("disable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    whitelist.reload();
                                    context.getSource().sendSystemMessage(Component.literal(
                                            configuration.getCommandMessage("reload")
                                    ));
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            context.getSource().sendSystemMessage(Component.literal(
                                    configuration.getMessage("help")
                            ));
                            return 1;
                        });
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(builder);
        List.of("swl", "simplewl").forEach(c ->
                dispatcher.register(
                        Commands.literal(c)
                                .requires(source -> source.hasPermission(3))
                                .redirect(command))
        );
    }
}
