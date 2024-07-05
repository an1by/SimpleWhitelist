package net.aniby.simplewhitelist.forge;

import com.mojang.logging.LogUtils;
import net.aniby.simplewhitelist.forge.event.CommandRegistrationHandler;
import net.aniby.simplewhitelist.forge.plugin.PluginConfiguration;
import net.aniby.simplewhitelist.forge.plugin.PluginWhitelist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;

@Mod(SimpleWhitelist.MOD_ID)
public class SimpleWhitelist {
    private final PluginWhitelist whitelist;
    private final PluginConfiguration configuration;

    public PluginWhitelist whitelist() {
        return whitelist;
    }
    public PluginConfiguration configuration() {
        return configuration;
    }

    public static final String MOD_ID = "simplewhitelist";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleWhitelist() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        whitelist = new PluginWhitelist();
        configuration = new PluginConfiguration(path);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);

        MinecraftForge.EVENT_BUS.register(new CommandRegistrationHandler(this));
    }

}
