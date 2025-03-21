package com.p1nero.efmm.efmodel;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.p1nero.efmm.EpicFightMeshModelMod.EFMM_CONFIG_PATH;

public class ModelManager {

    public static void loadNative(){
        EFMMArmatures.loadNativeArmatures();
        loadNativeModelConfig("Anon Chihaya", ServerModelManager.ALL_MODELS, new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));
        loadNativeModelConfig("Anon Chihaya", ClientModelManager.ALL_MODELS, new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));
        ServerModelManager.NATIVE_MODELS.add("Anon Chihaya");
        ClientModelManager.NATIVE_MODELS.add("Anon Chihaya");
    }

    public static boolean hasArmature(Entity entity){
        if(entity == null){
            return false;
        }
        if(entity.level().isClientSide){
            return ClientModelManager.hasNewModel(entity);
        } else {
            return ServerModelManager.hasArmature(entity);
        }
    }

    public static Armature getArmatureFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getArmatureFor(entity);
        } else {
            return ServerModelManager.getArmatureFor(entity);
        }
    }

    public static Vec3f getScaleFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getScaleFor(entity);
        } else {
            return ServerModelManager.getScaleFor(entity);
        }
    }

    public static void loadNativeModelConfig(String modelId, Map<String, ModelConfig> allModels, ResourceLocation resourceLocation) {
        allModels.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(wrapConfigLocation(resourceLocation));
            return jsonModelLoader.loadModelConfig();
        });
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

    public static ResourceLocation wrapConfigLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + "_config.json");
    }

}
