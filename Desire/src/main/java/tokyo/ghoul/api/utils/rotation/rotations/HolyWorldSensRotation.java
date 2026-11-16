package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.api.utils.rotation.misc.MouseProfile;

import java.util.Random;

/**
 * Ротация в пространстве "отсчётов мыши": максимальная скорость берётся из замеров реальной мыши игрока
 * (DPI * скорость руки), а угол одного отсчёта — из игровой сенсы. Скорость и плавность рандомятся сами.
 */
public class HolyWorldSensRotation extends RotationMode {
    private static final float MIN_HEIGHT_FRAC = 0.52f;
    private static final float MAX_HEIGHT_FRAC = 0.78f;
    private static final float PITCH_WEIGHT = 0.75f;
    private static final float MIN_SMOOTH = 0.4f;
    private static final float MAX_SMOOTH = 0.85f;
    private static final float SPEED_BOOST = 1.3f * 1.3f;
    private static final long TICK_NS = 50_000_000L;
    private static final long FRAME_FALLBACK_NS = 16_000_000L;

    private final Random random = new Random();

    private boolean initialized;
    private int lastTick;
    private long lastFrameNs;
    private Rotation cached;

    private Entity trackedEntity;
    private int reactionTicks;

    // скорость курсора в отсчётах за тик
    private float velYaw, velPitch;
    private float residualYaw, residualPitch;
    private double noiseYaw, noisePitch;

    private float engagementSpeed = 1f;
    private float speedScale = 1f, speedScaleTarget = 1f;
    private int speedRollTicks;

    private float smooth = 0.6f, smoothTarget = 0.6f;
    private int smoothRollTicks;

    private float heightFrac = 0.65f, heightFracTarget = 0.65f;
    private int heightRollTicks;
    private float sideOffset, sideOffsetTarget;

    // уровень земли под целью: от него считается высота прицела, чтобы точка не прыгала вместе с целью
    private double groundY;
    private int targetAirTicks;

    private int attackDipTicks;
    private float attackDip = 1f;

    public HolyWorldSensRotation() {
        super("HolyWorld");
    }

    public void reset() {
        initialized = false;
        cached = null;
        trackedEntity = null;
        reactionTicks = 0;
        velYaw = velPitch = 0f;
        residualYaw = residualPitch = 0f;
        noiseYaw = noisePitch = 0.0;
        speedScale = speedScaleTarget = 1f;
        speedRollTicks = 0;
        smoothRollTicks = 0;
        heightRollTicks = 0;
        attackDipTicks = 0;
        attackDip = 1f;
    }

