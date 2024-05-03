package net.aniby.simplewhitelist.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

public class WhitelistCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> builder =
                Commands.literal("simplewhitelist")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.argument("add", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = StringArgumentType.getString(context, "add");
                                    if (name == null) {
                                        context.getSource().sendSystemMessage(Component.literal(
                                                SimpleCore.getConfiguration().getMessages().get("need_player")
                                        ));
                                        return 0;
                                    }
                                    boolean result = SimpleCore.getConfiguration().whitelistAdd(name);

                                    String message = result
                                            ? SimpleCore.getConfiguration().getCommandMessages().get("add")
                                            : SimpleCore.getConfiguration().getMessages().get("already_whitelisted");

                                    context.getSource().sendSystemMessage(Component.literal(message));

                                    return result ? 1 : 0;
                                })
                        )
                        .then(Commands.argument("remove", StringArgumentType.word())
                                .executes(context -> {
                                    final String name = StringArgumentType.getString(context, "add");
                                    if (name == null) {
                                        context.getSource().sendSystemMessage(Component.literal(
                                                SimpleCore.getConfiguration().getMessages().get("need_player")
                                        ));
                                        return 0;
                                    }
                                    SimpleCore.getConfiguration().whitelistRemove(name);

                                    context.getSource().sendSystemMessage(Component.literal(
                                            SimpleCore.getConfiguration().getCommandMessages().get("remove")
                                    ));

                                    return 1;
                                })
                        )
                        .then(Commands.literal("enable")
                                .executes(context -> {
                                    SimpleCore.getConfiguration().setEnabled(true);
                                    context.getSource().sendSystemMessage(Component.literal(
                                            SimpleCore.getConfiguration().getCommandMessages().get("enable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("disable")
                                .executes(context -> {
                                    SimpleCore.getConfiguration().setEnabled(false);
                                    context.getSource().sendSystemMessage(Component.literal(
                                            SimpleCore.getConfiguration().getCommandMessages().get("disable")
                                    ));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    try {
                                        SimpleCore.getConfiguration().load();
                                        context.getSource().sendSystemMessage(Component.literal(
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
                            context.getSource().sendSystemMessage(Component.literal(
                                    SimpleCore.getConfiguration().getMessages().get("help")
                            ));
                            return 1;
                        });
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(builder);
        List.of("swl", "simplewl").forEach(c ->
                dispatcher.register(Commands.literal(c).redirect(command))
        );
    }
}