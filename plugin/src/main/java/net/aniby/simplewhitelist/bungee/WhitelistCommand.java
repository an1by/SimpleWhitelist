package net.aniby.simplewhitelist.bungee;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand extends Command implements TabExecutor {
    private final SimpleWhitelist plugin;

    public WhitelistCommand(SimpleWhitelist plugin) {
        super("simplewhitelist", null, "simplewl", "swl");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
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
                                    sender.sendMessage(new ComponentBuilder(
                                            config.getMessage("already_whitelisted")
                                    ).create());
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
                    case "list" ->
                            subMessage = subMessage.replace("<list>", whitelist.getWhitelistedAsString());
                }
                sender.sendMessage(new ComponentBuilder(subMessage).create());
                return;
            }
            sender.sendMessage(new ComponentBuilder(result).create());
            return;
        }
        sender.sendMessage(new ComponentBuilder(
                config.getMessage("invalid_arguments")
        ).create());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.PERMISSION))
            list = plugin.configuration().getCompleter(plugin.whitelist(), args);
        return list;
    }
}
