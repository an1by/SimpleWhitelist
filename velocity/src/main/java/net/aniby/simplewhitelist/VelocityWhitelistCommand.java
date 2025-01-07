package net.aniby.simplewhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VelocityWhitelistCommand implements SimpleCommand {
    private final WhitelistCommand command;

    public VelocityWhitelistCommand(VelocityWhitelistPlugin plugin) {
        this.command = new WhitelistCommand(plugin,
                (sender, permission) -> ((CommandSource) sender).hasPermission(permission),
                (sender, string) -> ((CommandSource) sender).sendMessage(LegacyComponentSerializer.legacySection().deserialize(string)));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        this.command.execute(sender, args);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFuture.completedFuture(
                this.command.onTabComplete(
                        invocation.source(),
                        invocation.arguments()
                )
        );
    }
}
