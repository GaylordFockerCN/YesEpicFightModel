package com.p1nero.efmm;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.command.EFMMCommand;
import com.p1nero.efmm.efmodel.LogicServerModelManager;
import com.p1nero.efmm.efmodel.ModelManager;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import com.p1nero.efmm.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;

import java.io.IOException;
import java.nio.file.Files;

@Mod(EpicFightMeshModelMod.MOD_ID)
public class EpicFightMeshModelMod {

    public static final String MOD_ID = "efmm";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EpicFightMeshModelMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::buildMesh);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EFMMConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
        PacketHandler.register();
        ModelManager.loadNative();//附属作者同样可以用这种方式把模型加入到模组里
        try {
            if(!Files.exists(LogicServerModelManager.EFMM_CONFIG_PATH)){
                Files.createDirectory(LogicServerModelManager.EFMM_CONFIG_PATH);
            }
        } catch (IOException e){
            LOGGER.error("Failed to create config path!", e);
        }
    }

    private void buildMesh(final ModelBuildEvent.MeshBuild event){
        EFMMMeshes.loadNativeMeshes();
    }

    private void onServerStart(ServerStartedEvent event) {
        LogicServerModelManager.loadAllModels();
        LogicServerModelManager.loadAllowedModels();
        LogicServerModelManager.loadUploadWhiteList();
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        LogicServerModelManager.clientTick();
    }

    private void onServerStop(ServerStoppedEvent event) {
        LogicServerModelManager.saveAllowedModels();
        LogicServerModelManager.saveUploadWhiteList();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        if(!event.getEntity().level().isClientSide){
            try {
                LogicServerModelManager.authAllAllowedModelToClient(event.getEntity());
            } catch (IOException e){
                LOGGER.error("Failed to auth allowed model to client!", e);
            }
        }
    }

    private void registerCommand(RegisterCommandsEvent event){
        EFMMCommand.register(event.getDispatcher());
    }

}
