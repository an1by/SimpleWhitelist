package net.aniby.simplewhitelist.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import net.aniby.simplewhitelist.api.WhitelistPlugin;
import net.aniby.simplewhitelist.configuration.WhitelistConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RawCommandRunner<S> {
    private final CommandDispatcher<S> dispatcher;
    private final WhitelistCommandSourceExecutor<S> executor;
    private final WhitelistPlugin plugin;

    public RawCommandRunner(WhitelistCommandSourceExecutor<S> executor, WhitelistPlugin plugin) {
        this.executor = executor;
        this.plugin = plugin;
        this.dispatcher = new CommandDispatcher<>();
        this.dispatcher.register(new WhitelistCommand<>(executor, plugin).builder());
    }

    public WhitelistCommandSourceExecutor<S> getExecutor() {
        return this.executor;
    }

    public boolean hasPermission(S source) {
        return WhitelistPermission.hasAnyPermission(source, this.executor);
    }

    public boolean execute(S source, String[] args) {
        String joined = (args == null || args.length == 0) ? "" : String.join(" ", args);
        return this.execute(source, joined);
    }

    public boolean execute(S source, String rawArgs) {
        WhitelistConfiguration configuration = this.plugin.getConfiguration();
        if (!this.hasPermission(source)) {
            this.executor.sendFailure(source, configuration.getMessage("noPermission"));
            return false;
        }

        String trimmed = rawArgs == null ? "" : rawArgs.trim();
        if (!trimmed.isEmpty()) {
            int spaceIdx = trimmed.indexOf(' ');
            String firstToken = (spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx)).toLowerCase(Locale.ROOT);
            WhitelistPermission requiredPerm = subcommandPermission(firstToken);
            if (requiredPerm != null && !requiredPerm.hasPermission(source, this.executor)) {
                this.executor.sendFailure(source, configuration.getMessage("noPermission"));
                return false;
            }
        }

        String input = trimmed.isEmpty() ? "simplewhitelist" : "simplewhitelist " + trimmed;
        try {
            return this.dispatcher.execute(input, source) > 0;
        } catch (CommandSyntaxException e) {
            if ("add".equalsIgnoreCase(trimmed) || "remove".equalsIgnoreCase(trimmed)) {
                this.executor.sendFailure(source, configuration.getMessage("needPlayer"));
            } else {
                this.executor.sendFailure(source, configuration.getMessage("invalidArguments"));
            }
            return false;
        }
    }

    private static WhitelistPermission subcommandPermission(String subcommand) {
        switch (subcommand) {
            case "add":
                return WhitelistPermission.ADD;
            case "remove":
                return WhitelistPermission.REMOVE;
            case "list":
                return WhitelistPermission.LIST;
            case "enable":
                return WhitelistPermission.ENABLE;
            case "disable":
                return WhitelistPermission.DISABLE;
            case "reload":
                return WhitelistPermission.RELOAD;
            default:
                return null;
        }
    }

    public List<String> suggest(S source, String[] args) {
        if (args == null || args.length == 0) {
            return this.suggest(source, "");
        }
        return this.suggest(source, String.join(" ", args));
    }

    public List<String> suggest(S source, String rawArgs) {
        if (!this.hasPermission(source)) {
            return Collections.emptyList();
        }
        String normalized = rawArgs == null ? "" : stripLeadingSpaces(rawArgs);
        String input = "simplewhitelist " + normalized;
        ParseResults<S> parseResults = this.dispatcher.parse(input, source);
        boolean completingSubcommand = normalized.indexOf(' ') < 0;
        return this.dispatcher.getCompletionSuggestions(parseResults)
                .join()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .filter(text -> {
                    if (!completingSubcommand) {
                        return true;
                    }
                    WhitelistPermission perm = subcommandPermission(text.toLowerCase(Locale.ROOT));
                    return perm == null || perm.hasPermission(source, this.executor);
                })
                .collect(Collectors.toList());
    }

    private static String stripLeadingSpaces(String value) {
        int i = 0;
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        return value.substring(i);
    }
}
