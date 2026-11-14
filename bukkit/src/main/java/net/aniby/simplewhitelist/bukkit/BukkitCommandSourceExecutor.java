package net.aniby.simplewhitelist.bukkit;

import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class BukkitCommandSourceExecutor implements WhitelistCommandSourceExecutor<CommandSender> {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = buildLegacySerializer();

    private final BukkitWhitelistPlugin plugin;

    public BukkitCommandSourceExecutor(BukkitWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    private static LegacyComponentSerializer buildLegacySerializer() {
        LegacyComponentSerializer.Builder builder = LegacyComponentSerializer.builder().character('\u00A7');
        try {
            if (Material.getMaterial("NETHERITE_INGOT") != null) {
                builder.hexColors().useUnusualXRepeatedCharacterHexFormat();
            }
        } catch (Throwable ignored) {
        }
        return builder.build();
    }

    public static String toLegacyString(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    @Override
    public boolean hasPermission(CommandSender source, String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSender source, Component message) {
        BukkitAudiences audiences = this.plugin.getAudiences();
        if (audiences != null) {
            try {
                audiences.sender(source).sendMessage(message);
                return;
            } catch (Throwable ignored) {
            }
        }
        source.sendMessage(toLegacyString(message));
    }
}
