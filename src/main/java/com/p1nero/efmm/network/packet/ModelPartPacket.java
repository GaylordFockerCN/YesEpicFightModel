package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.network.ModelPacketSplitter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class ModelPartPacket implements BasePacket{
    private final FriendlyByteBuf buf;

    public ModelPartPacket(FriendlyByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBytes(this.buf);
    }

    public static ModelPartPacket decode(FriendlyByteBuf buf){
        System.out.println(buf.getInt(0));
        return new ModelPartPacket(buf);
    }

    @Override
    public void execute(@Nullable Player player) {
        ModelPacketSplitter.getInstance().merge(player == null ? null : player.getUUID(), buf, registerModelPacket -> registerModelPacket.execute(player));
    }

}
