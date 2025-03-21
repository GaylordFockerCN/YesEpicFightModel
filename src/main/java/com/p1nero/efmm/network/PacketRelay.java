package com.p1nero.efmm.network;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.network.packet.BasePacket;
import com.p1nero.efmm.network.packet.ModelPartPacket;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID)
public class PacketRelay {

    public static <MSG> void sendToPlayer(SimpleChannel handler, MSG message, ServerPlayer player) {
        handler.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToNear(SimpleChannel handler, MSG message, double x, double y, double z, double radius, ResourceKey<Level> dimension) {
        handler.send(PacketDistributor.NEAR.with(TargetPoint.p(x, y, z, radius, dimension)), message);
    }

    public static <MSG> void sendToAll(SimpleChannel handler, MSG message) {
        handler.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToServer(SimpleChannel handler, MSG message) {
        handler.sendToServer(message);
    }

    public static <MSG> void sendToDimension(SimpleChannel handler, MSG message, ResourceKey<Level> dimension) {
        handler.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }
    public static void sendModelToServer(RegisterModelPacket message) {
        split(message, (modelPartPacket -> sendToServer(PacketHandler.MODEL_CHANNEL, modelPartPacket)), LogicalSide.CLIENT);
    }

    public static void sendModelToPlayer(RegisterModelPacket message, ServerPlayer serverPlayer) {
        split(message, (modelPartPacket -> sendToPlayer(PacketHandler.MODEL_CHANNEL, modelPartPacket, serverPlayer)), LogicalSide.SERVER);
    }

    public static void split(final RegisterModelPacket message, Consumer<BasePacket> sender, LogicalSide originalSide) {
        // we need to reserve enough capacity add header/footer data.
//        var partSize = (originalSide == LogicalSide.CLIENT ? 32768 : 1048576) - 256;
        var partSize = 32768 - 256;
        ModelPacketSplitter.getInstance().split(message, ModelPartPacket::new, partSize, (packet -> {
            if(packet == null){
                return;
            }
            sender.accept(packet);
        }));
    }

}
