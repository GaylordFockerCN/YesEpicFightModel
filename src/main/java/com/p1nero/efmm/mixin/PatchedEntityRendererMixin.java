package com.p1nero.efmm.mixin;

import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = PatchedEntityRenderer.class, remap = false)
public abstract class PatchedEntityRendererMixin<E extends LivingEntity, T extends LivingEntityPatch<E>> {
    @Shadow public abstract MeshProvider<AnimatedMesh> getDefaultMesh();

    @Inject(method = "getMeshProvider", at = @At("HEAD"), cancellable = true, remap = false)
    private void efmm$replaceMesh(T entityPatch, CallbackInfoReturnable<MeshProvider<AnimatedMesh>> cir){
        if(ClientModelManager.hasNewModel(entityPatch.getOriginal()) && !entityPatch.getOriginal().isSpectator()){
            AnimatedMesh mesh = ClientModelManager.getMeshFor(entityPatch.getOriginal());
            if(!(this.getDefaultMesh().get() instanceof HumanoidMesh)){
                EpicFightMeshModelMod.LOGGER.error("Failed to apply model to none humanoid mob!");
                ClientModelManager.removeModelFor(entityPatch.getOriginal());
                return;
            }
            cir.setReturnValue(() ->  mesh);
        }
    }
}
