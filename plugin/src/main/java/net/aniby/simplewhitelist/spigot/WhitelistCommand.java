package net.aniby.simplewhitelist.spigot;

import net.aniby.simplewhitelist.common.SimpleCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                                    sender.sendMessage(
                                            SimpleCore.getConfiguration().getMessages().get("already_whitelisted")
                                    );
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
                sender.sendMessage(
                        subMessage
                );
                return true;
            }
            sender.sendMessage(result);
            return true;
        }
        sender.sendMessage(
                SimpleCore.getConfiguration().getMessages().get("invalid_arguments")
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.permission))
            list = SimpleCore.getCompleter(args);
        return list;
    }
}
