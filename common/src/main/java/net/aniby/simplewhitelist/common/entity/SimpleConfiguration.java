package net.aniby.simplewhitelist.common.entity;

public interface SimpleConfiguration extends BasicConfiguration {
    ConfigurationEntity getConfiguration();

    default String getMessage(String name) {
        return this.getConfiguration().getMessages().get(name);
    }

    default String getCommandMessage(String name) {
        return this.getConfiguration().getCommandMessages().get(name);
    }

    void setEnabled(boolean enabled);

    boolean isEnabled();
}
