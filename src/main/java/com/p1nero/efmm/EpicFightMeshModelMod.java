package com.p1nero.efmm;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.command.EFMMClientCommand;
import com.p1nero.efmm.command.EFMMCommand;
import com.p1nero.efmm.compat.OculusCompat;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import com.p1nero.efmm.efmodel.ModelManager;
import com.p1nero.efmm.network.PacketHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Mod(EpicFightMeshModelMod.MOD_ID)
public class EpicFightMeshModelMod {

    public static final String MOD_ID = "efmm";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Path EFMM_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

    public EpicFightMeshModelMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onModLoadCompat);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
        MinecraftForge.EVENT_BUS.addListener(this::registerClientCommand);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EFMMConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
        event.enqueueWork(PacketHandler::register);
        event.enqueueWork(ModelManager::loadNative);//附属作者同样可以用这种方式把模型加入到模组里
        event.enqueueWork(()->{
            try {
                if(!Files.exists(EFMM_CONFIG_PATH)){
                    Files.createDirectory(EFMM_CONFIG_PATH);
                }
            } catch (IOException e){
                LOGGER.error("Failed to create config path!", e);
            }
        });
    }

    private void onModLoadCompat(final InterModEnqueueEvent event){
        event.enqueueWork(()-> checkModLoad("oculus", () -> OculusCompat::registerPBRLoader));
    }

    public static void checkModLoad(String modId, Supplier<Runnable> runnableSupplier) {
        if (ModList.get().isLoaded(modId)) {
            runnableSupplier.get().run();
        }
    }

    private void onServerStart(ServerStartedEvent event) {
        ServerModelManager.loadAllModels();
        ServerModelManager.loadAllowedModels();
        ServerModelManager.loadUploadWhiteList();
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        ClientModelManager.clientTick();
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        ServerModelManager.serverTick();
    }

    private void onServerStop(ServerStoppedEvent event) {
        ServerModelManager.saveAllowedModels();
        ServerModelManager.saveUploadWhiteList();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        if(!event.getEntity().level().isClientSide){
            try {
                ServerModelManager.authAllAllowedModelToClient(event.getEntity());
                ServerModelManager.bindExistingModelToClient(event.getEntity());
            } catch (IOException e){
                LOGGER.error("Failed to sync model to client!", e);
            }
        }
    }

    private void registerCommand(RegisterCommandsEvent event){
        EFMMCommand.register(event.getDispatcher());
    }
    private void registerClientCommand(RegisterClientCommandsEvent event){
        EFMMClientCommand.register(event.getDispatcher());
    }

}
