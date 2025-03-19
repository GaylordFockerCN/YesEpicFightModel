package com.p1nero.efmm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.efmodel.ServerModelManager;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.ResetClientModelPacket;
import com.p1nero.efmm.network.packet.BindModelPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public class EFMMCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("authEFModel").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entities", EntityArgument.entities())
                        .then(Commands.argument("model_id", StringArgumentType.string())
                                .suggests(((commandContext, suggestionsBuilder) -> {
                                    for (String s : ServerModelManager.getAllModels()) {
                                        suggestionsBuilder.suggest("\"" + s + "\"");
                                    }
                                    return suggestionsBuilder.buildFuture();
                                }))
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        String modelId = StringArgumentType.getString(context, "model_id");
                                        ServerModelManager.authModelFor(entity, modelId);
                                        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.auth_send", modelId).append(serverPlayer.getDisplayName()), false);
                                        }
                                        LOGGER.info("give {} permission to use \"{}\" ", entity.getDisplayName().getString(), modelId);
                                    }
                                    return 0;
                                })
                        )
                        .then(Commands.literal("addAll")
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        ServerModelManager.authAllModelFor(entity);
                                        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.auth_all_send").append(entity.getDisplayName()), false);
                                        }
                                        LOGGER.info("Give {} permission to use all models", entity.getDisplayName().getString());
                                    }
                                    return 0;
                                })
                        )
                )
        );

        dispatcher.register(Commands.literal("removeEFModelAuth").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entities", EntityArgument.entities())
                        .then(Commands.argument("model_id", StringArgumentType.string())
                                .suggests(((commandContext, suggestionsBuilder) -> {
                                    for (String s : ServerModelManager.getAllModels()) {
                                        suggestionsBuilder.suggest("\"" + s + "\"");
                                    }
                                    return suggestionsBuilder.buildFuture();
                                }))
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        String modelId = StringArgumentType.getString(context, "model_id");
                                        ServerModelManager.removeAuthFor(entity, modelId);
                                        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.auth_remove", modelId).append(entity.getDisplayName()), false);
                                        }
                                        LOGGER.info("remove {} permission to use \"{}\" ", entity.getDisplayName().getString(), modelId);
                                    }
                                    return 0;
                                })
                        )
                        .then(Commands.literal("removeAll")
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        ServerModelManager.removeAllAuthFor(entity);
                                        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.auth_all_remove").append(entity.getDisplayName()), false);
                                        }
                                        LOGGER.info("Remove {} permission to use all models", entity.getDisplayName().getString());
                                    }
                                    return 0;
                                })
                        )
                )
        );

        dispatcher.register(Commands.literal("reloadEFModels").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .executes((context) -> {
                    ServerModelManager.reloadEFModels();
                    if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(Component.translatable("tip.efmm.model_reload"), false);
                    }
                    return 0;
                })
        );

        dispatcher.register(Commands.literal("bindEFModelSelf")
                .then(Commands.argument("model_id", StringArgumentType.string())
                        .suggests(((context, suggestionsBuilder) -> {
                            Entity entity = context.getSource().getEntity();
                            if (entity instanceof ServerPlayer serverPlayer) {
                                for (String s : ServerModelManager.getOrCreateAllowedModelsFor(serverPlayer)) {
                                    suggestionsBuilder.suggest("\"" + s + "\"");
                                }
                            }
                            return suggestionsBuilder.buildFuture();
                        }))
                        .executes((context) -> {
                            Entity entity = context.getSource().getEntity();
                            if (entity == null) {
                                return -1;
                            }
                            String modelId = StringArgumentType.getString(context, "model_id");
                            if (ServerModelManager.getOrCreateAllowedModelsFor(entity).contains(modelId)) {
                                bind(context, entity, modelId);
                            } else {
                                if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                    serverPlayer.displayClientMessage(Component.translatable("tip.efmm.bind_failed", modelId), false);
                                }
                            }
                            return 0;
                        })
                )
        );

        dispatcher.register(Commands.literal("bindEFModelFor").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entities", EntityArgument.entities())
                        .then(Commands.argument("model_id", StringArgumentType.string())
                                .suggests(((commandContext, suggestionsBuilder) -> {
                                    for (String s : ServerModelManager.getAllModels()) {
                                        suggestionsBuilder.suggest("\"" + s + "\"");
                                    }
                                    return suggestionsBuilder.buildFuture();
                                }))
                                .executes((context) -> {
                                    for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                        String modelId = StringArgumentType.getString(context, "model_id");
                                        if (ServerModelManager.ALLOWED_MODELS.get(entity.getUUID()).contains(modelId)) {
                                            bind(context, entity, modelId);
                                        } else {
                                            if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                                serverPlayer.displayClientMessage(entity.getDisplayName().copy().append(Component.translatable("tip.efmm.bind_without_permission", modelId)), false);
                                            }
                                            LOGGER.warn("{} doesn't have permission to use \"{}\" ", entity.getDisplayName().getString(), modelId);
                                        }
                                    }
                                    return 0;
                                })
                        )
                )
        );

        dispatcher.register(Commands.literal("resetEFModelSelf")
                .executes((context) -> {
                    Entity entity = context.getSource().getEntity();
                    if (entity == null) {
                        return -1;
                    }
                    ServerModelManager.removeModelFor(entity);
                    PacketRelay.sendToAll(PacketHandler.INSTANCE, new ResetClientModelPacket(entity.getId()));
                    if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(Component.literal("reset model for ").append(entity.getDisplayName()), true);
                    }
                    return 0;
                })
        );

        dispatcher.register(Commands.literal("resetEFModelFor").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entities", EntityArgument.entities())
                        .executes((context) -> {
                            for (Entity entity : EntityArgument.getEntities(context, "entities")) {
                                ServerModelManager.removeModelFor(entity);
                                if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                    serverPlayer.displayClientMessage(Component.translatable("tip.efmm.reset_model").append(entity.getDisplayName()), false);
                                }
                                PacketRelay.sendToAll(PacketHandler.INSTANCE, new ResetClientModelPacket(entity.getId()));
                            }
                            return 0;
                        })
                )
        );
    }

    public static void bind(CommandContext<CommandSourceStack> context, Entity entity, String modelId) {
        ServerModelManager.bindModelSync(context.getSource().getPlayer(), entity, modelId);
    }

}
