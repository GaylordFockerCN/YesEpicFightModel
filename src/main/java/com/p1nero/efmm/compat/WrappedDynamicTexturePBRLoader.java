package com.p1nero.efmm.compat;

import com.p1nero.efmm.client.texture.BytesTexture;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WrappedDynamicTexturePBRLoader implements PBRTextureLoader<BytesTexture> {
    @Override
    public void load(BytesTexture bytesTexture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
        ResourceLocation id = bytesTexture.getRegisterId();
        ResourceLocation pbrNormalId = new ResourceLocation(id.getNamespace(), id.getPath() + PBRType.NORMAL.getSuffix());
        ResourceLocation pbrSpecularId = new ResourceLocation(id.getNamespace(), id.getPath() + PBRType.SPECULAR.getSuffix());
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        if (textureManager.byPath.containsKey(pbrNormalId)) {
            pbrTextureConsumer.acceptNormalTexture(textureManager.getTexture(pbrNormalId));
        }
        if (textureManager.byPath.containsKey(pbrSpecularId)) {
            pbrTextureConsumer.acceptSpecularTexture(textureManager.getTexture(pbrSpecularId));
        }
    }
}
