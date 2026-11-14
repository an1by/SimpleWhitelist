package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

public class BungeeCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSender> {
    private static final BungeeComponentSerializer SERIALIZER = resolveSerializer();

    private static BungeeComponentSerializer resolveSerializer() {
        try {
            ChatColor.class.getMethod("of", String.class);
            return BungeeComponentSerializer.get();
        } catch (NoSuchMethodException ignored) {
            return BungeeComponentSerializer.legacy();
        }
    }

    public static BaseComponent[] toBungee(Component component) {
        return SERIALIZER.serialize(component);
    }

    @Override
    public boolean hasPermission(CommandSender source, String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSender source, Component message) {
        source.sendMessage(toBungee(message));
    }
}
