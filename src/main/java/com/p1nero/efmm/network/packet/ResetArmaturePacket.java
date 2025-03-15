package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record ResetArmaturePacket(int entityId) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static ResetArmaturePacket decode(FriendlyByteBuf buf) {
        return new ResetArmaturePacket(buf.readInt());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if(entity != null){
                EFMMArmatures.removeArmature(entity);
                EFMMMeshes.removeMesh(entity);
            }
        }
    }

}