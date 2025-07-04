package net.aniby.simplewhitelist.api;

import net.aniby.simplewhitelist.configuration.WhitelistSettings;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class WhitelistCore {
    public static final MiniMessage MINI_MESSAGE = MiniMessage
            .builder()
            .tags(TagResolver.standard())
            .build();
    public static final Yaml YAML;

    static {
        YAML = new Yaml();
        TypeDescription whitelistSettingsDescription = new TypeDescription(WhitelistSettings.class);
        YAML.addTypeDescription(whitelistSettingsDescription);
    }

    public static void saveDefaultFile(Path path, String resourcePath) throws IOException {
        saveDefaultFile(path, resourcePath, WhitelistCore.class);
    }

    public static void saveDefaultFile(Path filePath, String resourcePath, Class<?> _class) throws IOException {
        if (!Files.exists(filePath)) {
            Path folderPath = filePath.getParent();
            if (folderPath != null && Files.notExists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            try (InputStream initialStream = _class.getResourceAsStream(resourcePath)) {
                if (initialStream == null) {
                    throw new IOException("Resource not found: " + resourcePath);
                }
                Files.copy(initialStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
