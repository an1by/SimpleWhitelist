package net.aniby.simplewhitelist.common;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.Types;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleConfiguration {
    private final YAMLConfigurationLoader loader;
    private ConfigurationNode root;

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.root.getNode("enabled").setValue(enabled);
        save();
    }

    private List<String> whitelisted;
    private final Map<String, String> messages = new HashMap<>();
    public Map<String, String> getMessages() {
        return messages;
    }

    private final Map<String, String> commandMessages = new HashMap<>();
    public Map<String, String> getCommandMessages() {
        return commandMessages;
    }

    public List<String> getWhitelisted() {
        return whitelisted;
    }
    public String getWhitelistedString() {
        return whitelisted.stream().collect(Collectors.joining(", ", "", ""));
    }
    public boolean whitelistAdd(String name) {
        if (whitelistContains(name))
            return false;
        whitelisted.add(name.toLowerCase());
        this.root.getNode("whitelist").setValue(whitelisted);
        save();
        return true;
    }
    public void whitelistRemove(String name) {
        whitelisted.remove(name.toLowerCase());
        this.root.getNode("whitelist").setValue(whitelisted);
        save();

    }
    public boolean whitelistContains(String name) {
        return whitelisted.contains(name.toLowerCase());
    }

    public SimpleConfiguration(final Path path) throws IOException {
        Path path1 = loadFiles(path);
        this.loader = YAMLConfigurationLoader.builder()
                .setPath(path1)
                .build();

        load();
    }

    public void load() throws IOException {
        this.root = loader.load();

        this.enabled = this.root.getNode("enabled")
                .getBoolean(false);

        this.root.getNode("messages").getChildrenMap().forEach((key, value) -> {
            this.messages.put(
                    (String) key, value.getString().replace("\\n", "\n")
            );
        });
        System.out.println(this.messages.get("help"));

        this.root.getNode("command_messages").getChildrenMap().forEach((key, value) -> {
            this.commandMessages.put(
                    (String) key, value.getString().replace("\\n", "\n")
            );
        });

        this.whitelisted = this.root.getNode("whitelist")
                .getList(Types::asString)
                .stream().map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public void save() {
        try {
            this.loader.save(this.root);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static Path loadFiles(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectory(path);
        }
        final Path configPath = path.resolve("config.yml");
        if (Files.notExists(configPath)) {
            try (var stream = SimpleConfiguration.class.getClassLoader().getResourceAsStream("default_config.yml")) {
                Files.copy(Objects.requireNonNull(stream), configPath);
            }
        }
        return configPath;
    }
}
