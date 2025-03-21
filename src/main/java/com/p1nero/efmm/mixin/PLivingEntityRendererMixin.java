package com.p1nero.efmm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.efmm.efmodel.ClientModelManager;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.LayerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedElytraLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.mixin.MixinLivingEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Map;

@Mixin(value = PatchedLivingEntityRenderer.class, remap = false)
public abstract class PLivingEntityRendererMixin<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> extends PatchedEntityRenderer<E, T, R, AM> implements LayerRenderer<E, T, M> {

    @Shadow
    @Final
    protected Map<Class<?>, PatchedLayer<E, T, M, ? extends RenderLayer<E, M>>> patchedLayers;

    @Shadow
    @Final
    protected List<PatchedLayer<E, T, M, ? extends RenderLayer<E, M>>> customLayers;

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    private void efmm$renderLayer(LivingEntityRenderer<E, M> renderer, T entitypatch, E entity, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks, CallbackInfo ci) {
        if(!ClientModelManager.hasNewModel(entity)){
            return;
        }
        float f = MathUtils.lerpBetween(entity.yBodyRotO, entity.yBodyRot, partialTicks);
        float f1 = MathUtils.lerpBetween(entity.yHeadRotO, entity.yHeadRot, partialTicks);
        float f2 = f1 - f;
        float f7 = entity.getViewXRot(partialTicks);
        float bob = ((MixinLivingEntityRenderer) renderer).invokeGetBob(entity, partialTicks);

        for (RenderLayer<E, M> layer : renderer.layers) {
            Class<?> layerClass = layer.getClass();

            if (layer instanceof HumanoidArmorLayer && ClientModelManager.getConfigFor(entity).shouldHideWearable()) {
                continue;
            }
            if (layer instanceof ElytraLayer<E, M> && ClientModelManager.getConfigFor(entity).shouldHideElytra()) {
                continue;
            }

            if (layerClass.isAnonymousClass()) {
                layerClass = layerClass.getSuperclass();
            }

            if (this.patchedLayers.containsKey(layerClass)) {
                this.patchedLayers.get(layerClass).renderLayer(entity, entitypatch, layer, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
            }
        }

        for (PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer : this.customLayers) {
            if (patchedLayer instanceof WearableItemLayer<?, ?, ?, ?> && ClientModelManager.getConfigFor(entity).shouldHideWearable()) {
                continue;
            }
            if (patchedLayer instanceof PatchedElytraLayer<?, ?, ?> && ClientModelManager.getConfigFor(entity).shouldHideElytra()) {
                continue;
            }
            patchedLayer.renderLayer(entity, entitypatch, null, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
        }
        ci.cancel();
    }

}
