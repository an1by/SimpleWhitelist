package net.aniby.simplewhitelist.api.plugin;

import net.aniby.simplewhitelist.api.WhitelistCore;
import net.aniby.simplewhitelist.api.entity.BasicConfiguration;
import net.aniby.simplewhitelist.api.entity.WhitelistHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PluginWhitelist implements WhitelistHandler, BasicConfiguration {
    private final Path whitelistFile;
    private final List<String> list = new ArrayList<>();

    public PluginWhitelist(Path whitelistFile) {
        this.whitelistFile = whitelistFile;
        this.saveDefault();
    }

    @Override
    public boolean isWhitelisted(String playerName) {
        return this.list.contains(playerName);
    }

    @Override
    public void addWhitelist(String playerName) {
        if (!this.isWhitelisted(playerName)) {
            this.list.add(playerName);
        }
    }

    @Override
    public void removeWhitelist(String playerName) {
        this.list.remove(playerName);
    }

    @Override
    public List<String> getWhitelisted() {
        return this.list;
    }

    @Override
    public void load() {
        try {
            this.list.clear();
            this.list.addAll(Files.readAllLines(this.whitelistFile).stream().map(String::strip).filter(s -> !s.isEmpty()).toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save() {
        try {
            String string = String.join("\n", this.list);
            Files.writeString(this.whitelistFile, string, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDefault() {
        // this.whitelistFile = pluginFolder.resolve("whitelist.txt");
        try {
            WhitelistCore.saveDefaultFile(this.whitelistFile.toFile(), "/whitelist.txt");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.load();
    }
}
