package com.p1nero.efmm.efmodel;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;

public class ModelManager {

    public static boolean hasArmature(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.hasArmature(entity);
        } else {
            return ServerModelManager.hasArmature(entity);
        }
    }

    public static Armature getArmatureFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getArmatureFor(entity);
        } else {
            return ServerModelManager.getArmatureFor(entity);
        }
    }

    public static Vec3f getScaleFor(Entity entity){
        if(entity.level().isClientSide){
            return ClientModelManager.getScaleFor(entity);
        } else {
            return ServerModelManager.getScaleFor(entity);
        }
    }

}
