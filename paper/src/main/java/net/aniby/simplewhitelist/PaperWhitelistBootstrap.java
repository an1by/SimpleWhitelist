package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.aniby.simplewhitelist.command.WhitelistCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperWhitelistBootstrap implements PluginBootstrap {
    private final WhitelistCommand<CommandSourceStack> command = new WhitelistCommand<>(
            new PaperCommandSourceExecutor(), null
    );

    @Override
    public void bootstrap(BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> lifecycleManager = context.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, (event) -> {
            CommandDispatcher<CommandSourceStack> dispatcher = event.registrar().getDispatcher();
            this.command.register(dispatcher);
        });
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        PaperWhitelistPlugin plugin = new PaperWhitelistPlugin();
        this.command.plugin(plugin);
        return plugin;
    }
}
