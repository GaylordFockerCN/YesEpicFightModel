package com.p1nero.efmm.efmodel;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;

import java.util.Map;

public class ModelManager {

    public static void loadNative(){
        EFMMArmatures.loadNativeArmatures();
        loadNativeModelConfig("Anon Chihaya", LogicServerModelManager.ALL_MODELS, new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));
        loadNativeModelConfig("Anon Chihaya", ClientModelManager.ALL_MODELS, new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));
        LogicServerModelManager.NATIVE_MODELS.add("Anon Chihaya");
    }

    public static boolean hasArmature(Entity entity){
        if(entity == null){
            return false;
        }
        if(entity.level().isClientSide){
            return ClientModelManager.hasArmature(entity);
        } else {
            return LogicServerModelManager.hasArmature(entity);
        }
    }

    public static Armature getArmatureFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getArmatureFor(entity);
        } else {
            return LogicServerModelManager.getArmatureFor(entity);
        }
    }

    public static Vec3f getScaleFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getScaleFor(entity);
        } else {
            return LogicServerModelManager.getScaleFor(entity);
        }
    }

    public static void loadNativeModelConfig(String modelId, Map<String, ModelConfig> allModels, ResourceLocation resourceLocation) {
        allModels.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(wrapConfigLocation(resourceLocation));
            return jsonModelLoader.loadModelConfig();
        });
    }

    public static ResourceLocation wrapConfigLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + "_config.json");
    }

}
