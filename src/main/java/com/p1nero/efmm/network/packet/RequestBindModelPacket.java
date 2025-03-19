package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.efmodel.ServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record RequestBindModelPacket(String modelId) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(modelId);
    }

    public static RequestBindModelPacket decode(FriendlyByteBuf buf) {
        return new RequestBindModelPacket(buf.readUtf());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(player instanceof ServerPlayer serverPlayer){
            ServerModelManager.bindModelSync(serverPlayer, serverPlayer, modelId);
        }
    }

}