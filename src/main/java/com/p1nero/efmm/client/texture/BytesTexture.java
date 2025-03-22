package com.p1nero.efmm.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BytesTexture extends AbstractTexture {

    private final byte[] buff;
    private final ResourceLocation id;
    private static final Logger LOGGER = LogUtils.getLogger();

    public BytesTexture(byte[] buff, ResourceLocation id){
        this.buff = buff;
        this.id = id;
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::doLoad);
        } else {
            this.doLoad();
        }
    }

    private void doLoad() {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(buff)) {
            NativeImage imageIn = NativeImage.read(NativeImage.Format.RGBA, bis);
            int width = imageIn.getWidth();
            int height = imageIn.getHeight();
            TextureUtil.prepareImage(this.getId(), 0, width, height);
            imageIn.upload(0, 0, 0, 0, 0, width, height, false, false, false, true);
        } catch (IOException e) {
            LOGGER.error("Failed to load image! " + id, e);
        }
    }

    public ResourceLocation getRegisterId() {
        return id;
    }
}
