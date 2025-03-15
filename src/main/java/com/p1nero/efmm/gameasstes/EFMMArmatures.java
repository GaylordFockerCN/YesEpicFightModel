package com.p1nero.efmm.gameasstes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.p1nero.efmm.EFMMConfig;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

import java.util.UUID;

import static yesman.epicfight.gameasset.Armatures.wrapLocation;

@SuppressWarnings("unchecked")
@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EFMMArmatures {

    public static final BiMap<ResourceLocation, ModelConfig> MODEL_CONFIGS = HashBiMap.create();
    public static final BiMap<ResourceLocation, Armature> ARMATURES = HashBiMap.create();
    public static final BiMap<UUID, ResourceLocation> ARMATURE_LOCATION_MAP = HashBiMap.create();

    @SubscribeEvent
    public static void build(ModelBuildEvent.ArmatureBuild event) {
        for(ResourceLocation resourceLocation : EFMMConfig.MODELS){
            getOrCreateArmature(resourceLocation);
            getOrCreateModelConfig(resourceLocation);
            EpicFightMeshModelMod.LOGGER.info("LOAD ADDITIONAL EPIC FIGHT ARMATURE >> {}", resourceLocation.toString());
        }
    }

    public static void bindArmature(Entity entity, String resourceLocation){
        bindArmature(entity, new ResourceLocation(resourceLocation));
    }

    public static void removeArmature(Entity entity){
        if(!ARMATURE_LOCATION_MAP.containsKey(entity.getUUID())){
            return;
        }
        ARMATURE_LOCATION_MAP.remove(entity.getUUID());
    }

    public static boolean bindArmature(Entity entity, ResourceLocation resourceLocation){
        if(!ARMATURES.containsKey(resourceLocation)){
            EpicFightMeshModelMod.LOGGER.info("armature {} doesn't exist", resourceLocation.toString());
            return false;
        }
        ARMATURE_LOCATION_MAP.put(entity.getUUID(), resourceLocation);
        EpicFightMeshModelMod.LOGGER.info("bind armature {} to {}", resourceLocation.toString(), entity.getDisplayName().getString());
        return true;
    }

    public static boolean hasArmature(Entity entity){
        return ARMATURE_LOCATION_MAP.containsKey(entity.getUUID());
    }

    public static Vec3f getScaleFor(Entity entity){
        ResourceLocation location = ARMATURE_LOCATION_MAP.get(entity.getUUID());
        if(!MODEL_CONFIGS.containsKey(location)){
            return new Vec3f(1.0F, 1.0F, 1.0F);
        }
        ModelConfig config = MODEL_CONFIGS.get(ARMATURE_LOCATION_MAP.get(entity.getUUID()));
        return new Vec3f(config.scaleX(), config.scaleY(), config.scaleZ());
    }

    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getTextureFor(Entity entity){
        ResourceLocation location = ARMATURE_LOCATION_MAP.get(entity.getUUID());
        if(!MODEL_CONFIGS.containsKey(location)){
            return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity);
        }
        return MODEL_CONFIGS.get(ARMATURE_LOCATION_MAP.get(entity.getUUID())).textureLocation();
    }

    public static Armature getArmatureFor(Entity entity){
        UUID uuid = entity.getUUID();
        if(ARMATURE_LOCATION_MAP.containsKey(uuid)){
            if(ARMATURES.containsKey(ARMATURE_LOCATION_MAP.get(uuid))){
                return ARMATURES.get(ARMATURE_LOCATION_MAP.get(uuid));
            }
        }
        return Armatures.BIPED;
    }

    public static <A extends Armature> A getOrCreateArmature(ResourceLocation rl, Armatures.ArmatureContructor<A> constructor) {
        return (A) ARMATURES.computeIfAbsent(rl, (key) -> {
            EFMMJsonModelLoader jsonModelLoader = new EFMMJsonModelLoader(wrapLocation(rl));
            return jsonModelLoader.loadArmature(constructor);
        });
    }

    public static ModelConfig getOrCreateModelConfig(ResourceLocation rl) {
        return MODEL_CONFIGS.computeIfAbsent(rl, (key) -> {
            EFMMJsonModelLoader jsonModelLoader = new EFMMJsonModelLoader(wrapConfigLocation(rl));
            return jsonModelLoader.loadModelConfig();
        });
    }

    public static <A extends Armature> A getOrCreateArmature(ResourceLocation rl) {
        return (A) EFMMArmatures.getOrCreateArmature(rl, HumanoidArmature::new);
    }

    public static ResourceLocation wrapConfigLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + "_config.json");
    }

}
