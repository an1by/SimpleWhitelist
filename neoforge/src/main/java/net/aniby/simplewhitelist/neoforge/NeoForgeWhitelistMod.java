package net.aniby.simplewhitelist.neoforge;

import net.aniby.simplewhitelist.mod.FmlPermissionBridge;
import net.aniby.simplewhitelist.mod.VanillaModBridge;
import net.neoforged.fml.common.Mod;

import java.nio.file.Path;

@Mod("simplewhitelist")
public class NeoForgeWhitelistMod {
    public NeoForgeWhitelistMod() {
        Path configDir = FmlPermissionBridge.resolveFmlConfigDir(
                "net.neoforged.fml.loading.FMLPaths",
                "simplewhitelist"
        );
        VanillaModBridge.INSTANCE.init(configDir);

        FmlPermissionBridge.installPermissionApi(
                "simplewhitelist",
                "net.neoforged.neoforge.common.NeoForge",
                "net.neoforged.neoforge.server.permission",
                NeoForgeWhitelistMod.class.getClassLoader()
        );
    }
}
