package net.aniby.simplewhitelist.configuration;

import java.util.HashSet;
import java.util.Set;

public class Whitelist {
    private final Set<String> set;
    private final boolean isCaseSensitive;

    public Whitelist(Set<String> set, boolean isCaseSensitive) {
        this.set = new HashSet<>(set);
        this.isCaseSensitive = isCaseSensitive;
    }

    public boolean contains(String playerName) {
        if (this.isCaseSensitive) {
            return this.set.contains(playerName);
        }
        return this.set.stream().anyMatch(playerName::equalsIgnoreCase);
    }

    public void add(String playerName) {
        if (!this.contains(playerName)) {
            this.set.add(playerName);
        }
    }

    public void remove(String playerName) {
        this.set.remove(playerName);
    }

    public Set<String> getSet() {
        return this.set;
    }
}
