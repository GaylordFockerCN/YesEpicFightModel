package com.p1nero.efmm.network;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final PacketHandler INSTANCE = new PacketHandler();
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "custom_payload");

    public static final SimpleChannel MAIN_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    private static int index;

    public static void register() {

        var channel = NetworkRegistry.ChannelBuilder
                .named(CHANNEL_NAME)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(sv -> true)
                .serverAcceptedVersions(cv -> cv.equals(PROTOCOL_VERSION))
                .eventNetworkChannel();
        channel.registerObject(INSTANCE);

        //client
        register(BindModelPacket.class, BindModelPacket::decode);
        register(ResetClientModelPacket.class, ResetClientModelPacket::decode);
        register(AuthModelPacket.class, AuthModelPacket::decode);

        //server
        register(RequestSyncModelPacket.class, RequestSyncModelPacket::decode);
        register(RequestBindModelPacket.class, RequestBindModelPacket::decode);
        register(RequestResetModelPacket.class, RequestResetModelPacket::decode);
    }

    @SubscribeEvent
    public void onServerEvent(final NetworkEvent.ClientCustomPayloadEvent event) {
        var context = event.getSource().get();
        var player = context.getSender();
        if (player == null) {
            return;
        }

        FriendlyByteBuf friendlyByteBuf = event.getPayload();
        ModelPacketSplitter.getInstance().merge(player.getUUID(), friendlyByteBuf, (registerModelPacket -> context.enqueueWork(() -> registerModelPacket.execute(player))));
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientEvent(final NetworkEvent.ServerCustomPayloadEvent event) {
        if (event instanceof NetworkEvent.ServerCustomPayloadLoginEvent || Minecraft.getInstance().player == null) {
            return;
        }
        var context = event.getSource().get();
        FriendlyByteBuf friendlyByteBuf = event.getPayload();
        ModelPacketSplitter.getInstance().merge(null, friendlyByteBuf, (registerModelPacket -> context.enqueueWork(() -> registerModelPacket.execute(Minecraft.getInstance().player))));
        context.setPacketHandled(true);
    }

    private static <MSG extends BasePacket> void register(final Class<MSG> packet, Function<FriendlyByteBuf, MSG> decoder) {
        MAIN_CHANNEL.messageBuilder(packet, index++).encoder(BasePacket::encode).decoder(decoder).consumerMainThread(BasePacket::handle).add();
    }

}
