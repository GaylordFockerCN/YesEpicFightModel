package com.p1nero.efmm.gameasstes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.AnimatedVertexBuilder;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.client.mesh.HumanoidMesh;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("unchecked")
public class EFMMMeshes {

    public static final BiMap<String, AnimatedMesh> MESHES = HashBiMap.create();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void loadNativeMeshes(ModelBuildEvent.MeshBuild event) {
        getOrCreateAnimatedMesh("Anon Chihaya", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"), HumanoidMesh::new);
        ClientModelManager.TEXTURE_CACHE.put("Anon Chihaya", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "textures/entity/anon.png"));
        getOrCreateAnimatedMesh("Nagasaki Soyo", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/soyo"), HumanoidMesh::new);
        ClientModelManager.TEXTURE_CACHE.put("Nagasaki Soyo", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "textures/entity/soyo.png"));
    }
    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(String modelId, ResourceLocation resourceLocation, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(Meshes.wrapLocation(resourceLocation));
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(ResourceLocation resourceLocation, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(resourceLocation.toString(), (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(Meshes.wrapLocation(resourceLocation));
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

}
