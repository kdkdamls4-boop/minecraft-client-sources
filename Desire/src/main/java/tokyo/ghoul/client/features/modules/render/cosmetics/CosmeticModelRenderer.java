package tokyo.ghoul.client.features.modules.render.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import tokyo.ghoul.client.features.modules.render.cosmetics.imported.ImportedCosmeticRenderer;
import tokyo.ghoul.client.features.modules.render.cosmetics.imported.ImportedCosmetics;
import org.joml.Vector3f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticModelRenderer {
    private static final Map<Integer, Cosmetic> CACHE = new ConcurrentHashMap<>();
    private static final float VULCANO_FLAP_AMPLITUDE = 1.9F;

    private CosmeticModelRenderer() { }

    public static void render(int entryIndex, AttachmentPoses poses, PoseStack matrices, VertexConsumer consumer, int light, float walkPosition, float walkSpeed, float headPitch, float headYaw) {
        CosmeticsModule.Entry entry = CosmeticsModule.entry(entryIndex);
        if (entry == null) return;
        ImportedCosmetics.Imported imported = ImportedCosmetics.of(entry);
        if (imported != null) {
            ImportedCosmeticRenderer.render(imported, poses, matrices, consumer, light, vertexColor);
            return;
        }
        Cosmetic cosmetic = CACHE.computeIfAbsent(entry.resourceIndex(), CosmeticModelRenderer::load);
        if (cosmetic == null) return;

        matrices.pushPose();
        int attachment = isHeadMounted(entry) ? 2 : cosmetic.pos;
        attach(attachment, poses, matrices);
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
        matrices.translate(cosmetic.x, cosmetic.y + attachmentYOffset(attachment) + headLift(entry), cosmetic.z);
        matrices.mulPose(Axis.YP.rotationDegrees(cosmetic.yaw));
        matrices.mulPose(Axis.XP.rotationDegrees(cosmetic.pitch));
        matrices.mulPose(Axis.ZP.rotationDegrees(cosmetic.roll));
        matrices.scale(cosmetic.scale, cosmetic.scale, cosmetic.scale);
        animateAndDraw(entry, cosmetic, matrices, consumer, light, walkSpeed);
        matrices.popPose();
    }

    public static void renderIsolated(int entryIndex, PoseStack matrices, VertexConsumer consumer, int light) {
        CosmeticsModule.Entry entry = CosmeticsModule.entry(entryIndex);
        if (entry == null) return;
        ImportedCosmetics.Imported imported = ImportedCosmetics.of(entry);
        if (imported != null) {
            ImportedCosmeticRenderer.renderIsolated(imported, matrices, consumer, light);
            return;
        }
        Cosmetic cosmetic = CACHE.computeIfAbsent(entry.resourceIndex(), CosmeticModelRenderer::load);
        if (cosmetic == null) return;

        Bounds bounds = cosmetic.bounds;
        float extent = Math.max(bounds.maxExtent(), 0.05F);
        float fit = 0.95F / extent;

        matrices.pushPose();
        matrices.translate(0.0F, 1.0F, 0.0F);
        float spin = (float) ((System.nanoTime() / 1_000_000_000.0) * 72.0 % 360.0);
        matrices.mulPose(Axis.YP.rotationDegrees(spin));
        matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
        matrices.scale(fit, fit, fit);
        matrices.translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
        animateAndDraw(entry, cosmetic, matrices, consumer, light, 0.0F);
        matrices.popPose();
    }

    private static void animateAndDraw(CosmeticsModule.Entry entry, Cosmetic cosmetic, PoseStack matrices, VertexConsumer consumer, int light, float walkSpeed) {
        AnimationClip clip = cosmetic.animations.pick(walkSpeed);
        double seconds = System.nanoTime() / 1_000_000_000.0;
        float animationTime;
        if (clip != null && clip.length > 0.0F) {
            animationTime = clip.loop ? (float) (seconds % clip.length) : Math.min((float) seconds, clip.length);
        } else {
            animationTime = (float) (seconds % 1.5417D);
        }
        boolean useJson = clip != null && !clip.bones.isEmpty();
        if (!useJson && "pet".equals(entry.type())) {
            matrices.translate(0.0F, (float) Math.sin(seconds * 2.4D) * 0.035F, 0.0F);
        }
        for (Bone bone : cosmetic.roots) {
            renderBone(bone, matrices, consumer, light, entry.resourceIndex(), animationTime, clip, useJson, 0, 0);
        }
    }

    public static RenderType layer(int entryIndex) {
        CosmeticsModule.Entry entry = CosmeticsModule.entry(entryIndex);
        if (entry == null) return null;
        ImportedCosmetics.Imported imported = ImportedCosmetics.of(entry);
        return imported != null ? ImportedCosmeticRenderer.layer(imported) : RenderTypes.entityTranslucent(texture(entry));
    }

    public static RenderType outlineLayer(int entryIndex) {
        CosmeticsModule.Entry entry = CosmeticsModule.entry(entryIndex);
        if (entry == null) return null;
        ImportedCosmetics.Imported imported = ImportedCosmetics.of(entry);
        return imported != null ? ImportedCosmeticRenderer.outlineLayer(imported) : RenderTypes.outline(texture(entry));
    }

    private static Identifier texture(CosmeticsModule.Entry entry) {
        return Identifier.fromNamespaceAndPath("desire", "cosmetics/textures/cosmetic_" + entry.resourceIndex() + ".png");
    }

    private static int vertexColor = -1;

    public static void renderOutline(int entryIndex, AttachmentPoses poses, PoseStack matrices, VertexConsumer consumer, int color,
                                     float walkPosition, float walkSpeed, float headPitch, float headYaw) {
        vertexColor = color;
        try {
            render(entryIndex, poses, matrices, consumer, 15728880, walkPosition, walkSpeed, headPitch, headYaw);
        } finally {
            vertexColor = -1;
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light,
                             float x1,float y1,float z1,float u1,float v1, float x2,float y2,float z2,float u2,float v2,
                             float x3,float y3,float z3,float u3,float v3, float x4,float y4,float z4,float u4,float v4,
                             float nx,float ny,float nz) {
        vertex(pose,consumer,light,x1,y1,z1,u1,v1,nx,ny,nz); vertex(pose,consumer,light,x2,y2,z2,u2,v2,nx,ny,nz);
        vertex(pose,consumer,light,x3,y3,z3,u3,v3,nx,ny,nz); vertex(pose,consumer,light,x4,y4,z4,u4,v4,nx,ny,nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light, float x,float y,float z,float u,float v,float nx,float ny,float nz) {
        consumer.addVertex(pose,x,y,z).setColor(vertexColor).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
    }

    private static Cosmetic load(int index) {
        String path = "/assets/desire/cosmetics/cosmetics/models/cosmetic_" + index + ".json";
        try (InputStream input = CosmeticModelRenderer.class.getResourceAsStream(path)) {
            if (input == null) return null;
            JsonObject root = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("model") || !root.getAsJsonObject("model").has("minecraft:geometry")) return null;
            JsonObject model = root.getAsJsonObject("model");
            JsonObject geometry = model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            int textureWidth = description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
            int textureHeight = description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;
            Map<String, Bone> bones = new HashMap<>();
            for (JsonElement element : geometry.getAsJsonArray("bones")) {
                Bone bone = parseBone(element.getAsJsonObject(), textureWidth, textureHeight);
                bones.put(bone.name, bone);
            }
            List<Bone> roots = new ArrayList<>();
            for (JsonElement element : geometry.getAsJsonArray("bones")) {
                JsonObject data = element.getAsJsonObject();
                Bone bone = bones.get(data.get("name").getAsString());
                if (data.has("parent") && bones.containsKey(data.get("parent").getAsString())) {
                    bone.parentName = data.get("parent").getAsString();
                    bones.get(bone.parentName).children.add(bone);
                } else roots.add(bone);
            }
            Bounds bounds = computeBounds(roots);
            AnimationSet animations = parseAnimations(root.has("animation") ? root.getAsJsonObject("animation") : null);
            return new Cosmetic(roots, root.has("pos") ? root.get("pos").getAsInt() : 1,
                    number(root, "scale", 1), number(root, "x", 0), number(root, "y", 0), number(root, "z", 0),
                    number(root, "yaw", 0), number(root, "pitch", 0), number(root, "roll", 0), bounds, animations);
        } catch (Exception ignored) { return null; }
    }

    private static AnimationSet parseAnimations(JsonObject animationRoot) {
        if (animationRoot == null || !animationRoot.has("animations")) return AnimationSet.EMPTY;
        JsonObject animations = animationRoot.getAsJsonObject("animations");
        Map<String, AnimationClip> clips = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            AnimationClip clip = parseClip(entry.getKey(), entry.getValue().getAsJsonObject());
            if (clip != null) clips.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), clip);
        }
        if (clips.isEmpty()) return AnimationSet.EMPTY;
        AnimationClip idle = firstClip(clips, "idle", "main", "anim", "animation");
        if (idle == null) idle = clips.values().iterator().next();
        AnimationClip walk = firstClip(clips, "walking", "walk", "run");
        return new AnimationSet(idle, walk);
    }

    private static AnimationClip firstClip(Map<String, AnimationClip> clips, String... names) {
        for (String name : names) {
            AnimationClip clip = clips.get(name);
            if (clip != null) return clip;
        }
        return null;
    }

    private static AnimationClip parseClip(String name, JsonObject data) {
        boolean loop = data.has("loop") && data.get("loop").getAsBoolean();
        float length = data.has("animation_length") ? data.get("animation_length").getAsFloat() : 1.0F;
        Map<String, BoneTrack> bones = new HashMap<>();
        if (data.has("bones") && data.get("bones").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("bones").entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject bone = entry.getValue().getAsJsonObject();
                bones.put(entry.getKey(), new BoneTrack(
                        parseKeyframes(bone.get("rotation")),
                        parseKeyframes(bone.get("position")),
                        parseKeyframes(bone.get("scale"))
                ));
            }
        }
        return new AnimationClip(name, loop, length, bones);
    }

    private static NavigableMap<Float, float[]> parseKeyframes(JsonElement element) {
        NavigableMap<Float, float[]> frames = new TreeMap<>();
        if (element == null || !element.isJsonObject()) return frames;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                float[] values = readVector3(entry.getValue());
                if (values != null) frames.put(time, values);
            } catch (NumberFormatException ignored) {
            }
        }
        return frames;
    }

    private static float[] readVector3(JsonElement element) {
        JsonArray array = null;
        if (element.isJsonObject() && element.getAsJsonObject().has("vector")) {
            array = element.getAsJsonObject().getAsJsonArray("vector");
        } else if (element.isJsonArray()) {
            array = element.getAsJsonArray();
        }
        if (array == null || array.size() < 3) return null;
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static float[] interpolateKeyframes(NavigableMap<Float, float[]> frames, float time) {
        if (frames == null || frames.isEmpty()) return null;
        Map.Entry<Float, float[]> floor = frames.floorEntry(time);
        Map.Entry<Float, float[]> ceil = frames.ceilingEntry(time);
        if (floor == null) return ceil.getValue();
        if (ceil == null || floor.getKey().equals(ceil.getKey())) return floor.getValue();
        float progress = (time - floor.getKey()) / (ceil.getKey() - floor.getKey());
        float[] a = floor.getValue();
        float[] b = ceil.getValue();
        return new float[]{
                a[0] + (b[0] - a[0]) * progress,
                a[1] + (b[1] - a[1]) * progress,
                a[2] + (b[2] - a[2]) * progress
        };
    }

    private static Bounds computeBounds(List<Bone> roots) {
        Bounds bounds = new Bounds();
        Matrix4f identity = new Matrix4f();
        for (Bone root : roots) accumulateBoneBounds(root, identity, bounds);
        return bounds.valid() ? bounds : Bounds.unit();
    }

    private static void accumulateBoneBounds(Bone bone, Matrix4f parent, Bounds out) {
        Matrix4f local = new Matrix4f(parent);
        applyTransform(local, bone.pivotX, bone.pivotY, bone.pivotZ, bone.xRot, bone.yRot, bone.zRot);
        for (Cube cube : bone.cubes) {
            Matrix4f cubeMatrix = new Matrix4f(local);
            applyTransform(cubeMatrix, cube.pivotX, cube.pivotY, cube.pivotZ, cube.xRot, cube.yRot, cube.zRot);
            for (Quad quad : cube.quads) {
                if (quad == null) continue;
                for (Vertex vertex : quad.vertices()) {
                    Vector3f point = cubeMatrix.transformPosition(vertex.x(), vertex.y(), vertex.z(), new Vector3f());
                    out.include(point.x, point.y, point.z);
                }
            }
        }
        for (Bone child : bone.children) accumulateBoneBounds(child, local, out);
    }

    private static void applyTransform(Matrix4f matrix, float x, float y, float z, float rx, float ry, float rz) {
        matrix.translate(x, y, z);
        if (rz != 0.0F) matrix.rotate(Axis.ZP.rotationDegrees(rz));
        if (ry != 0.0F) matrix.rotate(Axis.YP.rotationDegrees(ry));
        if (rx != 0.0F) matrix.rotate(Axis.XP.rotationDegrees(rx));
        matrix.translate(-x, -y, -z);
    }

    private static Bone parseBone(JsonObject data, int texW, int texH) {
        Bone bone = new Bone(data.get("name").getAsString());
        float[] pivot = vector(data, "pivot", new float[3]);
        bone.pivotX = -pivot[0] / 16F; bone.pivotY = pivot[1] / 16F; bone.pivotZ = pivot[2] / 16F;
        float[] rotation = vector(data, "rotation", new float[3]);
        bone.xRot = -rotation[0]; bone.yRot = -rotation[1]; bone.zRot = rotation[2];
        if (data.has("cubes")) for (JsonElement element : data.getAsJsonArray("cubes")) bone.cubes.add(parseCube(element.getAsJsonObject(), texW, texH));
        return bone;
    }

    private static Cube parseCube(JsonObject data, int texW, int texH) {
        float[] origin = vector(data, "origin", new float[3]);
        float[] size = vector(data, "size", new float[]{1, 1, 1});
        float[] pivot = vector(data, "pivot", origin.clone());
        float[] rotation = vector(data, "rotation", new float[3]);
        Cube cube = new Cube();
        cube.pivotX = -pivot[0] / 16F; cube.pivotY = pivot[1] / 16F; cube.pivotZ = pivot[2] / 16F;
        cube.xRot = -rotation[0]; cube.yRot = -rotation[1]; cube.zRot = rotation[2];
        float inflate = number(data, "inflate", 0F);
        float x0 = -(origin[0] + size[0] + inflate) / 16F, x1 = -(origin[0] - inflate) / 16F;
        float y0 = (origin[1] - inflate) / 16F, y1 = (origin[1] + size[1] + inflate) / 16F;
        float z0 = (origin[2] - inflate) / 16F, z1 = (origin[2] + size[2] + inflate) / 16F;
        JsonElement uv = data.get("uv");
        if (uv != null && uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            cube.add(face(faces, "west", x0,y0,z0, x0,y1,z1, -1,0,0, texW,texH));
            cube.add(face(faces, "east", x1,y0,z0, x1,y1,z1, 1,0,0, texW,texH));
            cube.add(face(faces, "down", x0,y0,z0, x1,y0,z1, 0,-1,0, texW,texH));
            cube.add(face(faces, "up", x0,y1,z0, x1,y1,z1, 0,1,0, texW,texH));
            cube.add(face(faces, "north", x0,y0,z0, x1,y1,z0, 0,0,-1, texW,texH));
            cube.add(face(faces, "south", x0,y0,z1, x1,y1,z1, 0,0,1, texW,texH));
        } else if (uv != null && uv.isJsonArray()) {
            float[] start = vector(data, "uv", new float[2]);
            float u = start[0], v = start[1], dx = size[0], dy = size[1], dz = size[2];
            cube.add(boxFace("west", x0,y0,z0,x0,y1,z1,-1,0,0, u + dz + dx, v + dz, dz,dy,texW,texH));
            cube.add(boxFace("east", x1,y0,z0,x1,y1,z1,1,0,0, u, v + dz, dz,dy,texW,texH));
            cube.add(boxFace("down", x0,y0,z0,x1,y0,z1,0,-1,0, u + dz + dx, v, dx,dz,texW,texH));
            cube.add(boxFace("up", x0,y1,z0,x1,y1,z1,0,1,0, u + dz, v, dx,dz,texW,texH));
            cube.add(boxFace("north", x0,y0,z0,x1,y1,z0,0,0,-1, u + dz, v + dz, dx,dy,texW,texH));
            cube.add(boxFace("south", x0,y0,z1,x1,y1,z1,0,0,1, u + dz + dx + dz, v + dz, dx,dy,texW,texH));
        }
        return cube;
    }

    private static Quad boxFace(String name, float x0, float y0, float z0, float x1, float y1, float z1, float nx, float ny, float nz, float u, float v, float du, float dv, int width, int height) {
        JsonObject fake = new JsonObject();
        JsonObject face = new JsonObject();
        JsonArray origin = new JsonArray(), size = new JsonArray();
        origin.add(u); origin.add(v); size.add(du); size.add(dv);
        face.add("uv", origin); face.add("uv_size", size); fake.add(name, face);
        return face(fake, name, x0,y0,z0,x1,y1,z1,nx,ny,nz,width,height);
    }

    private static Quad face(JsonObject faces, String name, float x0, float y0, float z0, float x1, float y1, float z1, float nx, float ny, float nz, int w, int h) {
        if (!faces.has(name)) return null;
        JsonObject face = faces.getAsJsonObject(name);
        float[] uv = vector(face, "uv", new float[2]);
        float[] size = vector(face, "uv_size", new float[2]);
        float u0 = uv[0] / w, v0 = uv[1] / h, u1 = (uv[0] + size[0]) / w, v1 = (uv[1] + size[1]) / h;
        Vertex[] vertices = switch (name) {
            case "north" -> new Vertex[]{v(x1,y1,z0,u0,v0),v(x0,y1,z0,u1,v0),v(x0,y0,z0,u1,v1),v(x1,y0,z0,u0,v1)};
            case "south" -> new Vertex[]{v(x0,y1,z1,u0,v0),v(x1,y1,z1,u1,v0),v(x1,y0,z1,u1,v1),v(x0,y0,z1,u0,v1)};
            case "east" -> new Vertex[]{v(x1,y1,z1,u0,v0),v(x1,y1,z0,u1,v0),v(x1,y0,z0,u1,v1),v(x1,y0,z1,u0,v1)};
            case "west" -> new Vertex[]{v(x0,y1,z0,u0,v0),v(x0,y1,z1,u1,v0),v(x0,y0,z1,u1,v1),v(x0,y0,z0,u0,v1)};
            case "up" -> new Vertex[]{v(x0,y1,z0,u0,v0),v(x0,y1,z1,u0,v1),v(x1,y1,z1,u1,v1),v(x1,y1,z0,u1,v0)};
            default -> new Vertex[]{v(x1,y0,z0,u0,v0),v(x1,y0,z1,u0,v1),v(x0,y0,z1,u1,v1),v(x0,y0,z0,u1,v0)};
        };
        return new Quad(vertices, nx, ny, nz);
    }

    private static void renderBone(Bone bone, PoseStack stack, VertexConsumer consumer, int light, int resourceIndex,
                                   float animationTime, AnimationClip clip, boolean useJson,
                                   int inheritedSide, int inheritedWingDepth) {
        String name = bone.name.toLowerCase(java.util.Locale.ROOT);
        boolean right = isRightWingBone(name);
        boolean left = isLeftWingBone(name);
        int side = right ? 1 : left ? -1 : inheritedSide;
        boolean parentIsWing = bone.parentName != null && (isRightWingBone(bone.parentName.toLowerCase(java.util.Locale.ROOT)) || isLeftWingBone(bone.parentName.toLowerCase(java.util.Locale.ROOT)));
        boolean primaryWing = (right || left) && !parentIsWing;
        int wingDepth = primaryWing ? 0 : (side == 0 ? 0 : inheritedWingDepth + 1);

        float animRx = 0.0F, animRy = 0.0F, animRz = 0.0F;
        float animPx = 0.0F, animPy = 0.0F, animPz = 0.0F;
        float animSx = 1.0F, animSy = 1.0F, animSz = 1.0F;
        if (useJson && clip != null) {
            BoneTrack track = clip.bones.get(bone.name);
            if (track != null) {
                float[] rot = interpolateKeyframes(track.rotation(), animationTime);
                float[] pos = interpolateKeyframes(track.position(), animationTime);
                float[] scale = interpolateKeyframes(track.scale(), animationTime);
                if (rot != null) {
                    animRx = -rot[0];
                    animRy = -rot[1];
                    animRz = rot[2];
                }
                if (pos != null) {
                    animPx = pos[0];
                    animPy = pos[1];
                    animPz = pos[2];
                }
                if (scale != null) {
                    animSx = scale[0];
                    animSy = scale[1];
                    animSz = scale[2];
                }
            }
        }

        RotationOffset offset = RotationOffset.NONE;
        float wingFlap = 0.0F;
        if (!useJson) {
            wingFlap = right ? 0.0F : left ? 0.0F : 0.0F;
            if (resourceIndex == 20 && name.equals("root")) wingFlap = 0.0F;
            offset = resourceIndex == 26
                    ? vulcanoAnimation(name, animationTime)
                    : genericWingAnimation(bone, side, animationTime, resourceIndex, primaryWing, wingDepth);
            offset = offset.add(petAnimation(resourceIndex, name));
        }

        stack.pushPose();
        if (animPx != 0.0F || animPy != 0.0F || animPz != 0.0F) {
            stack.translate(-animPx / 16.0F, animPy / 16.0F, animPz / 16.0F);
        }
        stack.translate(bone.pivotX, bone.pivotY, bone.pivotZ);
        float rx = bone.xRot + offset.x() + animRx;
        float ry = bone.yRot + offset.y() + animRy;
        float rz = bone.zRot + wingFlap + offset.z() + animRz;
        if (rz != 0.0F) stack.mulPose(Axis.ZP.rotationDegrees(rz));
        if (ry != 0.0F) stack.mulPose(Axis.YP.rotationDegrees(ry));
        if (rx != 0.0F) stack.mulPose(Axis.XP.rotationDegrees(rx));
        if (animSx != 1.0F || animSy != 1.0F || animSz != 1.0F) {
            stack.scale(animSx, animSy, animSz);
        }
        stack.translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);
        for (Cube cube : bone.cubes) renderCube(cube, stack, consumer, light);
        for (Bone child : bone.children) {
            renderBone(child, stack, consumer, light, resourceIndex, animationTime, clip, useJson, side, wingDepth);
        }
        stack.popPose();
    }

    private static boolean isRightWingBone(String name) {
        return name.equals("wing_3") || name.contains("right_wing") || name.contains("rightwing")
                || name.contains("wing_right") || name.contains("spiderwingright") || name.contains("rightlargechain");
    }

    private static boolean isLeftWingBone(String name) {
        return name.equals("wing_2") || name.contains("left_wing") || name.contains("leftwing")
                || name.contains("wing_left") || name.contains("spiderwingleft") || name.contains("leftlargechain");
    }

    private static RotationOffset genericWingAnimation(Bone bone, int side, float time, int resourceIndex, boolean primaryWing, int wingDepth) {
        if (resourceIndex == 20 && bone.name.equalsIgnoreCase("root")) {
            return new RotationOffset(0.0F, 0.0F, vulcanoKey(time, ROOT_TIMES, ROOT_RIGHT_Z) * 0.35F);
        }
        if (side == 0) return RotationOffset.NONE;
        if (primaryWing) {
            return side > 0
                    ? new RotationOffset(-vulcanoKey(time, ROOT_TIMES, ROOT_X), -vulcanoKey(time, ROOT_TIMES, ROOT_RIGHT_Y), vulcanoKey(time, ROOT_TIMES, ROOT_RIGHT_Z))
                    : new RotationOffset(-vulcanoKey(time, ROOT_TIMES, ROOT_X), -vulcanoKey(time, ROOT_TIMES, ROOT_LEFT_Y), vulcanoKey(time, ROOT_TIMES, ROOT_LEFT_Z));
        }
        if (wingDepth > 0) {
            float attenuation = 1.0F / (1.0F + (wingDepth - 1) * 0.55F);
            return new RotationOffset(0.0F, -vulcanoKey(time, FEATHER_TIMES, side > 0 ? FEATHER_RIGHT_Y : FEATHER_LEFT_Y) * attenuation, 0.0F);
        }
        return RotationOffset.NONE;
    }

    private static RotationOffset petAnimation(int resourceIndex, String bone) {
        float wave = (float) Math.sin(System.nanoTime() / 200_000_000.0D);
        if (resourceIndex == 48 || resourceIndex == 49) {
            if (bone.equals("angel")) return new RotationOffset(wave * 2.0F, 0.0F, wave * 1.5F);
            if (bone.equals("left_wing")) return new RotationOffset(0.0F, 0.0F, 18.0F + wave * 14.0F);
            if (bone.equals("right_wing")) return new RotationOffset(0.0F, 0.0F, -18.0F - wave * 14.0F);
            if (bone.contains("upper_wing") || bone.contains("wing_structure")) return new RotationOffset(0.0F, wave * 8.0F, 0.0F);
        }
        if (resourceIndex == 70) {
            if (bone.equals("bee")) return new RotationOffset(wave * 3.0F, 0.0F, wave * 5.0F);
            if (bone.equals("leftwing")) return new RotationOffset(0.0F, 0.0F, 28.0F + wave * 24.0F);
            if (bone.equals("bone2")) return new RotationOffset(0.0F, 0.0F, -28.0F - wave * 24.0F);
        }
        return RotationOffset.NONE;
    }

    private static final float[] ROOT_TIMES = {0.0F, 0.75F, 1.5417F};
    private static final float[] ROOT_X = {0.0F, 4.73477F, 0.0F};
    private static final float[] ROOT_RIGHT_Y = {-10.0F, 25.08553F, -10.0F};
    private static final float[] ROOT_LEFT_Y = {10.0F, -25.08553F, 10.0F};
    private static final float[] ROOT_RIGHT_Z = {0.0F, 11.05395F, 0.0F};
    private static final float[] ROOT_LEFT_Z = {0.0F, -11.05395F, 0.0F};
    private static final float[] FEATHER_TIMES = {0.0F, 0.5F, 1.125F, 1.5417F};
    private static final float[] FEATHER_RIGHT_Y = {30.0F, -15.0F, 40.0F, 30.0F};
    private static final float[] FEATHER_LEFT_Y = {-30.0F, 15.0F, -40.0F, -30.0F};

    private static RotationOffset vulcanoAnimation(String bone, float time) {
        return switch (bone) {
            case "right_1" -> new RotationOffset(
                    -vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{0.0F, 4.73477F, 0.0F}),
                    -vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{-10.0F, 25.08553F, -10.0F}),
                    vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{0.0F, 11.05395F, 0.0F})
            );
            case "right_2" -> new RotationOffset(
                    -vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{0.0F, 4.73477F, 0.0F}),
                    -vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{10.0F, -25.08553F, 10.0F}),
                    vulcanoKey(time, new float[]{0.0F, 0.75F, 1.5417F}, new float[]{0.0F, -11.05395F, 0.0F})
            );
            case "bone" -> new RotationOffset(0.0F, -vulcanoKey(time,
                    new float[]{0.0F, 0.5F, 1.125F, 1.5417F}, new float[]{30.0F, -15.0F, 40.0F, 30.0F}), 0.0F);
            case "bone2" -> new RotationOffset(0.0F, -vulcanoKey(time,
                    new float[]{0.0F, 0.5F, 1.125F, 1.5417F}, new float[]{-30.0F, 15.0F, -40.0F, -30.0F}), 0.0F);
            default -> RotationOffset.NONE;
        };
    }

    private static float vulcanoKey(float time, float[] times, float[] values) {
        return keyframe(time, times, values) * VULCANO_FLAP_AMPLITUDE;
    }

    private static float keyframe(float time, float[] times, float[] values) {
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                float progress = (time - times[i - 1]) / (times[i] - times[i - 1]);
                progress = progress < 0.5F ? 2.0F * progress * progress : 1.0F - (float) Math.pow(-2.0F * progress + 2.0F, 2.0F) / 2.0F;
                return values[i - 1] + (values[i] - values[i - 1]) * progress;
            }
        }
        return values[values.length - 1];
    }

    private static void renderCube(Cube cube, PoseStack stack, VertexConsumer consumer, int light) {
        stack.pushPose(); transform(stack, cube.pivotX,cube.pivotY,cube.pivotZ,cube.xRot,cube.yRot,cube.zRot);
        PoseStack.Pose pose = stack.last();
        for (Quad quad : cube.quads) if (quad != null) for (Vertex vertex : quad.vertices)
            consumer.addVertex(pose, vertex.x,vertex.y,vertex.z).setColor(vertexColor).setUv(vertex.u,vertex.v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,quad.x,quad.y,quad.z);
        stack.popPose();
    }

    private static void transform(PoseStack stack, float x, float y, float z, float rx, float ry, float rz) {
        stack.translate(x,y,z);
        if (rz != 0) stack.mulPose(Axis.ZP.rotationDegrees(rz));
        if (ry != 0) stack.mulPose(Axis.YP.rotationDegrees(ry));
        if (rx != 0) stack.mulPose(Axis.XP.rotationDegrees(rx));
        stack.translate(-x,-y,-z);
    }

    private static void attach(int pos, AttachmentPoses poses, PoseStack stack) {
        BonePose part = switch (pos) {
            case 2, 3 -> poses.head();
            case 1 -> poses.body();
            case 4 -> poses.rightArm();
            case 5 -> poses.leftArm();
            case 6 -> poses.rightLeg();
            case 7 -> poses.leftLeg();
            default -> null;
        };
        if (part != null) part.apply(stack);
    }

    private static float headLift(CosmeticsModule.Entry entry) {
        return switch (entry.resourceIndex()) {
            case 58, 59, 60, 61, 62 -> 0.45F;
            default -> 0.0F;
        };
    }

    private static boolean isHeadMounted(CosmeticsModule.Entry entry) {
        return "hat".equals(entry.type()) || "headwear".equals(entry.type());
    }
    private static float attachmentYOffset(int pos) { return switch (pos) { case 2 -> 0.0F; case 3 -> .40F; case 1 -> -.3F; case 4,5 -> -.25F; case 6,7 -> -.35F; default -> 0.0F; }; }
    private static float[] vector(JsonObject object, String key, float[] fallback) { if (!object.has(key) || !object.get(key).isJsonArray()) return fallback; JsonArray array=object.getAsJsonArray(key); float[] result=fallback.clone(); for(int i=0;i<Math.min(array.size(),result.length);i++) result[i]=array.get(i).getAsFloat(); return result; }
    private static float number(JsonObject object, String key, float fallback) { return object.has(key) ? object.get(key).getAsFloat() : fallback; }
    private static Vertex v(float x,float y,float z,float u,float vv) { return new Vertex(x,y,z,u,vv); }
    private record Cosmetic(List<Bone> roots,int pos,float scale,float x,float y,float z,float yaw,float pitch,float roll, Bounds bounds, AnimationSet animations) { }

    private record AnimationSet(AnimationClip idle, AnimationClip walk) {
        private static final AnimationSet EMPTY = new AnimationSet(null, null);

        private AnimationClip pick(float walkSpeed) {
            if (walkSpeed > 0.15F && walk != null) return walk;
            return idle;
        }
    }

    private record AnimationClip(String name, boolean loop, float length, Map<String, BoneTrack> bones) { }

    private record BoneTrack(NavigableMap<Float, float[]> rotation, NavigableMap<Float, float[]> position, NavigableMap<Float, float[]> scale) { }

    private static final class Bounds {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        void include(float x, float y, float z) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        boolean valid() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        float centerX() { return (minX + maxX) * 0.5F; }
        float centerY() { return (minY + maxY) * 0.5F; }
        float centerZ() { return (minZ + maxZ) * 0.5F; }

        float maxExtent() {
            return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }

        static Bounds unit() {
            Bounds bounds = new Bounds();
            bounds.include(-0.5F, -0.5F, -0.5F);
            bounds.include(0.5F, 0.5F, 0.5F);
            return bounds;
        }
    }

    public record AttachmentPoses(BonePose root, BonePose head, BonePose body, BonePose rightArm, BonePose leftArm, BonePose rightLeg, BonePose leftLeg) {
        public static AttachmentPoses of(HumanoidModel<?> model) {
            return ofParts(model.root(), model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg);
        }

        public static AttachmentPoses ofParts(ModelPart root, ModelPart head, ModelPart body,
                                               ModelPart rightArm, ModelPart leftArm,
                                               ModelPart rightLeg, ModelPart leftLeg) {
            return new AttachmentPoses(
                    BonePose.of(root),
                    BonePose.of(head),
                    BonePose.of(body),
                    BonePose.of(rightArm),
                    BonePose.of(leftArm),
                    BonePose.of(rightLeg),
                    BonePose.of(leftLeg)
            );
        }
    }

    public record BonePose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
        private static final BonePose ZERO = new BonePose(0, 0, 0, 0, 0, 0, 1, 1, 1);

        public static BonePose of(ModelPart part) {
            return part == null ? ZERO
                    : new BonePose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
        }

        public void apply(PoseStack stack) {
            stack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
            if (xRot != 0.0F || yRot != 0.0F || zRot != 0.0F) {
                stack.mulPose(new org.joml.Quaternionf().rotationZYX(zRot, yRot, xRot));
            }
            if (xScale != 1.0F || yScale != 1.0F || zScale != 1.0F) {
                stack.scale(xScale, yScale, zScale);
            }
        }
    }
    private record RotationOffset(float x, float y, float z) {
        private static final RotationOffset NONE = new RotationOffset(0.0F, 0.0F, 0.0F);
        private RotationOffset add(RotationOffset other) { return new RotationOffset(x + other.x, y + other.y, z + other.z); }
    }
    private static final class Bone { final String name; String parentName; float pivotX,pivotY,pivotZ,xRot,yRot,zRot; final List<Cube> cubes=new ArrayList<>(); final List<Bone> children=new ArrayList<>(); Bone(String name){this.name=name;} }
    private static final class Cube { float pivotX,pivotY,pivotZ,xRot,yRot,zRot; final List<Quad> quads=new ArrayList<>(); void add(Quad quad){if(quad!=null)quads.add(quad);} }
    private record Quad(Vertex[] vertices,float x,float y,float z) { }
    private record Vertex(float x,float y,float z,float u,float v) { }
}
