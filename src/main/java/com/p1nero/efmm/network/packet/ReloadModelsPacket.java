package com.p1nero.efmm.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;

public record ReloadModelsPacket(String modelId, JsonObject newModel) implements BasePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(modelId);
        try {
            buf.writeNbt(TagParser.parseTag(newModel.toString()));
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to parse json to nbt!", e);
        }
    }

    public static ReloadModelsPacket decode(FriendlyByteBuf buf) {
        return new ReloadModelsPacket(buf.readUtf(), JsonParser.parseString(Objects.requireNonNull(buf.readNbt()).toString()).getAsJsonObject());
    }

    @Override
    public void execute(@Nullable Player player) {
        //TODO 接收来自服务端的
    }

}