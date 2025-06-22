package net.aniby.simplewhitelist.event;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ServerPlayConnectionEvent extends Event implements ICancellableEvent {
    private final Connection connection;
    private final ServerPlayer player;
    private final CommonListenerCookie cookie;
    private final CallbackInfo callbackInfo;

    public ServerPlayConnectionEvent(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo callbackInfo) {
        this.connection = connection;
        this.player = player;
        this.cookie = cookie;
        this.callbackInfo = callbackInfo;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public CommonListenerCookie getCookie() {
        return this.cookie;
    }

    public void cancel() {
        this.callbackInfo.cancel();
        ICancellableEvent.super.setCanceled(true);
    }
}
