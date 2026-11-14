package net.aniby.simplewhitelist.configuration;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Whitelist {
    private final Set<String> set;
    private final boolean isCaseSensitive;

    public Whitelist(Set<String> set, boolean isCaseSensitive) {
        this.set = ConcurrentHashMap.newKeySet();
        if (set != null) {
            this.set.addAll(set);
        }
        this.isCaseSensitive = isCaseSensitive;
    }

    public boolean contains(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        if (this.isCaseSensitive) {
            return this.set.contains(playerName);
        }
        return this.set.stream().anyMatch(playerName::equalsIgnoreCase);
    }

    public boolean add(String playerName) {
        if (playerName == null) {
            return false;
        }
        String normalized = playerName.trim();
        if (normalized.isEmpty() || this.contains(normalized)) {
            return false;
        }
        return this.set.add(normalized);
    }

    public boolean remove(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        String normalized = playerName.trim();
        if (this.isCaseSensitive) {
            return this.set.remove(normalized);
        }
        return this.set.removeIf(normalized::equalsIgnoreCase);
    }

    public Set<String> getSet() {
        return Collections.unmodifiableSet(this.set);
    }
}
