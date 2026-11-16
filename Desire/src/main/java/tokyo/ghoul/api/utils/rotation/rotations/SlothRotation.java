package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

import java.util.Random;

public class SlothRotation extends RotationMode {
    private static final Random RNG = new Random();

    private static float currentYaw;
    private static float currentPitch;
    private static float smoothYaw;
    private static float smoothPitch;
    private static float velocityYaw;
    private static float velocityPitch;
    private static boolean initialized;

    private static int trackedEntityId = Integer.MIN_VALUE;
    private static long firstSeenTime;
    private static int reactionMs;
    private static boolean reactionComplete;

    private static long aimStartTime;
    private static float aimFatigue;

    private static long lastAttackTime = Long.MIN_VALUE;
    private static float attackSlowdown = 0.55f;
    private static float attackRecoveryMs = 260f;
    private static float pitchBeforeHit;
    private static int hitPhase = 0;
    private static int hitTimer = 0;

    private static float noiseAngle = 0.0f;
    private static final float NOISE_AMPLITUDE = 1.35f;

    private static double lastGroundedPlayerY;
    private static float jumpOffsetVariation = 0.0f;
    private static boolean wasInJump = false;

    private static Vec3 wanderOffset = Vec3.ZERO;
    private static Vec3 wanderVel = Vec3.ZERO;

    private static float randomSpeedSpread = 1.0f;
    private static int speedRollTicks = 0;

    public SlothRotation() {
        super("Sloth");
    }

    public static void reset() {
        initialized = false;
        trackedEntityId = Integer.MIN_VALUE;
        firstSeenTime = 0L;
        reactionMs = 0;
        reactionComplete = false;

        velocityYaw = 0.0f;
        velocityPitch = 0.0f;
        aimStartTime = 0L;
        aimFatigue = 0.0f;

        hitPhase = 0;
        hitTimer = 0;
        lastAttackTime = Long.MIN_VALUE;

        noiseAngle = (float) (Math.random() * Math.PI * 2);
        jumpOffsetVariation = 0.0f;
        wasInJump = false;
        lastGroundedPlayerY = mc.player != null ? mc.player.getY() : 0.0;

        wanderOffset = Vec3.ZERO;
        wanderVel = Vec3.ZERO;
        randomSpeedSpread = 1.0f;
        speedRollTicks = 0;

        if (mc.player != null) {
            currentYaw = smoothYaw = mc.player.getYRot();
            currentPitch = smoothPitch = mc.player.getXRot();
        }
    }

    public static boolean isInJump() {
        if (mc.player == null) return false;
        return !mc.player.onGround()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable()
                && !mc.player.isPassenger()
                && !mc.player.isFallFlying()
                && !mc.player.getAbilities().flying;
    }

    public static void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        attackSlowdown = MathUtil.randomInRange(0.68f, 0.84f);
        attackRecoveryMs = MathUtil.randomInRange(150f, 240f);

        hitPhase = 1;
        hitTimer = 0;
        pitchBeforeHit = currentPitch;

        velocityYaw *= 0.65f;
        velocityPitch *= 0.65f;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (targetRotation == null || mc.player == null) return currentRotation;

        if (!initialized) {
            currentYaw = smoothYaw = currentRotation.getYRot();
            currentPitch = smoothPitch = currentRotation.getXRot();
            initialized = true;
            lastGroundedPlayerY = mc.player.getY();
            aimStartTime = System.currentTimeMillis();
        }

        long now = System.currentTimeMillis();
        Vec3 eye = mc.player.getEyePosition(1.0f);
        boolean onHitbox = entity != null && isAimOnHitbox(new Rotation(currentYaw, currentPitch), entity);

        boolean inJump = isInJump();
        if (mc.player.onGround()) {
            lastGroundedPlayerY = mc.player.getY();
        }
        if (inJump && !wasInJump) {
            jumpOffsetVariation = MathUtil.randomInRange(-1.5f, 1.5f);
        }
        wasInJump = inJump;

        if (entity != null) {
            if (entity.getId() != trackedEntityId) {
                trackedEntityId = entity.getId();
                firstSeenTime = now;
                float angleDiff = measureAngleDiff(currentRotation, entity, eye);
                reactionMs = computeReactionTime(angleDiff);
                reactionComplete = false;

                aimStartTime = now;
                aimFatigue = 0.0f;
                velocityYaw = 0.0f;
                velocityPitch = 0.0f;
                wanderOffset = Vec3.ZERO;
                wanderVel = Vec3.ZERO;
            }
        }

