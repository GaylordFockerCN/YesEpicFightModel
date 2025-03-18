package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record ResetClientModelPacket(int entityId) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static ResetClientModelPacket decode(FriendlyByteBuf buf) {
        return new ResetClientModelPacket(buf.readInt());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if(entity != null){
                ClientModelManager.removeModelFor(entity);
            }
        }
    }

}