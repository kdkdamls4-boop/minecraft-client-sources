package tokyo.ghoul.client.features.modules.combat.crystal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CrystalVisuals implements QuickImports {
    private static final int BLOCK_COLOR = 0x2D7DFF;
    private static final int CRYSTAL_COLOR = 0xB446FF;
    private static final int MINING_COLOR = 0xFF3737;

    private static final long MINING_HOLD_MS = 140L;
    private static final float MINING_FADE_MS = 160.0F;

    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<EndCrystal> trackedCrystals = new ArrayList<>();

    private BlockPos miningPos;
    private float miningProgress;
    private long miningUpdated;

    public void onBlockPlaced(BlockPos pos) {
        ghosts.add(new Ghost(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false, false));
    }

    public void onBlockBroken(BlockPos pos) {
        ghosts.add(new Ghost(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false, true));
    }

    public void onMining(BlockPos pos, float progress) {
        miningPos = pos;
        miningProgress = progress;
        miningUpdated = System.currentTimeMillis();
    }

    public void clearMining() {
        miningPos = null;
    }

    public boolean isMining(BlockPos pos) {
        return pos != null && pos.equals(miningPos);
    }

    public void tick(double range, boolean track) {
        Iterator<Ghost> iterator = ghosts.iterator();
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            ghost.age++;
            ghost.prevY = ghost.y;
            ghost.y += ghost.rise;
            ghost.rise *= 0.95;
            if (ghost.age >= ghost.lifetime) iterator.remove();
        }

        for (EndCrystal crystal : trackedCrystals) {
            Entity.RemovalReason reason = crystal.getRemovalReason();
            if (reason != null && reason.shouldDestroy()) {
                ghosts.add(new Ghost(crystal.getX(), crystal.getY(), crystal.getZ(), true, true));
            }
        }

        trackedCrystals.clear();
        if (track && mc.level != null && mc.player != null) {
            mc.level.getEntities(EntityTypes.END_CRYSTAL, mc.player.getBoundingBox().inflate(range), crystal -> !crystal.isRemoved(), trackedCrystals);
        }
    }

    public void clear() {
        ghosts.clear();
        trackedCrystals.clear();
        miningPos = null;
    }

    public void render(Render3DEvent.Render3DEventData event) {
        var camera = event.cameraState();
        if (camera == null || !camera.initialized) return;

        List<ColoredBox> boxes = new ArrayList<>();
        collectGhosts(boxes, event.partialTicks());
        collectMining(boxes);
        if (boxes.isEmpty()) return;

        double camX = camera.pos.x, camY = camera.pos.y, camZ = camera.pos.z;
        PoseStack matrices = event.matrixStack();
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.QUADS, (pose, buffer) -> {
            for (ColoredBox box : boxes) emitBox(pose.pose(), buffer, box.box(), camX, camY, camZ, withAlpha(box.color(), box.alpha() * 0.45F), false);
        });
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) -> {
            for (ColoredBox box : boxes) emitBox(pose.pose(), buffer, box.box(), camX, camY, camZ, withAlpha(box.color(), box.alpha()), true);
        });
    }

    private void collectGhosts(List<ColoredBox> out, float partialTicks) {
        for (Ghost ghost : ghosts) {
            float t = Math.min(1.0F, (ghost.age + partialTicks) / ghost.lifetime);
            float alpha = ghost.vanishing ? 1.0F - easeOutCubic(t) : 1.0F - t;
            if (alpha <= 0.01F) continue;

            float scale = ghost.vanishing ? 1.0F - 0.2F * t : Mth.lerp(easeOutBack(t), 0.6F, 1.0F);
            double y = Mth.lerp(partialTicks, ghost.prevY, ghost.y);
            double half = 0.5 * scale;
            double center = y + (ghost.crystal ? 1.2 : 0.5);
            AABB box = new AABB(ghost.x - half, center - half, ghost.z - half, ghost.x + half, center + half, ghost.z + half);
            out.add(new ColoredBox(box, ghost.crystal ? CRYSTAL_COLOR : BLOCK_COLOR, alpha));
        }
    }

    private void collectMining(List<ColoredBox> out) {
        if (miningPos == null) return;

        long elapsed = System.currentTimeMillis() - miningUpdated;
        float fade;
        if (elapsed <= MINING_HOLD_MS) {
            fade = 1.0F;
        } else {
            float t = (elapsed - MINING_HOLD_MS) / MINING_FADE_MS;
            if (t >= 1.0F) {
                miningPos = null;
                return;
            }
            fade = 1.0F - t;
        }

        float size = 0.55F + 0.45F * miningProgress;
        float alpha = (0.45F + 0.4F * miningProgress) * fade;
        double half = 0.5 * Mth.clamp(size, 0.05F, 1.0F);
        double cx = miningPos.getX() + 0.5, cy = miningPos.getY() + 0.5, cz = miningPos.getZ() + 0.5;
        out.add(new ColoredBox(new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half), MINING_COLOR, alpha));
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float x = t - 1.0F;
        return 1.0F + c3 * x * x * x + c1 * x * x;
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return a << 24 | rgb & 0xFFFFFF;
    }

    private static void emitBox(Matrix4f matrix, VertexConsumer buffer, AABB box, double camX, double camY, double camZ, int argb, boolean outline) {
        float x1 = (float) (box.minX - camX), y1 = (float) (box.minY - camY), z1 = (float) (box.minZ - camZ);
        float x2 = (float) (box.maxX - camX), y2 = (float) (box.maxY - camY), z2 = (float) (box.maxZ - camZ);
        if (outline) {
            line(buffer, matrix, x1, y1, z1, x2, y1, z1, argb);
            line(buffer, matrix, x2, y1, z1, x2, y1, z2, argb);
            line(buffer, matrix, x2, y1, z2, x1, y1, z2, argb);
            line(buffer, matrix, x1, y1, z2, x1, y1, z1, argb);
            line(buffer, matrix, x1, y2, z1, x2, y2, z1, argb);
            line(buffer, matrix, x2, y2, z1, x2, y2, z2, argb);
            line(buffer, matrix, x2, y2, z2, x1, y2, z2, argb);
            line(buffer, matrix, x1, y2, z2, x1, y2, z1, argb);
            line(buffer, matrix, x1, y1, z1, x1, y2, z1, argb);
            line(buffer, matrix, x2, y1, z1, x2, y2, z1, argb);
            line(buffer, matrix, x1, y1, z2, x1, y2, z2, argb);
            line(buffer, matrix, x2, y1, z2, x2, y2, z2, argb);
            return;
        }
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, argb);
        quad(buffer, matrix, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, argb);
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, argb);
        quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, argb);
        quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, argb);
        quad(buffer, matrix, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, argb);
    }

    private static void line(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x3, y3, z3).setColor(argb);
        buffer.addVertex(matrix, x4, y4, z4).setColor(argb);
    }

    private record ColoredBox(AABB box, int color, float alpha) {}

    private static final class Ghost {
        private final double x, z;
        private final boolean crystal;
        private final boolean vanishing;
        private final int lifetime;
        private double y, prevY;
        private double rise;
        private int age;

        private Ghost(double x, double y, double z, boolean crystal, boolean vanishing) {
            this.x = x;
            this.y = y;
            this.prevY = y;
            this.z = z;
            this.crystal = crystal;
            this.vanishing = vanishing;
            this.lifetime = vanishing ? 20 : 8;
            this.rise = vanishing ? 0.16 : 0.0;
        }
    }
}
