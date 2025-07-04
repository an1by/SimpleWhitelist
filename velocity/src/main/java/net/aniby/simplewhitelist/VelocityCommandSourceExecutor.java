package net.aniby.simplewhitelist;

import com.velocitypowered.api.command.CommandSource;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;

public class VelocityCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSource> {
    @Override
    public boolean hasPermission(CommandSource source, String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSource source, Component message) {
        source.sendMessage(message);
    }
}
