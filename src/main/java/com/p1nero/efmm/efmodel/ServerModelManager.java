package com.p1nero.efmm.efmodel;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.AuthModelPacketPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ServerModelManager {
    public static final Map<UUID, Set<String>> ALLOWED_MODELS = new HashMap<>();
    public static final Map<String, ModelConfig> ALL_MODELS = new HashMap<>();
    public static final Map<UUID, String> ENTITY_MODEL_MAP = new HashMap<>();
    public static final Map<String, JsonObject> MODEL_JSON_CACHE = new HashMap<>();
    public static final Map<String, JsonObject> CONFIG_JSON_CACHE = new HashMap<>();
    public static final Map<String, byte[]> IMAGE_CACHE = new HashMap<>();

    public static final Path EFMM_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("efmm");
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Set<String> getOrCreateAllowedModelsFor(Entity player) {
        return ALLOWED_MODELS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
    }

    public static Set<String> getAllModels() {
        return ALL_MODELS.keySet();
    }

    public static void syncAllAllowedModelToClient(Entity player) {
        Set<String> Strings = getOrCreateAllowedModelsFor(player);
        //TODO 发包
    }

    public static void authModelFor(Entity player, String modelId) {
        getOrCreateAllowedModelsFor(player).add(modelId);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacketPacket(modelId, MODEL_JSON_CACHE.get(modelId), CONFIG_JSON_CACHE.get(modelId), IMAGE_CACHE.get(modelId)), serverPlayer);
            LOGGER.info("Send auth packet \"{}\" to {}", modelId, player.getDisplayName().getString());
        }
    }

    public static void authAllModelFor(Entity player) {
        getOrCreateAllowedModelsFor(player).addAll(ALL_MODELS.keySet());
        //TODO 发包
    }

    public static void removeAuthFor(Entity player, String modelId) {
        getOrCreateAllowedModelsFor(player).remove(modelId);
        //TODO 发包
    }

    public static void removeAllAuthFor(Entity player) {
        getOrCreateAllowedModelsFor(player).clear();
        //TODO 发包
    }

    public static boolean bindModelFor(Entity entity, String modelId) {
        if (!ALL_MODELS.containsKey(modelId)) {
            return false;
        }
        ENTITY_MODEL_MAP.put(entity.getUUID(), modelId);
        return true;
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

    public static void loadNativeModelConfig(ResourceLocation resourceLocation) {
        ALL_MODELS.computeIfAbsent(resourceLocation.toString(), (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(wrapConfigLocation(resourceLocation));
            return jsonModelLoader.loadModelConfig();
        });
    }

    public static ResourceLocation wrapConfigLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + "_config.json");
    }

    public static void saveAllowedModels() {
        //TODO 持久化
        //写入json，uuid为key，string集合为value


    }

    public static void loadAllowedModels() throws FileNotFoundException {
        //TODO 持久化
        Path authListsPath = EFMM_CONFIG_PATH.resolve("auth_lists");
        EFMMJsonModelLoader mainJsonLoader = new EFMMJsonModelLoader(authListsPath.toFile());


    }

    /**
     * 从config读取所有模型文件并缓存
     */
    public static void loadAllModels() {
        EFMMArmatures.loadNativeArmatures();
        loadNativeModelConfig(new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));

        try (Stream<Path> subDirs = Files.list(EFMM_CONFIG_PATH)) {
            subDirs.filter(Files::isDirectory).forEach(modelFileDir -> {
                try {
                    String modelId = modelFileDir.toFile().getName();
                    Path mainJsonPath = modelFileDir.resolve("main.json");
                    EFMMJsonModelLoader mainJsonLoader = new EFMMJsonModelLoader(mainJsonPath.toFile());
                    EFMMArmatures.ARMATURES.put(modelId, mainJsonLoader.loadArmature(HumanoidArmature::new));
                    MODEL_JSON_CACHE.put(modelId, mainJsonLoader.getRootJson());
                    Path configJsonPath = modelFileDir.resolve("config.json");
                    EFMMJsonModelLoader configJsonLoader = new EFMMJsonModelLoader(configJsonPath.toFile());
                    ALL_MODELS.put(modelId, configJsonLoader.loadModelConfig());
                    CONFIG_JSON_CACHE.put(modelId, configJsonLoader.getRootJson());
                    Path texturePath = modelFileDir.resolve("texture.png");
                    IMAGE_CACHE.put(modelId, Files.readAllBytes(texturePath));
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

    public static void clearModels() {
        ALL_MODELS.clear();
    }

    public static void reloadEFModels() {
        clearModels();
        loadAllModels();
    }
}
