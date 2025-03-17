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
    private void efmm$replaceMesh(AbstractClientPlayerPatch<AbstractClientPlayer> playerPatch, CallbackInfoReturnable<MeshProvider<HumanoidMesh>> cir){
        if(EFMMMeshes.hasMesh(playerPatch.getOriginal()) && !playerPatch.getOriginal().isSpectator()){
            AnimatedMesh mesh = EFMMMeshes.getMeshFor(playerPatch.getOriginal());
            if(mesh instanceof HumanoidMesh humanoidMesh){
                cir.setReturnValue(() -> humanoidMesh);
            }
        }
    }

    /**
     * 绕过part操作
     */
    @Inject(method = "prepareModel(Lyesman/epicfight/client/mesh/HumanoidMesh;Lnet/minecraft/client/player/AbstractClientPlayer;Lyesman/epicfight/client/world/capabilites/entitypatch/player/AbstractClientPlayerPatch;Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void efmm$prepareModel(HumanoidMesh mesh, AbstractClientPlayer player, AbstractClientPlayerPatch<AbstractClientPlayer> playerPatch, PlayerRenderer renderer, CallbackInfo ci){
        if(EFMMMeshes.hasMesh(playerPatch.getOriginal()) && !player.isSpectator()){
            mesh.initialize();
            renderer.setModelProperties(player);
            ci.cancel();
        }
    }

}
