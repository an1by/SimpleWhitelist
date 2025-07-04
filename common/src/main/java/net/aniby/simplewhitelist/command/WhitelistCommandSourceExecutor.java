package net.aniby.simplewhitelist.command;

import net.kyori.adventure.text.Component;

public interface WhitelistCommandSourceExecutor<S> {
    boolean hasPermission(S source, String permission);

    void sendMessage(S source, Component message);

    default void sendFailure(S source, Component message) {
        this.sendMessage(source, message);
    }

    default void sendSuccess(S source, Component message, boolean sendOps) {
        this.sendMessage(source, message);
    }
}
