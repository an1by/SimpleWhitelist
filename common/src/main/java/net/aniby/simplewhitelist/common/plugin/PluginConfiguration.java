package net.aniby.simplewhitelist.common.plugin;

import net.aniby.simplewhitelist.common.SimpleCore;
import net.aniby.simplewhitelist.common.entity.SimpleConfiguration;
import net.aniby.simplewhitelist.common.entity.WhitelistHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PluginConfiguration implements SimpleConfiguration {
    private ConfigurationObject object = null;
    private final Path configFile;

    public PluginConfiguration(final Path pluginFolder) {
        this.configFile = pluginFolder.resolve("config.json");
        try {
            SimpleCore.saveDefaultFile(this.configFile.toFile(), "/default_config.json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        load();
    }

    @Override
    public ConfigurationObject getConfiguration() {
        return object;
    }

    @Override
    public void setEnabled(boolean enabled) {
        object.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return object.isEnabled();
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

    public List<String> getCompleter(WhitelistHandler whitelistHandler, String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 0, 1 -> list = object.getCommandMessages().keySet().stream().filter(
                    n -> args.length == 0 || n.startsWith(args[0])
            ).collect(Collectors.toList());
            case 2 -> {
                if (args[0].equals("remove")) {
                    list = whitelistHandler.getWhitelisted().stream()
                            .filter(c -> args[1].isEmpty() || c.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        return list;
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