    public void onAttack() {
        // при клике рука на пару тиков притормаживает
        attackDipTicks = 2 + random.nextInt(3);
        attackDip = 0.5f + random.nextFloat() * 0.25f;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (targetRotation == null || mc.player == null) return currentRotation;

        int tick = mc.player.tickCount;
        long now = System.nanoTime();
        if (!initialized || tick - lastTick > 2 || tick < lastTick) {
            reset();
            initialized = true;
            lastTick = tick - 1;
            lastFrameNs = now - FRAME_FALLBACK_NS;
        }

        // шаг считается каждый кадр, dt — доля тика, прошедшая с прошлого кадра
        float dt = Mth.clamp((now - lastFrameNs) / (float) TICK_NS, 0f, 1.5f);
        if (dt < 1.0e-3f && cached != null) {
            return cached;
        }
        lastFrameNs = now;

        boolean newTick = tick != lastTick;
        lastTick = tick;

        if (entity != trackedEntity) {
            trackedEntity = entity;
            reactionTicks = entity != null ? 2 + random.nextInt(3) : 0;
            heightFrac = heightFracTarget = randomHeight();
            sideOffset = sideOffsetTarget = 0f;
            // у каждого захвата цели свой темп и своя плавность
            engagementSpeed = 0.7f + random.nextFloat() * 0.4f;
            smooth = smoothTarget = randomSmooth();
            smoothRollTicks = 20 + random.nextInt(41);
            if (entity != null) {
                groundY = entity.getBoundingBox().minY;
                targetAirTicks = 0;
            }
        }

        boolean elytra = mc.player.isFallFlying();

        // всё случайное и счётчики живут в тиках, сама кривая движения — в кадрах
        if (newTick) {
            updateSpeedScale();
            updateSmooth();
            if (entity != null) {
                updateDrift();
                updateGround(entity, entity.getBoundingBox());
            }
            if (attackDipTicks > 0) {
                attackDipTicks--;
            } else {
                attackDip = 1f;
            }
            if (reactionTicks > 0) {
                reactionTicks--;
            }
            float speed = (float) Math.hypot(velYaw, velPitch);
            double noiseStd = speed > 3f ? 0.015 * speed : 0.0;
            noiseYaw = noiseYaw * 0.85 + random.nextGaussian() * noiseStd;
            noisePitch = noisePitch * 0.85 + random.nextGaussian() * noiseStd * 0.6;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Rotation aim = entity != null ? aimRotation(entity, vec3d, elytra, partialTick) : targetRotation;

        float gcd = MouseUtil.getGCD();
        float errYaw = Mth.wrapDegrees(aim.getYRot() - currentRotation.getYRot()) / gcd;
        float errPitch = (aim.getXRot() - currentRotation.getXRot()) / gcd;
        float errDeg = (float) Math.hypot(errYaw, errPitch) * gcd;

        float maxVel = MouseProfile.getPeakCounts() * SPEED_BOOST * engagementSpeed * speedScale * attackDip
                * (elytra ? 2.0f : 1.0f);

        // доля ошибки, закрываемая за тик, пересчитана на длину кадра
        float gain = Math.min(0.8f, Mth.lerp(smooth, 0.55f, 0.22f) * SPEED_BOOST);
        float frameGain = (1f - (float) Math.pow(1f - gain, dt)) / Math.max(dt, 1.0e-3f);
        float wantYaw = errYaw * frameGain;
        float wantPitch = errPitch * frameGain * PITCH_WEIGHT;

        // на хитбоксе рука плавно сбавляет, без резкого переключения
        boolean onHitbox = entity != null
                && RaytracingUtil.rayTraceBox(currentRotation, 8.0, entity.getBoundingBox());
        if (onHitbox) {
            float settle = 0.35f + 0.65f * Mth.clamp(errDeg / 6f, 0f, 1f);
            wantYaw *= settle;
            wantPitch *= settle;
        }

        float precision = Mth.clamp(errDeg / 25f, 0.3f, 1f);
        float wantLen = (float) Math.hypot(wantYaw, wantPitch);
        float cap = maxVel * (0.35f + 0.65f * precision);
        if (wantLen > cap && wantLen > 1.0e-4f) {
            wantYaw *= cap / wantLen;
            wantPitch *= cap / wantLen;
        }

        if (reactionTicks > 0) {
            wantYaw = 0f;
            wantPitch = 0f;
        }

        // ограничение ускорения даёт плавный разгон и торможение
        float accel = Math.max(1f, maxVel * Mth.lerp(smooth, 0.7f, 0.18f)) * dt;
        float dvYaw = wantYaw - velYaw;
        float dvPitch = wantPitch - velPitch;
        float dvLen = (float) Math.hypot(dvYaw, dvPitch);
        if (dvLen > accel) {
            dvYaw *= accel / dvLen;
            dvPitch *= accel / dvLen;
        }
        velYaw += dvYaw;
        velPitch += dvPitch;

        // в прыжке под крит питч не трогаем вообще
        boolean lockPitch = entity != null && !elytra && HolyWorldRotation.isInJump();
        if (lockPitch) {
            velPitch = 0f;
            noisePitch = 0.0;
            residualPitch = 0f;
        }

        float noiseScale = onHitbox ? 0f : 1f;
        float outYaw = (velYaw + (float) noiseYaw * noiseScale) * dt + residualYaw;
        float outPitch = lockPitch ? 0f : (velPitch + (float) noisePitch * noiseScale) * dt + residualPitch;
        int countsYaw = Math.round(outYaw);
        int countsPitch = Math.round(outPitch);
        residualYaw = outYaw - countsYaw;
        residualPitch = lockPitch ? 0f : outPitch - countsPitch;

        float pitch = currentRotation.getXRot() + countsPitch * gcd;
        if (pitch > 90f || pitch < -90f) {
            pitch = Mth.clamp(pitch, -90f, 90f);
            velPitch = 0f;
            residualPitch = 0f;
        }

        cached = new Rotation(currentRotation.getYRot() + countsYaw * gcd, pitch);
        return cached;
    }

    private Rotation aimRotation(Entity entity, Vec3 fallback, boolean elytra, float partialTick) {
        Vec3 eye = mc.player.getEyePosition(partialTick);
        if (elytra && fallback != null) {
            return RotationUtil.fromVec3(fallback.subtract(eye));
        }

        // хитбокс в интерполированной позиции, чтобы точка ехала плавно, а не скачками по тикам
        double lagX = Mth.lerp(partialTick, entity.xo, entity.getX()) - entity.getX();
        double lagY = Mth.lerp(partialTick, entity.yo, entity.getY()) - entity.getY();
        double lagZ = Mth.lerp(partialTick, entity.zo, entity.getZ()) - entity.getZ();
        AABB bb = entity.getBoundingBox().move(lagX, lagY, lagZ);
        double insetX = bb.getXsize() * 0.3;
        double insetZ = bb.getZsize() * 0.3;
        double x = Mth.clamp(eye.x, bb.minX + insetX, bb.maxX - insetX);
        double z = Mth.clamp(eye.z, bb.minZ + insetZ, bb.maxZ - insetZ);
        double height = bb.getYsize();
        double y = Mth.clamp(groundY + lagY + height * heightFrac, bb.minY + height * 0.15, bb.maxY - height * 0.1);

        double dx = x - eye.x;
        double dz = z - eye.z;
        double horiz = Math.hypot(dx, dz);
        if (horiz > 1.0e-3) {
            double side = sideOffset * bb.getXsize() * 0.5;
            x = Mth.clamp(x - dz / horiz * side, bb.minX + insetX, bb.maxX - insetX);
            z = Mth.clamp(z + dx / horiz * side, bb.minZ + insetZ, bb.maxZ - insetZ);
        }

        Vec3 point = new Vec3(x, y, z);
        if (fallback != null && !isVisible(eye, point)) {
            point = fallback;
        }
        return RotationUtil.fromVec3(point.subtract(eye));
    }

    private void updateDrift() {
        if (heightRollTicks-- <= 0) {
            heightFracTarget = randomHeight();
            sideOffsetTarget = (random.nextFloat() * 2f - 1f) * 0.45f;
            heightRollTicks = 15 + random.nextInt(21);
        }
        heightFrac += (heightFracTarget - heightFrac) * 0.06f;
        sideOffset += (sideOffsetTarget - sideOffset) * 0.06f;
    }

    private void updateGround(Entity entity, AABB bb) {
        boolean climbing = entity instanceof LivingEntity living && living.onClimbable();
        if (entity.onGround() || entity.isInWater() || climbing) {
            groundY = bb.minY;
            targetAirTicks = 0;
            return;
        }
        targetAirTicks++;
        if (bb.minY < groundY) {
            // падает ниже точки отрыва — опускаемся вместе с ним
            groundY = bb.minY;
        } else if (targetAirTicks > 14) {
            // долго в воздухе (отброс, полёт) — это уже не прыжок, догоняем
            groundY += (bb.minY - groundY) * 0.15;
        }
    }

    private void updateSpeedScale() {
        if (speedRollTicks-- <= 0) {
            speedScaleTarget = 0.75f + random.nextFloat() * 0.5f;
            speedRollTicks = 8 + random.nextInt(18);
        }
        speedScale += (speedScaleTarget - speedScale) * 0.1f;
    }

    private void updateSmooth() {
        if (smoothRollTicks-- <= 0) {
            smoothTarget = randomSmooth();
            smoothRollTicks = 20 + random.nextInt(41);
        }
        smooth += (smoothTarget - smooth) * 0.05f;
    }

    private float randomSmooth() {
        return MIN_SMOOTH + random.nextFloat() * (MAX_SMOOTH - MIN_SMOOTH);
    }

    private float randomHeight() {
        return MIN_HEIGHT_FRAC + random.nextFloat() * (MAX_HEIGHT_FRAC - MIN_HEIGHT_FRAC);
    }

    private boolean isVisible(Vec3 eye, Vec3 point) {
        HitResult result = mc.level.clip(new ClipContext(
                eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }
}
