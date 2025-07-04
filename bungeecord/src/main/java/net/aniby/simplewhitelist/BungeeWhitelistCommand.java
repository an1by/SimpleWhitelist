package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BungeeWhitelistCommand extends Command implements TabExecutor {
    private final CommandDispatcher<CommandSender> dispatcher;
    private final BungeeWhitelistPlugin plugin;

    protected BungeeWhitelistCommand(BungeeWhitelistPlugin plugin) {
        super("simplewhitelist", WhitelistCommand.PERMISSION, "simplewl", "swl");
        this.plugin = plugin;
        this.dispatcher = new CommandDispatcher<>();
        this.dispatcher.register(new WhitelistCommand<>(
                new BungeeCommandSourceExecutor(), this.plugin
        ).builder());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String input = ("simplewhitelist " + String.join(" ", args)).trim();
        try {
            this.dispatcher.execute(input, sender);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 0:
            case 1: {
                list = this.plugin.getConfiguration().getSettings().getCommandMessages()
                        .keySet().stream().filter(
                                n -> args.length == 0 || n.startsWith(args[0])
                        ).collect(Collectors.toList());
                break;
            }
            case 2: {
                if (args[0].equals("remove")) {
                    list = this.plugin.getConfiguration().getWhitelist().getSet()
                            .stream().filter(
                                    c -> args[1].isEmpty() || c.startsWith(args[1].toLowerCase())
                            )
                            .collect(Collectors.toList());
                }
                break;
            }
        }
        return list;
    }
}
