package net.aniby.simplewhitelist.api.entity;

import java.util.Map;

public interface ConfigurationEntity {
    Map<String, String> getMessages();

    Map<String, String> getCommandMessages();
}
