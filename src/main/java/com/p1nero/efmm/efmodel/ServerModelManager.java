package com.p1nero.efmm.efmodel;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.EFMMConfig;
import com.p1nero.efmm.EpicFightMeshModelMod;
import com.p1nero.efmm.data.EFMMJsonModelLoader;
import com.p1nero.efmm.data.ModelConfig;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.network.PacketHandler;
import com.p1nero.efmm.network.PacketRelay;
import com.p1nero.efmm.network.packet.AuthModelPacket;
import com.p1nero.efmm.network.packet.BindModelPacket;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import com.p1nero.efmm.network.packet.ResetClientModelPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.p1nero.efmm.EpicFightMeshModelMod.EFMM_CONFIG_PATH;
import static com.p1nero.efmm.efmodel.ModelManager.*;

@Mod.EventBusSubscriber(modid = EpicFightMeshModelMod.MOD_ID)
public class ServerModelManager {
    public static final Map<UUID, Set<String>> ALLOWED_MODELS = new HashMap<>();
    public static final Set<String> NATIVE_MODELS = new HashSet<>();
    public static final Map<String, ModelConfig> ALL_MODELS = new HashMap<>();
    public static final Map<UUID, String> ENTITY_MODEL_MAP = new HashMap<>();
    private static final Path ENTITY_MODEL_MAP_PATH = EFMM_CONFIG_PATH.resolve("entity_model_map.json");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path WHITE_LIST_PATH = EFMM_CONFIG_PATH.resolve("white_list.json");
    public static final Set<UUID> UPLOAD_WHITE_LIST = new HashSet<>();
    private static final Path AUTO_BIND_ITEM_LIST_PATH = EFMM_CONFIG_PATH.resolve("auto_bind_item.json");
    public static final Map<Item, String> AUTO_BIND_ITEM_MAP = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int responseDelayTimer;

