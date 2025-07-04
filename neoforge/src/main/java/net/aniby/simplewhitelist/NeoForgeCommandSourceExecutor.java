package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;

public class NeoForgeCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSourceStack> {
    private final NeoForgeWhitelistMod mod;

    protected NeoForgeCommandSourceExecutor(NeoForgeWhitelistMod mod) {
        this.mod = mod;
    }

    @Override
    public boolean hasPermission(CommandSourceStack source, String permission) {
        return NeoForgePermissionsProvider.hasPermission(source, permission);
    }

    @Override
    public void sendMessage(CommandSourceStack source, Component message) {
        source.sendSystemMessage(this.mod.getAdventure().asNative(message));
    }

    @Override
    public void sendFailure(CommandSourceStack source, Component message) {
        source.sendFailure(this.mod.getAdventure().asNative(message));
    }

    @Override
    public void sendSuccess(CommandSourceStack source, Component message, boolean sendOps) {
        source.sendSuccess(() -> this.mod.getAdventure().asNative(message), sendOps);
    }
}
