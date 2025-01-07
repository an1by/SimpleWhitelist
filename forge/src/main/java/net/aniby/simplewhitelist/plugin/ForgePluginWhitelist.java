package net.aniby.simplewhitelist.plugin;

import com.mojang.authlib.GameProfile;
import net.aniby.simplewhitelist.api.plugin.PluginWhitelist;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.List;

public class ForgePluginWhitelist extends PluginWhitelist {
    public ForgePluginWhitelist() {
        super(null);
    }

    private static GameProfile createProfile(String name) {
        return ServerLifecycleHooks.getCurrentServer().usesAuthentication()
                ? ServerLifecycleHooks.getCurrentServer().getProfileCache().get(name).orElseThrow()
                : UUIDUtil.createOfflineProfile(name);
    }

    private static PlayerList getPlayerList() {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList();
    }

    @Override
    public void reload() {
        getPlayerList().reloadWhiteList();
    }

    @Override
    public boolean isWhitelisted(String playerName) {
        return getPlayerList().getWhiteList().isWhiteListed(createProfile(playerName));
    }

    @Override
    public void addWhitelist(String playerName) {
        getPlayerList().getWhiteList().add(new UserWhiteListEntry(createProfile(playerName)));
    }

    @Override
    public void removeWhitelist(String playerName) {
        getPlayerList().getWhiteList().remove(new UserWhiteListEntry(createProfile(playerName)));
    }

    @Override
    public List<String> getWhitelisted() {
        return Arrays.asList(getPlayerList().getWhiteList().getUserList());
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
    }

    @Override
    public void saveDefault() {
    }
}
