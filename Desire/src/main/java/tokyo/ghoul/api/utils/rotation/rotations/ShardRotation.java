package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

import java.util.Random;

/**
 * Ротация под ShardAC.
 * - точка на хитбоксе плавно уходит к случайным якорям через случайные интервалы: без обводки и повторов;
 * - во время своего прыжка pitch считается от глаз "как стоя", голова не дёргается вверх/вниз;
 * - после удара скорость проседает, затем разгоняется с перелётом и возвращается к норме, все фазы рандомные;
 * - никакого высокочастотного шума: вся случайность низкочастотная, движение идёт через инерцию.
 */
public class ShardRotation extends RotationMode {
    private static final long FRAME_NS = 1_000_000_000L / 60L;

    private final Random random = new Random();

    private boolean initialized;
    private long lastFrameNs;
    private float prevYaw, prevPitch;
    private float velYaw, velPitch;
    private float residualYaw, residualPitch;

    private Entity trackedEntity;
    private boolean hasEngagement;
    private long engageMs;
    private float reactionMs, rampMs;

    // параметры одного захвата цели
    private float gain, yawSmooth, pitchSmooth, pitchScale, maxSpeed, leadTicks;

    // мультипоинт: доли хитбокса (side/depth от центра по ширине, height от низа по высоте)
    private float anchorSide, anchorDepth, anchorHeight;
    private float pointSide, pointDepth, pointHeight;
    private float wanderSide, wanderHeight;
    private float anchorRate;
    private long nextAnchorMs;
    private double targetBaseY;

    private float speedNoise = 1f, speedNoiseTarget = 1f, speedNoiseRate;
    private long nextSpeedRollMs;

    // огибающая скорости после удара
    private long attackMs = Long.MIN_VALUE;
    private float dropMs, holdMs, recoverMs, settleMs;
    private float slowFloor, recoverPeak;

    private double groundEyeY;
    private boolean wasAirborne;

    public ShardRotation() {
        super("Shard");
    }

    public void reset() {
        initialized = false;
        trackedEntity = null;
        hasEngagement = false;
        velYaw = velPitch = 0f;
        residualYaw = residualPitch = 0f;
        speedNoise = speedNoiseTarget = 1f;
        nextSpeedRollMs = 0L;
        attackMs = Long.MIN_VALUE;
        nextAnchorMs = 0L;
        wasAirborne = false;
    }

