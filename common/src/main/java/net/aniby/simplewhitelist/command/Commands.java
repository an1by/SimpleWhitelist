package net.aniby.simplewhitelist.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class Commands<S> {
    public LiteralArgumentBuilder<S> literal(String string) {
        return LiteralArgumentBuilder.literal(string);
    }

    public  <T> RequiredArgumentBuilder<S, T> argument(String string, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(string, argumentType);
    }
}
