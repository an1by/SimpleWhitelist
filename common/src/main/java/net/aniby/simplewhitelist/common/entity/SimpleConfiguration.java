package net.aniby.simplewhitelist.common.entity;

public interface SimpleConfiguration extends BasicConfiguration {
    ConfigurationEntity getConfiguration();

    default String getMessage(String name) {
        return getConfiguration().getMessages().get(name);
    }
    default String getCommandMessage(String name) {
        return getConfiguration().getCommandMessages().get(name);
    }

    void setEnabled(boolean enabled);
    boolean isEnabled();
}
