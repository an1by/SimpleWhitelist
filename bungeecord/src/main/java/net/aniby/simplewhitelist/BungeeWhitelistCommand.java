package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.command.RawCommandRunner;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeWhitelistCommand extends Command implements TabExecutor {
    private final RawCommandRunner<CommandSender> runner;

    protected BungeeWhitelistCommand(BungeeWhitelistPlugin plugin) {
        super("simplewhitelist", null, "simplewl", "swl");
        this.runner = new RawCommandRunner<>(new BungeeCommandSourceExecutor(), plugin);
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return this.runner.hasPermission(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.runner.execute(sender, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return this.runner.suggest(sender, args);
    }
}
