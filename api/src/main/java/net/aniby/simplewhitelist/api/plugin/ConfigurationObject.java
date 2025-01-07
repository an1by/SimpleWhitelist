package net.aniby.simplewhitelist.api.plugin;

import com.google.gson.annotations.SerializedName;
import net.aniby.simplewhitelist.api.entity.ConfigurationEntity;

import java.util.Map;

public class ConfigurationObject implements ConfigurationEntity {
    private boolean enabled;

    private final Map<String, String> messages;

    @SerializedName("command_messages")
    private final Map<String, String> commandMessages;

    public ConfigurationObject(boolean enabled, Map<String, String> messages, Map<String, String> commandMessages) {
        this.enabled = enabled;
        this.messages = messages;
        this.commandMessages = commandMessages;
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

    public Map<String, String> getCommandMessages() {
        return this.commandMessages;
    }
}
