package com.p1nero.efmm;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EFMMConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MODELS_STRINGS = BUILDER.comment("需要加载的模型列表（用逗号隔开）", "Models that should be loaded").defineListAllowEmpty("items", List.of("efmm:entity/anon"), (s) -> true);
    static final ForgeConfigSpec SPEC = BUILDER.build();
    public static Set<ResourceLocation> MODELS;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        MODELS = MODELS_STRINGS.get().stream().map(ResourceLocation::new).collect(Collectors.toSet());
    }
}
