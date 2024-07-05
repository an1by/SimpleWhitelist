package net.aniby.simplewhitelist.common.plugin;

import com.google.gson.annotations.SerializedName;
import net.aniby.simplewhitelist.common.entity.ConfigurationEntity;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationObject implements ConfigurationEntity {
    private boolean enabled;
    private final Map<String, String> messages;
    @SerializedName("command_messages")
    private final Map<String, String> commandMessages;

    public ConfigurationObject() {
        this(true, new HashMap<>(), new HashMap<>());
    }

    public ConfigurationObject(boolean enabled, Map<String, String> messages, Map<String, String> commandMessages) {
        this.enabled = enabled;
        this.messages = messages;
        this.commandMessages = commandMessages;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getMessages() {
        return messages;
    }
    public Map<String, String> getCommandMessages() {
        return commandMessages;
    }
}