        double sensitivity = Minecraft.getInstance().options.sensitivity().get();
        float sensFactor = (float) (sensitivity * 0.6 + 0.2);
        float gcd = MouseUtil.getGCD();

        float reactionMultiplier = 1.0f;
        if (!reactionComplete && entity != null) {
            long elapsed = now - firstSeenTime;
            if (elapsed < reactionMs) {
                float t = (float) elapsed / Math.max(1L, reactionMs);
                reactionMultiplier = 0.40f + 0.60f * (t * t);
            } else {
                reactionComplete = true;
            }
        }

        float targetYaw;
        float targetPitch;
        double distanceToTarget = entity != null ? eye.distanceTo(entity.getBoundingBox().getCenter()) : 3.0;

        if (entity != null) {
            updateWanderPoint(entity);

            Vec3 targetVelocity = entity.getDeltaMovement();
            float leadFactor = (float) Mth.clamp(distanceToTarget / 24.0, 0.04, 0.15);
            Vec3 leadTargetPos = entity.position().add(targetVelocity.scale(leadFactor * 20.0));

            Vec3 aimPoint = leadTargetPos.add(wanderOffset.x, wanderOffset.y, wanderOffset.z);

            if (!hasLineOfSight(eye, aimPoint)) {
                AABB bb = entity.getBoundingBox();
                aimPoint = new Vec3(bb.getCenter().x, bb.minY + bb.getYsize() * 0.35, bb.getCenter().z);
            }

            Rotation calculatedRot = RotationUtil.fromVec3(aimPoint.subtract(eye));
            targetYaw = calculatedRot.getYRot();
            targetPitch = calculatedRot.getXRot();

            if (inJump) {
                double jumpElevation = mc.player.getY() - lastGroundedPlayerY;
                if (jumpElevation > 0.02) {
                    double horizDist = Math.max(0.5, Math.hypot(aimPoint.x - eye.x, aimPoint.z - eye.z));
                    float jumpAngleComp = (float) -Math.toDegrees(Math.atan2(jumpElevation, horizDist));
                    targetPitch += (jumpAngleComp + jumpOffsetVariation);
                }
            }
        } else {
            targetYaw = targetRotation.getYRot();
            targetPitch = targetRotation.getXRot();
        }

        float diffYaw = Mth.wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;
        float totalDiff = (float) Math.hypot(diffYaw, diffPitch);

        if (speedRollTicks-- <= 0) {
            randomSpeedSpread = MathUtil.randomInRange(0.92f, 1.15f);
            speedRollTicks = (int) MathUtil.randomInRange(12f, 24f);
        }

        float approachMultiplier = calculateApproachMultiplier(entity, distanceToTarget);

        float attackMultiplier = calculateAttackMultiplier(now);

        float fightingSeconds = (now - aimStartTime) / 1000.0f;
        aimFatigue = Mth.clamp(fightingSeconds / 240.0f, 0.0f, 0.25f);
        if (onHitbox) aimFatigue *= 0.98f;

        float baseSpeed = Math.max(2.4f, sensFactor * 4.6f);
        float overallSpeedMultiplier = baseSpeed
                * randomSpeedSpread
                * approachMultiplier
                * attackMultiplier
                * reactionMultiplier
                * (1.0f - aimFatigue * 0.20f);

        float baseStiffness = !onHitbox ? 0.32f : 0.18f;
        float stiffness = (baseStiffness + RNG.nextFloat() * 0.05f) * (overallSpeedMultiplier / 2.2f);
        if (totalDiff > 30.0f) {
            stiffness += 0.10f;
        } else if (totalDiff < 4.0f) {
            stiffness *= 0.60f;
        }
        stiffness = Mth.clamp(stiffness, 0.10f, 0.55f);

        float damping = 0.50f + (0.10f * (1.0f - Mth.clamp(overallSpeedMultiplier / 3.0f, 0.0f, 1.0f)));

        velocityYaw = springInterp(currentYaw, currentYaw + diffYaw, velocityYaw, stiffness, damping);
        velocityPitch = springInterp(currentPitch, currentPitch + diffPitch, velocityPitch, stiffness * 0.88f, damping);

        float maxVelYaw = (!onHitbox ? Math.max(28.0f, Math.min(65.0f, totalDiff * 1.6f)) : Math.max(12.0f, Math.min(26.0f, totalDiff * 1.1f))) * (overallSpeedMultiplier / 2.2f);
        float maxVelPitch = maxVelYaw * 0.72f;

