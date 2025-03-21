package com.p1nero.efmm.efmodel;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EFMMConfig;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import com.p1nero.efmm.network.packet.RequestSyncModelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.p1nero.efmm.EpicFightMeshModelMod.EFMM_CONFIG_PATH;
import static com.p1nero.efmm.efmodel.ModelManager.*;

public class ClientModelManager {
    public static final Set<String> AUTHED_MODELS = new HashSet<>();
    public static final Set<String> MODELS_BLACK_LIST = new HashSet<>();
    public static final Set<String> NATIVE_MODELS = new HashSet<>();
    public static final Map<String, ModelConfig> LOCAL_MODELS = new HashMap<>();
    public static final Map<String, ModelConfig> ALL_MODELS = new HashMap<>();
    public static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    public static final Map<UUID, String> ENTITY_MODEL_MAP = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Set<String> getAllowedModels(){
        return ALL_MODELS.keySet();
    }
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[^a-z_]");
    public static final int MAX_REQUEST_INTERVAL = 40;
    private static int requestDelayTimer;

    private static final int MAX_SEND_COOLDOWN = 600;

    private static int sendTimer;

    /**
     * 仅开窗口时加载
     */
    @OnlyIn(Dist.CLIENT)
    public static void loadNativeModels() {

        try (Stream<Path> subDirs = Files.list(EFMM_CONFIG_PATH)) {
            subDirs.filter(Files::isDirectory).forEach(modelFileDir -> {
                try {
                    String modelId = modelFileDir.toFile().getName();
                    EFMMJsonModelLoader mainJsonLoader = getModelJsonLoader(modelId);
                    EFMMArmatures.ARMATURES.put(modelId, mainJsonLoader.loadArmature(HumanoidArmature::new));
                    EFMMMeshes.MESHES.put(modelId, mainJsonLoader.loadAnimatedMesh(AnimatedMesh::new));

                    ResourceLocation textureId = new ResourceLocation("efmm_texture_cache", INVALID_CHARS_PATTERN.matcher(modelId.toLowerCase(Locale.ROOT)).replaceAll(""));
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(getModelTexture(modelId))) {
                        NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, bis);
                        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                        Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
                        TEXTURE_CACHE.put(modelId, textureId);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read image data for model {}", modelId, e);
                    }

                    EFMMJsonModelLoader configJsonLoader = getModelConfigJsonLoader(modelId);
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

    @OnlyIn(Dist.CLIENT)
    public static void sendModelToServer(String modelId) throws IOException{
        if(NATIVE_MODELS.contains(modelId)){
            if(Minecraft.getInstance().player != null){
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("tip.efmm.model_already_exist", modelId), false);
            }
        } else {
            if(sendTimer == 0){
                sendTimer = MAX_SEND_COOLDOWN;
                PacketRelay.sendToServer(PacketHandler.INSTANCE, new RegisterModelPacket(modelId, getModelJsonLoader(modelId).getRootJson(), getModelConfigJsonLoader(modelId).getRootJson(), getModelTexture(modelId)));
                LOGGER.info("Send model \"{}\" to server", modelId);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerModelFromServer(String modelId, JsonObject modelJson, JsonObject configJson, byte[] imageCache) {
        EFMMJsonModelLoader modelLoader = new EFMMJsonModelLoader(modelJson);
        HumanoidMesh mesh = modelLoader.loadAnimatedMesh(HumanoidMesh::new);
        if(modelLoader.getPositionCountAfterLoadMesh() <= EFMMConfig.MAX_POSITIONS_COUNT.get()){
            EFMMMeshes.MESHES.put(modelId, mesh);
        } else {
            MODELS_BLACK_LIST.add(modelId);
            if(Minecraft.getInstance().player != null){
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("tip.efmm.model_to_large", modelId), false);
            }
            LOGGER.info("Received a too large model \"{}\" from server! Skipped.", modelId);
            return;
        }
        EFMMArmatures.ARMATURES.put(modelId, modelLoader.loadArmature(HumanoidArmature::new));
        EFMMJsonModelLoader modelConfigLoader = new EFMMJsonModelLoader(configJson);
        ResourceLocation textureId = new ResourceLocation("efmm_texture_cache", INVALID_CHARS_PATTERN.matcher(modelId.toLowerCase(Locale.ROOT)).replaceAll(""));
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageCache)) {
            NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, bis);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
            TEXTURE_CACHE.put(modelId, textureId);
        } catch (Exception e) {
            LOGGER.error("Failed to read image data for model {}", modelId, e);
        }
        ALL_MODELS.put(modelId, modelConfigLoader.loadModelConfig());
        LOGGER.info("Registered new model \"{}\" from server.", modelId);
    }

    public static void removeModel(String modelId){
        ALL_MODELS.remove(modelId);
    }

    public static void clear(){
        ALL_MODELS.clear();
    }

    public static void authModel(String modelId){
        AUTHED_MODELS.add(modelId);
    }

    public static void removeAuthModel(String modelId){
        AUTHED_MODELS.remove(modelId);
    }

    public static Set<String> getAuthedModels(){
        return AUTHED_MODELS;
    }

    /**
     * 不管有无都要绑定，无的情况下自己会请求服务端发
     */
    public static boolean bindModelFor(Entity entity, String modelId){
        ENTITY_MODEL_MAP.put(entity.getUUID(), modelId);
        return true;
    }

    public static void removeModelFor(Entity entity){
        ENTITY_MODEL_MAP.remove(entity.getUUID());
    }

    public static Vec3f getScaleFor(Entity entity){
        String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
        if(!ALL_MODELS.containsKey(modelId)){
            return new Vec3f(1.0F, 1.0F, 1.0F);
        }
        ModelConfig config = ALL_MODELS.get(ENTITY_MODEL_MAP.get(entity.getUUID()));
        return new Vec3f(config.scaleX(), config.scaleY(), config.scaleZ());
    }

    public static boolean hasNewModel(Entity entity){
        return ENTITY_MODEL_MAP.containsKey(entity.getUUID());
    }

    public static boolean isNativeModel(String modelId) {
        return NATIVE_MODELS.contains(modelId);
    }

    /**
     * 拿不到就请求服务端发
     */
    public static Armature getArmatureFor(Entity entity){
        UUID uuid = entity.getUUID();
        if(ENTITY_MODEL_MAP.containsKey(uuid)){
            String modelId = ENTITY_MODEL_MAP.get(uuid);
            if(EFMMArmatures.ARMATURES.containsKey(modelId)){
                return EFMMArmatures.ARMATURES.get(modelId);
            } else {
                sendRequestModelPacket(modelId);
            }
        }
        return Armatures.BIPED;
    }

    /**
     * 拿不到就请求服务端发
     */
    public static ModelConfig getConfigFor(Entity entity){
        UUID uuid = entity.getUUID();
        if(ENTITY_MODEL_MAP.containsKey(uuid)){
            String modelId = ENTITY_MODEL_MAP.get(uuid);
            if(ALL_MODELS.containsKey(modelId)){
                return ALL_MODELS.get(modelId);
            } else {
                sendRequestModelPacket(modelId);
            }
        }
        return ModelConfig.getDefault();
    }

    @OnlyIn(Dist.CLIENT)
    public static AnimatedMesh getMeshFor(Entity entity){
        if(hasNewModel(entity)){
            String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
            if(EFMMMeshes.MESHES.containsKey(modelId)){
                return EFMMMeshes.MESHES.get(modelId);
            } else {
                sendRequestModelPacket(modelId);
            }
        }
        return Meshes.BIPED;
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static AnimatedMesh getOrRequestMesh(String modelId){
        if(EFMMMeshes.MESHES.containsKey(modelId)){
            return EFMMMeshes.MESHES.get(modelId);
        } else {
            sendRequestModelPacket(modelId);
        }
        return null;
    }

    public static ModelConfig getOrRequestModelConfig(String modelId){
        if(ALL_MODELS.containsKey(modelId)){
            return ALL_MODELS.get(modelId);
        } else {
            sendRequestModelPacket(modelId);
        }
        return ModelConfig.getDefault();
    }

    public static void sendRequestModelPacket(String modelId){
        if(MODELS_BLACK_LIST.contains(modelId)){
            return;
        }
        if(requestDelayTimer > 0){
            requestDelayTimer--;
        } else {
            PacketRelay.sendToServer(PacketHandler.INSTANCE, new RequestSyncModelPacket(modelId));
            requestDelayTimer = MAX_REQUEST_INTERVAL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getTextureFor(Entity entity){
        String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
        if(!ALL_MODELS.containsKey(modelId) || !TEXTURE_CACHE.containsKey(modelId)){
            sendRequestModelPacket(modelId);
            return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity);
        }
        return TEXTURE_CACHE.get(modelId);
    }

    public static void clientTick() {
        if(sendTimer > 0){
            sendTimer--;
        }
    }
}
