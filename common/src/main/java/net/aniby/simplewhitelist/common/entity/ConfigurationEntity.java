package net.aniby.simplewhitelist.common.entity;

import java.util.Map;

public interface ConfigurationEntity {
    Map<String, String> getMessages();
    Map<String, String> getCommandMessages();
}
