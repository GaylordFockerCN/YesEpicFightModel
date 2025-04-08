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

    public static void registerEFMMNativeModelConfig() {
        EFMMArmatures.loadNativeArmatures();
        registerNativeModelConfig("Anon Chihaya", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"));
        registerNativeModelConfig("Nagasaki Soyo", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/soyo"));
        registerNativeModelConfig("Vergil", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/vergil"));
    }

    public static void registerNativeModelConfig(String modelId, ResourceLocation resourceLocation){
        loadNativeModelConfig(modelId, ServerModelManager.ALL_MODELS, resourceLocation);
        loadNativeModelConfig(modelId, ClientModelManager.ALL_MODELS, resourceLocation);
        ServerModelManager.NATIVE_MODELS.add(modelId);
        ClientModelManager.NATIVE_MODELS.add(modelId);
    }

    public static boolean hasNewModel(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.level().isClientSide) {
            return ClientModelManager.hasNewModel(entity);
        } else {
            return ServerModelManager.hasNewModel(entity);
        }
    }

    public static ModelConfig getConfigFor(Entity entity){
        if(hasNewModel(entity)){
            if(entity.level().isClientSide){
                return ClientModelManager.getConfigFor(entity);
            } else {
                return ServerModelManager.getConfigFor(entity);
            }
        }
        return ModelConfig.getDefault();
    }

    public static Armature getArmatureFor(Entity entity) {
        if (entity.level().isClientSide) {
            return ClientModelManager.getArmatureFor(entity);
        } else {
            return ServerModelManager.getArmatureFor(entity);
        }
    }

    public static Vec3f getScaleFor(Entity entity) {
        if (entity.level().isClientSide) {
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

    /**
     * 有pbr就一起发，没有就传空的
     */
    public static byte[] getModelTexture(String modelId, String suffix) throws IOException {
        Path texturePath = EFMM_CONFIG_PATH.resolve(modelId).resolve("texture" + suffix + ".png");
        if (!Files.exists(texturePath)) {
            return new byte[0];
        }
        return Files.readAllBytes(texturePath);
    }

    public static ResourceLocation wrapConfigLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + "_config.json");
    }

}
