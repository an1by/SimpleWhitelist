package net.aniby.simplewhitelist.common.entity;

public interface BasicConfiguration {
    void load();

    void save();

    default void reload() {
        this.load();
    }
}
