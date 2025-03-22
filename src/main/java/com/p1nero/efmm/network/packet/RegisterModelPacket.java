package com.p1nero.efmm.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    private byte[] pbrN;
    private final byte[] pbrS;
    private byte[] modelJsonBytes;
    private byte[] configJsonBytes;
    private static final Logger LOGGER = LogUtils.getLogger();

    public RegisterModelPacket(String modelId, @NotNull JsonObject modelJsonCache,  @NotNull JsonObject configJsonCache, byte[] imageCache, byte[] pbrN, byte[] pbrS) {
        this.modelId = modelId;
        this.modelJsonCache = modelJsonCache;
        this.configJsonCache = configJsonCache;
        this.imageCache = imageCache;
        this.pbrN = pbrN;
        this.pbrS = pbrS;
    }

    public RegisterModelPacket(String modelId, byte[] modelJsonBytes, byte[] configJsonBytes, byte[] imageCache, byte[] pbrN, byte[] pbrS) {
        this.modelId = modelId;
        this.modelJsonBytes = modelJsonBytes;
        this.configJsonBytes = configJsonBytes;
        this.imageCache = imageCache;
        this.pbrN = pbrN;
        this.pbrS = pbrS;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(modelId);

        byte[] modelJsonBytes = modelJsonCache.toString().getBytes(StandardCharsets.UTF_8);

        writeSegmentedData(buf, modelJsonBytes);

        byte[] configJsonBytes = configJsonCache.toString().getBytes(StandardCharsets.UTF_8);
        writeSegmentedData(buf, configJsonBytes);

        writeSegmentedData(buf, imageCache);

        writeSegmentedData(buf, pbrN == null ? new byte[0] : pbrN);
        writeSegmentedData(buf, pbrS == null ? new byte[0] : pbrS);

    }

    private void writeSegmentedData(FriendlyByteBuf buf, byte[] data) {
        int segmentSize = 32768 - 20; // 天道禁锢，但仅单人
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
        byte[] pbrN = readSegmentedData(buf);
        byte[] pbrS = readSegmentedData(buf);
        return new RegisterModelPacket(modelId, modelJsonBytes, configJsonBytes, imageCache, pbrN, pbrS);
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
    public void execute(@NotNull Player player) {
        if(player instanceof ServerPlayer serverPlayer) {
            if(ServerModelManager.UPLOAD_WHITE_LIST.contains(serverPlayer.getUUID())){
                JsonObject modelJson = parseJson(new String(modelJsonBytes, StandardCharsets.UTF_8));
                JsonObject configJson = parseJson(new String(configJsonBytes, StandardCharsets.UTF_8));
                ServerModelManager.registerModel(serverPlayer, modelId, modelJson, configJson, imageCache, pbrN, pbrS);
                return;
            }
            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.sender_no_permission"), false);
            LOGGER.info("Sender don't have permission!");
        } else {
            if(ClientModelManager.MODELS_BLACK_LIST.contains(modelId)){
                player.displayClientMessage(Component.translatable("tip.efmm.model_to_large", modelId), false);
                return;
            }
            JsonObject modelJson = parseJson(new String(modelJsonBytes, StandardCharsets.UTF_8));
            JsonObject configJson = parseJson(new String(configJsonBytes, StandardCharsets.UTF_8));
            ClientModelManager.registerModelFromServer(modelId, modelJson, configJson, imageCache, pbrN, pbrS);
        }
    }

}