package net.aniby.simplewhitelist.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhitelistCommand implements SimpleCommand {
    private final SimpleWhitelist plugin;

    public WhitelistCommand(SimpleWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        PluginWhitelist whitelist = plugin.whitelist();
        PluginConfiguration config = plugin.configuration();
        if (args.length >= 1) {
            String result = config.checkSubcommand(args[0], sender.hasPermission(SimpleCore.PERMISSION));
            if (result == null) {
                String subMessage = config.getCommandMessage(args[0]);
                switch (args[0]) {
                    case "reload" -> {
                        config.reload();
                    }
                    case "add", "remove" -> {
                        if (args.length < 2) {
                            subMessage = config.getMessage("need_player");
                        } else {
                            String name = args[1];
                            if (args[0].equals("add")) {
                                if (whitelist.isWhitelisted(name)) {
                                    sender.sendMessage(Component.text(
                                            config.getMessage("already_whitelisted")
                                    ));
                                    return;
                                }
                                whitelist.addWhitelist(name);
                            } else {
                                whitelist.removeWhitelist(name);
                            }
                            whitelist.save();
                            subMessage = subMessage.replace("<name>", name);
                        }
                    }
                    case "enable" -> {
                        config.setEnabled(true);
                        config.save();
                    }
                    case "disable" -> {
                        config.setEnabled(false);
                        config.save();
                    }
                    case "list" -> {
                        String list = whitelist.getWhitelistedAsString();
                        if (list.isEmpty())
                            list = config.getMessage("empty");
                        subMessage = subMessage.replace("<list>", list);
                    }
                }
                sender.sendMessage(Component.text(subMessage));
                return;
            }
            sender.sendMessage(Component.text(result));
            return;
        }
        sender.sendMessage(Component.text(
                config.getMessage("invalid_arguments")
        ));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        CommandSource sender = invocation.source();
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.PERMISSION)) {
            String[] args = invocation.arguments();
            list = plugin.configuration().getCompleter(plugin.whitelist(), args);
        }
        return CompletableFuture.completedFuture(list);
    }
}
