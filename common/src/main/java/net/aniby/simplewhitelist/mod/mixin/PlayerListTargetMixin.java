package net.aniby.simplewhitelist.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.minecraft.server.players.PlayerList",
        "net.minecraft.class_3324",
        "net.minecraft.server.management.PlayerList"
})
public abstract class PlayerListTargetMixin {
}
