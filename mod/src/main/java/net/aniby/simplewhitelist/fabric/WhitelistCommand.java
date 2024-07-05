package net.aniby.simplewhitelist.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public class WhitelistCommand {
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher, SimpleWhitelist plugin) {
        PluginConfiguration configuration = plugin.configuration();
        PluginWhitelist whitelist = plugin.whitelist();

        final LiteralArgumentBuilder<ServerCommandSource> builder =
                CommandManager.literal("simplewhitelist")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            final String name = StringArgumentType.getString(context, "name");
                                            if (name == null) {
                                                context.getSource().sendMessage(Text.of(
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

                                            context.getSource().sendMessage(Text.of(message));

                                            return result ? 1 : 0;
                                        })
                                ))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            final String name = StringArgumentType.getString(context, "name");
                                            if (name == null) {
                                                context.getSource().sendMessage(Text.of(
                                                        configuration.getMessage("need_player")
                                                ));
                                                return 0;
                                            }
                                            whitelist.removeWhitelist(name);
                                            whitelist.save();

                                            context.getSource().sendMessage(Text.of(
                                                    configuration.getCommandMessage("remove")
                                            ));

                                            return 1;
                                        })
                                ))
                        .then(CommandManager.literal("list")
                                .executes(context -> {
                                    String list = whitelist.getWhitelistedAsString();
                                    if (list.isEmpty())
                                        list = configuration.getMessage("empty");
                                    context.getSource().sendMessage(Text.of(
                                            configuration.getCommandMessage("list")
                                                    .replace("<list>", list)
                                    ));
                                    return 1;
                                }))
                        .then(CommandManager.literal("enable")
                                .executes(context -> {
                                    configuration.getConfiguration().setEnabled(true);
                                    configuration.save();
                                    context.getSource().sendMessage(Text.of(
                                            configuration.getCommandMessage("enable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("disable")
                                .executes(context -> {
                                    configuration.getConfiguration().setEnabled(false);
                                    configuration.save();
                                    context.getSource().sendMessage(Text.of(
                                            configuration.getCommandMessage("disable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("reload")
                                .executes(context -> {
                                    configuration.reload();
                                    context.getSource().sendMessage(Text.of(
                                            configuration.getCommandMessage("reload")
                                    ));
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            context.getSource().sendMessage(
                                    Text.of(configuration.getMessage("help"))
                            );
                            return 1;
                        });
        LiteralCommandNode<ServerCommandSource> command = dispatcher.register(builder);
        List.of("swl", "simplewl").forEach(c ->
                dispatcher.register(CommandManager.literal(c).redirect(command))
        );
    }
}
