package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record SyncArmaturePacket(int entityId, ResourceLocation resourceLocation) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeResourceLocation(resourceLocation);
    }

    public static SyncArmaturePacket decode(FriendlyByteBuf buf) {
        return new SyncArmaturePacket(buf.readInt(), buf.readResourceLocation());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if(entity != null){
                EFMMArmatures.bindArmature(entity, resourceLocation);
                EFMMMeshes.bindMesh(entity, resourceLocation);
            }
        }
    }

}