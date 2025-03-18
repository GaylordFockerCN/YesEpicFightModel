package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record BindModelPacket(int entityId, String modelId) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(modelId);
    }

    public static BindModelPacket decode(FriendlyByteBuf buf) {
        return new BindModelPacket(buf.readInt(), buf.readUtf());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if(entity != null){
                ClientModelManager.bindModelFor(entity, modelId);
            }
        }
    }

}