package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistCore;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class WhitelistCommand {
    private final WhitelistProxy plugin;
    private final BiFunction<Object, String, Boolean> hasPermission;
    private final BiConsumer<Object, String> sendMessage;

    public WhitelistCommand(WhitelistProxy plugin, BiFunction<Object, String, Boolean> hasPermission, BiConsumer<Object, String> sendMessage) {
        this.plugin = plugin;
        this.hasPermission = hasPermission;
        this.sendMessage = sendMessage;
    }

    public void execute(Object sender, String[] args) {
        PluginWhitelist whitelist = this.plugin.getWhitelist();
        PluginConfiguration config = this.plugin.getConfiguration();
        if (args.length >= 1) {
            String result = config.checkSubcommand(args[0], this.hasPermission.apply(sender, WhitelistCore.COMMAND_PERMISSION));
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
                                    this.sendMessage.accept(sender, config.getMessage("already_whitelisted"));
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
                        if (list.isEmpty()) list = config.getMessage("empty");
                        subMessage = subMessage.replace("<list>", list);
                    }
                }
                this.sendMessage.accept(sender, subMessage);
                return;
            }
            this.sendMessage.accept(sender, result);
            return;
        }
        this.sendMessage.accept(sender, config.getMessage("invalid_arguments"));
    }

    public List<String> onTabComplete(Object sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (this.hasPermission.apply(sender, WhitelistCore.COMMAND_PERMISSION)) {
            list = this.plugin.getConfiguration().getCompleter(this.plugin.getWhitelist(), args);
        }
        return list;
    }
}
