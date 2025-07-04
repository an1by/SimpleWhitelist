package net.aniby.simplewhitelist.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

public class ServerPlayConnectionEvent extends Event implements ICancellableEvent {
    private final SocketAddress socketAddress;
    private final GameProfile gameProfile;
    private final CallbackInfoReturnable<Component> callback;

    public ServerPlayConnectionEvent(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> callback) {
        this.socketAddress = socketAddress;
        this.gameProfile = gameProfile;
        this.callback = callback;
    }

    public SocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public void cancel(@Nullable Component component) {
        this.callback.setReturnValue(component);
        ICancellableEvent.super.setCanceled(true);
    }
}
