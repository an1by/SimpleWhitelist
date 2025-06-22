package net.aniby.simplewhitelist.mixin;

import net.aniby.simplewhitelist.event.ServerPlayConnectionEvent;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerAbilitiesPacket;<init>(Lnet/minecraft/world/entity/player/Abilities;)V"))
    private void handlePlayerConnection(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new ServerPlayConnectionEvent(connection, player, cookie, ci));
    }
}
