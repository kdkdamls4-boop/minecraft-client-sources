package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;

import java.awt.Color;

@ModuleRegister(name = "BlockOverlay", description = "Подсветка выбранного блока", category = Category.RENDER)
public class BlockOverlayModule extends Module {
    @Getter private static final BlockOverlayModule instance = new BlockOverlayModule();

    private static final double MAX_DISTANCE_SQ = 36.0;
    private static final double INFLATE = 0.003;
    private static final int PLASMA_GRID = 6;

    private final ModeSetting mode = new ModeSetting("Mode").values("Plasma", "Crystal", "Fill");
    private final BooleanSetting outline = new BooleanSetting("Theme Outline").value(true);
    private final SliderSetting opacity = new SliderSetting("Opacity").value(0.72f).range(0.1f, 1f).step(0.05f);
    private final SliderSetting plasmaSpeed = new SliderSetting("Plasma Speed").value(1.15f)
            .range(0.1f, 5f).step(0.1f).setVisible(() -> mode.is("Plasma"));
    private final SliderSetting plasmaScale = new SliderSetting("Plasma Scale").value(2.4f)
            .range(0.5f, 5f).step(0.1f).setVisible(() -> mode.is("Plasma"));
    private final SliderSetting outlineOpacity = new SliderSetting("Outline Opacity").value(70f)
            .range(5f, 100f).step(1f).setVisible(outline::getValue);
    private final BooleanSetting smooth = new BooleanSetting("Smooth").value(true);
    private final SliderSetting smoothSpeed = new SliderSetting("Smooth Speed").value(10f)
            .range(2f, 25f).step(0.5f).setVisible(smooth::getValue);

    private AABB current;
    private long lastNanos = -1L;

    public BlockOverlayModule() {
        addSettings(mode, outline, opacity, plasmaSpeed, plasmaScale, outlineOpacity, smooth, smoothSpeed);
    }

    @Override
    public void onEvent() {
        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(this::onWorldRender));
        addEvents(render);
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    public static boolean hidesVanillaOutline() {
        BlockOverlayModule m = instance;
        return m.isEnabled() && mc.level != null && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private void reset() {
        current = null;
        lastNanos = -1L;
    }

    private void onWorldRender(Render3DEvent.Render3DEventData event) {
        if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || hit.isInside()) {
            reset();
            return;
        }
        var camera = event.cameraState();
        if (camera == null || !camera.initialized) return;
        Vec3 cam = camera.pos;

        BlockPos pos = hit.getBlockPos();
        if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
            reset();
            return;
        }
        AABB target = blockBox(pos);
        if (target.contains(cam)) {
            reset();
            return;
        }
        follow(target);

