package net.aniby.simplewhitelist;

import net.kyori.adventure.text.Component;

public interface WhitelistProfile {
    boolean hasPermission(String permission);

    void sendMessage(Component message);

    default void sendFailure(Component message) {
        this.sendMessage(message);
    }

    default void sendSuccess(Component message, boolean sendOps) {
        this.sendMessage(message);
    }
}
