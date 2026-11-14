package net.aniby.simplewhitelist.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.minecraft.commands.Commands",
        "net.minecraft.class_2170",
        "net.minecraft.command.Commands"
})
public abstract class CommandsTargetMixin {
}
