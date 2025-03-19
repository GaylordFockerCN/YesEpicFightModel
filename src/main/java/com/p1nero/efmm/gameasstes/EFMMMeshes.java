package com.p1nero.efmm.gameasstes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.AnimatedVertexBuilder;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.client.mesh.HumanoidMesh;

import java.io.File;
import java.io.FileNotFoundException;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EFMMMeshes {

    public static final BiMap<String, AnimatedMesh> MESHES = HashBiMap.create();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void build(ModelBuildEvent.MeshBuild event) {
        getOrCreateAnimatedMesh(new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"), HumanoidMesh::new);
        ClientModelManager.TEXTURE_CACHE.put("efmm:entity/anon", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "textures/entity/anon.png"));
    }

    @SuppressWarnings("unchecked")
    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(String modelId, JsonObject meshJson, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(meshJson);
            LOGGER.info("LOAD ADDITIONAL EPIC FIGHT MESH >> {}", modelId);
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

    @SuppressWarnings("unchecked")
    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(String modelId, File meshFile, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            try {
                jsonModelLoader = new EFMMJsonModelLoader(meshFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            LOGGER.info("LOAD ADDITIONAL EPIC FIGHT MESH >> {}", modelId);
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

    @SuppressWarnings("unchecked")
    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(ResourceLocation resourceLocation, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(resourceLocation.toString(), (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(Meshes.wrapLocation(resourceLocation));
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

}
