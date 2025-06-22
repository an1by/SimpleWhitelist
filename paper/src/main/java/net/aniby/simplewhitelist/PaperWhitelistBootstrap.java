package net.aniby.simplewhitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperWhitelistBootstrap implements PluginBootstrap {
    private final PaperWhitelistPlugin plugin = new PaperWhitelistPlugin();

    @Override
    public void bootstrap(BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> lifecycleManager = context.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, (event) -> {
            Commands registrar = event.registrar();
            RootCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> root
                    = registrar.getDispatcher().getRoot();
            if (root instanceof ApiMirrorRootNode apiMirrorRootNode) {
                CommandDispatcher<CommandSourceStack> dispatcher = apiMirrorRootNode.getDispatcher();
                WhitelistCommand.register(dispatcher, this.plugin);
            }
        });
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return this.plugin;
    }
}
