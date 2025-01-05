package net.aniby.simplewhitelist.spigot;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.aniby.simplewhitelist.common.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.common.plugin.PluginWhitelist;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand implements CommandExecutor, TabCompleter {
    private final SimpleWhitelist plugin;

    public WhitelistCommand(SimpleWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        PluginWhitelist whitelist = this.plugin.whitelist();
        PluginConfiguration config = this.plugin.configuration();
        if (args.length >= 1) {
            String result = config.checkSubcommand(args[0], sender.hasPermission(SimpleCore.PERMISSION));
            if (result == null) {
                String subMessage = config.getCommandMessage(args[0]);
                switch (args[0]) {
                    case "reload" -> config.reload();
                    case "add", "remove" -> {
                        if (args.length < 2) {
                            subMessage = config.getMessage("need_player");
                        } else {
                            String name = args[1];
                            if (args[0].equals("add")) {
                                if (whitelist.isWhitelisted(name)) {
                                    sender.sendMessage(
                                            config.getMessage("already_whitelisted")
                                    );
                                    return true;
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
                sender.sendMessage(subMessage);
                return true;
            }
            sender.sendMessage(result);
            return true;
        }
        sender.sendMessage(
                config.getMessage("invalid_arguments")
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.PERMISSION)) {
            list = this.plugin.configuration().getCompleter(this.plugin.whitelist(), args);
        }
        return list;
    }
}