        AABB box = current.inflate(INFLATE);
        PoseStack matrices = event.matrixStack();
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        Color accent = UIColors.primary();
        Color hot = new Color(Math.min(255, accent.getRed() + 70), Math.min(255, accent.getGreen() + 55),
                Math.min(255, accent.getBlue() + 40));

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.QUADS, (pose, buffer) ->
                emitFill(pose.pose(), buffer, box, cam, accent, hot, time));
        if (outline.getValue()) {
            event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) ->
                    emitOutline(pose.pose(), buffer, box, cam, accent, hot));
        }
    }

    private AABB blockBox(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        AABB bounds = state.getShape(mc.level, pos).bounds();
        boolean degenerate = bounds.hasNaN() || (bounds.minX >= bounds.maxX - 1.0E-7
                && bounds.minY >= bounds.maxY - 1.0E-7 && bounds.minZ >= bounds.maxZ - 1.0E-7);
        return degenerate ? new AABB(pos) : bounds.move(pos.getX(), pos.getY(), pos.getZ());
    }

    private void follow(AABB target) {
        long now = System.nanoTime();
        if (!smooth.getValue() || current == null || lastNanos < 0L) {
            current = target;
        } else {
            double dt = Math.max(0.0, (now - lastNanos) / 1.0E9);
            double t = 1.0 - Math.exp(-smoothSpeed.getValue() * dt);
            current = new AABB(
                    Mth.lerp(t, current.minX, target.minX), Mth.lerp(t, current.minY, target.minY),
                    Mth.lerp(t, current.minZ, target.minZ), Mth.lerp(t, current.maxX, target.maxX),
                    Mth.lerp(t, current.maxY, target.maxY), Mth.lerp(t, current.maxZ, target.maxZ));
        }
        lastNanos = now;
    }


    private void emitFill(Matrix4f m, VertexConsumer b, AABB box, Vec3 cam, Color accent, Color hot, float time) {
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
        if (cam.y < y1) face(m, b, box, cam, accent, hot, time, x1, y1, z1, x2 - x1, 0, 0, 0, 0, z2 - z1);
        if (cam.y > y2) face(m, b, box, cam, accent, hot, time, x1, y2, z1, x2 - x1, 0, 0, 0, 0, z2 - z1);
        if (cam.z < z1) face(m, b, box, cam, accent, hot, time, x1, y1, z1, x2 - x1, 0, 0, 0, y2 - y1, 0);
        if (cam.z > z2) face(m, b, box, cam, accent, hot, time, x1, y1, z2, x2 - x1, 0, 0, 0, y2 - y1, 0);
        if (cam.x < x1) face(m, b, box, cam, accent, hot, time, x1, y1, z1, 0, 0, z2 - z1, 0, y2 - y1, 0);
        if (cam.x > x2) face(m, b, box, cam, accent, hot, time, x2, y1, z1, 0, 0, z2 - z1, 0, y2 - y1, 0);
    }

    private void face(Matrix4f m, VertexConsumer b, AABB box, Vec3 cam, Color accent, Color hot, float time,
                      double ox, double oy, double oz, double ux, double uy, double uz,
                      double vx, double vy, double vz) {
        double[] u = {ux, uy, uz};
        double[] v = {vx, vy, vz};
        int n = mode.is("Plasma") ? PLASMA_GRID : 1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double a0 = (double) i / n, a1 = (double) (i + 1) / n, b0 = (double) j / n, b1 = (double) (j + 1) / n;
                vertex(m, b, cam, ox + u[0] * a0 + v[0] * b0, oy + u[1] * a0 + v[1] * b0, oz + u[2] * a0 + v[2] * b0, accent, hot, time);
                vertex(m, b, cam, ox + u[0] * a1 + v[0] * b0, oy + u[1] * a1 + v[1] * b0, oz + u[2] * a1 + v[2] * b0, accent, hot, time);
                vertex(m, b, cam, ox + u[0] * a1 + v[0] * b1, oy + u[1] * a1 + v[1] * b1, oz + u[2] * a1 + v[2] * b1, accent, hot, time);
                vertex(m, b, cam, ox + u[0] * a0 + v[0] * b1, oy + u[1] * a0 + v[1] * b1, oz + u[2] * a0 + v[2] * b1, accent, hot, time);
            }
        }
    }

    private void vertex(Matrix4f m, VertexConsumer b, Vec3 cam, double x, double y, double z,
                        Color accent, Color hot, float time) {
        b.addVertex(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
                .setColor(fillColor(x, y, z, accent, hot, time));
    }

    private int fillColor(double x, double y, double z, Color accent, Color hot, float time) {
        float alpha = opacity.getValue();
        switch (mode.getValue()) {
            case "Plasma" -> {
                float t = time * plasmaSpeed.getValue();
                float k = plasmaScale.getValue() * 3f;
                float v = (Mth.sin((float) (x * k) + t) + Mth.sin((float) (y * k * 1.3) - t * 0.7f)
                        + Mth.sin((float) (z * k * 0.9) + t * 1.1f) + Mth.sin((float) ((x + y + z) * k * 0.6) - t * 0.5f)) / 4f;
                float p = Mth.clamp((0.5f + 0.5f * v - 0.15f) / 0.8f, 0f, 1f);
                p = p * p * (3f - 2f * p);
                float glow = p * p * p * 0.35f;
                return pack(mix(accent.getRed(), hot.getRed(), p) + hot.getRed() * glow,
                        mix(accent.getGreen(), hot.getGreen(), p) + hot.getGreen() * glow,
                        mix(accent.getBlue(), hot.getBlue(), p) + hot.getBlue() * glow, alpha);
            }
            case "Crystal" -> {
                float pulse = 0.5f + 0.5f * Mth.sin(time * 2.4f);
                float f = 0.75f + pulse * 0.35f + pulse * pulse * 0.25f;
                return pack(accent.getRed() * f, accent.getGreen() * f, accent.getBlue() * f, alpha * (0.85f + pulse * 0.15f));
            }
            default -> {
                return pack(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha);
            }
        }
    }


    private void emitOutline(Matrix4f m, VertexConsumer b, AABB box, Vec3 cam, Color accent, Color hot) {
        Color mixed = new Color(
                Mth.clamp((int) (mix(accent.getRed(), hot.getRed(), 0.65f) * 1.15f), 0, 255),
                Mth.clamp((int) (mix(accent.getGreen(), hot.getGreen(), 0.65f) * 1.15f), 0, 255),
                Mth.clamp((int) (mix(accent.getBlue(), hot.getBlue(), 0.65f) * 1.15f), 0, 255));
        int argb = pack(mixed.getRed(), mixed.getGreen(), mixed.getBlue(), outlineOpacity.getValue() / 100f);

        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
        boolean down = cam.y < box.minY, up = cam.y > box.maxY;
        boolean north = cam.z < box.minZ, south = cam.z > box.maxZ;
        boolean west = cam.x < box.minX, east = cam.x > box.maxX;

        if (down || north) line(b, m, x1, y1, z1, x2, y1, z1, argb);
        if (down || south) line(b, m, x1, y1, z2, x2, y1, z2, argb);
        if (down || west) line(b, m, x1, y1, z1, x1, y1, z2, argb);
        if (down || east) line(b, m, x2, y1, z1, x2, y1, z2, argb);
        if (up || north) line(b, m, x1, y2, z1, x2, y2, z1, argb);
        if (up || south) line(b, m, x1, y2, z2, x2, y2, z2, argb);
        if (up || west) line(b, m, x1, y2, z1, x1, y2, z2, argb);
        if (up || east) line(b, m, x2, y2, z1, x2, y2, z2, argb);
        if (north || west) line(b, m, x1, y1, z1, x1, y2, z1, argb);
        if (north || east) line(b, m, x2, y1, z1, x2, y2, z1, argb);
        if (south || west) line(b, m, x1, y1, z2, x1, y2, z2, argb);
        if (south || east) line(b, m, x2, y1, z2, x2, y2, z2, argb);
    }

    private static void line(VertexConsumer b, Matrix4f m, float x1, float y1, float z1,
                             float x2, float y2, float z2, int argb) {
        b.addVertex(m, x1, y1, z1).setColor(argb);
        b.addVertex(m, x2, y2, z2).setColor(argb);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int pack(float r, float g, float b, float a) {
        int ai = Mth.clamp((int) (a * 255f), 0, 255);
        int ri = Mth.clamp((int) r, 0, 255);
        int gi = Mth.clamp((int) g, 0, 255);
        int bi = Mth.clamp((int) b, 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
