package net.aniby.simplewhitelist.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;

public class WhitelistCommand {
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> builder =
                CommandManager.literal("simplewhitelist")
                        .requires(source -> source.hasPermissionLevel(3)).then(CommandManager.argument("add", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = StringArgumentType.getString(context, "add");
                                    if (name == null) {
                                        context.getSource().sendMessage(Text.of(
                                                SimpleCore.getConfiguration().getMessages().get("need_player")
                                        ));
                                        return 0;
                                    }
                                    boolean result = SimpleCore.getConfiguration().whitelistAdd(name);

                                    String message = result
                                            ? SimpleCore.getConfiguration().getCommandMessages().get("add")
                                            : SimpleCore.getConfiguration().getMessages().get("already_whitelisted");

                                    context.getSource().sendMessage(Text.of(message));

                                    return result ? 1 : 0;
                                })
                        )
                        .then(CommandManager.argument("remove", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = StringArgumentType.getString(context, "add");
                                    if (name == null) {
                                        context.getSource().sendMessage(Text.of(
                                                SimpleCore.getConfiguration().getMessages().get("need_player")
                                        ));
                                        return 0;
                                    }
                                    SimpleCore.getConfiguration().whitelistRemove(name);

                                    context.getSource().sendMessage(Text.of(
                                            SimpleCore.getConfiguration().getCommandMessages().get("remove")
                                    ));

                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("enable")
                                .executes(context -> {
                                    SimpleCore.getConfiguration().setEnabled(true);
                                    context.getSource().sendMessage(Text.of(
                                            SimpleCore.getConfiguration().getCommandMessages().get("enable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("disable")
                                .executes(context -> {
                                    SimpleCore.getConfiguration().setEnabled(false);
                                    context.getSource().sendMessage(Text.of(
                                            SimpleCore.getConfiguration().getCommandMessages().get("disable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("reload")
                                .executes(context -> {
                                    try {
                                        SimpleCore.getConfiguration().load();
                                        context.getSource().sendMessage(Text.of(
                                                SimpleCore.getConfiguration().getCommandMessages().get("reload")
                                        ));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            context.getSource().sendMessage(
                                    Text.of(SimpleCore.getConfiguration().getMessages().get("help"))
                            );
                            return 1;
                        });
        LiteralCommandNode<ServerCommandSource> command = dispatcher.register(builder);
        List.of("swl", "simplewl").forEach(c ->
                dispatcher.register(CommandManager.literal(c).redirect(command))
        );
    }
}
