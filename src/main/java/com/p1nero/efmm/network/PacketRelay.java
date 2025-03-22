package com.p1nero.efmm.network;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.MutablePair;

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
        split(message, PacketDistributor.SERVER.noArg());
    }

    public static void sendModelToPlayer(RegisterModelPacket message, ServerPlayer serverPlayer) {
        split(message, PacketDistributor.PLAYER.with(() -> serverPlayer));
    }

    public static void split(final RegisterModelPacket message, PacketDistributor.PacketTarget target) {
        // we need to reserve enough capacity add header/footer data.
//        var partSize = (target.getDirection().getOriginationSide() == LogicalSide.CLIENT ? 32768 : 1048576) - 256;
        var partSize = 32768 - 256;
        ModelPacketSplitter.getInstance().split(message, friendlyByteBuf -> target.getDirection().buildPacket(new MutablePair<>(friendlyByteBuf, 0), PacketHandler.CHANNEL_NAME).getThis(), partSize, (packet -> {
            if(packet == null){
                return;
            }
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(target.getDirection().getOriginationSide());
            executor.submitAsync(() -> {
                target.send(packet);
            });
        }));
    }

}
