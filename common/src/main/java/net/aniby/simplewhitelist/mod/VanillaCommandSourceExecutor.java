package net.aniby.simplewhitelist.mod;

import net.aniby.simplewhitelist.command.WhitelistCommandSourceExecutor;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class VanillaCommandSourceExecutor implements WhitelistCommandSourceExecutor<Object> {
    private final VanillaModBridge bridge;

    public VanillaCommandSourceExecutor(VanillaModBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean hasPermission(Object source, String permission) {
        if (source == null) {
            return false;
        }

        // 1. Platform permission hook (NeoForge/Forge PermissionAPI, custom test hook, etc.)
        BiFunction<Object, String, Boolean> custom = this.bridge.getCustomPermissionChecker();
        if (custom != null) {
            try {
                Boolean res = custom.apply(source, permission);
                if (res != null) {
                    return res;
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. Fabric / Quilt Permissions API v0 (LuckPerms on Fabric/Quilt)
        Boolean fabricPerm = checkFabricPermissionsApi(source, permission);
        if (fabricPerm != null) {
            return fabricPerm;
        }

        // 3a. Vanilla MC 1.13.2 - 1.21.10: CommandSourceStack.hasPermission(int)
        for (Method m : source.getClass().getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == int.class
                    && m.getReturnType() == boolean.class) {
                try {
                    return (Boolean) m.invoke(source, 3);
                } catch (Throwable ignored) {
                }
            }
        }

        // 3b. Vanilla MC 1.21.11 & 26.x: Commands.hasPermission(Commands.LEVEL_ADMINS)
        Predicate<Object> adminPred = this.bridge.getCommandsAdminPredicate();
        if (adminPred == null) {
            ClassLoader loader = source.getClass().getClassLoader();
            for (String cmdCls : new String[]{"net.minecraft.commands.Commands", "net.minecraft.class_2170"}) {
                Class<?> c = VanillaModBridge.tryLoadClass(cmdCls, loader);
                if (c != null) {
                    this.bridge.cacheAdminPredicateFromCommands(c);
                    adminPred = this.bridge.getCommandsAdminPredicate();
                    if (adminPred != null) {
                        break;
                    }
                }
            }
        }
        if (adminPred != null) {
            try {
                return adminPred.test(source);
            } catch (Throwable ignored) {
            }
        }

        // 3c. Console / non-entity command source fallback
        Object entity = extractEntity(source);
        return entity == null;
    }

    @Override
    public void sendMessage(Object source, Component message) {
        this.sendSuccess(source, message, false);
    }

    @Override
    public void sendSuccess(Object source, Component message, boolean broadcastToOps) {
        if (source == null || message == null) {
            return;
        }
        ClassLoader loader = source.getClass().getClassLoader();
        Class<?> compClass = resolveSourceComponentClass(source, loader);
        if (compClass == null) {
            return;
        }
        final Object vanillaComp = VanillaModBridge.toVanillaComponent(message, compClass, loader);
        if (vanillaComp == null) {
            return;
        }

        // MC 1.20 - 26.3: sendSuccess(Supplier<Component>, boolean)
        for (Method m : source.getClass().getMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (!Modifier.isStatic(m.getModifiers())
                    && params.length == 2
                    && Supplier.class.isAssignableFrom(params[0])
                    && params[1] == boolean.class
                    && m.getReturnType() == void.class) {
                try {
                    Supplier<Object> supplier = () -> vanillaComp;
                    m.invoke(source, supplier, broadcastToOps);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }

        // MC 1.13.2 - 1.19.4: sendSuccess(Component, boolean)
        for (Method m : source.getClass().getMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (!Modifier.isStatic(m.getModifiers())
                    && params.length == 2
                    && params[0].isAssignableFrom(compClass)
                    && params[1] == boolean.class
                    && m.getReturnType() == void.class) {
                try {
                    m.invoke(source, vanillaComp, broadcastToOps);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public void sendFailure(Object source, Component message) {
        if (source == null || message == null) {
            return;
        }
        ClassLoader loader = source.getClass().getClassLoader();
        Class<?> compClass = resolveSourceComponentClass(source, loader);
        if (compClass == null) {
            return;
        }
        Object vanillaComp = VanillaModBridge.toVanillaComponent(message, compClass, loader);
        if (vanillaComp == null) {
            return;
        }

        for (String name : new String[]{"sendFailure", "method_9213", "func_197021_a", "sendErrorMessage"}) {
            try {
                Method m = source.getClass().getMethod(name, compClass);
                m.invoke(source, vanillaComp);
                return;
            } catch (Throwable ignored) {
            }
        }

        for (Method m : source.getClass().getMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (!Modifier.isStatic(m.getModifiers())
                    && params.length == 1
                    && params[0].isAssignableFrom(compClass)
                    && m.getReturnType() == void.class) {
                try {
                    m.invoke(source, vanillaComp);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private Class<?> resolveSourceComponentClass(Object source, ClassLoader loader) {
        Class<?> cached = this.bridge.resolveComponentClass(this.bridge.getPlayerList());
        if (cached != null) {
            return cached;
        }
        for (Method m : source.getClass().getMethods()) {
            if (m.getParameterCount() == 1 && m.getReturnType() == void.class && !m.getParameterTypes()[0].isPrimitive()) {
                String pName = m.getParameterTypes()[0].getName();
                if (pName.contains("Component") || pName.contains("class_2561")) {
                    return m.getParameterTypes()[0];
                }
            }
        }
        return this.bridge.resolveComponentClass(source);
    }

    private static Boolean checkFabricPermissionsApi(Object source, String permission) {
        ClassLoader loader = source.getClass().getClassLoader();
        Class<?> permsClass = VanillaModBridge.tryLoadClass("me.lucko.fabric.api.permissions.v0.Permissions", loader);
        if (permsClass == null) {
            return null;
        }
        try {
            for (Method m : permsClass.getMethods()) {
                if ("getPermissionValue".equals(m.getName())
                        && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].isInstance(source)
                        && m.getParameterTypes()[1] == String.class) {
                    Object triState = m.invoke(null, source, permission);
                    if (triState != null) {
                        String stateName = triState.toString();
                        if ("TRUE".equalsIgnoreCase(stateName)) {
                            return Boolean.TRUE;
                        }
                        if ("FALSE".equalsIgnoreCase(stateName)) {
                            return Boolean.FALSE;
                        }
                    }
                    return null;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Object extractEntity(Object source) {
        for (String name : new String[]{"getEntity", "method_9228", "func_197022_f"}) {
            try {
                Method m = source.getClass().getMethod(name);
                return m.invoke(source);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
