package net.aniby.simplewhitelist.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WhitelistSettings {
    private boolean enabled = true;
    private boolean caseSensitive = false;
    private boolean enforceWhitelist = true;
    private boolean logBlockedJoins = true;
    private List<String> onBlockedJoinCommands = new ArrayList<>();
    private Map<String, String> messages = new LinkedHashMap<>();
    private Map<String, String> commandMessages = new LinkedHashMap<>();

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnforceWhitelist() {
        return this.enforceWhitelist;
    }

    public void setEnforceWhitelist(boolean enforceWhitelist) {
        this.enforceWhitelist = enforceWhitelist;
    }

    public boolean isLogBlockedJoins() {
        return this.logBlockedJoins;
    }

    public void setLogBlockedJoins(boolean logBlockedJoins) {
        this.logBlockedJoins = logBlockedJoins;
    }

    public List<String> getOnBlockedJoinCommands() {
        return this.onBlockedJoinCommands;
    }

    public void setOnBlockedJoinCommands(List<String> onBlockedJoinCommands) {
        this.onBlockedJoinCommands = onBlockedJoinCommands != null ? onBlockedJoinCommands : new ArrayList<>();
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages != null ? messages : new LinkedHashMap<>();
    }

    public Map<String, String> getCommandMessages() {
        return this.commandMessages;
    }

    public void setCommandMessages(Map<String, String> commandMessages) {
        this.commandMessages = commandMessages != null ? commandMessages : new LinkedHashMap<>();
    }
}
