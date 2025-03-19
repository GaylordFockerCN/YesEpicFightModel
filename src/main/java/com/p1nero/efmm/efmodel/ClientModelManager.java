package com.p1nero.efmm.efmodel;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.RequestSyncModelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

public class ClientModelManager {
    public static final Set<String> AUTHED_MODELS = new HashSet<>();
    public static final BiMap<String, ModelConfig> ALL_MODELS = HashBiMap.create();
    public static final BiMap<String, ResourceLocation> TEXTURE_CACHE = HashBiMap.create();
    public static final Map<UUID, String> ENTITY_MODEL_MAP = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Set<String> getAllowedModels(){
        return ALL_MODELS.keySet();
    }
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[^a-z_]");
    public static final int MAX_REQUEST_INTERVAL = 60;
    private static int requestDelayTimer;

    @OnlyIn(Dist.CLIENT)
    public static void registerModel(String modelId, JsonObject modelJson, JsonObject configJson, byte[] imageCache) {
        EFMMJsonModelLoader modelLoader = new EFMMJsonModelLoader(modelJson);
        EFMMArmatures.ARMATURES.put(modelId, modelLoader.loadArmature(HumanoidArmature::new));
        EFMMMeshes.MESHES.put(modelId, modelLoader.loadAnimatedMesh(HumanoidMesh::new));
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

    public static boolean hasArmature(Entity entity){
        return ENTITY_MODEL_MAP.containsKey(entity.getUUID());
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

    public static boolean hasMesh(Entity entity){
        return ENTITY_MODEL_MAP.containsKey(entity.getUUID());
    }

    public static AnimatedMesh getMeshFor(Entity entity){
        if(hasMesh(entity)){
            String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
            if(EFMMMeshes.MESHES.containsKey(modelId)){
                return EFMMMeshes.MESHES.get(modelId);
            } else {
                sendRequestModelPacket(modelId);
            }
        }
        return Meshes.BIPED;
    }

    public static void sendRequestModelPacket(String modelId){
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
            return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity);
        }
        return TEXTURE_CACHE.get(modelId);
    }

}
