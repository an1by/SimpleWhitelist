package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.CommandSender;

public class BungeeCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSender> {
    @Override
    public boolean hasPermission(CommandSender source, String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSender source, Component message) {
        source.sendMessage(BungeeComponentSerializer.get().serialize(message));
    }
}