    /**
     * 接受客户端发送的模型
     */
    public static void registerModel(ServerPlayer sender, String modelId, JsonObject modelJson, JsonObject configJson, byte[] imageCache, byte[] pbrN, byte[] pbrS) {
        if (responseDelayTimer > 0) {
            sender.displayClientMessage(Component.translatable("tip.efmm.sender_in_cooldown", responseDelayTimer / 20), false);
            return;
        }
        responseDelayTimer = EFMMConfig.MAX_RESPONSE_INTERVAL.get();
        if (!UPLOAD_WHITE_LIST.contains(sender.getUUID())) {
            sender.displayClientMessage(Component.translatable("tip.efmm.sender_no_permission"), false);
            LOGGER.info("Sender don't have permission!");
            return;
        }

        if (ALL_MODELS.containsKey(modelId)) {
            sender.displayClientMessage(Component.translatable("tip.efmm.model_already_exist", modelId), false);
            LOGGER.info("Model already exist [{}] in server!", modelId);
            return;
        }

        EFMMJsonModelLoader modelLoader = new EFMMJsonModelLoader(modelJson);
        int positionsCount = modelLoader.getPositionsCountFromJson();
        if (positionsCount > EFMMConfig.MAX_POSITIONS_COUNT.get()) {
            sender.displayClientMessage(Component.translatable("tip.efmm.model_to_large_server", modelId, positionsCount), false);
            LOGGER.info("Received a too large model [{}] from client! Skipped.", modelId);
            return;
        }

        Path modelPath = EFMM_CONFIG_PATH.resolve(modelId);
        try {
            Files.createDirectory(modelPath);
            Path mainJsonPath = modelPath.resolve("main.json");
            Files.writeString(mainJsonPath, GSON.toJson(modelJson));
            Path configJsonPath = modelPath.resolve("config.json");
            Files.writeString(configJsonPath, GSON.toJson(configJson));
            Path texturePath = modelPath.resolve("texture.png");
            Files.write(texturePath, imageCache);
            if (pbrN.length != 0) {
                Path textureNPath = modelPath.resolve("texture_n.png");
                Files.write(textureNPath, pbrN);
            }
            if (pbrS.length != 0) {
                Path textureSPath = modelPath.resolve("texture_s.png");
                Files.write(textureSPath, pbrS);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save client model [{}]", modelId, e);
        }
        LOGGER.info("Setup client model [{}].", modelId);
        EFMMArmatures.ARMATURES.put(modelId, modelLoader.loadArmature(HumanoidArmature::new));
        EFMMJsonModelLoader modelConfigLoader = new EFMMJsonModelLoader(configJson);
        ALL_MODELS.put(modelId, modelConfigLoader.loadModelConfig());
        LOGGER.info("Registered new model [{}] from client.", modelId);
        sender.displayClientMessage(Component.translatable("tip.efmm.success_send_to_server", modelId), false);
        authModelFor(sender, modelId);//自动授权给发送者
    }

    public static Set<String> getOrCreateAllowedModelsFor(Entity player) {
        return ALLOWED_MODELS.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
    }

    public static Set<String> getAllModels() {
        return ALL_MODELS.keySet();
    }

    public static void authAllAllowedModelToClient(Entity player) throws IOException {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new AuthModelPacket(false, getOrCreateAllowedModelsFor(player).stream().toList()), serverPlayer);
            LOGGER.info("Send all allowed models permission to {}", player.getDisplayName().getString());
            for (String modelId : ALLOWED_MODELS.get(player.getUUID())) {
                if (!NATIVE_MODELS.contains(modelId)) {
                    sendModelWithoutCooldown(serverPlayer, modelId);
                }
            }
        }
    }

    public static void bindExistingModelToClient(Entity player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ENTITY_MODEL_MAP.forEach((uuid, modelId) -> {
                Entity entity = serverPlayer.serverLevel().getEntity(uuid);
                if (entity != null) {
                    PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new BindModelPacket(entity.getId(), modelId), serverPlayer);
                }
            });

            LOGGER.info("Send existing model map to {}", player.getDisplayName().getString());
        }
    }

    public static void authModelFor(Entity player, String modelId) {
        getOrCreateAllowedModelsFor(player).add(modelId);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new AuthModelPacket(false, List.of(modelId)), serverPlayer);
            LOGGER.info("Send [{}] permission to {}", modelId, player.getDisplayName().getString());
            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.permission_got", modelId), false);
        }
    }

    public static void sendModelWithoutCooldown(ServerPlayer serverPlayer, String modelId) throws IOException {
        responseDelayTimer = EFMMConfig.MAX_RESPONSE_INTERVAL.get();
        PacketRelay.sendModelToPlayer(new RegisterModelPacket(modelId, getModelJsonLoader(modelId).getRootJson(), getModelConfigJsonLoader(modelId).getRootJson(), getModelTexture(modelId, ""), getModelTexture(modelId, "_n"), getModelTexture(modelId, "_s")), serverPlayer);
        LOGGER.info("Send model [{}] to {}", modelId, serverPlayer.getDisplayName().getString());
    }

    public static void sendModelTo(ServerPlayer serverPlayer, String modelId) throws IOException {
        if (responseDelayTimer > 0) {
            serverPlayer.displayClientMessage(Component.translatable("tip.efmm.sender_in_cooldown", responseDelayTimer / 20), false);
            return;
        }
        responseDelayTimer = EFMMConfig.MAX_RESPONSE_INTERVAL.get();
        PacketRelay.sendModelToPlayer(new RegisterModelPacket(modelId, getModelJsonLoader(modelId).getRootJson(), getModelConfigJsonLoader(modelId).getRootJson(), getModelTexture(modelId, ""), getModelTexture(modelId, "_n"), getModelTexture(modelId, "_s")), serverPlayer);
        LOGGER.info("Send model [{}] to {}", modelId, serverPlayer.getDisplayName().getString());
    }

    public static void authAllModelFor(Entity player) {
        if (getOrCreateAllowedModelsFor(player).addAll(ALL_MODELS.keySet()) && player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new AuthModelPacket(false, ALL_MODELS.keySet().stream().toList()), serverPlayer);
            LOGGER.info("Send all models permission to {}", player.getDisplayName().getString());
        }
    }

    public static void removeAuthFor(Entity player, String modelId) {
        if (getOrCreateAllowedModelsFor(player).remove(modelId) && player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new AuthModelPacket(true, List.of(modelId)), serverPlayer);
            LOGGER.info("Send remove [{}] auth packet to {}", modelId, player.getDisplayName().getString());
        }
    }

    public static void removeAllAuthFor(Entity player) {
        getOrCreateAllowedModelsFor(player).clear();
        if (player instanceof ServerPlayer serverPlayer) {
            PacketRelay.sendToPlayer(PacketHandler.MAIN_CHANNEL, new AuthModelPacket(true, ALL_MODELS.keySet().stream().toList()), serverPlayer);
            LOGGER.info("Send remove all models packet to {}", player.getDisplayName().getString());
        }
    }

    /**
     * 返回当前实体绑定的模型的名字（id）
     */
    @Nullable
    public static String getModelFor(Entity entity) {
        return ENTITY_MODEL_MAP.get(entity.getUUID());
    }

    /**
     * 检查玩家物品是否带有默认绑定的模型
     *
     * @param serverPlayer 被检查的玩家
     * @return 若玩家模型一致则返回true
     */
    public static boolean checkIsModelCorrectWithItem(ServerPlayer serverPlayer) {
        Item item = serverPlayer.getMainHandItem().getItem();
        String modelId = ServerModelManager.AUTO_BIND_ITEM_MAP.get(item);
        return modelId != null && Objects.equals(modelId, ServerModelManager.getModelFor(serverPlayer));
    }

    /**
     * 检查玩家物品是否带有默认绑定的模型，如有则对比当前模型，不一致则替换
     *
     * @param serverPlayer 被检查的玩家
     */
    public static void checkOrBindModelWithItem(ServerPlayer serverPlayer) {
        Item item = serverPlayer.getMainHandItem().getItem();
        String modelId = ServerModelManager.AUTO_BIND_ITEM_MAP.get(item);
        if (modelId != null && !Objects.equals(modelId, ServerModelManager.getModelFor(serverPlayer))) {
            ServerModelManager.bindModelSync(serverPlayer, serverPlayer, modelId);
        }
    }

    public static boolean bindModelFor(Entity entity, String modelId) {
        if (!ALL_MODELS.containsKey(modelId)) {
            return false;
        }
        if (ENTITY_MODEL_MAP.containsKey(entity.getUUID()) && ENTITY_MODEL_MAP.get(entity.getUUID()).equals(modelId)) {
            return true;
        }
        ENTITY_MODEL_MAP.put(entity.getUUID(), modelId);
        entity.refreshDimensions();
        return true;
    }

    public static void bindModelSync(@Nullable ServerPlayer caster, Entity entity, String modelId) {
        //模型与物品的一致则不处理
        if (entity instanceof ServerPlayer serverPlayer && checkIsModelCorrectWithItem(serverPlayer)) {
            if (caster != null) {
                caster.displayClientMessage(entity.getDisplayName().copy().append(Component.translatable("tip.efmm.duplicate_model", getModelFor(serverPlayer))), false);
            }
            return;
        }
        if (bindModelFor(entity, modelId)) {
            PacketRelay.sendToAll(PacketHandler.MAIN_CHANNEL, new BindModelPacket(entity.getId(), modelId));
            if (caster != null) {
                caster.displayClientMessage(Component.translatable("tip.efmm.bind_success", modelId).append(entity.getDisplayName()), false);
            }
        } else {
            if (caster != null) {
                caster.displayClientMessage(Component.translatable("tip.efmm.bind_model_lost", modelId), false);
            }
        }
    }

    public static void removeModelForSync(@Nullable ServerPlayer caster, Entity entity) {
        //模型与物品的一致则不处理
        if (entity instanceof ServerPlayer serverPlayer && checkIsModelCorrectWithItem(serverPlayer)) {
            if (caster != null) {
                caster.displayClientMessage(entity.getDisplayName().copy().append(Component.translatable("tip.efmm.duplicate_model", getModelFor(serverPlayer))), false);
            }
            return;
        }
        removeModelFor(entity);
        PacketRelay.sendToAll(PacketHandler.MAIN_CHANNEL, new ResetClientModelPacket(entity.getId()));
        if (caster != null) {
            caster.displayClientMessage(Component.translatable("tip.efmm.reset_model").append(entity.getDisplayName()), false);
        }
    }

    public static void removeModelFor(Entity entity) {
        ENTITY_MODEL_MAP.remove(entity.getUUID());
        entity.refreshDimensions();
    }

    public static boolean hasNewModel(Entity entity) {
        if(entity instanceof Player player){
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if(playerPatch == null || !playerPatch.isBattleMode()){
                return false;
            }
        }
        return ENTITY_MODEL_MAP.containsKey(entity.getUUID());
    }

    public static Armature getArmatureFor(Entity entity) {
        UUID uuid = entity.getUUID();
        if (ENTITY_MODEL_MAP.containsKey(uuid)) {
            if (EFMMArmatures.ARMATURES.containsKey(ENTITY_MODEL_MAP.get(uuid))) {
                return EFMMArmatures.ARMATURES.get(ENTITY_MODEL_MAP.get(uuid));
            }
        }
        return Armatures.BIPED;
    }


    public static ModelConfig getConfigFor(Entity entity) {
        UUID uuid = entity.getUUID();
        if (ENTITY_MODEL_MAP.containsKey(uuid)) {
            String modelId = ENTITY_MODEL_MAP.get(uuid);
            if (ALL_MODELS.containsKey(modelId)) {
                return ALL_MODELS.get(modelId);
            }
        }
        return ModelConfig.getDefault();
    }

    public static Vec3f getScaleFor(Entity entity) {
        String modelId = ENTITY_MODEL_MAP.get(entity.getUUID());
        if (!ALL_MODELS.containsKey(modelId)) {
            return new Vec3f(1.0F, 1.0F, 1.0F);
        }
        ModelConfig config = ALL_MODELS.get(ENTITY_MODEL_MAP.get(entity.getUUID()));
        return new Vec3f(config.scaleX(), config.scaleY(), config.scaleZ());
    }

    public static void loadAutoBindItemList() {
        AUTO_BIND_ITEM_MAP.clear();
        try {
            if (!Files.exists(AUTO_BIND_ITEM_LIST_PATH)) {
                Files.createFile(AUTO_BIND_ITEM_LIST_PATH);
                try (Writer writer = Files.newBufferedWriter(AUTO_BIND_ITEM_LIST_PATH)) {
                    writer.write("{\n\"efmm:example\" : \"Anon Chihaya\"\n}");
                }
                return;
            }
            try (Reader reader = Files.newBufferedReader(AUTO_BIND_ITEM_LIST_PATH)) {
                Gson gson = new Gson();
                Map<String, String> rawMap = gson.fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
                if (rawMap == null) {
                    Files.deleteIfExists(AUTO_BIND_ITEM_LIST_PATH);
                    return;
                }
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    String itemId = entry.getKey();
                    String modelId = entry.getValue();
                    if (!ALL_MODELS.containsKey(modelId)) {
                        LOGGER.error("Illegal model : [{}]", modelId);
                        return;
                    }
                    ResourceLocation location = new ResourceLocation(itemId);
                    if (ForgeRegistries.ITEMS.containsKey(location)) {
                        Item item = ForgeRegistries.ITEMS.getValue(location);
                        AUTO_BIND_ITEM_MAP.put(item, modelId);
                        LOGGER.info("Load auto bind item : [{}] -> [{}]", itemId, modelId);
                    } else {
                        LOGGER.error("Illegal Item : [{}]", itemId);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load auto bind item list!", e);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create auto bind item list!", e);
        }
    }

    public static void saveUploadWhiteList() {
        try {
            List<String> uuidStrings = UPLOAD_WHITE_LIST.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            try (Writer writer = Files.newBufferedWriter(WHITE_LIST_PATH)) {
                GSON.toJson(uuidStrings, writer);
            }
            LOGGER.info("Saved {} UUIDs to whitelist", UPLOAD_WHITE_LIST.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save upload whitelist", e);
        }
    }

    public static void loadUploadWhiteList() {
        UPLOAD_WHITE_LIST.clear();
        if (!Files.exists(WHITE_LIST_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(WHITE_LIST_PATH)) {
            JsonArray uuidArray = GSON.fromJson(reader, JsonArray.class);
            for (JsonElement element : uuidArray) {
                try {
                    UUID uuid = UUID.fromString(element.getAsString());
                    UPLOAD_WHITE_LIST.add(uuid);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID format in white list: {}", element.getAsString());
                }
            }
            LOGGER.info("Loaded {} UUIDs from whitelist", UPLOAD_WHITE_LIST.size());
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load upload whitelist", e);
        }
    }

    public static void saveEntityModelMap() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(ENTITY_MODEL_MAP_PATH, StandardCharsets.UTF_8)) {
                gson.toJson(ENTITY_MODEL_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save entity model map!", e);
        }
    }

    public static void loadEntityModelMap() {
        try {
            if (Files.exists(ENTITY_MODEL_MAP_PATH)) {
                Gson gson = new Gson();
                Type mapType = new TypeToken<Map<UUID, String>>() {
                }.getType();
                try (Reader reader = Files.newBufferedReader(ENTITY_MODEL_MAP_PATH, StandardCharsets.UTF_8)) {
                    Map<UUID, String> loadedMap = gson.fromJson(reader, mapType);
                    if (loadedMap != null) {
                        ENTITY_MODEL_MAP.putAll(loadedMap);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read entity model map!", e);
        }
    }

    public static void saveAllowedModels() {
        try {
            Path authListsPath = EFMM_CONFIG_PATH.resolve("auth_lists.json");
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<UUID, Set<String>> entry : ALLOWED_MODELS.entrySet()) {
                UUID uuid = entry.getKey();
                Set<String> models = entry.getValue();
                JsonElement modelsArray = GSON.toJsonTree(models);
                jsonObject.add(uuid.toString(), modelsArray);
            }

            String jsonString = GSON.toJson(jsonObject);

            Path parentDir = authListsPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(authListsPath, jsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            LOGGER.info("Saved all allowed models to : " + authListsPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save allowed models!", e);
        }

    }

    public static void loadAllowedModels() {
        try {
            Path authListsPath = EFMM_CONFIG_PATH.resolve("auth_lists.json");
            if (!Files.exists(authListsPath)) {
                return;
            }
            EFMMJsonModelLoader authListLoader = new EFMMJsonModelLoader(authListsPath.toFile());
            JsonObject rootJson = authListLoader.getRootJson();
            for (Map.Entry<String, JsonElement> entry : rootJson.entrySet()) {
                String uuidStr = entry.getKey();
                JsonElement valueElement = entry.getValue();

                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID : {}, skipped", uuidStr);
                    continue;
                }

                if (!valueElement.isJsonArray()) {
                    LOGGER.error("Invalid value in UUID: {} , skipped", uuid);
                    continue;
                }

                JsonArray jsonArray = valueElement.getAsJsonArray();
                Set<String> models = new HashSet<>();
                for (JsonElement element : jsonArray) {
                    if (element.isJsonPrimitive()) {
                        String modelId = element.getAsString();
                        if (!ALL_MODELS.containsKey(modelId)) {
                            LOGGER.error("Model [{}] doesn't exist! skipped", modelId);
                            continue;
                        }
                        models.add(element.getAsString());
                    } else {
                        LOGGER.error("Invalid value in UUID: {}, skipped", uuid);
                    }
                }
                ALLOWED_MODELS.put(uuid, models);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to load allowed models!", e);
        }

    }

    /**
     * 从config读取所有模型文件并缓存
     */
    public static void loadAllModels() {

        try (Stream<Path> subDirs = Files.list(EFMM_CONFIG_PATH)) {
            subDirs.filter(Files::isDirectory).forEach(modelFileDir -> {
                try {
                    String modelId = modelFileDir.toFile().getName();
                    Path mainJsonPath = modelFileDir.resolve("main.json");
                    EFMMJsonModelLoader mainJsonLoader = new EFMMJsonModelLoader(mainJsonPath.toFile());
                    EFMMArmatures.ARMATURES.put(modelId, mainJsonLoader.loadArmature(HumanoidArmature::new));
                    Path configJsonPath = modelFileDir.resolve("config.json");
                    EFMMJsonModelLoader configJsonLoader = new EFMMJsonModelLoader(configJsonPath.toFile());
                    ALL_MODELS.put(modelId, configJsonLoader.loadModelConfig());
                    LOGGER.info("LOAD EPIC FIGHT MODEL >> {}", modelId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error when loading models", e);
            throw new RuntimeException(e);
        }
    }

    public static void clearModels() {
        ALL_MODELS.clear();
    }

    public static void reloadEFModels() {
        clearModels();
        loadAllModels();
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onEntitySizeChange(EntityEvent.Size event){
        if(hasNewModel(event.getEntity()) && !event.getEntity().level().isClientSide){
            ModelConfig modelConfig = ALL_MODELS.get(getModelFor(event.getEntity()));
            EntityDimensions original = event.getNewSize();
            EntityDimensions newSize = original.fixed ? EntityDimensions.fixed(original.width * modelConfig.getDimScaleXZ(), original.height * modelConfig.getDimScaleY()) : original.scale(modelConfig.getDimScaleXZ(), modelConfig.getDimScaleY());
            event.setNewSize(newSize, true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (responseDelayTimer > 0) {
            responseDelayTimer--;
        }
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        ServerModelManager.loadAllModels();
        ServerModelManager.loadEntityModelMap();
        ServerModelManager.loadAllowedModels();
        ServerModelManager.loadUploadWhiteList();
        ServerModelManager.loadAutoBindItemList();
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        ServerModelManager.saveAllowedModels();
        ServerModelManager.saveUploadWhiteList();
        ServerModelManager.saveEntityModelMap();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide) {
            try {
                ServerModelManager.authAllAllowedModelToClient(event.getEntity());
                ServerModelManager.bindExistingModelToClient(event.getEntity());
                event.getEntity().refreshDimensions();
                if (event.getEntity().getServer() != null && event.getEntity().getServer().isSingleplayer()) {
                    ServerModelManager.authAllModelFor(event.getEntity());
                }

            } catch (IOException e) {
                LOGGER.error("Failed to sync model to client!", e);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (ServerModelManager.AUTO_BIND_ITEM_MAP.containsKey(event.getTo().getItem())) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerModelManager.checkOrBindModelWithItem(player);
            }
        }
        if (ServerModelManager.AUTO_BIND_ITEM_MAP.containsKey(event.getFrom().getItem()) && !ServerModelManager.AUTO_BIND_ITEM_MAP.containsKey(event.getTo().getItem())) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerModelManager.removeModelForSync(player, player);
            }
        }
    }

}
