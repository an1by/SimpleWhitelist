package net.aniby.simplewhitelist.bungee;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand extends Command implements TabExecutor {
    public WhitelistCommand() {
        super("simplewhitelist", null, "simplewl", "swl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length >= 1) {
            String result = SimpleCore.checkSubcommand(args[0], sender.hasPermission(SimpleCore.permission));
            if (result == null) {
                String subMessage = SimpleCore.getConfiguration().getCommandMessages().get(args[0]);
                switch (args[0]) {
                    case "reload" -> {
                        try {
                            SimpleCore.getConfiguration().load();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    case "add", "remove" -> {
                        if (args.length < 2) {
                            subMessage = SimpleCore.getConfiguration().getMessages().get("need_player");
                        } else {
                            String name = args[1];
                            if (args[0].equals("add")) {
                                if (!SimpleCore.getConfiguration().whitelistAdd(name)) {
                                    sender.sendMessage(new ComponentBuilder(
                                            SimpleCore.getConfiguration().getMessages().get("already_whitelisted")
                                    ).create());
                                }
                            }
                            else SimpleCore.getConfiguration().whitelistRemove(name);
                            subMessage = subMessage.replace("<name>", name);
                        }
                    }
                    case "enable" -> SimpleCore.getConfiguration().setEnabled(true);
                    case "disable" -> SimpleCore.getConfiguration().setEnabled(false);
                    case "list" -> subMessage = subMessage.replace("<list>", SimpleCore.getConfiguration().getWhitelistedString());
                }
                sender.sendMessage(new ComponentBuilder(subMessage).create());
                return;
            }
            sender.sendMessage(new ComponentBuilder(result).create());
            return;
        }
        sender.sendMessage(new ComponentBuilder(
                SimpleCore.getConfiguration().getMessages().get("invalid_arguments")
        ).create());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.permission))
            list = SimpleCore.getCompleter(args);
        return list;
    }
}
