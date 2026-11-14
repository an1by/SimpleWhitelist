package net.minecraftforge.fml.common.network;

import net.minecraft.network.INetHandler;

public class FMLNetworkEvent {
    public static class ServerConnectionFromClientEvent {
        public INetHandler getHandler() {
            return null;
        }
    }
}
