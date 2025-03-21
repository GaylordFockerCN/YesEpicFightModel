package com.p1nero.efmm.mixin;

import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = AbstractClientPlayerPatch.class, remap = false)
public abstract class AbstractClientPlayerPatchMixin<T extends Player> extends LivingEntityPatch<T> {
    @Inject(method = "getModelMatrix", at = @At("RETURN"), remap = false, cancellable = true)
    private void efmm$getModelMatrix(float partialTicks, CallbackInfoReturnable<OpenMatrix4f> cir){
        if(ClientModelManager.hasNewModel(this.getOriginal()) && !this.getOriginal().isSpectator()){
            Vec3f scale = ClientModelManager.getScaleFor(this.getOriginal());
            cir.setReturnValue(cir.getReturnValue().scale(scale.x, scale.y, scale.z));
        }
    }
}
