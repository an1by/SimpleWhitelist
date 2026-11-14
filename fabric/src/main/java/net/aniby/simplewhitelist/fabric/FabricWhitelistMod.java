package net.aniby.simplewhitelist.fabric;

import net.aniby.simplewhitelist.mod.VanillaModBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FabricWhitelistMod implements ModInitializer {
    @Override
    public void onInitialize() {
        VanillaModBridge.INSTANCE.init(
                FabricLoader.getInstance().getConfigDir().resolve("simplewhitelist")
        );
    }
}
