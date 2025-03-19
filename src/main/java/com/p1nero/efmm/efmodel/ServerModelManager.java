package com.p1nero.efmm.efmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.AuthModelPacket;
import com.p1nero.efmm.network.packet.BindModelPacket;
import com.p1nero.efmm.network.packet.RegisterModelPacketPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

import static net.minecraft.util.datafix.fixes.BlockEntitySignTextStrictJsonFix.GSON;

public class ServerModelManager {
    public static final Map<UUID, Set<String>> ALLOWED_MODELS = new HashMap<>();
    public static final Set<String> NATIVE_MODELS = new HashSet<>();
    public static final Map<String, ModelConfig> ALL_MODELS = new HashMap<>();
    public static final Map<UUID, String> ENTITY_MODEL_MAP = new HashMap<>();

    public static final Path EFMM_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("efmm");
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Set<String> getOrCreateAllowedModelsFor(Entity player) {
        return ALLOWED_MODELS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
    }

    public static Set<String> getAllModels() {
        return ALL_MODELS.keySet();
    }

    public static void authAllAllowedModelToClient(Entity player) throws IOException {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(false, getOrCreateAllowedModelsFor(player).stream().toList()), serverPlayer);
            LOGGER.info("Send all models permission to {}", player.getDisplayName().getString());
            for(String modelId : ALLOWED_MODELS.get(player.getUUID())){
                if(!NATIVE_MODELS.contains(modelId)){
                    sendModelTo(serverPlayer, modelId);
                }
            }
        }
    }

    public static void authModelFor(Entity player, String modelId) {
        getOrCreateAllowedModelsFor(player).add(modelId);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(false, List.of(modelId)), serverPlayer);
            LOGGER.info("Send \"{}\" permission to {}", modelId, player.getDisplayName().getString());
        }
    }

    public static void sendModelTo(ServerPlayer serverPlayer, String modelId) throws IOException {
        PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new RegisterModelPacketPacket(modelId, getModelJsonLoader(modelId).getRootJson(), getModelConfigJsonLoader(modelId).getRootJson(), getModelTexture(modelId)), serverPlayer);
        LOGGER.info("Send model \"{}\" to {}", modelId, serverPlayer.getDisplayName().getString());
    }

    public static void authAllModelFor(Entity player) {
        if (getOrCreateAllowedModelsFor(player).addAll(ALL_MODELS.keySet()) && player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(false, ALL_MODELS.keySet().stream().toList()), serverPlayer);
            LOGGER.info("Send all models permission to {}", player.getDisplayName().getString());
        }
    }

    public static void removeAuthFor(Entity player, String modelId) {
        if (getOrCreateAllowedModelsFor(player).remove(modelId) && player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(true, List.of(modelId)), serverPlayer);
            LOGGER.info("Send remove \"{}\" auth packet to {}", modelId, player.getDisplayName().getString());
        }
    }

    public static void removeAllAuthFor(Entity player) {
        getOrCreateAllowedModelsFor(player).clear();
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(true, ALL_MODELS.keySet().stream().toList()), serverPlayer);
            LOGGER.info("Send remove all models packet to {}", player.getDisplayName().getString());
        }
    }

    public static boolean bindModelFor(Entity entity, String modelId) {
        if (!ALL_MODELS.containsKey(modelId)) {
            return false;
        }
        ENTITY_MODEL_MAP.put(entity.getUUID(), modelId);
        return true;
    }

    public static void bindModelSync(@Nullable ServerPlayer caster, Entity entity, String modelId) {
        if (bindModelFor(entity, modelId)) {
            PacketRelay.sendToAll(PacketHandler.INSTANCE, new BindModelPacket(entity.getId(), modelId));
            if(caster != null){
                caster.displayClientMessage(Component.translatable("tip.efmm.bind_success", modelId).append(caster.getDisplayName()), false);
            }
        } else {
            if(caster != null){
                caster.displayClientMessage(Component.translatable("tip.efmm.bind_model_lost", modelId), false);
            }
        }
    }


    public static void removeModelFor(Entity entity) {
        ENTITY_MODEL_MAP.remove(entity.getUUID());
    }

    public static boolean hasArmature(Entity entity) {
        return ENTITY_MODEL_MAP.containsKey(entity.getUUID());
    }

    public static Armature getArmatureFor(Entity entity) {
        UUID uuid = entity.getUUID();
        if (ENTITY_MODEL_MAP.containsKey(uuid)) {
            if (EFMMArmatures.ARMATURES.containsKey(ENTITY_MODEL_MAP.get(uuid))) {
                return EFMMArmatures.ARMATURES.get(ENTITY_MODEL_MAP.get(uuid));
            }
        }
        return Armatures.BIPED;
    }

    public static Vec3f getScaleFor(Entity entity) {
        String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
        if (!ALL_MODELS.containsKey(modelId)) {
            return new Vec3f(1.0F, 1.0F, 1.0F);
        }
        ModelConfig config = ALL_MODELS.get(ENTITY_MODEL_MAP.get(entity.getUUID()));
        return new Vec3f(config.scaleX(), config.scaleY(), config.scaleZ());
    }

    public static void saveAllowedModels() {
        try {
            Path authListsPath = EFMM_CONFIG_PATH.resolve("auth_lists.json");
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<UUID, Set<String>> entry : ALLOWED_MODELS.entrySet()) {
                UUID uuid = entry.getKey();
                Set<String> models = entry.getValue();
                JsonElement modelsArray = GSON.toJsonTree(models);
                jsonObject.add(uuid.toString(), modelsArray);
            }

            String jsonString = GSON.toJson(jsonObject);

            Path parentDir = authListsPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(authListsPath, jsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            LOGGER.info("Saved all allowed models to : " + authListsPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save allowed models!" , e);
        }

    }

    public static void loadAllowedModels() {
        try {
            Path authListsPath = EFMM_CONFIG_PATH.resolve("auth_lists.json");
            if(!Files.exists(authListsPath)){
                return;
            }
            EFMMJsonModelLoader authListLoader = new EFMMJsonModelLoader(authListsPath.toFile());
            JsonObject rootJson = authListLoader.getRootJson();
            for (Map.Entry<String, JsonElement> entry : rootJson.entrySet()) {
                String uuidStr = entry.getKey();
                JsonElement valueElement = entry.getValue();

                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID : {}, skipped", uuidStr);
                    continue;
                }

                if (!valueElement.isJsonArray()) {
                    LOGGER.error("Invalid value in UUID: {} , skipped", uuid);
                    continue;
                }

                JsonArray jsonArray = valueElement.getAsJsonArray();
                Set<String> models = new HashSet<>();
                for (JsonElement element : jsonArray) {
                    if (element.isJsonPrimitive()) {
                        models.add(element.getAsString());
                    } else {
                        LOGGER.error("Invalid value in UUID: {}, skipped", uuid);
                    }
                }
                ALLOWED_MODELS.put(uuid, models);
            }
        } catch (FileNotFoundException e){
            LOGGER.error("Failed to load allowed models!", e);
        }

    }

    /**
     * 从config读取所有模型文件并缓存
     */
    public static void loadAllModels() {

        try (Stream<Path> subDirs = Files.list(EFMM_CONFIG_PATH)) {
            subDirs.filter(Files::isDirectory).forEach(modelFileDir -> {
                try {
                    String modelId = modelFileDir.toFile().getName();
                    Path mainJsonPath = modelFileDir.resolve("main.json");
                    EFMMJsonModelLoader mainJsonLoader = new EFMMJsonModelLoader(mainJsonPath.toFile());
                    EFMMArmatures.ARMATURES.put(modelId, mainJsonLoader.loadArmature(HumanoidArmature::new));
                    Path configJsonPath = modelFileDir.resolve("config.json");
                    EFMMJsonModelLoader configJsonLoader = new EFMMJsonModelLoader(configJsonPath.toFile());
                    ALL_MODELS.put(modelId, configJsonLoader.loadModelConfig());
                    LOGGER.info("LOAD EPIC FIGHT MODEL >> {}", modelId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error when loading models", e);
            throw new RuntimeException(e);
        }
    }

    public static EFMMJsonModelLoader getModelJsonLoader(String modelId) throws FileNotFoundException {
        Path mainJsonPath = EFMM_CONFIG_PATH.resolve(modelId).resolve("main.json");
        return new EFMMJsonModelLoader(mainJsonPath.toFile());
    }

    public static EFMMJsonModelLoader getModelConfigJsonLoader(String modelId) throws FileNotFoundException {
        Path mainJsonPath = EFMM_CONFIG_PATH.resolve(modelId).resolve("config.json");
        return new EFMMJsonModelLoader(mainJsonPath.toFile());
    }

    public static byte[] getModelTexture(String modelId) throws IOException {
        Path texturePath = EFMM_CONFIG_PATH.resolve(modelId).resolve("texture.png");
        return Files.readAllBytes(texturePath);
    }

    public static void clearModels() {
        ALL_MODELS.clear();
    }

    public static void reloadEFModels() {
        clearModels();
        loadAllModels();
    }
}
