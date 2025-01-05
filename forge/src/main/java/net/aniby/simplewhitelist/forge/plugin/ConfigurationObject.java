package net.aniby.simplewhitelist.forge.plugin;

import com.google.gson.annotations.SerializedName;
import net.aniby.simplewhitelist.common.entity.ConfigurationEntity;

import java.util.Map;

public class ConfigurationObject implements ConfigurationEntity {
    private final Map<String, String> messages;

    @SerializedName("command_messages")
    private final Map<String, String> commandMessages;

    public ConfigurationObject(Map<String, String> messages, Map<String, String> commandMessages) {
        this.messages = messages;
        this.commandMessages = commandMessages;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public Map<String, String> getCommandMessages() {
        return this.commandMessages;
    }
}
