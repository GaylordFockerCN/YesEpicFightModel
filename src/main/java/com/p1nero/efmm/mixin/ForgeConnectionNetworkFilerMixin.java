package com.p1nero.efmm.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.filters.ForgeConnectionNetworkFilter;
import net.minecraftforge.network.filters.VanillaPacketSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(value = ForgeConnectionNetworkFilter.class, remap = false)
public class ForgeConnectionNetworkFilerMixin {

    @ModifyExpressionValue(method = "buildHandlers", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;builder()Lcom/google/common/collect/ImmutableMap$Builder;"))
    private static ImmutableMap.Builder<Class<? extends Packet<?>>, BiConsumer<Packet<?>, List<? super Packet<?>>>> efmm$build(ImmutableMap.Builder<Class<? extends Packet<?>>, BiConsumer<Packet<?>, List<? super Packet<?>>>> original){
        original.put(RegisterModelPacket.class, ((packet, out) -> {
            VanillaPacketSplitter.appendPackets(
                    ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, packet, out
            );
        }));
        return original;
    }

}
