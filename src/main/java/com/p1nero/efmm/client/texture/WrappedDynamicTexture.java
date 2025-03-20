package com.p1nero.efmm.client.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
//TODO Iris Compat
public class WrappedDynamicTexture extends AbstractTexture {

    DynamicTexture dynamicTexture;

    public WrappedDynamicTexture(DynamicTexture dynamicTexture){
        this.dynamicTexture = dynamicTexture;
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) {
        dynamicTexture.load(resourceManager);
    }
}
