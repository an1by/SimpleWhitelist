package net.aniby.simplewhitelist.api.entity;

import java.util.Map;

public interface ConfigurationFile {
    boolean isCaseSensitive();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Map<String, String> getMessages();

    Map<String, String> getCommandMessages();
}
