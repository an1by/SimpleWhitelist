package net.aniby.simplewhitelist.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;

public class FabricCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSourceStack> {
    @Override
    public boolean hasPermission(CommandSourceStack source, String permission) {
        return Permissions.check(source, permission, 3);
    }

    @Override
    public void sendMessage(CommandSourceStack source, Component message) {
        source.sendMessage(message);
    }

    @Override
    public void sendFailure(CommandSourceStack source, Component message) {
        source.sendFailure(message);
    }

    @Override
    public void sendSuccess(CommandSourceStack source, Component message, boolean sendOps) {
        source.sendSuccess(message, sendOps);
    }
}
