package net.aniby.simplewhitelist;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class Commands {
    public static LiteralArgumentBuilder<WhitelistProfile> literal(String string) {
        return LiteralArgumentBuilder.literal(string);
    }

    public static <T> RequiredArgumentBuilder<WhitelistProfile, T> argument(String string, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(string, argumentType);
    }
}
