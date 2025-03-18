package com.p1nero.efmm;

import com.mojang.logging.LogUtils;
import com.p1nero.efmm.command.EFMMCommand;
import com.p1nero.efmm.efmodel.ServerModelManager;
import com.p1nero.efmm.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EpicFightMeshModelMod.MOD_ID)
public class EpicFightMeshModelMod {

    public static final String MOD_ID = "efmm";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EpicFightMeshModelMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EFMMConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }

    private void onServerStart(ServerStartedEvent event){
        ServerModelManager.loadAllModels();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        if(!event.getEntity().level().isClientSide){
            ServerModelManager.syncAllAllowedModelToClient(event.getEntity());
        }
    }

    private void registerCommand(RegisterCommandsEvent event){
        EFMMCommand.register(event.getDispatcher());
    }

}
