package com.p1nero.efmm.mixin;

import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.ModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = LivingEntityPatch.class, remap = false)
public abstract class LivingEntityPatchMixin<T extends LivingEntity> extends HurtableEntityPatch<T> {
    @Shadow protected Armature armature;

    @Inject(method = "resetSize", at = @At("HEAD"), cancellable = true)
    private void efmm$resetSize(EntityDimensions size, CallbackInfo ci){
        ModelConfig config = ModelManager.getConfigFor(this.getOriginal());
        size = EntityDimensions.scalable(size.width * config.getDimScaleXZ(), size.height * config.getDimScaleY());
        EntityDimensions entitySize = this.original.dimensions;
        entitySize = EntityDimensions.scalable(entitySize.width * config.getDimScaleXZ(), entitySize.height * config.getDimScaleY());
        this.original.dimensions = size;
        if (size.width < entitySize.width) {
            double d0 = (double)size.width / 2.0;
            this.original.setBoundingBox(new AABB(this.original.getX() - d0, this.original.getY(), this.original.getZ() - d0, this.original.getX() + d0, this.original.getY() + (double)size.height, this.original.getZ() + d0));
        } else {
            AABB axisalignedbb = this.original.getBoundingBox();
            this.original.setBoundingBox(new AABB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)size.width, axisalignedbb.minY + (double)size.height, axisalignedbb.minZ + (double)size.width));
            if (size.width > entitySize.width && !this.original.level().isClientSide()) {
                float f = entitySize.width - size.width;
                this.original.move(MoverType.SELF, new Vec3(f, 0.0, f));
            }
        }
        ci.cancel();
    }

    @Inject(method = "getArmature", at = @At("HEAD"), cancellable = true)
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

    @Inject(method = "getModelMatrix", at = @At("RETURN"), cancellable = true)
    private void efmm$getModelMatrix(float partialTicks, CallbackInfoReturnable<OpenMatrix4f> cir){
        if(ModelManager.hasNewModel(this.getOriginal())){
            Vec3f scale = ModelManager.getScaleFor(this.getOriginal());
            cir.setReturnValue(cir.getReturnValue().scale(scale.x, scale.y, scale.z));
        }
    }

    @Inject(method = "poseTick", at = @At("HEAD"), cancellable = true)
    private void efmm$getArmature(DynamicAnimation animation, Pose pose, float elapsedTime, float partialTicks, CallbackInfo ci){
        if(ModelManager.hasNewModel(this.getOriginal())){
            if (pose.getJointTransformData().containsKey("Head") && animation.doesHeadRotFollowEntityHead()) {
                Armature customArmature = ModelManager.getArmatureFor(this.getOriginal());
                float headRotO = this.original.yBodyRotO - this.original.yHeadRotO;
                float headRot = this.original.yBodyRot - this.original.yHeadRot;
                float partialHeadRot = MathUtils.lerpBetween(headRotO, headRot, partialTicks);
                OpenMatrix4f toOriginalRotation = (new OpenMatrix4f(customArmature.getBindedTransformFor(pose,customArmature.searchJointByName("Head")))).removeScale().removeTranslation().invert();
                Vec3f xAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.X_AXIS, null);
                Vec3f yAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.Y_AXIS, null);
                OpenMatrix4f headRotation = OpenMatrix4f.createRotatorDeg(-this.original.getXRot(), xAxis).mulFront(OpenMatrix4f.createRotatorDeg(partialHeadRot, yAxis));
                pose.getOrDefaultTransform("Head").frontResult(JointTransform.fromMatrix(headRotation), OpenMatrix4f::mul);
            }
            ci.cancel();
        }
    }

}
