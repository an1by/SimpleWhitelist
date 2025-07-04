package net.aniby.simplewhitelist.configuration;

import java.util.Map;

public class WhitelistSettings {
    private boolean enabled;
    private boolean caseSensitive;
    private Map<String, String> messages;
    private Map<String, String> commandMessages;

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

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public Map<String, String> getCommandMessages() {
        return this.commandMessages;
    }

    public void setCommandMessages(Map<String, String> commandMessages) {
        this.commandMessages = commandMessages;
    }
}
