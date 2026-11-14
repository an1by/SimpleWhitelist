package net.aniby.simplewhitelist.api;

import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;
import net.aniby.simplewhitelist.configuration.WhitelistSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public interface WhitelistPlugin {
    Pattern SAFE_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.+* -]{1,32}$");

    WhitelistConfiguration getConfiguration();

    default Collection<String> getOnlinePlayerNames() {
        return Collections.emptyList();
    }

    default void kickUnwhitelistedPlayers() {
    }

    default void logConsoleMessage(Component message) {
        if (message != null) {
            Logger.getLogger("SimpleWhitelist").info(WhitelistCore.MINI_MESSAGE.serialize(message));
        }
    }

    default void executeConsoleCommand(String command) {
    }

    default Component getNotInWhitelistMessage(String username) {
        String display = (username == null || username.trim().isEmpty()) ? "Unknown" : username.trim();
        return this.getConfiguration().getMessage(
                "notInWhitelist",
                Placeholder.unparsed("name", display)
        );
    }

    default void handleBlockedJoin(String username) {
        WhitelistConfiguration cfg = this.getConfiguration();
        if (cfg == null) {
            return;
        }
        WhitelistSettings settings = cfg.getSettings();
        if (settings == null) {
            return;
        }
        String trimmed = username == null ? "" : username.trim();
        String display = trimmed.isEmpty() ? "Unknown" : trimmed;

        if (settings.isLogBlockedJoins()) {
            try {
                Component logMsg = cfg.getMessage(
                        "blockedJoin",
                        Placeholder.unparsed("name", display)
                );
                this.logConsoleMessage(logMsg);
            } catch (Throwable ignored) {
            }
        }

        List<String> commands = settings.getOnBlockedJoinCommands();
        if (commands != null && !commands.isEmpty() && isSafePlayerName(trimmed)) {
            for (String rawCommand : commands) {
                if (rawCommand == null) {
                    continue;
                }
                String resolved = rawCommand
                        .replace("<name>", trimmed)
                        .replace("%name%", trimmed)
                        .replace("%player%", trimmed)
                        .replace("{name}", trimmed)
                        .trim();
                if (resolved.startsWith("/")) {
                    resolved = resolved.substring(1).trim();
                }
                if (!resolved.isEmpty()) {
                    try {
                        this.executeConsoleCommand(resolved);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    static boolean isSafePlayerName(String username) {
        return username != null && SAFE_PLAYER_NAME_PATTERN.matcher(username).matches();
    }
}
