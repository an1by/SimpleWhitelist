package net.aniby.simplewhitelist.plugin;

import net.aniby.simplewhitelist.ForgeWhitelistMod;
import net.aniby.simplewhitelist.api.WhitelistCore;
import net.aniby.simplewhitelist.api.plugin.PluginConfiguration;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ForgePluginConfiguration extends PluginConfiguration {
    public ForgePluginConfiguration(final Path configFile) {
        super(configFile);
    }

    @Override
    public void setEnabled(boolean enabled) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().setUsingWhiteList(enabled);
    }

    @Override
    public boolean isEnabled() {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().isUsingWhitelist();
    }

    @Override
    public void load() {
        try {
            this.object = WhitelistCore.GSON.fromJson(Files.readString(this.configFile), ForgeConfigurationObject.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save() {
        try {
            String json = WhitelistCore.GSON.toJson(this.object);
            Files.writeString(this.configFile, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDefault() {
        try {
            WhitelistCore.saveDefaultFile(this.configFile.toFile(), "/default_forge_config.json", ForgeWhitelistMod.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.load();
    }
}
