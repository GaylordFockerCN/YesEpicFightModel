package com.p1nero.efmm.network;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static final SimpleChannel MODEL_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "model"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    private static int index, indexOfModelChannel;

    public static void register() {
        MODEL_CHANNEL.messageBuilder(ModelPartPacket.class, indexOfModelChannel++).encoder(ModelPartPacket::encode).decoder(ModelPartPacket::decode).consumerMainThread(BasePacket::handle).add();
        MODEL_CHANNEL.messageBuilder(RegisterModelPacket.class, indexOfModelChannel++).encoder(RegisterModelPacket::encode).decoder(RegisterModelPacket::decode).consumerMainThread(BasePacket::handle).add();

        //client
        register(BindModelPacket.class, BindModelPacket::decode);
        register(ResetClientModelPacket.class, ResetClientModelPacket::decode);
        register(AuthModelPacket.class, AuthModelPacket::decode);

        //server
        register(RequestSyncModelPacket.class, RequestSyncModelPacket::decode);
        register(RequestBindModelPacket.class, RequestBindModelPacket::decode);
        register(RequestResetModelPacket.class, RequestResetModelPacket::decode);
    }

    private static <MSG extends BasePacket> void register(final Class<MSG> packet, Function<FriendlyByteBuf, MSG> decoder) {
        INSTANCE.messageBuilder(packet, index++).encoder(BasePacket::encode).decoder(decoder).consumerMainThread(BasePacket::handle).add();
    }

}
