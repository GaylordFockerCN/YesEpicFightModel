package com.p1nero.efmm.data;

public record ModelConfig(float scaleX, float scaleY, float scaleZ) {
    public static ModelConfig getDefault(){
        return new ModelConfig(1.0F, 1.0F, 1.0F);
    }
}
