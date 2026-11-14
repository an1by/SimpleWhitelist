package net.aniby.simplewhitelist.configuration;

import net.aniby.simplewhitelist.api.WhitelistCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WhitelistConfiguration {
    private static final Logger LOGGER = Logger.getLogger("SimpleWhitelist");
    private static final WhitelistSettings DEFAULT_SETTINGS = loadBundledDefaults();

    private volatile WhitelistSettings settings = loadBundledDefaults();
    private volatile Whitelist whitelist = new Whitelist(Collections.emptySet(), false);
    private final Path configFile;
    private final Path whitelistFile;

    public WhitelistConfiguration(Path configFile, Path whitelistFile) {
        this.configFile = configFile;
        this.whitelistFile = whitelistFile;

        this.saveDefault();
    }

    private static WhitelistSettings loadBundledDefaults() {
        try (InputStream stream = WhitelistCore.class.getResourceAsStream("/config.yml")) {
            if (stream != null) {
                WhitelistSettings loaded = WhitelistCore.createYaml().loadAs(stream, WhitelistSettings.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Throwable ignored) {
        }
        return new WhitelistSettings();
    }

    public Component getMessage(String path, TagResolver... resolvers) {
        String raw = this.settings.getMessages().get(path);
        if (raw == null) {
            raw = DEFAULT_SETTINGS.getMessages().getOrDefault(path, path);
        }
        return WhitelistCore.MINI_MESSAGE.deserialize(raw, resolvers);
    }

    public Component getCommandMessage(String path, TagResolver... resolvers) {
        String raw = this.settings.getCommandMessages().get(path);
        if (raw == null) {
            raw = DEFAULT_SETTINGS.getCommandMessages().getOrDefault(path, path);
        }
        return WhitelistCore.MINI_MESSAGE.deserialize(raw, resolvers);
    }

    public WhitelistSettings getSettings() {
        return this.settings;
    }

    public Whitelist getWhitelist() {
        return this.whitelist;
    }

    public synchronized void load() {
        boolean hadLegacyGlobalTag = false;
        try {
            String rawYaml = new String(Files.readAllBytes(this.configFile), StandardCharsets.UTF_8);
            String sanitized = stripLeadingGlobalTag(rawYaml);
            hadLegacyGlobalTag = !sanitized.equals(rawYaml);

            WhitelistSettings loaded = WhitelistCore.createYaml().loadAs(
                    sanitized,
                    WhitelistSettings.class
            );
            if (loaded == null) {
                loaded = loadBundledDefaults();
            }
            DEFAULT_SETTINGS.getMessages().forEach(loaded.getMessages()::putIfAbsent);
            DEFAULT_SETTINGS.getCommandMessages().forEach(loaded.getCommandMessages()::putIfAbsent);
            this.settings = loaded;
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "[SimpleWhitelist] Failed to parse config.yml, keeping fail-safe settings", e);
            if (this.settings == null) {
                this.settings = loadBundledDefaults();
            }
        }

        if (hadLegacyGlobalTag) {
            try {
                this.saveSettings();
            } catch (Throwable ignored) {
            }
        }

        try {
            Set<String> whitelistedPlayers = Files.readAllLines(this.whitelistFile, StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .collect(Collectors.toSet());
            this.whitelist = new Whitelist(whitelistedPlayers, this.settings.isCaseSensitive());
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "[SimpleWhitelist] Failed to read whitelist.txt, keeping existing whitelist", e);
            if (this.whitelist == null) {
                this.whitelist = new Whitelist(Collections.emptySet(), this.settings.isCaseSensitive());
            }
        }
    }

    private static final java.util.regex.Pattern LEADING_GLOBAL_TAG_PATTERN = java.util.regex.Pattern.compile(
            "^(?:\\uFEFF)?(?:[ \\t]*#[^\\n]*\\r?\\n)*\\s*(?:---\\s+)?!!\\S+[ \\t]*(?:\\r?\\n)?"
    );

    private static String stripLeadingGlobalTag(String rawYaml) {
        if (rawYaml == null) {
            return "";
        }
        return LEADING_GLOBAL_TAG_PATTERN.matcher(rawYaml).replaceFirst("");
    }

    public synchronized void saveWhitelist() {
        try {
            List<String> sorted = new ArrayList<>(this.whitelist.getSet());
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            String content = sorted.isEmpty() ? "" : String.join("\n", sorted) + "\n";
            Files.write(this.whitelistFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void saveSettings() {
        try {
            String yaml = WhitelistCore.createYaml().dumpAsMap(this.settings);
            Files.write(this.configFile, yaml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveDefault() {
        try {
            WhitelistCore.saveDefaultFile(this.configFile, "/config.yml");
            WhitelistCore.saveDefaultFile(this.whitelistFile, "/whitelist.txt");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[SimpleWhitelist] Failed to save default configuration files", e);
        }
        this.load();
    }
}
