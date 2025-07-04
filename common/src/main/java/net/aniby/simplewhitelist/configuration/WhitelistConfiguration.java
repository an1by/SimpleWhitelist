package net.aniby.simplewhitelist.configuration;

import net.aniby.simplewhitelist.api.WhitelistCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistConfiguration {
    private WhitelistSettings settings = null;
    private Whitelist whitelist = null;
    private final Path configFile;
    private final Path whitelistFile;

    public WhitelistConfiguration(Path configFile, Path whitelistFile) {
        this.configFile = configFile;
        this.whitelistFile = whitelistFile;

        this.saveDefault();
    }

    public Component getMessage(String path, TagResolver... resolvers) {
        return WhitelistCore.MINI_MESSAGE.deserialize(
                this.settings.getMessages().get(path),
                resolvers
        );
    }

    public Component getCommandMessage(String path, TagResolver... resolvers) {
        return WhitelistCore.MINI_MESSAGE.deserialize(
                this.settings.getCommandMessages().get(path),
                resolvers
        );
    }

    public WhitelistSettings getSettings() {
        return this.settings;
    }

    public Whitelist getWhitelist() {
        return this.whitelist;
    }

    public void load() {
        try {
            this.settings = WhitelistCore.YAML.loadAs(
                    Files.readString(this.configFile),
                    WhitelistSettings.class
            );

            Set<String> whitelistedPlayers = Files.readAllLines(this.whitelistFile)
                    .stream().map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            this.whitelist = new Whitelist(whitelistedPlayers, this.settings.isCaseSensitive());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveWhitelist() {
        try {
            String string = String.join("\n", this.whitelist.getSet());
            Files.writeString(this.whitelistFile, string, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveSettings() {
        try {
            String json = WhitelistCore.YAML.dump(this.settings);
            Files.writeString(this.configFile, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveDefault() {
        try {
            WhitelistCore.saveDefaultFile(this.configFile, "/config.yml");
            WhitelistCore.saveDefaultFile(this.whitelistFile, "/whitelist.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.load();
    }
}
