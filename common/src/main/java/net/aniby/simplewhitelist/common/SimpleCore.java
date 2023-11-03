package net.aniby.simplewhitelist.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleCore {
    public static final String permission = "simplewhitelist.command";
    private static SimpleConfiguration configuration;

    public static SimpleConfiguration getConfiguration() {
        return configuration;
    }

    public static void init(Path path) throws IOException {
        configuration = new SimpleConfiguration(path);
    }

    public static List<String> getCompleter(String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 0, 1 -> list = configuration.getCommandMessages().keySet().stream().filter(
                    n -> args.length == 0 || n.startsWith(args[0])
            ).collect(Collectors.toList());
            case 2 -> {
                if (args[0].equals("remove")) {
                    list = configuration.getWhitelisted().stream()
                            .filter(c -> args[1].isEmpty() || c.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        return list;
    }

    // true = null
    // else = sendMessage
    public static String checkSubcommand(String subcommand, boolean permission) {
        String subMessage = configuration.getCommandMessages().get(subcommand);
        if (subMessage == null) {
            return configuration.getMessages().get("invalid_arguments");
        }

        if (!permission) {
            return configuration.getMessages().get("no_permission");
        }
        return null;
    }
}
