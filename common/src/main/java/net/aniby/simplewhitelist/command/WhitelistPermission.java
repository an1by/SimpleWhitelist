package net.aniby.simplewhitelist.command;

import java.util.Arrays;

public enum WhitelistPermission {
    BASE("simplewhitelist.command"),
    ALL("simplewhitelist.command.*"),
    ADD("simplewhitelist.command.add"),
    REMOVE("simplewhitelist.command.remove"),
    LIST("simplewhitelist.command.list"),
    ENABLE("simplewhitelist.command.enable"),
    DISABLE("simplewhitelist.command.disable"),
    RELOAD("simplewhitelist.command.reload");

    private final String permission;

    WhitelistPermission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return this.permission;
    }

    public <S> boolean hasPermission(S source, WhitelistCommandSourceExecutor<S> executor) {
        return executor.hasPermission(source, this.permission)
                || executor.hasPermission(source, BASE.permission)
                || executor.hasPermission(source, ALL.permission);
    }

    public static <S> boolean hasAnyPermission(S source, WhitelistCommandSourceExecutor<S> executor) {
        return Arrays.stream(values()).anyMatch(p -> executor.hasPermission(source, p.permission));
    }
}
