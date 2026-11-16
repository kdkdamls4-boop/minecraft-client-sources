package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;

import java.util.ArrayList;
import java.util.List;

public final class WorldShapeHelper {
    private static final List<float[]> QUADS = new ArrayList<>();
    private static final List<float[]> LINES = new ArrayList<>();

    private WorldShapeHelper() {}

    public static Vec3 interpolate(Entity entity, float partialTicks) {
        return new Vec3(
                Mth.lerp(partialTicks, entity.xOld, entity.getX()),
                Mth.lerp(partialTicks, entity.yOld, entity.getY()),
                Mth.lerp(partialTicks, entity.zOld, entity.getZ())
        );
    }

    public static float attachedYaw(LivingEntity entity, float partialTicks) {
        return Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
    }

    public static float attachedPitch(LivingEntity entity, float partialTicks) {
        return Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    public static Vec3 local(Vec3 base, float yawDeg, double x, double y, double z) {
        double yaw = Math.toRadians(yawDeg);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return base.add(x * cos - z * sin, y, x * sin + z * cos);
    }

    public static Vec3 headLocal(Vec3 base, float yawDeg, float pitchDeg, double headY, double lx, double ly, double lz) {
        double relY = ly - headY;
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double ny = relY * cp - lz * sp;
        double nz = relY * sp + lz * cp;
        double nx = lx * cy - nz * sy;
        double nz2 = lx * sy + nz * cy;
        return base.add(nx, headY + ny, nz2);
    }

    public static void begin() {
        QUADS.clear();
        LINES.clear();
    }

    public static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        QUADS.add(pack(a, argb));
        QUADS.add(pack(b, argb));
        QUADS.add(pack(c, argb));
        QUADS.add(pack(d, argb));
    }

    public static void triangle(Vec3 a, Vec3 b, Vec3 c, int argb) {
        quad(a, b, c, a, argb);
    }

    public static void line(List<Vec3> points, int argb, float width) {
        if (points.size() < 2) return;
        for (int i = 0; i < points.size() - 1; i++) {
            emitLine(points.get(i), points.get(i + 1), argb, width);
        }
    }

