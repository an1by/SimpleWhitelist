package net.aniby.simplewhitelist.mixin;

import com.mojang.authlib.GameProfile;
import net.aniby.simplewhitelist.event.ServerPlayConnectionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "canPlayerLogin", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void handlePlayerConnection(SocketAddress socketAddress, GameProfile gameProfile,
                                        CallbackInfoReturnable<Component> cir) {
        NeoForge.EVENT_BUS.post(new ServerPlayConnectionEvent(socketAddress, gameProfile, cir));
    }
}
