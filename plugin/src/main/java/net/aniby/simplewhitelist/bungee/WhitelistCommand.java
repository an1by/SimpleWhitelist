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
        PluginWhitelist whitelist = this.plugin.whitelist();
        PluginConfiguration config = this.plugin.configuration();
        if (1 <= args.length) {
            String result = config.checkSubcommand(args[0], sender.hasPermission(SimpleCore.PERMISSION));
            if (null == result) {
                String subMessage = config.getCommandMessage(args[0]);
                switch (args[0]) {
                    case "reload" -> config.reload();
                    case "add", "remove" -> {
                        if (2 > args.length) {
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
                    case "list" -> {
                        String list = whitelist.getWhitelistedAsString();
                        if (list.isEmpty())
                            list = config.getMessage("empty");
                        subMessage = subMessage.replace("<list>", list);
                    }
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
            list = this.plugin.configuration().getCompleter(this.plugin.whitelist(), args);
        return list;
    }
}
