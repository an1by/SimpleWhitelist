package net.aniby.simplewhitelist.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.aniby.simplewhitelist.common.SimpleCore;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhitelistCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length >= 1) {
            String result = SimpleCore.checkSubcommand(args[0], source.hasPermission(SimpleCore.permission));
            if (result == null) {
                String subMessage = SimpleCore.getConfiguration().getCommandMessages().get(args[0]);
                switch (args[0]) {
                    case "reload" -> {
                        try {
                            SimpleCore.getConfiguration().load();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    case "add", "remove" -> {
                        if (args.length < 2) {
                            subMessage = SimpleCore.getConfiguration().getMessages().get("need_player");
                        } else {
                            String name = args[1];
                            if (args[0].equals("add")) {
                                if (!SimpleCore.getConfiguration().whitelistAdd(name)) {
                                    source.sendMessage(Component.text(
                                            SimpleCore.getConfiguration().getMessages().get("already_whitelisted")
                                    ));
                                }
                            }
                            else SimpleCore.getConfiguration().whitelistRemove(name);
                            subMessage = subMessage.replace("<name>", name);
                        }
                    }
                    case "enable" -> SimpleCore.getConfiguration().setEnabled(true);
                    case "disable" -> SimpleCore.getConfiguration().setEnabled(false);
                    case "list" -> subMessage = subMessage.replace("<list>", SimpleCore.getConfiguration().getWhitelistedString());
                }
                source.sendMessage(Component.text(
                        subMessage
                ));
                return;
            }
            source.sendMessage(Component.text(result));
            return;
        }
        source.sendMessage(Component.text(
                SimpleCore.getConfiguration().getMessages().get("invalid_arguments")
        ));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        CommandSource sender = invocation.source();
        List<String> list = new ArrayList<>();
        if (sender.hasPermission(SimpleCore.permission)) {
            String[] args = invocation.arguments();
            list = SimpleCore.getCompleter(args);
        }
        return CompletableFuture.completedFuture(list);
    }
}
