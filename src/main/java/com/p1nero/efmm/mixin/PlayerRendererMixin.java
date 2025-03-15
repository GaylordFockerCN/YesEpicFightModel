package com.p1nero.efmm.mixin;

import com.p1nero.efmm.gameasstes.EFMMArmatures;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void efhm$replaceTexture(AbstractClientPlayer abstractClientPlayer, CallbackInfoReturnable<ResourceLocation> cir){
        if(EFMMArmatures.hasArmature(abstractClientPlayer)){
            cir.setReturnValue(EFMMArmatures.getTextureFor(abstractClientPlayer));
        }
    }
}
