package net.aniby.simplewhitelist.forge.plugin;

import com.mojang.authlib.GameProfile;
import net.aniby.simplewhitelist.common.entity.WhitelistHandler;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PluginWhitelist implements WhitelistHandler {
    // Utilities
    private static UUID getOfflineUUID(String name) {
        String requestString = "OfflinePlayer:" + name;
        return UUID.nameUUIDFromBytes(requestString.getBytes(StandardCharsets.UTF_8));
    }
    private static GameProfile createProfile(String name) {
        return new GameProfile(getOfflineUUID(name), name);
    }
    private static UserWhiteList getWhitelist() {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getWhiteList();
    }
    public void reload() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().reloadWhiteList();
    }


    // Logic
    @Override
    public boolean isWhitelisted(String playerName) {
        return getWhitelist().isWhiteListed(createProfile(playerName));
    }

    @Override
    public void addWhitelist(String playerName) {
        getWhitelist().add(new UserWhiteListEntry(createProfile(playerName)));
    }

    @Override
    public void removeWhitelist(String playerName) {
        getWhitelist().remove(new UserWhiteListEntry(createProfile(playerName)));
    }

    @Override
    public List<String> getWhitelisted() {
        return Arrays.asList(getWhitelist().getUserList());
    }
}
