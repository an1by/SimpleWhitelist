package net.aniby.simplewhitelist;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;

public class PaperCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSourceStack> {
    @Override
    public boolean hasPermission(CommandSourceStack source, String permission) {
        return source.getSender().hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSourceStack source, Component message) {
        source.getSender().sendMessage(message);
    }
}
