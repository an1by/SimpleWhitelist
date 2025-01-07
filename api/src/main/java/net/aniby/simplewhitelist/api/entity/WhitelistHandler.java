package net.aniby.simplewhitelist.api.entity;

import java.util.List;

public interface WhitelistHandler {
    boolean isWhitelisted(String playerName);

    void addWhitelist(String playerName);

    void removeWhitelist(String playerName);

    List<String> getWhitelisted();

    default String getWhitelistedAsString() {
        return String.join(", ", this.getWhitelisted());
    }
}
