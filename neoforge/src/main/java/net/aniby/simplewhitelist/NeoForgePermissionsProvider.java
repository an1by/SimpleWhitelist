package net.aniby.simplewhitelist;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.Optional;

public class NeoForgePermissionsProvider {
    private static Optional<PermissionNode<?>> getPermissionNode(String name) {
        return PermissionAPI.getRegisteredNodes()
                .stream()
                .filter(p -> p.getNodeName().equals(name))
                .findFirst();
    }

    public static boolean hasPermission(ServerPlayer player, String permission, int fallbackPermissionLevel) {
        Optional<PermissionNode<?>> optionalNode = getPermissionNode(permission);
        if (optionalNode.isPresent()) {
            PermissionNode<?> node = optionalNode.get();
            if (node.getType() == PermissionTypes.BOOLEAN) {
                PermissionNode<Boolean> permissionNode = (PermissionNode<Boolean>) node;
                try {
                    return PermissionAPI.getPermission(player, permissionNode);
                } catch (Exception ignored) {
                }
            }
        }
        return player.hasPermissions(fallbackPermissionLevel);
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        return hasPermission(player, permission, 4);
    }

    public static boolean hasPermission(CommandSourceStack source, String permission, int fallbackPermissionLevel) {
        if (source.isPlayer()) {
            Optional<PermissionNode<?>> optionalNode = getPermissionNode(permission);
            if (optionalNode.isPresent()) {
                PermissionNode<?> node = optionalNode.get();
                if (node.getType() == PermissionTypes.BOOLEAN) {
                    PermissionNode<Boolean> permissionNode = (PermissionNode<Boolean>) node;
                    try {
                        return PermissionAPI.getPermission(source.getPlayer(), permissionNode);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return source.hasPermission(fallbackPermissionLevel);
    }

    public static boolean hasPermission(CommandSourceStack source, String permission) {
        return hasPermission(source, permission, 4);
    }
}
