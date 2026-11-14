package net.aniby.simplewhitelist.mod;

import net.aniby.simplewhitelist.command.WhitelistPermission;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class FmlPermissionBridge {
    private FmlPermissionBridge() {
    }

    public static Path resolveFmlConfigDir(String fmlPathsClassName, String modId) {
        try {
            Class<?> fmlPaths = Class.forName(fmlPathsClassName);
            Object configDirEnum = fmlPaths.getField("CONFIGDIR").get(null);
            Method getMethod = configDirEnum.getClass().getMethod("get");
            Object base = getMethod.invoke(configDirEnum);
            if (base instanceof Path) {
                return ((Path) base).resolve(modId);
            }
        } catch (Throwable ignored) {
        }
        return Paths.get("config", modId);
    }

    public static void installPermissionApi(
            String modId,
            String eventBusHolderClass,
            String permissionPackage,
            ClassLoader loader
    ) {
        try {
            Class<?> gatherNodesClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".events.PermissionGatherEvent$Nodes", loader
            );
            Class<?> permApiClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".PermissionAPI", loader
            );
            Class<?> permTypesClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".nodes.PermissionTypes", loader
            );
            Class<?> permNodeClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".nodes.PermissionNode", loader
            );
            Class<?> permTypeClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".nodes.PermissionType", loader
            );
            Class<?> permResolverClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".nodes.PermissionNode$PermissionResolver", loader
            );
            Class<?> dynamicKeyClass = VanillaModBridge.tryLoadClass(
                    permissionPackage + ".nodes.PermissionDynamicContextKey", loader
            );
            if (gatherNodesClass == null || permApiClass == null || permTypesClass == null
                    || permNodeClass == null || permTypeClass == null || permResolverClass == null
                    || dynamicKeyClass == null) {
                return;
            }

            Object booleanPermType = permTypesClass.getField("BOOLEAN").get(null);
            Object emptyDynamicKeys = Array.newInstance(dynamicKeyClass, 0);
            Constructor<?> nodeCtor = permNodeClass.getConstructor(
                    String.class,
                    String.class,
                    permTypeClass,
                    permResolverClass,
                    emptyDynamicKeys.getClass()
            );

            Object defaultResolver = Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{permResolverClass},
                    (proxy, method, args) -> {
                        if ("equals".equals(method.getName())) return proxy == args[0];
                        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                        if ("toString".equals(method.getName())) return "SimpleWhitelistDefaultResolver";
                        Object player = (args != null && args.length > 0) ? args[0] : null;
                        return player != null && hasPlayerOpLevel3(player);
                    }
            );

            final Map<String, Object> nodes = new HashMap<>();
            String prefix = modId + ".";
            for (WhitelistPermission wp : WhitelistPermission.values()) {
                if (wp == WhitelistPermission.ALL) {
                    continue;
                }
                String full = wp.getPermission();
                String nodePath = full.startsWith(prefix) ? full.substring(prefix.length()) : full;
                Object node = nodeCtor.newInstance(modId, nodePath, booleanPermType, defaultResolver, emptyDynamicKeys);
                nodes.put(full, node);
            }

            final Method addNodesArrayMethod = findAddNodesMethod(gatherNodesClass);
            Consumer<Object> gatherListener = event -> {
                if (event == null || addNodesArrayMethod == null) {
                    return;
                }
                try {
                    Object arr = Array.newInstance(permNodeClass, nodes.size());
                    int idx = 0;
                    for (Object n : nodes.values()) {
                        Array.set(arr, idx++, n);
                    }
                    addNodesArrayMethod.invoke(event, arr);
                } catch (Throwable ignored) {
                }
            };

            if (!registerEventListener(eventBusHolderClass, gatherNodesClass, gatherListener, loader)) {
                return;
            }

            final Method getPermMethod = findGetPermissionMethod(permApiClass, permNodeClass);
            if (getPermMethod != null) {
                VanillaModBridge.INSTANCE.setCustomPermissionChecker((source, perm) -> {
                    Object entity = VanillaCommandSourceExecutor.extractEntity(source);
                    if (entity == null || !getPermMethod.getParameterTypes()[0].isInstance(entity)) {
                        return null;
                    }
                    Object node = nodes.get(perm);
                    if (node == null) {
                        return Boolean.FALSE;
                    }
                    try {
                        Object val = getPermMethod.invoke(null, entity, node, emptyDynamicKeys);
                        if (val instanceof Boolean) {
                            return (Boolean) val;
                        }
                    } catch (Throwable ignored) {
                    }
                    return null;
                });
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean registerEventListener(
            String eventBusHolderClass,
            Class<?> eventClass,
            Consumer<Object> listener,
            ClassLoader loader
    ) {
        // Forge 1.21.6+ / 26.x EventBus 7: EventClass.BUS.addListener(Consumer)
        try {
            Field staticBusField = eventClass.getField("BUS");
            Object eventBusObj = staticBusField.get(null);
            if (eventBusObj != null) {
                Method addListener = eventBusObj.getClass().getMethod("addListener", Consumer.class);
                addListener.invoke(eventBusObj, listener);
                return true;
            }
        } catch (Throwable ignored) {
        }

        // Forge 1.13.2 - 1.21.5 & NeoForge 1.20.2 - 26.3: IEventBus.addListener(...)
        try {
            Class<?> holder = VanillaModBridge.tryLoadClass(eventBusHolderClass, loader);
            if (holder == null) {
                return false;
            }
            Object bus = holder.getField("EVENT_BUS").get(null);
            if (bus == null) {
                return false;
            }
            // Prefer addListener(Class<T>, Consumer<T>) if available (NeoForge & modern EventBus)
            for (Method m : bus.getClass().getMethods()) {
                if ("addListener".equals(m.getName()) && m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == Class.class
                        && Consumer.class.isAssignableFrom(m.getParameterTypes()[1])) {
                    m.invoke(bus, eventClass, listener);
                    return true;
                }
            }
            // Classic EventBus 1-6: addListener(EventPriority, boolean, Class<T>, Consumer<T>)
            for (Method m : bus.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ("addListener".equals(m.getName()) && p.length == 4
                        && p[0].isEnum() && p[1] == boolean.class
                        && p[2] == Class.class && Consumer.class.isAssignableFrom(p[3])) {
                    Object normalPriority = p[0].getEnumConstants()[2]; // NORMAL
                    m.invoke(bus, normalPriority, false, eventClass, listener);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasPlayerOpLevel3(Object serverPlayer) {
        for (Method m : serverPlayer.getClass().getMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class
                    && m.getReturnType() == boolean.class) {
                try {
                    return (Boolean) m.invoke(serverPlayer, 3);
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static Method findAddNodesMethod(Class<?> gatherNodesClass) {
        for (Method m : gatherNodesClass.getMethods()) {
            if ("addNodes".equals(m.getName()) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isArray()) {
                return m;
            }
        }
        return null;
    }

    private static Method findGetPermissionMethod(Class<?> permApiClass, Class<?> permNodeClass) {
        for (Method m : permApiClass.getMethods()) {
            if ("getPermission".equals(m.getName()) && m.getParameterCount() == 3
                    && m.getParameterTypes()[1] == permNodeClass && m.getParameterTypes()[2].isArray()) {
                return m;
            }
        }
        return null;
    }
}
