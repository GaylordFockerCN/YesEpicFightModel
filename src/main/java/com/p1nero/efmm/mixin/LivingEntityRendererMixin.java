package com.p1nero.efmm.mixin;

import com.p1nero.efmm.gameasstes.EFMMArmatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
    @Shadow protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void efmm$replaceTexture(T livingEntity, boolean p_115323_, boolean p_115324_, boolean p_115325_, CallbackInfoReturnable<RenderType> cir){
        if(EFMMArmatures.hasArmature(livingEntity) && !livingEntity.isSpectator()){
            ResourceLocation resourcelocation = this.getTextureLocation(livingEntity);

            if(EFMMArmatures.hasArmature(livingEntity)){
                resourcelocation = EFMMArmatures.getTextureFor(livingEntity);
            }

            if (p_115324_) {
                cir.setReturnValue(RenderType.itemEntityTranslucentCull(resourcelocation));
            } else if (p_115323_) {
                cir.setReturnValue(this.model.renderType(resourcelocation));
            } else {
                cir.setReturnValue(p_115325_ ? RenderType.outline(resourcelocation) : null);
            }
        }
    }
}
