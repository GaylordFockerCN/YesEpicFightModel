package com.p1nero.efmm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.ResetArmaturePacket;
import com.p1nero.efmm.network.packet.SyncArmaturePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class EFMMCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bindMesh")
                .then(Commands.argument("entities", EntityArgument.entities())
                        .then(Commands.argument("resource_location", StringArgumentType.string())
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        ResourceLocation resourceLocation = new ResourceLocation(StringArgumentType.getString(context, "resource_location"));
                                        if(EFMMArmatures.bindArmature(entity, resourceLocation)){
                                            PacketRelay.sendToAll(PacketHandler.INSTANCE, new SyncArmaturePacket(entity.getId(), resourceLocation));
                                        } else {
                                            if(context.getSource().getPlayer() != null){
                                                context.getSource().getPlayer().displayClientMessage(Component.literal("armature [" + resourceLocation + "] doesn't exist"), true);
                                            }
                                        }
                                    }
                                    return 0;
                                })
                        )
                )
        );
        dispatcher.register(Commands.literal("resetMeshFor")
                .then(Commands.argument("entities", EntityArgument.entities())
                        .executes((context) -> {
                            for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                EFMMArmatures.removeArmature(entity);
                                PacketRelay.sendToAll(PacketHandler.INSTANCE, new ResetArmaturePacket(entity.getId()));
                            }
                            return 0;
                        })
                )
        );
    }
}
