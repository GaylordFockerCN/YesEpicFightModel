package com.p1nero.efmm.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface BasePacket {
    void encode(FriendlyByteBuf var1);

    default void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context con = context.get();
        con.enqueueWork(() -> this.execute(con.getSender()));
        con.setPacketHandled(true);
    }

    void execute(Player var1);

}
