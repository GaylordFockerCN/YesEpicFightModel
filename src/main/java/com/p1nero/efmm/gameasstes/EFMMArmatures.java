package com.p1nero.efmm.gameasstes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

@SuppressWarnings("unchecked")
public class EFMMArmatures {
    public static final BiMap<String, Armature> ARMATURES = HashBiMap.create();

    public static void loadNativeArmatures(){
        addArmature("Anon Chihaya", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/anon"), HumanoidArmature::new);
        addArmature("Nagasaki Soyo", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/soyo"), HumanoidArmature::new);
        addArmature("Vergil", new ResourceLocation(EpicFightMeshModelMod.MOD_ID, "entity/vergil"), HumanoidArmature::new);
    }

    public static void reloadArmatures(){
        ARMATURES.clear();
        loadNativeArmatures();
    }

    public static <A extends Armature> void addArmature(String modelId, ResourceLocation resourceLocation, Armatures.ArmatureContructor<A> constructor) {
        ARMATURES.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(Armatures.wrapLocation(resourceLocation));
            return jsonModelLoader.loadArmature(constructor);
        });
    }

    public static <A extends Armature> A getOrCreateArmature(String modelId, JsonObject armatureJson, Armatures.ArmatureContructor<A> constructor) {
        return (A) ARMATURES.computeIfAbsent(modelId, (key) -> {
            EFMMJsonModelLoader jsonModelLoader;
            jsonModelLoader = new EFMMJsonModelLoader(armatureJson);
            return jsonModelLoader.loadArmature(constructor);
        });
    }

}