    public void onAttack() {
        attackMs = System.currentTimeMillis();
        dropMs = range(35f, 95f);
        holdMs = range(40f, 160f);
        recoverMs = range(150f, 380f);
        settleMs = range(180f, 480f);
        slowFloor = range(0.22f, 0.52f);
        recoverPeak = range(1.04f, 1.32f);
        // после удара рука чуть сползает - точка тоже уходит, но не сразу
        nextAnchorMs = Math.min(nextAnchorMs, attackMs + (long) range(70f, 260f));
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (targetRotation == null || mc.player == null || mc.level == null) return currentRotation;

        long nowNs = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        if (!initialized) {
            initialized = true;
            prevYaw = currentRotation.getYRot();
            prevPitch = currentRotation.getXRot();
            velYaw = velPitch = 0f;
            residualYaw = residualPitch = 0f;
            lastFrameNs = nowNs - FRAME_NS;
            groundEyeY = mc.player.getEyeY();
        }

        float dt = Mth.clamp((nowNs - lastFrameNs) / (float) FRAME_NS, 0f, 3f);
        if (dt < 0.05f) return new Rotation(prevYaw, prevPitch);
        lastFrameNs = nowNs;

        // кто-то другой повернул камеру - продолжаем с её положения, а не прыгаем назад
        if (Math.abs(Mth.wrapDegrees(currentRotation.getYRot() - prevYaw)) > 6f
                || Math.abs(currentRotation.getXRot() - prevPitch) > 6f) {
            prevYaw = currentRotation.getYRot();
            prevPitch = currentRotation.getXRot();
            residualYaw = residualPitch = 0f;
        }

        if (!hasEngagement || entity != trackedEntity) {
            startEngagement(entity, nowMs);
        }

        boolean elytra = mc.player.isFallFlying();
        boolean airborne = isAirborne();
        Vec3 eye = mc.player.getEyePosition();
        if (!airborne || elytra) {
            groundEyeY = eye.y;
        } else {
            if (!wasAirborne) groundEyeY = Math.min(groundEyeY, eye.y);
            // падение ниже уровня отрыва - тут голову уже честно ведём вниз
            if (eye.y < groundEyeY) groundEyeY = eye.y;
        }
        wasAirborne = airborne && !elytra;
        Vec3 aimEye = wasAirborne ? new Vec3(eye.x, groundEyeY, eye.z) : eye;

        float targetYaw;
        float targetPitch;
        boolean onHitbox = false;
        if (entity != null) {
            Vec3 point = updateAimPoint(entity, eye, nowMs, dt);
            Rotation aim = RotationUtil.fromVec3(point.subtract(aimEye));
            targetYaw = aim.getYRot();
            targetPitch = aim.getXRot();
            onHitbox = isOnHitbox(entity);
        } else {
            targetYaw = targetRotation.getYRot();
            targetPitch = targetRotation.getXRot();
        }

        float dYaw = Mth.wrapDegrees(targetYaw - prevYaw);
        float dPitch = targetPitch - prevPitch;
        float dist = (float) Math.hypot(dYaw, dPitch);

        float speed = updateSpeedNoise(nowMs, dt) * attackEnvelope(nowMs) * reactionRamp(nowMs);
        float limit = (elytra ? maxSpeed * 1.9f : maxSpeed) * speed;

        float desiredYaw = 0f;
        float desiredPitch = 0f;
        if (dist > 1.0e-3f && limit > 1.0e-3f) {
            // мягкое насыщение: далеко - упор в предел скорости, близко - пропорциональное доведение
            float mag = limit * (float) Math.tanh(dist * gain * speed / limit);
            if (onHitbox) {
                // уже на хитбоксе - лениво подтягиваемся, у самой точки замираем
                float hold = smoothStep(dist / 2.6f);
                mag *= 0.35f + 0.4f * hold;
                if (dist < 0.35f) mag = 0f;
            }
            desiredYaw = dYaw / dist * mag;
            desiredPitch = dPitch / dist * mag * pitchScale;
        }
        if (wasAirborne) {
            desiredPitch *= 0.3f;
        }

        float yawResponse = 1f - (float) Math.pow(1f - yawSmooth, dt);
        float pitchResponse = 1f - (float) Math.pow(1f - (wasAirborne ? pitchSmooth * 0.5f : pitchSmooth), dt);
        velYaw += (desiredYaw - velYaw) * yawResponse;
        velPitch += (desiredPitch - velPitch) * pitchResponse;

        float gcd = MouseUtil.getGCD();
        residualYaw += velYaw * dt;
        residualPitch += velPitch * dt;
        float stepYaw = Math.round(residualYaw / gcd) * gcd;
        float stepPitch = Math.round(residualPitch / gcd) * gcd;
        residualYaw -= stepYaw;
        residualPitch -= stepPitch;

        prevYaw += stepYaw;
        float newPitch = prevPitch + stepPitch;
        if (newPitch > 90f || newPitch < -89f) {
            newPitch = Mth.clamp(newPitch, -89f, 90f);
            velPitch = 0f;
            residualPitch = 0f;
        }
        prevPitch = newPitch;

        return new Rotation(prevYaw, prevPitch);
    }

    private void startEngagement(Entity entity, long nowMs) {
        hasEngagement = true;
        trackedEntity = entity;
        engageMs = nowMs;
        if (entity != null) {
            reactionMs = range(55f, 150f);
            rampMs = range(140f, 320f);
            targetBaseY = entity.getBoundingBox().minY;
            rollAnchor(nowMs, true);
            pointSide = anchorSide;
            pointDepth = anchorDepth;
            pointHeight = anchorHeight;
            wanderSide = wanderHeight = 0f;
        } else {
            reactionMs = range(20f, 70f);
            rampMs = range(90f, 200f);
        }
        gain = range(0.17f, 0.26f);
        yawSmooth = range(0.20f, 0.32f);
        pitchSmooth = range(0.16f, 0.27f);
        pitchScale = range(0.78f, 0.94f);
        maxSpeed = range(9.5f, 14.5f);
        leadTicks = range(0.35f, 1.15f);
    }

