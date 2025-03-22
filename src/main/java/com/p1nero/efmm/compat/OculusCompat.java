package com.p1nero.efmm.compat;

import com.p1nero.efmm.client.texture.BytesTexture;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;

public class OculusCompat {
    public static void registerPBRLoader() {
        PBRTextureLoaderRegistry.INSTANCE.register(BytesTexture.class, new WrappedDynamicTexturePBRLoader());
    }
}
