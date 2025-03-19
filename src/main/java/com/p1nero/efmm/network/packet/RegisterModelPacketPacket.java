package com.p1nero.efmm.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 在客户端注册并缓存模型
 * TODO 加密解密
 */
public record RegisterModelPacketPacket(String key, String modelId, JsonObject modelJsonCache, JsonObject configJsonCache, byte[] imageCache) implements BasePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(key);
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

    public static RegisterModelPacketPacket decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        String modelId = buf.readUtf();

        byte[] modelJsonBytes = readSegmentedData(buf);
        JsonObject modelJson = parseJson(new String(modelJsonBytes, StandardCharsets.UTF_8));

        byte[] configJsonBytes = readSegmentedData(buf);
        JsonObject configJson = parseJson(new String(configJsonBytes, StandardCharsets.UTF_8));

        byte[] imageCache = readSegmentedData(buf);
        return new RegisterModelPacketPacket(key, modelId, modelJson, configJson, imageCache);
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
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            ClientModelManager.registerModel(modelId, modelJsonCache, configJsonCache, imageCache);
        }
    }

}