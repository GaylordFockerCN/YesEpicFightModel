package com.p1nero.efmm.data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import com.mojang.logging.LogUtils;
import io.netty.util.internal.StringUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.AnimatedMesh.AnimatedModelPart;
import yesman.epicfight.api.client.model.AnimatedVertexBuilder;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.Meshes.MeshContructor;
import yesman.epicfight.api.client.model.RawMesh;
import yesman.epicfight.api.client.model.RawMesh.RawModelPart;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer.VanillaMeshPartDefinition;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.gameasset.Armatures.ArmatureContructor;

public class EFMMJsonModelLoader {
    public static final OpenMatrix4f BLENDER_TO_MINECRAFT_COORD = OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS);
    private final JsonObject rootJson;
    private final String fileHash;
    private String name = "";
    private static final Logger LOGGER = LogUtils.getLogger();

    private int positionCount = 0;

    public EFMMJsonModelLoader(ResourceLocation resourceLocation) throws IllegalStateException {
        JsonReader jsonReader;

        // In this case, reads the animation data from mod.jar (Especially in a server)
        Class<?> modClass = ModList.get().getModObjectById(resourceLocation.getNamespace()).get().getClass();
        InputStream inputStream = modClass.getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());

        if (inputStream == null) {
            throw new NoSuchElementException("Can't find specified file in mod resource " + resourceLocation);
        }

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        InputStreamReader reader = new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8);

        jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        this.rootJson = Streams.parse(jsonReader).getAsJsonObject();

        try {
            jsonReader.close();
        } catch (IOException e) {
            LOGGER.info("Failed to close jsonReader", e);
        }
        this.fileHash = getSHA256Hash(this.rootJson.toString());
    }

    public EFMMJsonModelLoader(File file) throws IllegalStateException, FileNotFoundException {
        this.name = file.getName();
        JsonReader jsonReader = new JsonReader(new FileReader(file));
        jsonReader.setLenient(true);
        this.rootJson = Streams.parse(jsonReader).getAsJsonObject();

        try {
            jsonReader.close();
        } catch (IOException e) {
            LOGGER.info("Failed to close jsonReader", e);
        }
        this.fileHash = getSHA256Hash(this.rootJson.toString());
    }

    public static String getSHA256Hash(String str){
        String hashStream = "";

        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte[] byteData = sh.digest();
            StringBuilder sb = new StringBuilder();

            for (byte byteDatum : byteData) {
                sb.append(Integer.toString((byteDatum & 0xFF) + 0x100, 16).substring(1));
            }

            hashStream = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error when getSHA256Hash", e);
            hashStream = null;
        }

        return hashStream;
    }

    public EFMMJsonModelLoader(InputStream inputstream) throws IOException {
        JsonReader jsonReader;
        jsonReader = new JsonReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
        jsonReader.setLenient(true);
        this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
        jsonReader.close();
        this.fileHash = StringUtil.EMPTY_STRING;
    }

    public EFMMJsonModelLoader(JsonObject rootJson) {
        this.rootJson = rootJson;
        this.fileHash = StringUtil.EMPTY_STRING;
    }

    @OnlyIn(Dist.CLIENT)
    public AnimatedMesh.RenderProperties getRenderProperties() {
        if (!this.rootJson.has("render_properties")) {
            return null;
        }

        JsonObject properties = this.rootJson.getAsJsonObject("render_properties");
        AnimatedMesh.RenderProperties renderProperties = AnimatedMesh.RenderProperties.create();

        if (properties != null) {
            if (properties.has("transparent")) {
                renderProperties.transparency(properties.get("transparent").getAsBoolean());
            }

            if (properties.has("texture_path")) {
                renderProperties.customTexturePath(properties.get("texture_path").getAsString());
            }

            if (properties.has("parent_part_visualizer")) {
                JsonObject partVisualizer = properties.get("parent_part_visualizer").getAsJsonObject();

                partVisualizer.entrySet().forEach((entry) -> renderProperties.newPartVisualizer(entry.getKey(), entry.getValue().getAsBoolean()));
            }

            return renderProperties;
        }

        return renderProperties;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getParent() {
        return this.rootJson.has("parent") ? new ResourceLocation(this.rootJson.get("parent").getAsString()) : null;
    }

    @OnlyIn(Dist.CLIENT)
    public <T extends RawMesh> T loadMesh(MeshContructor<RawModelPart, VertexBuilder, T> constructor) {
        JsonObject obj = this.rootJson.getAsJsonObject("vertices");
        JsonObject positions = obj.getAsJsonObject("positions");
        JsonObject normals = obj.getAsJsonObject("normals");
        JsonObject uvs = obj.getAsJsonObject("uvs");
        JsonObject parts = obj.getAsJsonObject("parts");
        JsonObject indices = obj.getAsJsonObject("indices");

        float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());
        positionCount = positions.get("count").getAsInt();
        for (int i = 0; i < positionArray.length / 3; i++) {
            int k = i * 3;
            Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
            OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
            positionArray[k] = posVector.x;
            positionArray[k+1] = posVector.y;
            positionArray[k+2] = posVector.z;
        }

        float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());

        for (int i = 0; i < normalArray.length / 3; i++) {
            int k = i * 3;
            Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
            OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
            normalArray[k] = normVector.x;
            normalArray[k+1] = normVector.y;
            normalArray[k+2] = normVector.z;
        }

        float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());

        Map<String, float[]> arrayMap = Maps.newHashMap();
        Map<MeshPartDefinition, List<VertexBuilder>> meshMap = Maps.newHashMap();

        arrayMap.put("positions", positionArray);
        arrayMap.put("normals", normalArray);
        arrayMap.put("uvs", uvArray);

        if (parts != null) {
            for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
                meshMap.put(VanillaMeshPartDefinition.of(e.getKey()), VertexBuilder.createVertexIndicator(ParseUtil.toIntArray(e.getValue().getAsJsonObject().get("array").getAsJsonArray())));
            }
        }

        if (indices != null) {
            meshMap.put(VanillaMeshPartDefinition.of("noGroups"), VertexBuilder.createVertexIndicator(ParseUtil.toIntArray(indices.get("array").getAsJsonArray())));
        }

        return constructor.invoke(arrayMap, meshMap, null, this.getRenderProperties());
    }

    public int getPositionsCountFromJson(){
        JsonObject obj = this.rootJson.getAsJsonObject("vertices");
        JsonObject positions = obj.getAsJsonObject("positions");
        return positions.get("count").getAsInt();
    }

    @OnlyIn(Dist.CLIENT)
    public <T extends AnimatedMesh> T loadAnimatedMesh(MeshContructor<AnimatedModelPart, AnimatedVertexBuilder, T> constructor) {
        JsonObject obj = this.rootJson.getAsJsonObject("vertices");
        JsonObject positions = obj.getAsJsonObject("positions");
        JsonObject normals = obj.getAsJsonObject("normals");
        JsonObject uvs = obj.getAsJsonObject("uvs");
        JsonObject vindices = obj.getAsJsonObject("vindices");
        JsonObject weights = obj.getAsJsonObject("weights");
        JsonObject vcounts = obj.getAsJsonObject("vcounts");
        JsonObject parts = obj.getAsJsonObject("parts");
        JsonObject indices = obj.getAsJsonObject("indices");

        float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());
        positionCount = positions.get("count").getAsInt();

        for (int i = 0; i < positionArray.length / 3; i++) {
            int k = i * 3;
            Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
            OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
            positionArray[k] = posVector.x;
            positionArray[k+1] = posVector.y;
            positionArray[k+2] = posVector.z;
        }

        float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());

        for (int i = 0; i < normalArray.length / 3; i++) {
            int k = i * 3;
            Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
            OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
            normalArray[k] = normVector.x;
            normalArray[k+1] = normVector.y;
            normalArray[k+2] = normVector.z;
        }

        float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());
        int[] animationIndexArray = ParseUtil.toIntArray(vindices.get("array").getAsJsonArray());
        float[] weightArray = ParseUtil.toFloatArray(weights.get("array").getAsJsonArray());
        int[] vcountArray = ParseUtil.toIntArray(vcounts.get("array").getAsJsonArray());

        Map<String, float[]> arrayMap = Maps.newHashMap();
        Map<MeshPartDefinition, List<AnimatedVertexBuilder>> meshMap = Maps.newHashMap();

        arrayMap.put("positions", positionArray);
        arrayMap.put("normals", normalArray);
        arrayMap.put("uvs", uvArray);
        arrayMap.put("weights", weightArray);

        if (parts != null) {
            for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
                meshMap.put(VanillaMeshPartDefinition.of(e.getKey()), VertexBuilder.createAnimated(ParseUtil.toIntArray(e.getValue().getAsJsonObject().get("array").getAsJsonArray()), vcountArray, animationIndexArray));
            }
        }

        if (indices != null) {
            meshMap.put(VanillaMeshPartDefinition.of("noGroups"), VertexBuilder.createAnimated(ParseUtil.toIntArray(indices.get("array").getAsJsonArray()), vcountArray, animationIndexArray));
        }

        return constructor.invoke(arrayMap, meshMap, null, this.getRenderProperties());
    }

    public int getPositionCountAfterLoadMesh() {
        return positionCount;
    }

    public ModelConfig loadModelConfig(){
        if(!this.rootJson.has("author")){
            throw new IllegalArgumentException("Model author is null!");
        }
        ModelConfig modelConfig = new ModelConfig(
                this.rootJson.get("author").getAsString(),
                this.rootJson.get("scaleX").getAsFloat(),
                this.rootJson.get("scaleY").getAsFloat(),
                this.rootJson.get("scaleZ").getAsFloat());

        if(this.rootJson.has("shouldHideWearable")){
            modelConfig.setShouldHideWearable(this.rootJson.get("shouldHideWearable").getAsBoolean());
        }
        if(this.rootJson.has("shouldHideElytra")){
            modelConfig.setShouldHideElytra(this.rootJson.get("shouldHideElytra").getAsBoolean());
        }

        return modelConfig;
    }

    public <T extends Armature> T loadArmature(ArmatureContructor<T> constructor) {
        JsonObject obj = this.rootJson.getAsJsonObject("armature");
        JsonObject hierarchy = obj.get("hierarchy").getAsJsonArray().get(0).getAsJsonObject();
        JsonArray nameAsVertexGroups = obj.getAsJsonArray("joints");
        Map<String, Joint> jointMap = Maps.newHashMap();
        Joint joint = getJoint(hierarchy, nameAsVertexGroups, jointMap, true);
        joint.initOriginTransform(new OpenMatrix4f());

        return constructor.invoke(name, jointMap.size(), joint, jointMap);
    }

    public static Joint getJoint(JsonObject object, JsonArray nameAsVertexGroups, Map<String, Joint> jointMap, boolean start) {
        float[] floatArray = ParseUtil.toFloatArray(object.get("transform").getAsJsonArray());
        OpenMatrix4f localMatrix = OpenMatrix4f.load(null, floatArray);
        localMatrix.transpose();

        if (start) {
            localMatrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
        }

        String name = object.get("name").getAsString();
        int index = -1;

        for (int i = 0; i < nameAsVertexGroups.size(); i++) {
            if (name.equals(nameAsVertexGroups.get(i).getAsString())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalStateException("[ModelParsingError]: Joint name " + name + " doesn't exist!");
        }

        Joint joint = new Joint(name, index, localMatrix);
        jointMap.put(name, joint);

        if (object.has("children")) {
            for (JsonElement children : object.get("children").getAsJsonArray()) {
                joint.addSubJoint(getJoint(children.getAsJsonObject(), nameAsVertexGroups, jointMap, false));
            }
        }

        return joint;
    }

    public JsonObject getRootJson() {
        return this.rootJson;
    }

    public String getFileHash() {
        return this.fileHash;
    }


}