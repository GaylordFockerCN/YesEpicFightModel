package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.efmodel.LogicServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record RequestResetModelPacket() implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {}

    public static RequestResetModelPacket decode(FriendlyByteBuf buf) {
        return new RequestResetModelPacket();
    }

    @Override
    public void execute(@Nullable Player player) {
        if(player instanceof ServerPlayer serverPlayer){
            LogicServerModelManager.removeModelForSync(serverPlayer, serverPlayer);
        }
    }

}