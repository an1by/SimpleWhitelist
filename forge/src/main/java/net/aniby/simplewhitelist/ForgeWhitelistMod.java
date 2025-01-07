package net.aniby.simplewhitelist;

import net.aniby.simplewhitelist.event.CommandRegistrationHandler;
import net.aniby.simplewhitelist.plugin.ForgePluginConfiguration;
import net.aniby.simplewhitelist.plugin.ForgePluginWhitelist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod(ForgeWhitelistMod.MOD_ID)
public class ForgeWhitelistMod extends WhitelistMod {
    public static final String MOD_ID = "simplewhitelist";

    private final ForgePluginWhitelist whitelist;

    private final ForgePluginConfiguration configuration;

    @Override
    public ForgePluginWhitelist getWhitelist() {
        return this.whitelist;
    }

    @Override
    public ForgePluginConfiguration getConfiguration() {
        return this.configuration;
    }

    public ForgeWhitelistMod() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

        this.whitelist = new ForgePluginWhitelist();
        this.configuration = new ForgePluginConfiguration(path.resolve("config.json"));

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);

        MinecraftForge.EVENT_BUS.register(new CommandRegistrationHandler(this));
    }
}
