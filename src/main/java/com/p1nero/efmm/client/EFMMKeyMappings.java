package com.p1nero.efmm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.p1nero.efmm.client.gui.screen.SelectEFModelScreen;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.RequestBindModelPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;

import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EFMMKeyMappings {
    public static final KeyMapping OPEN_SELECT_SCREEN = new KeyMapping("key.efmm.open_select_screen", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.efmm");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SELECT_SCREEN);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class HandleKeyInput{
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if(event.phase.equals(TickEvent.Phase.END)){
                while (OPEN_SELECT_SCREEN.consumeClick()){
                    if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null && !Minecraft.getInstance().isPaused()) {
                        Minecraft.getInstance().setScreen(new SelectEFModelScreen(SELECT_CALL_BACK, SELECT_CALL_BACK));
                    }
                }
            }
        }
    }

    private static final BiConsumer<String, MeshProvider<AnimatedMesh>> SELECT_CALL_BACK = ((modelId, meshProvider) -> {
        if(!modelId.isEmpty()){
            PacketRelay.sendToServer(PacketHandler.MAIN_CHANNEL, new RequestBindModelPacket(modelId));
        }
    });

}
