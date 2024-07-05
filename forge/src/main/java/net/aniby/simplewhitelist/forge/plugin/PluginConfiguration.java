package net.aniby.simplewhitelist.forge.plugin;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.aniby.simplewhitelist.common.entity.SimpleConfiguration;
import net.aniby.simplewhitelist.forge.SimpleWhitelist;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginConfiguration implements SimpleConfiguration {
    @Override
    public ConfigurationObject getConfiguration() {
        return object;
    }
    private ConfigurationObject object = null;
    private final Path configFile;

    public PluginConfiguration(final Path pluginFolder) {
        this.configFile = pluginFolder.resolve("config.json");
        try {
            SimpleCore.saveDefaultFile(this.configFile.toFile(), "/default_forge_config.json", SimpleWhitelist.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        load();
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
            this.object = SimpleCore.GSON.fromJson(Files.readString(this.configFile), ConfigurationObject.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save() {
        try {
            String json = SimpleCore.GSON.toJson(object);
            Files.writeString(configFile, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String checkSubcommand(String subcommand, boolean permission) {
        String subMessage = object.getCommandMessages().get(subcommand);
        if (subMessage == null) {
            return object.getMessages().get("invalid_arguments");
        }

        if (!permission) {
            return object.getMessages().get("no_permission");
        }
        return null;
    }
}
