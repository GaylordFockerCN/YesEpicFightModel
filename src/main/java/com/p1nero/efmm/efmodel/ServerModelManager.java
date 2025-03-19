package com.p1nero.efmm.efmodel;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.AuthModelPacket;
import com.p1nero.efmm.network.packet.RegisterModelPacketPacket;
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
            PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new AuthModelPacket(false, List.of(modelId)), serverPlayer);
            LOGGER.info("Send \"{}\" permission to {}", modelId, player.getDisplayName().getString());
        }
    }

    public static void sendModelTo(ServerPlayer serverPlayer, String modelId) throws IOException {
        PacketRelay.sendToPlayer(PacketHandler.INSTANCE, new RegisterModelPacketPacket("aaa", modelId, getModelJsonLoader(modelId).getRootJson(), getModelConfigJsonLoader(modelId).getRootJson(), getModelTexture(modelId)), serverPlayer);
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
