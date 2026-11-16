package tokyo.ghoul.client.features.modules.render.cosmetics.imported;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImportedModel {
    private static final float PIXEL = 16.0F;
    private static final float THIN = 1.0E-4F;

    private final List<Bone> roots;
    private Bounds bounds;

    private ImportedModel(List<Bone> roots) {
        this.roots = roots;
    }


    static ImportedModel parse(JsonObject root, boolean limb) {
        List<Bone> roots = new ArrayList<>();
        if (!root.has("minecraft:geometry")) return new ImportedModel(roots);
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries.isEmpty()) return new ImportedModel(roots);
        JsonObject geometry = geometries.get(0).getAsJsonObject();

        int textureWidth = 64;
        int textureHeight = 64;
        if (geometry.has("description")) {
            JsonObject description = geometry.getAsJsonObject("description");
            if (description.has("texture_width")) textureWidth = description.get("texture_width").getAsInt();
            if (description.has("texture_height")) textureHeight = description.get("texture_height").getAsInt();
        }
        if (!geometry.has("bones")) return new ImportedModel(roots);

        Map<String, Bone> bones = new LinkedHashMap<>();
        Map<String, String> parents = new HashMap<>();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject data = element.getAsJsonObject();
            Bone bone = new Bone(data.has("name") ? data.get("name").getAsString() : "bone");
            float[] pivot = vec(data, "pivot");
            bone.pivot = new float[]{-pivot[0], pivot[1], pivot[2]};
            float[] rotation = vec(data, "rotation");
            bone.rotation = new float[]{-rotation[0], -rotation[1], rotation[2]};
            if (data.has("dimasik_tint")) {
                try {
                    bone.tint = (int) Long.parseLong(data.get("dimasik_tint").getAsString().replace("#", ""), 16);
                } catch (NumberFormatException ignored) {
                }
            }
            boolean mirror = data.has("mirror") && data.get("mirror").getAsBoolean();
            if (data.has("cubes")) {
                for (JsonElement cube : data.getAsJsonArray("cubes")) {
                    bone.cubes.add(cube(cube.getAsJsonObject(), mirror, textureWidth, textureHeight, limb));
                }
            }
            if (data.has("parent")) parents.put(bone.name, data.get("parent").getAsString());
            bones.put(bone.name, bone);
        }
        for (Bone bone : bones.values()) {
            Bone parent = bones.get(parents.get(bone.name));
            if (parent == null) roots.add(bone);
            else parent.children.add(bone);
        }
        return new ImportedModel(roots);
    }

    private static Cube cube(JsonObject data, boolean boneMirror, int tw, int th, boolean limb) {
        float[] origin = vec(data, "origin");
        float[] size = vec(data, "size");
        float[] pivot = vec(data, "pivot");
        float[] rotation = vec(data, "rotation");
        Cube cube = new Cube();
        cube.pivot = new float[]{-pivot[0], pivot[1], pivot[2]};
        cube.rotation = new float[]{-rotation[0], -rotation[1], rotation[2]};

        float x = (-origin[0] - size[0]) / PIXEL;
        float y = origin[1] / PIXEL;
        float z = origin[2] / PIXEL;
        float w = size[0] / PIXEL;
        float h = size[1] / PIXEL;
        float d = size[2] / PIXEL;
        float inflate = (data.has("inflate") ? data.get("inflate").getAsFloat() : 0.0F) / PIXEL;
        boolean flatX = w == 0.0F;
        boolean flatY = h == 0.0F;
        boolean flatZ = d == 0.0F;
        if (flatX) {
            x -= THIN / 2.0F;
            w = THIN;
        }
        if (flatY) {
            y -= THIN / 2.0F;
            h = THIN;
        }
        if (flatZ) {
            z -= THIN / 2.0F;
            d = THIN;
        }
        Box box = new Box(x - inflate, y - inflate, z - inflate, x + w + inflate, y + h + inflate, z + d + inflate);
        boolean mirror = boneMirror || data.has("mirror") && data.get("mirror").getAsBoolean();

        JsonElement uv = data.get("uv");
        if (uv != null && uv.isJsonObject()) {
            perFace(cube.quads, uv.getAsJsonObject(), box, mirror, tw, th, size);
        } else if (uv != null && uv.isJsonArray() && uv.getAsJsonArray().size() >= 2) {
            float u = uv.getAsJsonArray().get(0).getAsFloat();
            float v = uv.getAsJsonArray().get(1).getAsFloat();
            if (limb) boxLimb(cube.quads, box, mirror, tw, th, u, v, size, flatX, flatY, flatZ);
            else perFace(cube.quads, boxAsFaces(u, v, size, mirror), box, mirror, tw, th, size);
        }
        return cube;
    }

    private static JsonObject boxAsFaces(float u, float v, float[] size, boolean mirror) {
        float dx = size[0];
        float dy = size[1];
        float dz = size[2];
        float[] westUv = {u, v + dz};
        float[] eastUv = {u + dz + dx, v + dz};
        if (mirror) {
            float[] swap = westUv;
            westUv = eastUv;
            eastUv = swap;
        }
        JsonObject faces = new JsonObject();
        faces.add("north", faceRect(u + dz, v + dz, dx, dy));
        faces.add("south", faceRect(u + dz + dx + dz, v + dz, dx, dy));
        faces.add("west", faceRect(westUv[0], westUv[1], dz, dy));
        faces.add("east", faceRect(eastUv[0], eastUv[1], dz, dy));
        faces.add("up", faceRect(u + dz, v, dx, dz));
        faces.add("down", faceRect(u + dz + dx, v, dx, dz));
        return faces;
    }

    private static JsonObject faceRect(float u, float v, float width, float height) {
        JsonObject face = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(u);
        uv.add(v);
        JsonArray uvSize = new JsonArray();
        uvSize.add(width);
        uvSize.add(height);
        face.add("uv", uv);
        face.add("uv_size", uvSize);
        return face;
    }

    private static void perFace(List<Quad> out, JsonObject uv, Box b, boolean mirror, int tw, int th, float[] size) {
        face(out, uv, "north", mirror, tw, th, size, 0, 0, -1,
                b.x1, b.y1, b.z0, b.x1, b.y0, b.z0, b.x0, b.y0, b.z0, b.x0, b.y1, b.z0);
        face(out, uv, "south", mirror, tw, th, size, 0, 0, 1,
                b.x0, b.y1, b.z1, b.x0, b.y0, b.z1, b.x1, b.y0, b.z1, b.x1, b.y1, b.z1);
        face(out, uv, "west", mirror, tw, th, size, -1, 0, 0,
                b.x0, b.y1, b.z0, b.x0, b.y0, b.z0, b.x0, b.y0, b.z1, b.x0, b.y1, b.z1);
        face(out, uv, "east", mirror, tw, th, size, 1, 0, 0,
                b.x1, b.y1, b.z1, b.x1, b.y0, b.z1, b.x1, b.y0, b.z0, b.x1, b.y1, b.z0);
        face(out, uv, "up", mirror, tw, th, size, 0, 1, 0,
                b.x0, b.y1, b.z0, b.x0, b.y1, b.z1, b.x1, b.y1, b.z1, b.x1, b.y1, b.z0);
        face(out, uv, "down", mirror, tw, th, size, 0, -1, 0,
                b.x1, b.y0, b.z0, b.x1, b.y0, b.z1, b.x0, b.y0, b.z1, b.x0, b.y0, b.z0);
    }

    private static void face(List<Quad> out, JsonObject faces, String name, boolean mirror, int tw, int th, float[] size,
                             float nx, float ny, float nz, float... p) {
        JsonElement element = faces.get(name);
        if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("uv")) return;
        JsonObject face = element.getAsJsonObject();
        JsonArray uv = face.getAsJsonArray("uv");
        float width = name.equals("east") || name.equals("west") ? size[2] : size[0];
        float height = name.equals("up") || name.equals("down") ? size[2] : size[1];
        if (face.has("uv_size")) {
            width = face.getAsJsonArray("uv_size").get(0).getAsFloat();
            height = face.getAsJsonArray("uv_size").get(1).getAsFloat();
        }
        float u = uv.get(0).getAsFloat();
        float v = uv.get(1).getAsFloat();
        out.add(quad(p, u / tw, v / th, (u + width) / tw, (v + height) / th, mirror, nx, ny, nz));
    }

    private static void boxLimb(List<Quad> out, Box b, boolean mirror, int tw, int th, float u, float v, float[] size,
                                boolean flatX, boolean flatY, boolean flatZ) {
        float dx = size[0];
        float dy = size[1];
        float dz = size[2];
        float[] first = {(u + dz) / tw, v / th, (u + dz + dx) / tw, (v + dz) / th};
        float[] second = {(u + dz + dx) / tw, v / th, (u + dz + dx * 2) / tw, (v + dz) / th};
        float[] north = {(u + dz) / tw, (v + dz) / th, (u + dz + dx) / tw, (v + dz + dy) / th};
        float[] south = {(u + dz + dx + dz) / tw, (v + dz) / th, (u + dz + dx + dz + dx) / tw, (v + dz + dy) / th};
        float[] east = {(u + dz + dx) / tw, (v + dz) / th, (u + dz + dx + dz) / tw, (v + dz + dy) / th};
        float[] west = {u / tw, (v + dz) / th, (u + dz) / tw, (v + dz + dy) / th};

        if (flatY) {
            out.add(region(first, mirror, 0, 1, 0, b.x0, b.y1, b.z1, b.x0, b.y1, b.z0, b.x1, b.y1, b.z0, b.x1, b.y1, b.z1));
            out.add(region(first, mirror, 0, -1, 0, b.x0, b.y0, b.z1, b.x0, b.y0, b.z0, b.x1, b.y0, b.z0, b.x1, b.y0, b.z1));
        } else {
            out.add(region(second, mirror, 0, 1, 0, b.x0, b.y1, b.z0, b.x0, b.y1, b.z1, b.x1, b.y1, b.z1, b.x1, b.y1, b.z0));
            out.add(region(first, mirror, 0, -1, 0, b.x0, b.y0, b.z1, b.x0, b.y0, b.z0, b.x1, b.y0, b.z0, b.x1, b.y0, b.z1));
        }
        out.add(region(north, mirror, 0, 0, -1, b.x0, b.y0, b.z0, b.x0, b.y1, b.z0, b.x1, b.y1, b.z0, b.x1, b.y0, b.z0));
        if (flatZ) {
            out.add(region(north, mirror, 0, 0, 1, b.x0, b.y0, b.z1, b.x0, b.y1, b.z1, b.x1, b.y1, b.z1, b.x1, b.y0, b.z1));
        } else {
            out.add(region(south, mirror, 0, 0, 1, b.x1, b.y0, b.z1, b.x1, b.y1, b.z1, b.x0, b.y1, b.z1, b.x0, b.y0, b.z1));
        }
        out.add(region(west, mirror, -1, 0, 0, b.x0, b.y0, b.z1, b.x0, b.y1, b.z1, b.x0, b.y1, b.z0, b.x0, b.y0, b.z0));
        if (flatX) {
            out.add(region(west, mirror, 1, 0, 0, b.x1, b.y0, b.z1, b.x1, b.y1, b.z1, b.x1, b.y1, b.z0, b.x1, b.y0, b.z0));
        } else {
            out.add(region(east, mirror, 1, 0, 0, b.x1, b.y0, b.z0, b.x1, b.y1, b.z0, b.x1, b.y1, b.z1, b.x1, b.y0, b.z1));
        }
    }

    private static Quad region(float[] uv, boolean mirror, float nx, float ny, float nz, float... p) {
        return quad(p, uv[0], uv[1], uv[2], uv[3], mirror, nx, ny, nz);
    }

    private static Quad quad(float[] p, float u0, float v0, float u1, float v1, boolean mirror, float nx, float ny, float nz) {
        if (mirror) {
            float swap = u0;
            u0 = u1;
            u1 = swap;
        }
        return new Quad(p.clone(), new float[]{u0, v0, u0, v1, u1, v1, u1, v0}, nx, ny, nz);
    }

    private static float[] vec(JsonObject object, String key) {
        float[] result = new float[3];
        if (!object.has(key) || !object.get(key).isJsonArray()) return result;
        JsonArray array = object.getAsJsonArray(key);
        for (int i = 0; i < Math.min(3, array.size()); i++) result[i] = array.get(i).getAsFloat();
        return result;
    }


    public void render(PoseStack stack, VertexConsumer consumer, int light, int color, ImportedAnimation animation, float seconds) {
        for (Bone bone : roots) renderBone(bone, stack, consumer, light, color, animation, seconds);
    }

    private void renderBone(Bone bone, PoseStack stack, VertexConsumer consumer, int light, int color,
                            ImportedAnimation animation, float seconds) {
        ImportedAnimation.Pose pose = animation == null ? null : animation.sample(bone.name, seconds);
        float[] rotation = pose == null ? null : pose.rotation();
        float[] position = pose == null ? null : pose.position();
        float[] scale = pose == null ? null : pose.scale();

        float rx = bone.rotation[0] + (rotation == null ? 0.0F : -rotation[0]);
        float ry = bone.rotation[1] + (rotation == null ? 0.0F : -rotation[1]);
        float rz = bone.rotation[2] + (rotation == null ? 0.0F : rotation[2]);

        stack.pushPose();
        if (position != null) stack.translate(-position[0] / PIXEL, position[1] / PIXEL, position[2] / PIXEL);
        stack.translate(bone.pivot[0] / PIXEL, bone.pivot[1] / PIXEL, bone.pivot[2] / PIXEL);
        rotate(stack, rx, ry, rz);
        if (scale != null) stack.scale(scale[0], scale[1], scale[2]);
        stack.translate(-bone.pivot[0] / PIXEL, -bone.pivot[1] / PIXEL, -bone.pivot[2] / PIXEL);

        int tinted = tint(color, bone.tint);
        for (Cube cube : bone.cubes) {
            stack.pushPose();
            stack.translate(cube.pivot[0] / PIXEL, cube.pivot[1] / PIXEL, cube.pivot[2] / PIXEL);
            rotate(stack, cube.rotation[0], cube.rotation[1], cube.rotation[2]);
            stack.translate(-cube.pivot[0] / PIXEL, -cube.pivot[1] / PIXEL, -cube.pivot[2] / PIXEL);
            PoseStack.Pose last = stack.last();
            for (Quad quad : cube.quads) {
                for (int i = 0; i < 4; i++) {
                    consumer.addVertex(last, quad.pos[i * 3], quad.pos[i * 3 + 1], quad.pos[i * 3 + 2])
                            .setColor(tinted)
                            .setUv(quad.uv[i * 2], quad.uv[i * 2 + 1])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(last, quad.nx, quad.ny, quad.nz);
                }
            }
            stack.popPose();
        }
        for (Bone child : bone.children) renderBone(child, stack, consumer, light, color, animation, seconds);
        stack.popPose();
    }

    private static void rotate(PoseStack stack, float x, float y, float z) {
        if (z != 0.0F) stack.mulPose(Axis.ZP.rotationDegrees(z));
        if (y != 0.0F) stack.mulPose(Axis.YP.rotationDegrees(y));
        if (x != 0.0F) stack.mulPose(Axis.XP.rotationDegrees(x));
    }

    private static int tint(int color, int tint) {
        if ((tint & 0xFFFFFF) == 0xFFFFFF) return color;
        int r = (color >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
        int g = (color >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
        int b = (color & 0xFF) * (tint & 0xFF) / 255;
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }


    public synchronized Bounds bounds() {
        if (bounds != null) return bounds;
        Bounds result = new Bounds();
        Matrix4f identity = new Matrix4f();
        for (Bone bone : roots) accumulate(bone, identity, result);
        bounds = result.valid() ? result : Bounds.unit();
        return bounds;
    }

    private static void accumulate(Bone bone, Matrix4f parent, Bounds out) {
        Matrix4f local = transform(new Matrix4f(parent), bone.pivot, bone.rotation);
        for (Cube cube : bone.cubes) {
            Matrix4f matrix = transform(new Matrix4f(local), cube.pivot, cube.rotation);
            Vector3f point = new Vector3f();
            for (Quad quad : cube.quads) {
                for (int i = 0; i < 4; i++) {
                    matrix.transformPosition(quad.pos[i * 3], quad.pos[i * 3 + 1], quad.pos[i * 3 + 2], point);
                    out.include(point.x, point.y, point.z);
                }
            }
        }
        for (Bone child : bone.children) accumulate(child, local, out);
    }

    private static Matrix4f transform(Matrix4f matrix, float[] pivot, float[] rotation) {
        matrix.translate(pivot[0] / PIXEL, pivot[1] / PIXEL, pivot[2] / PIXEL);
        matrix.rotateZ((float) Math.toRadians(rotation[2]));
        matrix.rotateY((float) Math.toRadians(rotation[1]));
        matrix.rotateX((float) Math.toRadians(rotation[0]));
        return matrix.translate(-pivot[0] / PIXEL, -pivot[1] / PIXEL, -pivot[2] / PIXEL);
    }

    public static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        void include(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        boolean valid() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        static Bounds unit() {
            Bounds bounds = new Bounds();
            bounds.include(-0.5F, -0.5F, -0.5F);
            bounds.include(0.5F, 0.5F, 0.5F);
            return bounds;
        }

        public float centerX() {
            return (minX + maxX) * 0.5F;
        }

        public float centerY() {
            return (minY + maxY) * 0.5F;
        }

        public float centerZ() {
            return (minZ + maxZ) * 0.5F;
        }

        public float maxExtent() {
            return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }
    }

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1) {
    }

    private record Quad(float[] pos, float[] uv, float nx, float ny, float nz) {
    }

    private static final class Cube {
        float[] pivot;
        float[] rotation;
        final List<Quad> quads = new ArrayList<>();
    }

    private static final class Bone {
        final String name;
        float[] pivot;
        float[] rotation;
        int tint = 0xFFFFFF;
        final List<Cube> cubes = new ArrayList<>();
        final List<Bone> children = new ArrayList<>();

        Bone(String name) {
            this.name = name;
        }
    }
}
