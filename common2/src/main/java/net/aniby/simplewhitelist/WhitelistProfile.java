package net.aniby.simplewhitelist;

import net.kyori.adventure.text.Component;

public interface WhitelistProfile {
    boolean hasPermission(String permission);
    void sendMessage(Component message);
}
