package com.p1nero.efmm.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.LogicServerModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RegisterModelPacket implements BasePacket {
    private final String modelId;
    private JsonObject modelJsonCache, configJsonCache;
    private final byte[] imageCache;
    private byte[] modelJsonBytes;
    private byte[] configJsonBytes;
    private static final Logger LOGGER = LogUtils.getLogger();


    public RegisterModelPacket(String modelId, @NotNull JsonObject modelJsonCache,  @NotNull JsonObject configJsonCache, byte[] imageCache) {
        this.modelId = modelId;
        this.modelJsonCache = modelJsonCache;
        this.configJsonCache = configJsonCache;
        this.imageCache = imageCache;
    }

    public RegisterModelPacket(String modelId, byte[] modelJsonBytes, byte[] configJsonBytes, byte[] imageCache) {
        this.modelId = modelId;
        this.modelJsonBytes = modelJsonBytes;
        this.configJsonBytes = configJsonBytes;
        this.imageCache = imageCache;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(modelId);

        byte[] modelJsonBytes = modelJsonCache.toString().getBytes(StandardCharsets.UTF_8);
        writeSegmentedData(buf, modelJsonBytes);

        byte[] configJsonBytes = configJsonCache.toString().getBytes(StandardCharsets.UTF_8);
        writeSegmentedData(buf, configJsonBytes);

        writeSegmentedData(buf, imageCache);
    }

    private void writeSegmentedData(FriendlyByteBuf buf, byte[] data) {
        int segmentSize = 32768; // 天道禁锢
        int totalSegments = (data.length + segmentSize - 1) / segmentSize; // 计算总段数

        buf.writeInt(totalSegments);

        for (int i = 0; i < totalSegments; i++) {
            int start = i * segmentSize;
            int end = Math.min(start + segmentSize, data.length);
            byte[] segment = Arrays.copyOfRange(data, start, end);
            buf.writeByteArray(segment);
        }
    }

    public static RegisterModelPacket decode(FriendlyByteBuf buf) {
        String modelId = buf.readUtf();
        byte[] modelJsonBytes = readSegmentedData(buf);
        byte[] configJsonBytes = readSegmentedData(buf);
        byte[] imageCache = readSegmentedData(buf);
        return new RegisterModelPacket(modelId, modelJsonBytes, configJsonBytes, imageCache);
    }

    private static byte[] readSegmentedData(FriendlyByteBuf buf) {
        int totalSegments = buf.readInt();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < totalSegments; i++) {
            byte[] segment = buf.readByteArray();
            outputStream.write(segment, 0, segment.length);
        }

        return outputStream.toByteArray();
    }

    private static JsonObject parseJson(String jsonStr) {
        try (JsonReader reader = new JsonReader(new StringReader(jsonStr))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to parse JSON", e);
            return new JsonObject();
        }
    }

    @Override
    public void execute(@Nullable Player player) {
        if(player instanceof ServerPlayer serverPlayer) {
            if(LogicServerModelManager.UPLOAD_WHITE_LIST.contains(serverPlayer.getUUID())){
                JsonObject modelJson = parseJson(new String(modelJsonBytes, StandardCharsets.UTF_8));
                JsonObject configJson = parseJson(new String(configJsonBytes, StandardCharsets.UTF_8));
                LogicServerModelManager.registerModel(serverPlayer, modelId, modelJson, configJson, imageCache);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("tip.efmm.sender_no_permission"), false);
                LOGGER.info("Sender don't have permission!");
            }
        } else {
            if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
                JsonObject modelJson = parseJson(new String(modelJsonBytes, StandardCharsets.UTF_8));
                JsonObject configJson = parseJson(new String(configJsonBytes, StandardCharsets.UTF_8));
                ClientModelManager.registerModel(modelId, modelJson, configJson, imageCache);
            }
        }
    }

}