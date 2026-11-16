package tokyo.ghoul.client.features.modules.combat.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.math.MouseUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MatrixPlacement implements QuickImports {
    private static final double[] MATRIX_EYE_HEIGHTS = {1.62, 1.27, 0.4};
    private static final double[] NO_EXTRA_EYES = new double[0];
    private static final double[] FACE_SAMPLES = {0.5, 0.1, 0.9, 0.3, 0.7};
    private static final double RAY_LENGTH = 6.0;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private static final Vec3[] eyeBuffer = new Vec3[4];
    private static final PatternGuard patternGuard = new PatternGuard();
    private static boolean active;

    private MatrixPlacement() {}

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }


    public static float yawJitter(float yaw) {
        return active ? patternGuard.yawOffset(yaw) : ThreadLocalRandom.current().nextFloat();
    }

    public static float pitchJitter() {
        return active ? 0.0F : ThreadLocalRandom.current().nextFloat();
    }

    public static void onRotationSent(float yaw, float pitch) {
        patternGuard.onRotation(yaw, pitch);
    }

    public static void onAttack() {
        patternGuard.onAttack();
    }

    public static Vector2f snapToGcd(Vector2f rotation) {
        if (rotation == null) return null;
        double gcd = MouseUtil.getGCD();
        Rotation server = RotationManager.getInstance().getServerRotation();
        if (gcd <= 0.0 || server == null) return rotation;

        rotation.x = server.getYRot() + Math.round(Mth.wrapDegrees(rotation.x - server.getYRot()) / gcd) * (float) gcd;
        rotation.y = Mth.clamp(server.getXRot() + Math.round((rotation.y - server.getXRot()) / gcd) * (float) gcd, -90.0F, 90.0F);
        return rotation;
    }


    public static List<Vec3> eyePositions() {
        if (mc.player == null) return List.of();
        int count = fillEyes(eyeBuffer, MATRIX_EYE_HEIGHTS);
        return Arrays.asList(eyeBuffer).subList(0, count);
    }

    private static int fillEyes(Vec3[] out, double[] heights) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 feet = mc.player.position();
        out[0] = eye;
        int count = 1;
        for (double height : heights) {
            if (Math.abs(feet.y + height - eye.y) >= 1.0E-4) {
                out[count++] = new Vec3(feet.x, feet.y + height, feet.z);
            }
        }
        return count;
    }

    public static boolean withinCrystalReach(Vec3 crystal) {
        return mc.player != null && crystal != null && mc.player.position().distanceToSqr(crystal) <= 4.7 * 4.7;
    }

    public static boolean hasFastPickaxe() {
        if (mc.player == null) return false;
        return CrystalUtil.enchantmentLevel(CrystalUtil.miningTool(Blocks.OBSIDIAN.defaultBlockState()), Enchantments.EFFICIENCY) >= 4;
    }


    public static BlockHitResult hitFromAim(BlockPos pos, Direction side) {
        if (mc.player == null || mc.level == null || pos == null) return null;
        Vec3 look = CrystalUtil.direction(CrystalRotations.aimYaw(), CrystalRotations.aimPitch()).scale(RAY_LENGTH);
        int count = fillEyes(eyeBuffer, NO_EXTRA_EYES);

        for (int i = 0; i < count; i++) {
            Vec3 from = eyeBuffer[i];
            BlockHitResult hit = outlineRay(from, from.add(look));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) && (side == null || hit.getDirection() == side)) {
                return hit;
            }
        }
        return null;
    }

    public static boolean aimHitsEntity(Entity entity, double range) {
        if (mc.player == null || entity == null) return false;
        int count = fillEyes(eyeBuffer, MATRIX_EYE_HEIGHTS);
        for (int i = 0; i < count; i++) {
            if (CrystalUtil.rayHitsBox(eyeBuffer[i], CrystalRotations.aimYaw(), CrystalRotations.aimPitch(), range, true, entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    public static boolean visibleFromAnyEye(Vec3 point) {
        if (mc.player == null || mc.level == null || point == null) return false;
        int count = fillEyes(eyeBuffer, MATRIX_EYE_HEIGHTS);
        for (int i = 0; i < count; i++) {
            if (outlineRay(eyeBuffer[i], point).getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }


    public static Vector2f attackRotation(Entity crystal, Vec3 aim, double range) {
        if (mc.player == null || mc.level == null || crystal == null || aim == null) return null;
        int count = fillEyes(eyeBuffer, MATRIX_EYE_HEIGHTS);
        for (int i = 0; i < count; i++) {
            Vec3 from = eyeBuffer[i];
            Vector2f rotation = snapToGcd(CrystalUtil.rotationTo(from, aim));
            if (CrystalUtil.rayHitsBox(from, rotation.x, rotation.y, range, true, crystal.getBoundingBox())) {
                return rotation;
            }
        }
        return null;
    }

    public static Vector2f crystalRotation(BlockPos pos, double range, boolean throughWalls) {
        if (mc.player == null || mc.level == null || pos == null) return null;

        Vec3 eye = mc.player.getEyePosition();
        Vector2f best = null;
        Vector2f fallback = null;
        double bestDistance = Double.MAX_VALUE;
        double fallbackDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            Vec3 center = facePoint(pos, side, 0.5, 0.5);
            double distance = eye.distanceToSqr(center);
            if (distance > range * range) continue;

            Vector2f rotation = distance < bestDistance ? sampleFaceRotation(pos, side, range) : null;
            if (rotation != null) {
                bestDistance = distance;
                best = rotation;
            } else if (distance < fallbackDistance && throughWalls && ExplosionDamage.facesTowards(eye, center, side)) {
                fallbackDistance = distance;
                fallback = snapToGcd(CrystalUtil.rotationTo(eye, center));
            }
        }
        return best != null ? best : fallback;
    }

    public static Placement placeRotation(BlockPos pos, double range, boolean throughWalls) {
        if (mc.player == null || mc.level == null || pos == null) return null;

        Vec3 eye = mc.player.getEyePosition();
        int eyes = fillEyes(eyeBuffer, NO_EXTRA_EYES);
        Placement best = null;
        Placement fallback = null;
        double bestDistance = Double.MAX_VALUE;
        double fallbackDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side.getOpposite());
            BlockState state = mc.level.getBlockState(neighbor);
            if (state.canBeReplaced() || !ExplosionDamage.isFaceFull(state, neighbor, side)) continue;

            Vec3 center = facePoint(neighbor, side, 0.5, 0.5);
            double distance = eye.distanceToSqr(center);
            if (distance > range * range) continue;

            Placement placement = distance < bestDistance ? samplePlacement(neighbor, side, eyes, range) : null;
            if (placement != null) {
                bestDistance = distance;
                best = placement;
            } else if (distance < fallbackDistance && throughWalls && ExplosionDamage.facesTowards(eye, center, side)) {
                fallbackDistance = distance;
                fallback = new Placement(snapToGcd(CrystalUtil.rotationTo(eye, center)), neighbor, side, center);
            }
        }
        return best != null ? best : fallback;
    }

    private static Placement samplePlacement(BlockPos neighbor, Direction side, int eyes, double range) {
        Vec3 eye = mc.player.getEyePosition();
        for (double u : FACE_SAMPLES) {
            for (double v : FACE_SAMPLES) {
                Vec3 point = facePoint(neighbor, side, u, v);
                if (eye.distanceToSqr(point) > range * range) continue;

                for (int i = 0; i < eyes; i++) {
                    Vec3 from = eyeBuffer[i];
                    Vector2f rotation = snapToGcd(CrystalUtil.rotationTo(from, point));
                    Vec3 look = CrystalUtil.direction(rotation.x, rotation.y);
                    BlockHitResult hit = outlineRay(from, from.add(look.scale(RAY_LENGTH)));
                    if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(neighbor) && hit.getDirection() == side) {
                        return new Placement(rotation, neighbor, side, hit.getLocation());
                    }
                }
            }
        }
        return null;
    }

    private static Vector2f sampleFaceRotation(BlockPos pos, Direction side, double range) {
        Vec3 eye = mc.player.getEyePosition();
        for (double u : FACE_SAMPLES) {
            for (double v : FACE_SAMPLES) {
                Vec3 point = facePoint(pos, side, u, v);
                if (eye.distanceToSqr(point) > range * range) continue;

                Vector2f rotation = rotationHittingFace(pos, side, point);
                if (rotation != null) return rotation;
            }
        }
        return null;
    }

    private static Vector2f rotationHittingFace(BlockPos pos, Direction side, Vec3 point) {
        int eyes = fillEyes(eyeBuffer, NO_EXTRA_EYES);
        for (int i = 0; i < eyes; i++) {
            Vec3 from = eyeBuffer[i];
            Vector2f rotation = snapToGcd(CrystalUtil.rotationTo(from, point));
            Vec3 look = CrystalUtil.direction(rotation.x, rotation.y);
            BlockHitResult hit = outlineRay(from, from.add(look.scale(RAY_LENGTH)));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) && (side == null || hit.getDirection() == side)) {
                return rotation;
            }
        }
        return null;
    }

    public static Vec3 jitteredCenter(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 forward = center.subtract(eye);
        double length = forward.length();
        if (length < 1.0E-4) return center;

        forward = forward.scale(1.0 / length);
        Vec3 right = forward.cross(UP);
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double radius = Math.min(0.36, Math.tan(Math.toRadians(3.0)) * length);
        double angle = ExplosionDamage.random(0.0F, (float) (Math.PI * 2));
        return center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }


    private static Vec3 facePoint(BlockPos pos, Direction side, double u, double v) {
        Direction.Axis axis = side.getAxis();
        double x = pos.getX() + 0.5 + side.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + side.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.5;
        if (axis != Direction.Axis.X) x += sampleOffset(u);
        if (axis != Direction.Axis.Y) y += sampleOffset(axis == Direction.Axis.X ? u : v);
        if (axis != Direction.Axis.Z) z += sampleOffset(v);
        return new Vec3(x, y, z);
    }

    private static double sampleOffset(double value) {
        float spread = (float) Math.min(0.2F, 0.5 - Math.abs(value - 0.5) - 0.05);
        return value - 0.5 + ExplosionDamage.random(-spread, spread);
    }

    private static BlockHitResult outlineRay(Vec3 from, Vec3 to) {
        BlockHitResult hit = clipOutline(from, to);
        for (int i = 0; i < 4 && hit.getType() == HitResult.Type.BLOCK && hit.isInside(); i++) {
            Vec3 exit = ExplosionDamage.rayExit(from, to, hit.getBlockPos());
            if (exit == null) break;
            from = exit;
            hit = clipOutline(from, to);
        }
        return hit;
    }

    private static BlockHitResult clipOutline(Vec3 from, Vec3 to) {
        return mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    public record Placement(Vector2f rotation, BlockPos neighbor, Direction side, Vec3 aim) {}

    private static final class PatternGuard {
        private static final int HISTORY = 20;

        private final float[] yawDeltas = new float[HISTORY];
        private final float[] pitchDeltas = new float[HISTORY];
        private int filled;
        private float lastYaw = Float.NaN;
        private float lastPitch;
        private float lastYawDelta;
        private float lastPitchDelta;
        private int suspicion;
        private int attackWindow;
        private long lastSnapTime = Long.MIN_VALUE / 2;
        private float snapScore;

        float yawOffset(float yaw) {
            if (suspicion < 5 || Float.isNaN(lastYaw)) return 0.0F;

            float bestOffset = 0.0F;
            float bestChange = 0.0F;
            for (float offset = -12.3F; offset <= 12.3F; offset += 24.6F) {
                float delta = Math.abs(Mth.wrapDegrees(yaw + offset - lastYaw));
                float change = Math.abs(delta - lastYawDelta);
                if (change > bestChange) {
                    bestChange = change;
                    bestOffset = offset;
                }
            }
            return bestChange >= 6.0F ? bestOffset : 0.0F;
        }

        void onAttack() {
            attackWindow = 15;
            long sinceSnap = System.currentTimeMillis() - lastSnapTime;
            if (sinceSnap <= 350L) {
                snapScore += 8.0F - Math.round(sinceSnap / 50.0F);
            } else {
                snapScore = Math.max(0.0F, snapScore - 6.0F);
            }
            if (snapScore > 45.0F) snapScore = Math.max(0.0F, snapScore - 30.0F);
        }

        void onRotation(float yaw, float pitch) {
            if (Float.isNaN(lastYaw)) {
                lastYaw = yaw;
                lastPitch = pitch;
                return;
            }
            if (attackWindow > 0) attackWindow--;
            if (yaw == lastYaw && pitch == lastPitch) return;

            float yawDelta = Math.abs(Mth.wrapDegrees(yaw - lastYaw));
            float pitchDelta = Math.abs(pitch - lastPitch);
            if (pitchDelta > 20.0F) lastSnapTime = System.currentTimeMillis();

            push(yawDeltas, Math.abs(yawDelta - lastYawDelta));
            push(pitchDeltas, Math.abs(pitchDelta - lastPitchDelta));
            if (filled < HISTORY) filled++;

            if (filled >= HISTORY && attackWindow > 0) {
                boolean tinyYaw = yawDelta < 1.5F;
                boolean steady = mean(yawDeltas) < 1.0F || mean(pitchDeltas) < 1.0F && !tinyYaw;
                boolean robotic = deviation(yawDeltas) < 5.0F && deviation(pitchDeltas) > 5.0F && !tinyYaw;
                suspicion = steady && robotic ? suspicion + 1 : Math.max(0, suspicion - 1);
            }

            lastYawDelta = yawDelta;
            lastPitchDelta = pitchDelta;
            lastYaw = yaw;
            lastPitch = pitch;
        }

        private static void push(float[] values, float value) {
            System.arraycopy(values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
        }

        private static float mean(float[] values) {
            float sum = 0.0F;
            for (float value : values) sum += value;
            return sum / values.length;
        }

        private static float deviation(float[] values) {
            float mean = mean(values);
            float sum = 0.0F;
            for (float value : values) sum += (value - mean) * (value - mean);
            return (float) Math.sqrt(sum);
        }
    }
}
