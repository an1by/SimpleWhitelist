package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.api.WhitelistCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeWhitelistCommand extends Command implements TabExecutor {
    private final WhitelistCommand command;

    public BungeeWhitelistCommand(BungeeWhitelistPlugin plugin) {
        super("simplewhitelist", WhitelistCore.COMMAND_PERMISSION, "simplewl", "swl");
        this.command = new WhitelistCommand(plugin,
                (sender, permission) -> ((CommandSender) sender).hasPermission(permission),
                (sender, string) -> ((CommandSender) sender).sendMessage(new ComponentBuilder(
                        string
                ).create()));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.command.execute(sender, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return this.command.onTabComplete(sender, args);
    }
}
