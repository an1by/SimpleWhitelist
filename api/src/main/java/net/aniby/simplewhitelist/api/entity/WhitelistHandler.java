package net.aniby.simplewhitelist.api.entity;

import java.util.List;

/**
 * Interface for handling a whitelist system.
 * Implementations define how players are added to, removed from, and checked against a whitelist.
 */
public interface WhitelistHandler {

    /**
     * Checks whether a player is currently whitelisted.
     *
     * @param playerName the name of the player to check
     * @return {@code true} if the player is whitelisted, {@code false} otherwise
     */
    boolean isWhitelisted(String playerName);

    /**
     * Adds a player to the whitelist.
     *
     * @param playerName the name of the player to add
     */
    void addWhitelist(String playerName);

    /**
     * Removes a player from the whitelist.
     *
     * @param playerName the name of the player to remove
     */
    void removeWhitelist(String playerName);

    /**
     * Retrieves a list of all whitelisted players.
     *
     * @return a {@code List<String>} containing the names of all whitelisted players
     */
    List<String> getWhitelisted();

    /**
     * Retrieves the list of whitelisted players as a comma-separated {@code String}.
     *
     * @return a {@code String} containing all whitelisted player names separated by commas
     */
    default String getWhitelistedAsString() {
        return String.join(", ", this.getWhitelisted());
    }

    /**
     * Container class for holding a static instance of the {@code WhitelistHandler}.
     */
    class Api {
        /**
         * Static reference to a {@code WhitelistHandler} instance.
         * Should be assigned during plugin/mod initialization.
         */
        public static WhitelistHandler instance;
    }
}

