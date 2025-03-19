package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public record AuthModelPacket(boolean isRemove, List<String> modelIds) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isRemove);
        buf.writeInt(modelIds.size());
        for (String s : modelIds) {
            buf.writeUtf(s);
        }
    }

    public static AuthModelPacket decode(FriendlyByteBuf buf) {
        boolean isRemove = buf.readBoolean();
        int length = buf.readInt();
        List<String> models = new ArrayList<>();
        while (length --> 0){
            models.add(buf.readUtf());
        }
        return new AuthModelPacket(isRemove, models);
    }

    @Override
    public void execute(@Nullable Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            for(String modelId: modelIds){
                if(isRemove) {
                    ClientModelManager.removeAuthModel(modelId);
                } else {
                    ClientModelManager.authModel(modelId);
                }
            }
        }
    }

}