    public static void gradientLine(List<Vec3> points, int startArgb, int endArgb, float width) {
        if (points.size() < 2) return;
        int segments = points.size() - 1;
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            emitLine(points.get(i), points.get(i + 1), lerpArgb(startArgb, endArgb, t0), width);
            emitLine(points.get(i), points.get(i + 1), lerpArgb(startArgb, endArgb, t1), Math.max(1f, width * 0.65f));
        }
    }

    public static void box(AABB box, int argb, boolean outline) {
        Vec3 a = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 b = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 c = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 d = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 e = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 f = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 g = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 h = new Vec3(box.minX, box.maxY, box.maxZ);
        if (outline) {
            line(List.of(a, b, c, d, a), argb, 1.5f);
            line(List.of(e, f, g, h, e), argb, 1.5f);
            line(List.of(a, e), argb, 1.5f);
            line(List.of(b, f), argb, 1.5f);
            line(List.of(c, g), argb, 1.5f);
            line(List.of(d, h), argb, 1.5f);
        } else {
            quad(a, b, c, d, argb);
            quad(e, f, g, h, argb);
            quad(a, b, f, e, argb);
            quad(d, c, g, h, argb);
            quad(a, d, h, e, argb);
            quad(b, c, g, f, argb);
        }
    }

    public static void boxFillCulled(AABB box, int argb, double camX, double camY, double camZ) {
        Vec3 a = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 b = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 c = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 d = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 e = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 f = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 g = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 h = new Vec3(box.minX, box.maxY, box.maxZ);

        boolean inside = camX >= box.minX && camX <= box.maxX
                && camY >= box.minY && camY <= box.maxY
                && camZ >= box.minZ && camZ <= box.maxZ;

        if (inside) {
            quad(a, b, c, d, argb);
            quad(e, f, g, h, argb);
            quad(a, b, f, e, argb);
            quad(d, c, g, h, argb);
            quad(a, d, h, e, argb);
            quad(b, c, g, f, argb);
            return;
        }

        if (camY > box.maxY) quad(e, f, g, h, argb);
        if (camY < box.minY) quad(a, b, c, d, argb);
        if (camZ < box.minZ) quad(a, b, f, e, argb);
        if (camZ > box.maxZ) quad(d, c, g, h, argb);
        if (camX < box.minX) quad(a, d, h, e, argb);
        if (camX > box.maxX) quad(b, c, g, f, argb);
    }

    public static int pressedKey(tokyo.ghoul.api.event.events.client.KeyEvent.KeyEventData event) {
        return event.mouse() ? -100 + event.key() : event.key();
    }

    public static void submit(Render3DEvent.Render3DEventData event) {
        var camera = event.cameraState();
        if (camera == null || !camera.initialized) {
            begin();
            return;
        }
        double camX = camera.pos.x;
        double camY = camera.pos.y;
        double camZ = camera.pos.z;
        PoseStack matrices = event.matrixStack();
        if (!QUADS.isEmpty()) {
            List<float[]> quads = QUADS.size() > 65536 ? List.copyOf(QUADS.subList(0, 65536)) : List.copyOf(QUADS);
            event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.QUADS, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                for (float[] v : quads) {
                    vertex(buffer, matrix, (float) (v[0] - camX), (float) (v[1] - camY), (float) (v[2] - camZ), v[3]);
                }
            });
        }
        if (!LINES.isEmpty()) {
            List<float[]> lines = LINES.size() > 65536 ? List.copyOf(LINES.subList(0, 65536)) : List.copyOf(LINES);
            event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                for (float[] v : lines) {
                    vertex(buffer, matrix, (float) (v[0] - camX), (float) (v[1] - camY), (float) (v[2] - camZ), v[3]);
                }
            });
        }
        begin();
    }

    public static int applyOpacity(int argb, float opacity) {
        int a = Mth.clamp((int) (((argb >>> 24) & 0xFF) * opacity), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int rgb(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    public static int lerpArgb(int from, int to, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = lerpComp(from, to, 24, t);
        int r = lerpComp(from, to, 16, t);
        int g = lerpComp(from, to, 8, t);
        int b = lerpComp(from, to, 0, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpComp(int from, int to, int shift, float t) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return (int) (a + (b - a) * t);
    }

    private static void emitLine(Vec3 from, Vec3 to, int argb, float width) {
        LINES.add(pack(from, argb));
        LINES.add(pack(to, argb));
        if (width <= 1.5f) return;
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.hypot(dx, dz);
        if (len < 1.0E-4) return;
        double ox = (-dz / len) * 0.004 * width;
        double oz = (dx / len) * 0.004 * width;
        LINES.add(pack(from.add(ox, 0, oz), argb));
        LINES.add(pack(to.add(ox, 0, oz), argb));
        LINES.add(pack(from.add(-ox, 0, -oz), argb));
        LINES.add(pack(to.add(-ox, 0, -oz), argb));
    }

    private static float[] pack(Vec3 pos, int argb) {
        return new float[]{(float) pos.x, (float) pos.y, (float) pos.z, Float.intBitsToFloat(argb)};
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float packedArgb) {
        buffer.addVertex(matrix, x, y, z).setColor(Float.floatToRawIntBits(packedArgb));
    }

    public static void glow(Render3DEvent.Render3DEventData event, Vec3 world, float size, int argb) {
        var camera = event.cameraState();
        if (camera == null || !camera.initialized) return;
        PoseStack matrices = event.matrixStack();
        matrices.pushPose();
        matrices.translate(world.x - camera.pos.x, world.y - camera.pos.y, world.z - camera.pos.z);
        matrices.mulPose(camera.orientation);
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.textured(tokyo.ghoul.api.system.files.FileUtil.getImage("particles/glow")), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            buffer.addVertex(matrix, size, -size, 0f).setUv(0f, 1f).setColor(argb);
            buffer.addVertex(matrix, -size, -size, 0f).setUv(1f, 1f).setColor(argb);
            buffer.addVertex(matrix, -size, size, 0f).setUv(1f, 0f).setColor(argb);
            buffer.addVertex(matrix, size, size, 0f).setUv(0f, 0f).setColor(argb);
        });
        matrices.popPose();
    }
}
