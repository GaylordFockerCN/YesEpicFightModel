package com.p1nero.efmm;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EFMMConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue MAX_POSITIONS_COUNT = BUILDER.comment("Max Positions Count of Model",  "允许接收的模型的最大的顶点数")
            .defineInRange("max_positions_count", 114514, 0, Integer.MAX_VALUE);
    static final ForgeConfigSpec SPEC = BUILDER.build();
}