        velocityYaw = smoothClamp(velocityYaw, maxVelYaw);
        velocityPitch = smoothClamp(velocityPitch, maxVelPitch);

        currentYaw += velocityYaw;
        currentPitch += velocityPitch;
        currentPitch = Mth.clamp(currentPitch, -89.0f, 89.0f);

        float smoothFactor = !onHitbox ? 0.94f : 0.90f;
        smoothYaw = smoothLerp(smoothYaw, currentYaw, smoothFactor);
        smoothPitch = smoothLerp(smoothPitch, currentPitch, smoothFactor * 0.95f);

        float[] noise = generateHarmonicNoise((float) distanceToTarget, onHitbox);

        float rawOutYaw = smoothYaw + noise[0];
        float rawOutPitch = Mth.clamp(smoothPitch + noise[1], -89.0f, 89.0f);

        float finalYaw = currentYaw + Math.round((rawOutYaw - currentYaw) / gcd) * gcd;
        float finalPitch = currentPitch + Math.round((rawOutPitch - currentPitch) / gcd) * gcd;
        finalPitch = Mth.clamp(finalPitch, -89.0f, 89.0f);

        currentYaw = finalYaw;
        currentPitch = finalPitch;
        return new Rotation(finalYaw, finalPitch);
    }

    private static float springInterp(float current, float target, float vel, float stiffness, float damping) {
        float diff = target - current;
        float acc = diff * stiffness - vel * damping;
        return vel + acc;
    }

    private static float calculateApproachMultiplier(Entity target, double distance) {
        if (mc.player == null || target == null) return 1.0f;

        Vec3 playerPos = mc.player.position();
        Vec3 targetPos = target.position();
        Vec3 playerVel = mc.player.getDeltaMovement();
        Vec3 targetVel = target.getDeltaMovement();

        Vec3 toTarget = targetPos.subtract(playerPos);
        double distSq = toTarget.lengthSqr();
        if (distSq < 1.0e-5) return 0.52f;

        Vec3 toTargetNorm = toTarget.normalize();
        double playerSpeedToTarget = playerVel.dot(toTargetNorm);
        double targetSpeedToPlayer = targetVel.dot(toTargetNorm.scale(-1));
        double relativeClosingSpeed = playerSpeedToTarget + targetSpeedToPlayer;

        boolean movingForward = mc.player.zza > 0.05f || (mc.options.keyUp.isDown());
        boolean overtaking = relativeClosingSpeed > 0.06 && distance < 3.8;
        boolean inCloseRange = distance < 2.8;

        float mult = 1.0f;
        if (overtaking || (movingForward && inCloseRange)) {
            float intensity = (float) Mth.clamp((3.8 - distance) / 2.5, 0.0, 1.0);
            float baseSlow = 0.68f + RNG.nextFloat() * 0.10f;
            mult = Mth.lerp(intensity, 1.0f, baseSlow);
        } else if (inCloseRange) {
            mult = 0.88f + RNG.nextFloat() * 0.06f;
        }

        boolean playerMoving = playerVel.horizontalDistanceSqr() > 0.003;
        boolean targetMoving = targetVel.horizontalDistanceSqr() > 0.003;
        if (playerMoving && targetMoving) {
            mult *= 0.92f;
        }

        return Mth.clamp(mult, 0.60f, 1.0f);
    }

    private static float calculateAttackMultiplier(long now) {
        long sinceAttack = now - lastAttackTime;
        if (sinceAttack < 0L || sinceAttack >= attackRecoveryMs) return 1.0f;

        float t = (float) sinceAttack / attackRecoveryMs;
        float curved = 1.0f - (float) Math.pow(1.0f - t, 2.4);
        return attackSlowdown + (1.0f - attackSlowdown) * curved;
    }

    private static float[] generateHarmonicNoise(float dist, boolean onHitbox) {
        noiseAngle += 0.045f + (float) (Math.random() * 0.020f);

        float distScale = Mth.clamp(dist / 4.5f, 0.20f, 1.0f);
        float amp = NOISE_AMPLITUDE * distScale * (onHitbox ? 0.35f : 0.85f);

        float n1 = (float) Math.sin(noiseAngle * 0.87) * 0.38f;
        float n2 = (float) Math.sin(noiseAngle * 1.43 + 0.75) * 0.28f;
        float n3 = (float) Math.cos(noiseAngle * 1.18 + 0.35) * 0.32f;
        float n4 = (float) Math.cos(noiseAngle * 1.76 + 1.42) * 0.23f;

        float yawNoise = (n1 + n2) * amp;
        float pitchNoise = (n3 + n4) * amp * 0.55f;

        yawNoise += ((float) Math.random() - 0.5f) * amp * 0.12f;
        pitchNoise += ((float) Math.random() - 0.5f) * amp * 0.08f;

        return new float[]{yawNoise, pitchNoise};
    }

    private static int computeReactionTime(float angleDiff) {
        if (angleDiff > 90.0f) return 40 + RNG.nextInt(35);
        if (angleDiff > 45.0f) return 25 + RNG.nextInt(25);
        return 10 + RNG.nextInt(15);
    }

    private static float measureAngleDiff(Rotation currentRot, Entity entity, Vec3 eye) {
        Vec3 mid = entity.getBoundingBox().getCenter();
        Vec3 delta = mid.subtract(eye);
        float needYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float needPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float dYaw = Math.abs(Mth.wrapDegrees(needYaw - currentRot.getYRot()));
        float dPitch = Math.abs(needPitch - currentRot.getXRot());
        return dYaw + dPitch;
    }

    private static void updateWanderPoint(Entity entity) {
        AABB box = entity.getBoundingBox();
        double lenX = box.getXsize();
        double lenY = box.getYsize();
        double lenZ = box.getZsize();

        if (wanderVel.lengthSqr() < 1.0e-6) {
            wanderVel = new Vec3(
                    MathUtil.randomInRange(-0.012f, 0.012f),
                    MathUtil.randomInRange(-0.008f, 0.008f),
                    MathUtil.randomInRange(-0.012f, 0.012f)
            );
        }
        if (wanderOffset.lengthSqr() < 1.0e-6) {
            wanderOffset = new Vec3(0.0, lenY * 0.40, 0.0);
        }

        wanderOffset = wanderOffset.add(wanderVel);

        double xLim = Math.max(0.02, (lenX - 0.12) / 2.0);
        double zLim = Math.max(0.02, (lenZ - 0.12) / 2.0);

        double minVx = 0.003, maxVx = 0.011;
        double minVy = 0.002, maxVy = 0.008;

        if (wanderOffset.x >= xLim) {
            wanderVel = new Vec3(-MathUtil.randomInRange((float) minVx, (float) maxVx), wanderVel.y, wanderVel.z);
        } else if (wanderOffset.x <= -xLim) {
            wanderVel = new Vec3(MathUtil.randomInRange((float) minVx, (float) maxVx), wanderVel.y, wanderVel.z);
        }

        if (wanderOffset.y >= lenY * 0.65) {
            wanderVel = new Vec3(wanderVel.x, -MathUtil.randomInRange((float) minVy, (float) maxVy), wanderVel.z);
        } else if (wanderOffset.y <= lenY * 0.22) {
            wanderVel = new Vec3(wanderVel.x, MathUtil.randomInRange((float) minVy, (float) maxVy), wanderVel.z);
        }

        if (wanderOffset.z >= zLim) {
            wanderVel = new Vec3(wanderVel.x, wanderVel.y, -MathUtil.randomInRange((float) minVx, (float) maxVx));
        } else if (wanderOffset.z <= -zLim) {
            wanderVel = new Vec3(wanderVel.x, wanderVel.y, MathUtil.randomInRange((float) minVx, (float) maxVx));
        }
    }

    private static float smoothLerp(float from, float to, float alpha) {
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        float delta = Mth.wrapDegrees(to - from);
        return from + delta * alpha;
    }

    private static float smoothClamp(float value, float maxStep) {
        if (maxStep <= 0.0f) return 0.0f;
        float sign = Math.signum(value);
        float abs = Math.abs(value);
        float outer = maxStep * (0.86f + RNG.nextFloat() * 0.28f);
        float inner = outer * (0.50f + RNG.nextFloat() * 0.15f);
        if (abs <= inner) return value;
        float over = abs - inner;
        float range = Math.max(0.001f, outer - inner);
        float mid = range * (over / (over + range));
        float extra = Math.min(over * 0.1f, range * 0.8f);
        return sign * (inner + mid + extra);
    }

    private static boolean hasLineOfSight(Vec3 eye, Vec3 point) {
        HitResult result = mc.level.clip(new ClipContext(
                eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private static boolean isAimOnHitbox(Rotation rotation, Entity entity) {
        if (entity == null || rotation == null) return true;
        float distance = AuraModule.getInstance().getAttackDistance()
                + AuraModule.getInstance().getPreDistance();
        return RaytracingUtil.rayTraceBox(rotation, distance, entity.getBoundingBox());
    }
}
