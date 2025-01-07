package net.aniby.simplewhitelist.api.entity;

public interface BasicConfiguration {
    void load();

    void save();

    void saveDefault();

    default void reload() {
        this.load();
    }
}
