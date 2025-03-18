package com.p1nero.efmm.mixin;

import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = PatchedEntityRenderer.class, remap = false)
public class PatchedEntityRendererMixin<E extends LivingEntity, T extends LivingEntityPatch<E>> {
    @Inject(method = "getMeshProvider", at = @At("HEAD"), cancellable = true, remap = false)
    private void efmm$replaceMesh(T entityPatch, CallbackInfoReturnable<MeshProvider<AnimatedMesh>> cir){
        if(ClientModelManager.hasMesh(entityPatch.getOriginal()) && !entityPatch.getOriginal().isSpectator()){
            AnimatedMesh mesh = ClientModelManager.getMeshFor(entityPatch.getOriginal());
            cir.setReturnValue(() ->  mesh);
        }
    }
}
