package tokyo.ghoul.client.features.modules.render.custommodels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CustomMesh {
    private static final float PIXEL = 16.0F;

    private CustomMesh() {
    }

    record Baked(ModelPart root, float headPitch) {
    }

    static Baked bake(String name) {
        String path = "/assets/desire/models/" + name + ".json";
        try (InputStream input = CustomMesh.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing model " + path);
            JsonObject json = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            float headPitch = json.has("head_pitch") ? json.get("head_pitch").getAsFloat() : 1.0F;
            JsonArray textureSize = json.getAsJsonArray("texture_size");
            int texW = textureSize.get(0).getAsInt();
            int texH = textureSize.get(1).getAsInt();
            if (json.has("format") && json.get("format").getAsInt() >= 4) {
                Map<String, ModelPart> children = new HashMap<>();
                for (var part : json.getAsJsonArray("parts")) {
                    JsonObject data = part.getAsJsonObject();
                    children.put(data.get("name").getAsString(), buildPart(data, texW, texH));
                }
                return new Baked(new ModelPart(List.of(), children), headPitch);
            }
            MeshDefinition mesh = new MeshDefinition();
            for (var part : json.getAsJsonArray("parts")) {
                addPart(mesh.getRoot(), part.getAsJsonObject());
            }
            ModelPart root = LayerDefinition.create(mesh, texW, texH).bakeRoot();
            return new Baked(root, headPitch);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to load custom model " + name, e);
        }
    }

    private static void addPart(PartDefinition parent, JsonObject part) {
        CubeListBuilder cubes = CubeListBuilder.create();
        if (part.has("cubes")) {
            for (var element : part.getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                float[] origin = vec(cube.getAsJsonArray("origin"));
                float[] size = vec(cube.getAsJsonArray("size"));
                JsonArray uv = cube.getAsJsonArray("uv");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                        .addBox(origin[0], origin[1], origin[2], size[0], size[1], size[2], CubeDeformation.NONE);
            }
        }
        PartDefinition definition = parent.addOrReplaceChild(part.get("name").getAsString(), cubes, pose(part));
        if (part.has("children")) {
            for (var child : part.getAsJsonArray("children")) {
                addPart(definition, child.getAsJsonObject());
            }
        }
    }

    // Format 4: parts carry triangle meshes ("mesh") next to optional box cubes, so the tree is built by hand.
    private static ModelPart buildPart(JsonObject part, int texW, int texH) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        if (part.has("cubes")) {
            for (var element : part.getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                float[] origin = vec(cube.getAsJsonArray("origin"));
                float[] size = vec(cube.getAsJsonArray("size"));
                JsonArray uv = cube.getAsJsonArray("uv");
                cubes.add(new ModelPart.Cube(uv.get(0).getAsInt(), uv.get(1).getAsInt(),
                        origin[0], origin[1], origin[2], size[0], size[1], size[2], 0.0F, 0.0F, 0.0F,
                        false, texW, texH, EnumSet.allOf(Direction.class)));
            }
        }
        if (part.has("mesh")) cubes.add(MeshCube.parse(part.getAsJsonObject("mesh")));
        Map<String, ModelPart> children = new HashMap<>();
        if (part.has("children")) {
            for (var child : part.getAsJsonArray("children")) {
                JsonObject data = child.getAsJsonObject();
                children.put(data.get("name").getAsString(), buildPart(data, texW, texH));
            }
        }
        ModelPart result = new ModelPart(cubes, children);
        PartPose pose = pose(part);
        result.setInitialPose(pose);
        result.loadPose(pose);
        return result;
    }

    private static PartPose pose(JsonObject part) {
        float[] pivot = vec(part.getAsJsonArray("pivot"));
        float[] rotation = part.has("rotation") ? vec(part.getAsJsonArray("rotation")) : new float[3];
        return PartPose.offsetAndRotation(pivot[0], pivot[1], pivot[2], rotation[0], rotation[1], rotation[2]);
    }

    private static float[] vec(JsonArray array) {
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    /**
     * Smooth-shaded triangle mesh. "v" holds 6 floats per vertex (position in model pixels relative to the part
     * pivot, then the normal), "c" one packed RGB vertex color, "t" triangle indices. The texture is plain white,
     * so the vertex colors carry all the paint.
     */
    private static final class MeshCube extends ModelPart.Cube {
        private final float[] positions;
        private final float[] normals;
        private final int[] colors;
        private final int[] triangles;

        private MeshCube(float[] positions, float[] normals, int[] colors, int[] triangles, float[] min, float[] max) {
            super(0, 0, min[0], min[1], min[2], max[0] - min[0], max[1] - min[1], max[2] - min[2],
                    0.0F, 0.0F, 0.0F, false, 16.0F, 16.0F, EnumSet.noneOf(Direction.class));
            this.positions = positions;
            this.normals = normals;
            this.colors = colors;
            this.triangles = triangles;
        }

        static MeshCube parse(JsonObject mesh) {
            JsonArray v = mesh.getAsJsonArray("v");
            JsonArray c = mesh.getAsJsonArray("c");
            JsonArray t = mesh.getAsJsonArray("t");
            int count = v.size() / 6;
            float[] positions = new float[count * 3];
            float[] normals = new float[count * 3];
            int[] colors = new int[count];
            float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (int i = 0; i < count; i++) {
                for (int k = 0; k < 3; k++) {
                    float p = v.get(i * 6 + k).getAsFloat();
                    positions[i * 3 + k] = p / PIXEL;
                    normals[i * 3 + k] = v.get(i * 6 + 3 + k).getAsFloat();
                    min[k] = Math.min(min[k], p);
                    max[k] = Math.max(max[k], p);
                }
                colors[i] = c.get(i).getAsInt();
            }
            int[] triangles = new int[t.size()];
            for (int i = 0; i < triangles.length; i++) triangles[i] = t.get(i).getAsInt();
            return new MeshCube(positions, normals, colors, triangles, min, max);
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int overlayCoords, int color) {
            Matrix4f matrix = pose.pose();
            Vector3f pos = new Vector3f();
            Vector3f normal = new Vector3f();
            int alpha = color >>> 24;
            int tr = color >> 16 & 0xFF;
            int tg = color >> 8 & 0xFF;
            int tb = color & 0xFF;
            for (int i = 0; i < triangles.length; i += 3) {
                // Entity buffers are quads: a triangle is emitted as a quad with its last vertex doubled.
                for (int k = 0; k < 4; k++) {
                    int index = triangles[i + Math.min(k, 2)];
                    int rgb = colors[index];
                    int packed = alpha << 24
                            | (rgb >> 16 & 0xFF) * tr / 255 << 16
                            | (rgb >> 8 & 0xFF) * tg / 255 << 8
                            | (rgb & 0xFF) * tb / 255;
                    matrix.transformPosition(positions[index * 3], positions[index * 3 + 1], positions[index * 3 + 2], pos);
                    pose.transformNormal(normals[index * 3], normals[index * 3 + 1], normals[index * 3 + 2], normal);
                    consumer.addVertex(pos.x(), pos.y(), pos.z(), packed, 0.5F, 0.5F,
                            overlayCoords, lightCoords, normal.x(), normal.y(), normal.z());
                }
            }
        }
    }
}