    private Vec3 updateAimPoint(Entity entity, Vec3 eye, long nowMs, float dt) {
        AABB box = entity.getBoundingBox();
        double width = box.getXsize();
        double height = box.getYsize();

        // прыжки цели отслеживаются с запаздыванием, как глазом
        targetBaseY += (box.minY - targetBaseY) * (1f - (float) Math.pow(1f - 0.14f, dt));
        if (Math.abs(box.minY - targetBaseY) > 2.5) targetBaseY = box.minY;

        if (nowMs >= nextAnchorMs) {
            rollAnchor(nowMs, false);
        }

        float approach = 1f - (float) Math.pow(1f - anchorRate, dt);
        pointSide += (anchorSide - pointSide) * approach;
        pointDepth += (anchorDepth - pointDepth) * approach;
        pointHeight += (anchorHeight - pointHeight) * approach;

        // медленный блуждающий снос поверх якоря (процесс Орнштейна-Уленбека, без периодики)
        float decay = (float) Math.pow(0.97f, dt);
        float sigma = (float) Math.sqrt(dt);
        wanderSide = wanderSide * decay + (float) random.nextGaussian() * 0.0035f * sigma;
        wanderHeight = wanderHeight * decay + (float) random.nextGaussian() * 0.003f * sigma;

        float side = Mth.clamp(pointSide + wanderSide, -0.32f, 0.32f);
        float depth = Mth.clamp(pointDepth, -0.32f, 0.32f);
        float up = Mth.clamp(pointHeight + wanderHeight, 0.32f, 0.82f);

        Vec3 motion = entity.getDeltaMovement();
        double x = box.getCenter().x + side * width + motion.x * leadTicks;
        double z = box.getCenter().z + depth * width + motion.z * leadTicks;
        double y = targetBaseY + up * height;
        y = Mth.clamp(y, box.minY + height * 0.2, box.maxY - height * 0.1);
        Vec3 point = new Vec3(x, y, z);

        if (!hasLineOfSight(eye, point)) {
            Vec3 center = box.getCenter();
            if (hasLineOfSight(eye, center)) {
                anchorSide *= 0.3f;
                anchorDepth *= 0.3f;
                anchorHeight = 0.5f;
                return center;
            }
            Vec3 top = new Vec3(center.x, box.minY + height * 0.85, center.z);
            if (hasLineOfSight(eye, top)) {
                anchorHeight = 0.8f;
                return top;
            }
        }
        return point;
    }

    private void rollAnchor(long nowMs, boolean first) {
        double distance = trackedEntity != null ? mc.player.distanceTo(trackedEntity) : 3.0;
        float spread = (float) Mth.clamp(distance / 4.0, 0.45, 1.0);

        anchorSide = Mth.clamp((float) random.nextGaussian() * 0.12f * spread, -0.28f, 0.28f);
        anchorDepth = Mth.clamp((float) random.nextGaussian() * 0.12f * spread, -0.28f, 0.28f);
        anchorHeight = Mth.clamp(0.56f + (float) random.nextGaussian() * 0.09f, 0.38f, 0.76f);
        anchorRate = range(0.018f, 0.065f);
        nextAnchorMs = nowMs + (long) (first ? range(250f, 700f) : range(380f, 1500f));
    }

    private float updateSpeedNoise(long nowMs, float dt) {
        if (nowMs >= nextSpeedRollMs) {
            speedNoiseTarget = range(0.80f, 1.20f);
            speedNoiseRate = range(0.03f, 0.09f);
            nextSpeedRollMs = nowMs + (long) range(220f, 900f);
        }
        speedNoise += (speedNoiseTarget - speedNoise) * (1f - (float) Math.pow(1f - speedNoiseRate, dt));
        return speedNoise;
    }

    private float attackEnvelope(long nowMs) {
        if (attackMs == Long.MIN_VALUE) return 1f;
        float t = nowMs - attackMs;
        if (t < 0f) return 1f;
        if (t < dropMs) return Mth.lerp(easeOut(t / dropMs), 1f, slowFloor);
        t -= dropMs;
        if (t < holdMs) return slowFloor;
        t -= holdMs;
        if (t < recoverMs) return Mth.lerp(smoothStep(t / recoverMs), slowFloor, recoverPeak);
        t -= recoverMs;
        if (t < settleMs) return Mth.lerp(smoothStep(t / settleMs), recoverPeak, 1f);
        return 1f;
    }

    private float reactionRamp(long nowMs) {
        float t = nowMs - engageMs;
        if (t < reactionMs) return 0.06f * (t / reactionMs);
        t -= reactionMs;
        if (t < rampMs) return Mth.lerp(smoothStep(t / rampMs), 0.06f, 1f);
        return 1f;
    }

    private boolean isAirborne() {
        return !mc.player.onGround()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable()
                && !mc.player.isPassenger()
                && !mc.player.getAbilities().flying;
    }

    private boolean isOnHitbox(Entity entity) {
        float distance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        return RaytracingUtil.rayTraceBox(new Rotation(prevYaw, prevPitch), distance, entity.getBoundingBox());
    }

    private boolean hasLineOfSight(Vec3 eye, Vec3 point) {
        HitResult result = mc.level.clip(new ClipContext(
                eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private float range(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static float smoothStep(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return 1f - (1f - x) * (1f - x);
    }
}
