package com.p1nero.efmm.data;

public class ModelConfig {
    public static final ModelConfig DEFAULT_CONFIG = new ModelConfig();
    protected float scaleX;
    protected float scaleY;
    protected float scaleZ;
    protected String authorName;
    protected boolean shouldHideWearable;
    protected boolean shouldHideElytra;
    public ModelConfig(){
        authorName = "Anonymous";
        scaleX = scaleY = scaleZ = 1.0F;
    }

    public ModelConfig(String authorName, float scaleX, float scaleY, float scaleZ) {
        this.authorName = authorName;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public static ModelConfig getDefault(){
        return DEFAULT_CONFIG;
    }

    public float scaleX() {
        return scaleX;
    }

    public float scaleY() {
        return scaleY;
    }

    public float scaleZ() {
        return scaleZ;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setShouldHideWearable(boolean shouldHideWearable) {
        this.shouldHideWearable = shouldHideWearable;
    }

    public boolean shouldHideWearable() {
        return shouldHideWearable;
    }

    public void setShouldHideElytra(boolean shouldHideElytra) {
        this.shouldHideElytra = shouldHideElytra;
    }

    public boolean shouldHideElytra() {
        return shouldHideElytra;
    }
}
