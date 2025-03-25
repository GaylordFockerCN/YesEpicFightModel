package com.p1nero.efmm.mixin;

import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.ModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

@Mixin(HumanoidMobPatch.class)
public abstract class HumanoidMobPatchMixin<T extends PathfinderMob> extends MobPatch<T> {

    @Inject(method = "getArmature()Lyesman/epicfight/model/armature/HumanoidArmature;", at = @At("HEAD"), cancellable = true, remap = false)
    private void efmm$getArmature(CallbackInfoReturnable<Armature> cir){
        if(ModelManager.hasNewModel(this.getOriginal())){
            if(!(this.armature instanceof HumanoidArmature)){
                if(this.isLogicalClient()){
                    ClientModelManager.removeModelFor(this.getOriginal());
                } else {
                    ServerModelManager.removeModelFor(this.getOriginal());
                }
                return;
            }
            cir.setReturnValue(ModelManager.getArmatureFor(this.getOriginal()));
        }
    }

}
