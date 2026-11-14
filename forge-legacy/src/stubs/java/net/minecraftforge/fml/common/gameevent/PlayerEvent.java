package net.minecraftforge.fml.common.gameevent;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerEvent {
    public static class PlayerLoggedInEvent {
        public EntityPlayer player;
    }
}
