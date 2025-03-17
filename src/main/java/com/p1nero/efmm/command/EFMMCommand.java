package com.p1nero.efmm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.p1nero.efmm.EFMMConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.ReloadModelsPacket;
import com.p1nero.efmm.network.packet.ResetArmaturePacket;
import com.p1nero.efmm.network.packet.SyncArmaturePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class EFMMCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reloadMeshModels").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .executes((context) -> {
                    EFMMArmatures.reloadArmatures();
                    PacketRelay.sendToAll(PacketHandler.INSTANCE, new ReloadModelsPacket());
                    return 0;
                })
        );
        dispatcher.register(Commands.literal("bindMeshForSelf")
                .then(Commands.argument("resource_location", StringArgumentType.string())
                        .suggests(((commandContext, suggestionsBuilder) -> {
                            for (String s : EFMMConfig.MODELS_STRINGS.get()) {
                                suggestionsBuilder.suggest("\"" + s + "\"");
                            }
                            return suggestionsBuilder.buildFuture();
                        }))
                        .executes((context) -> {
                            Entity entity =  context.getSource().getEntity();
                            if(entity == null){
                                return -1;
                            }
                            bind(entity, StringArgumentType.getString(context, "resource_location"));
                            return 0;
                        })
                )
        );
        dispatcher.register(Commands.literal("bindMeshFor").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entities", EntityArgument.entities())
                        .then(Commands.argument("resource_location", StringArgumentType.string())
                                .suggests(((commandContext, suggestionsBuilder) -> {
                                    for (String s : EFMMConfig.MODELS_STRINGS.get()) {
                                        suggestionsBuilder.suggest("\"" + s + "\"");
                                    }
                                    return suggestionsBuilder.buildFuture();
                                }))
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        bind(entity, StringArgumentType.getString(context, "resource_location"));
                                    }
                                    return 0;
                                })
                        )
                )
        );
        dispatcher.register(Commands.literal("resetMeshForSelf")
                .executes((context) -> {
                    Entity entity = context.getSource().getEntity();
                    if(entity == null){
                        return -1;
                    }
                    EFMMArmatures.removeArmature(entity);
                    PacketRelay.sendToAll(PacketHandler.INSTANCE, new ResetArmaturePacket(entity.getId()));
                    return 0;
                })
        );
        dispatcher.register(Commands.literal("resetMeshFor").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
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

    public static void bind(Entity entity, String locationString){
        ResourceLocation resourceLocation = new ResourceLocation(locationString);
        if (EFMMArmatures.bindArmature(entity, resourceLocation)) {
            PacketRelay.sendToAll(PacketHandler.INSTANCE, new SyncArmaturePacket(entity.getId(), resourceLocation));
        } else {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("armature [" + resourceLocation + "] doesn't exist"), true);
            }
        }
    }

}
