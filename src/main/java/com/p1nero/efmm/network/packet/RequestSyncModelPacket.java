package com.p1nero.efmm.network.packet;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.efmodel.LogicServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;

public record RequestSyncModelPacket(String modelId) implements BasePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(modelId);
    }

    public static RequestSyncModelPacket decode(FriendlyByteBuf buf) {
        return new RequestSyncModelPacket(buf.readUtf());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(player instanceof ServerPlayer serverPlayer){
            try {
                LogicServerModelManager.sendModelTo(serverPlayer, modelId);
            } catch (IOException e){
                LOGGER.error("Failed to send model!", e);
            }
        }
    }

}