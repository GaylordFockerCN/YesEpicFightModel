package com.p1nero.efmm;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.command.EFMMCommand;
import com.p1nero.efmm.efmodel.ModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import com.p1nero.efmm.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

import java.io.IOException;
import java.nio.file.Files;

@Mod(EpicFightMeshModelMod.MOD_ID)
public class EpicFightMeshModelMod {

    public static final String MOD_ID = "efmm";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EpicFightMeshModelMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
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
            if(!Files.exists(ServerModelManager.EFMM_CONFIG_PATH)){
                Files.createDirectory(ServerModelManager.EFMM_CONFIG_PATH);
            }
        } catch (IOException e){
            LOGGER.error("Failed to create config path!", e);
        }
    }

    private void onServerStart(ServerStartedEvent event) {
        ServerModelManager.loadAllModels();
        ServerModelManager.loadAllowedModels();
    }

    private void onServerStop(ServerStoppedEvent event) {
        ServerModelManager.saveAllowedModels();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        if(!event.getEntity().level().isClientSide){
            ServerModelManager.authAllAllowedModelToClient(event.getEntity());
        }
    }

    private void registerCommand(RegisterCommandsEvent event){
        EFMMCommand.register(event.getDispatcher());
    }

}
