package com.p1nero.efmm.mixin;

import com.p1nero.efmm.gameasstes.EFMMMeshes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

@Mixin(value = PPlayerRenderer.class)
public class PPlayerRendererMixin {

    @Inject(method = "getMeshProvider(Lyesman/epicfight/client/world/capabilites/entitypatch/player/AbstractClientPlayerPatch;)Lyesman/epicfight/api/client/model/MeshProvider;", at = @At("HEAD"), cancellable = true, remap = false)
    private void efhm$replaceMesh(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, CallbackInfoReturnable<MeshProvider<HumanoidMesh>> cir){
        if(EFMMMeshes.hasMesh(entitypatch.getOriginal())){
            AnimatedMesh mesh = EFMMMeshes.getMeshFor(entitypatch.getOriginal());
            if(mesh instanceof HumanoidMesh humanoidMesh){
                cir.setReturnValue(() -> humanoidMesh);
            }
        }
    }

    @Inject(method = "prepareModel(Lyesman/epicfight/client/mesh/HumanoidMesh;Lnet/minecraft/client/player/AbstractClientPlayer;Lyesman/epicfight/client/world/capabilites/entitypatch/player/AbstractClientPlayerPatch;Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void efhm$prepareModel(HumanoidMesh mesh, AbstractClientPlayer entity, AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, PlayerRenderer renderer, CallbackInfo ci){
        if(EFMMMeshes.hasMesh(entitypatch.getOriginal())){
            mesh.initialize();
            renderer.setModelProperties(entity);
            ci.cancel();
        }
    }

}
