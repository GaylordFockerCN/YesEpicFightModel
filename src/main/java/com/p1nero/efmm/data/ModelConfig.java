package com.p1nero.efmm.data;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.Set;

public class ModelConfig {
    protected float scaleX;
    protected float scaleY;
    protected float scaleZ;

    protected String authorName;
    protected Set<EquipmentSlot> hideArmorList;
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
        return new ModelConfig();
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

    public Set<EquipmentSlot> getHideArmorList() {
        return hideArmorList;
    }

    public void setHideArmorList(Set<EquipmentSlot> hideArmorList) {
        this.hideArmorList = hideArmorList;
    }


}
