package com.p1nero.efmm.gameasstes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.p1nero.efmm.EFMMConfig;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.AnimatedVertexBuilder;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.client.mesh.HumanoidMesh;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EFMMMeshes {

    public static final BiMap<ResourceLocation, AnimatedMesh> MESHES = HashBiMap.create();
    public static final BiMap<UUID, ResourceLocation> MESH_LOCATION_MAP = HashBiMap.create();

    @SubscribeEvent
    public static void build(ModelBuildEvent.MeshBuild event) {
        for (ResourceLocation resourceLocation : EFMMConfig.MODELS) {
            getOrCreateAnimatedMesh(resourceLocation, HumanoidMesh::new);
            EpicFightMeshModelMod.LOGGER.info("LOAD ADDITIONAL EPIC FIGHT MESH >> {}", resourceLocation.toString());
        }

    }

    public static void bindMesh(Entity entity, String resourceLocation){
        bindMesh(entity, new ResourceLocation(resourceLocation));
    }

    public static void bindMesh(Entity entity, ResourceLocation resourceLocation){
        if(!MESHES.containsKey(resourceLocation)){
            EpicFightMeshModelMod.LOGGER.info("mesh {} doesn't exist", resourceLocation.toString());
            return;
        }
        MESH_LOCATION_MAP.put(entity.getUUID(), resourceLocation);
        EpicFightMeshModelMod.LOGGER.info("bind armature {} to {}", resourceLocation.toString(), entity.getDisplayName().getString());
    }

    public static boolean hasMesh(Entity entity){
        return MESH_LOCATION_MAP.containsKey(entity.getUUID());
    }

    public static AnimatedMesh getMeshFor(Entity entity){
        if(hasMesh(entity)){
            return MESHES.get(MESH_LOCATION_MAP.get(entity.getUUID()));
        }
        return Meshes.BIPED;
    }

    public static void removeMesh(Entity entity){
        if(!MESH_LOCATION_MAP.containsKey(entity.getUUID())){
            return;
        }
        MESH_LOCATION_MAP.remove(entity.getUUID());
    }

    @SuppressWarnings("unchecked")
    public static <M extends AnimatedMesh> M getOrCreateAnimatedMesh(ResourceLocation rl, Meshes.MeshContructor<AnimatedMesh.AnimatedModelPart, AnimatedVertexBuilder, M> constructor) {
        return (M) MESHES.computeIfAbsent(rl, (key) -> {
            EFMMJsonModelLoader jsonModelLoader = new EFMMJsonModelLoader(wrapLocation(rl));
            return jsonModelLoader.loadAnimatedMesh(constructor);
        });
    }

    public static ResourceLocation wrapLocation(ResourceLocation rl) {
        return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + ".json");
    }

}
