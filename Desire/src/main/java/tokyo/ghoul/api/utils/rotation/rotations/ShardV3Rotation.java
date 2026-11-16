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

public class ShardV3Rotation extends RotationMode {
    private static final Random RNG = new Random();
    private static final float UPDATES_PER_SECOND = 60f;

    private static final float BASE_SENS_SCALE = 3.6f;
    private static final float MIN_AIM_SPEED = 1.8f;
    private static final float SPEED_SMOOTHING = 0.12f;

    private static final float ATTACK_SLOWDOWN_MIN = 0.48f;
    private static final float ATTACK_SLOWDOWN_MAX = 0.72f;
    private static final float ATTACK_DURATION_MIN_MS = 220f;
    private static final float ATTACK_DURATION_MAX_MS = 360f;

    private static float prevYaw;
    private static float prevPitch;
    private static boolean initialized;

    private static float velYaw;
    private static float velPitch;

    private static float speedOsc = 0.0f;
    private static boolean accelerating = false;
    private static float accelPeak = 0.22f;
    private static float decelFloor = -0.02f;
    private static float noiseSm = 0.0f;
    private static int phaseTickCounter = 0;

    private static float speedMultiplierTarget = 1.0f;
    private static float speedMultiplierCurrent = 1.0f;
    private static int speedRollCooldown;

    private static long lastAttackMs = Long.MIN_VALUE;
    private static boolean lastAttackWasJumpClose;
    private static float attackSlowdown = 0.60f;
    private static float attackDurationMs = 280f;

    private static int trackedEntityId = Integer.MIN_VALUE;
    private static int reactionDelayTicks = 0;
    private static double lastGroundedPlayerY;
    private static float jumpPitchOffsetVariation = 0.0f;
    private static boolean wasInJump = false;

    private static Vec3 wanderOffset = Vec3.ZERO;
    private static Vec3 wanderVel = Vec3.ZERO;

    private static final float LOWER_PITCH_BIAS = 6.4f;
    private static float currentPitchBias = 6.4f;
    private static float targetPitchBias = 6.4f;
    private static int pitchBiasCooldown = 0;

    public ShardV3Rotation() {
        super("ShardV3");
    }

    public static void reset() {
        initialized = false;
        trackedEntityId = Integer.MIN_VALUE;
        lastAttackMs = Long.MIN_VALUE;
        lastAttackWasJumpClose = false;

        velYaw = 0.0f;
        velPitch = 0.0f;
        reactionDelayTicks = 0;

        speedOsc = 0.0f;
        accelerating = false;
        noiseSm = 0.0f;
        phaseTickCounter = 0;
        accelPeak = 0.22f;
        decelFloor = -0.02f;

        speedMultiplierTarget = speedMultiplierCurrent = 1.0f;
        speedRollCooldown = 0;

        wanderOffset = Vec3.ZERO;
        wanderVel = Vec3.ZERO;

        currentPitchBias = LOWER_PITCH_BIAS;
        targetPitchBias = LOWER_PITCH_BIAS;
        pitchBiasCooldown = 0;

        jumpPitchOffsetVariation = 0.0f;
        wasInJump = false;
        lastGroundedPlayerY = mc.player != null ? mc.player.getY() : 0.0;
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

    public static boolean isCloseToTarget(Entity entity) {
        if (mc.player == null || entity == null) return false;
        return mc.player.distanceToSqr(entity) <= (2.4 * 2.4);
    }

    public static void onAttack() {
        lastAttackMs = System.currentTimeMillis();
        Entity currentTarget = AuraModule.getInstance().target;
        lastAttackWasJumpClose = isInJump() && isCloseToTarget(currentTarget);

        attackSlowdown = MathUtil.randomInRange(ATTACK_SLOWDOWN_MIN, ATTACK_SLOWDOWN_MAX);
        attackDurationMs = MathUtil.randomInRange(ATTACK_DURATION_MIN_MS, ATTACK_DURATION_MAX_MS);

        accelerating = true;
        speedOsc = decelFloor;
        velYaw *= 0.50f;
        velPitch *= 0.50f;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (targetRotation == null) return currentRotation;
        if (!initialized || mc.player == null) {
            prevYaw = currentRotation.getYRot();
            prevPitch = currentRotation.getXRot();
            initialized = true;
            if (mc.player != null) {
                lastGroundedPlayerY = mc.player.getY();
            }
        }

        Vec3 eye = mc.player.getEyePosition(1.0f);
        boolean onHitbox = entity != null && isAimOnHitbox(new Rotation(prevYaw, prevPitch), entity);

        boolean inJump = isInJump();
        if (mc.player.onGround()) {
            lastGroundedPlayerY = mc.player.getY();
        }
        if (inJump && !wasInJump) {
            jumpPitchOffsetVariation = MathUtil.randomInRange(-1.6f, 1.6f);
        }
        wasInJump = inJump;

        if (entity != null) {
            if (entity.getId() != trackedEntityId) {
                trackedEntityId = entity.getId();
                reactionDelayTicks = 2 + RNG.nextInt(2);
                wanderOffset = Vec3.ZERO;
                wanderVel = Vec3.ZERO;
                velYaw = 0.0f;
                velPitch = 0.0f;
            }
        }

        float targetYaw;
        float targetPitch;
        double distanceToTarget = entity != null ? eye.distanceTo(entity.getBoundingBox().getCenter()) : 3.0;

        if (entity != null) {
            updateWanderOffset(entity);

            Vec3 targetVel = entity.getDeltaMovement();
            float leadFactor = (float) Mth.clamp(distanceToTarget / 25.0, 0.05, 0.16);
            Vec3 leadTargetPos = entity.position().add(targetVel.scale(leadFactor * 20.0));

            Vec3 aimPoint = leadTargetPos.add(wanderOffset.x, wanderOffset.y, wanderOffset.z);

            if (!hasLineOfSight(eye, aimPoint)) {
                AABB bb = entity.getBoundingBox();
                aimPoint = new Vec3(bb.getCenter().x, bb.minY + bb.getYsize() * 0.12, bb.getCenter().z);
            }

            Rotation calculatedRot = RotationUtil.fromVec3(aimPoint.subtract(eye));
            targetYaw = calculatedRot.getYRot();
            targetPitch = calculatedRot.getXRot();

            if (pitchBiasCooldown-- <= 0) {
                targetPitchBias = LOWER_PITCH_BIAS + MathUtil.randomInRange(-1.8f, 2.0f);
                pitchBiasCooldown = (int) MathUtil.randomInRange(16f, 32f);
            }
            currentPitchBias += (targetPitchBias - currentPitchBias) * 0.08f;
            float distScale = (float) Mth.clamp(1.0 - (distanceToTarget - 3.0) / 4.0, 0.3, 1.2);
            targetPitch += currentPitchBias * distScale;

            if (inJump) {
                double jumpElevation = mc.player.getY() - lastGroundedPlayerY;
                if (jumpElevation > 0.02) {
                    double horizDist = Math.max(0.6, Math.hypot(aimPoint.x - eye.x, aimPoint.z - eye.z));
                    float jumpAngleComp = (float) -Math.toDegrees(Math.atan2(jumpElevation, horizDist));
                    targetPitch += (jumpAngleComp + jumpPitchOffsetVariation);
                }
            }

            if (onHitbox) {
                targetYaw += postAttackLeadYaw();
            }
        } else {
            targetYaw = targetRotation.getYRot();
            targetPitch = targetRotation.getXRot();
        }

        float yawDelta = Mth.wrapDegrees(targetYaw - prevYaw);
        float pitchDelta = targetPitch - prevPitch;
        float yawDistance = Math.abs(yawDelta);
        float pitchDistance = Math.abs(pitchDelta);
        float angularDist = (float) Math.hypot(yawDelta, pitchDelta);

        if (speedRollCooldown-- <= 0) {
            float spread = MathUtil.randomInRange(0.10f, 0.28f);
            speedMultiplierTarget = MathUtil.randomInRange(1.0f - spread, 1.0f + spread);
            speedRollCooldown = (int) MathUtil.randomInRange(12f, 26f);
        }
        speedMultiplierCurrent += (speedMultiplierTarget - speedMultiplierCurrent) * SPEED_SMOOTHING;

        boolean attackReady = mc.player.getAttackStrengthScale(0.5f) > 0.85f;
        float phaseMultiplier = updatePhaseSpeed(angularDist, attackReady);

        float approachMultiplier = calculateApproachMultiplier(entity, distanceToTarget);

        float attackMultiplier = postAttackSpeedScale();

        float reactionMultiplier = 1.0f;
        if (reactionDelayTicks > 0) {
            reactionDelayTicks--;
            reactionMultiplier = 0.35f + RNG.nextFloat() * 0.15f;
        }

        double sensitivity = Minecraft.getInstance().options.sensitivity().get();
        float sensFactor = (float) (sensitivity * 0.6 + 0.2);
        float baseSpeed = Math.max(MIN_AIM_SPEED, sensFactor * BASE_SENS_SCALE);

        float totalSpeed = baseSpeed
                * speedMultiplierCurrent
                * phaseMultiplier
                * approachMultiplier
                * attackMultiplier
                * reactionMultiplier;

        float maxYawStep;
        float maxPitchStep;
        float moveFactor;

        if (!onHitbox) {
            float distNorm = Math.min(1.0f, angularDist / 35.0f);
            float ease = (float) Math.sin(distNorm * Math.PI * 0.5);

            maxYawStep = Math.max(14.0f, Math.min(42.0f, angularDist * 1.6f)) * totalSpeed * 0.32f;
            maxPitchStep = Math.max(9.0f, Math.min(26.0f, pitchDistance * 1.4f)) * totalSpeed * 0.28f;

            moveFactor = Math.max(0.18f, Math.min(0.85f, totalSpeed * 0.22f * ease));
        } else {
            maxYawStep = Math.max(6.0f, Math.min(20.0f, yawDistance * 1.2f)) * totalSpeed * 0.24f;
            maxPitchStep = Math.max(4.0f, Math.min(14.0f, pitchDistance * 1.1f)) * totalSpeed * 0.20f;

            moveFactor = Math.max(0.12f, Math.min(0.60f, totalSpeed * 0.16f));
        }

        float wantVelYaw = yawDelta * moveFactor;
        float wantVelPitch = pitchDelta * moveFactor;

        float inertia = onHitbox ? 0.65f : 0.58f;
        velYaw = velYaw * inertia + wantVelYaw * (1.0f - inertia);
        velPitch = velPitch * inertia + wantVelPitch * (1.0f - inertia);

        float noiseScale = onHitbox ? 0.008f : 0.016f;
        velYaw += (float) RNG.nextGaussian() * noiseScale;
        velPitch += (float) RNG.nextGaussian() * noiseScale * 0.7f;

        if (onHitbox && yawDistance < 4.0f && pitchDistance < 3.5f) {
            velYaw *= 0.45f;
            velPitch *= 0.45f;
        }

        velYaw = smoothClamp(velYaw, maxYawStep);
        velPitch = smoothClamp(velPitch, maxPitchStep);

        float gcd = MouseUtil.getGCD();
        float rawYaw = prevYaw + velYaw;
        float rawPitch = Mth.clamp(prevPitch + velPitch, -89.0F, 90.0F);

        float finalYaw = prevYaw + Math.round((rawYaw - prevYaw) / gcd) * gcd;
        float finalPitch = prevPitch + Math.round((rawPitch - prevPitch) / gcd) * gcd;
        finalPitch = Mth.clamp(finalPitch, -89.0F, 90.0F);

        prevYaw = finalYaw;
        prevPitch = finalPitch;
        return new Rotation(finalYaw, finalPitch);
    }

    private static float calculateApproachMultiplier(Entity target, double distance) {
        if (mc.player == null || target == null) return 1.0f;

        Vec3 playerPos = mc.player.position();
        Vec3 targetPos = target.position();

        Vec3 playerVel = mc.player.getDeltaMovement();
        Vec3 targetVel = target.getDeltaMovement();

        Vec3 toTarget = targetPos.subtract(playerPos);
        double distSq = toTarget.lengthSqr();
        if (distSq < 1.0e-5) return 0.58f;

        Vec3 toTargetNorm = toTarget.normalize();
        double playerSpeedToTarget = playerVel.dot(toTargetNorm);
        double targetSpeedToPlayer = targetVel.dot(toTargetNorm.scale(-1));
        double relativeClosingSpeed = playerSpeedToTarget + targetSpeedToPlayer;

        boolean movingForward = mc.player.zza > 0.05f || (mc.options.keyUp.isDown());
        boolean overtaking = relativeClosingSpeed > 0.04 && distance < 4.2;
        boolean inCloseRange = distance < 3.2;

        float mult = 1.0f;
        if (overtaking || (movingForward && inCloseRange)) {
            float intensity = (float) Mth.clamp((4.2 - distance) / 2.8, 0.0, 1.0);
            float baseSlow = 0.52f + RNG.nextFloat() * 0.10f;
            mult = Mth.lerp(intensity, 1.0f, baseSlow);
        } else if (inCloseRange) {
            mult = 0.80f + RNG.nextFloat() * 0.08f;
        }

        boolean playerMoving = playerVel.horizontalDistanceSqr() > 0.003;
        boolean targetMoving = targetVel.horizontalDistanceSqr() > 0.003;
        if (playerMoving && targetMoving) {
            mult *= 0.88f;
        }

        return Mth.clamp(mult, 0.45f, 1.0f);
    }

    private static float updatePhaseSpeed(float angularDistance, boolean attackReady) {
        phaseTickCounter++;
        noiseSm = noiseSm * 0.82f + (float) RNG.nextGaussian() * 0.04f;

        if (!accelerating) {
            float gain = 0.0055f;
            gain += (0.004f + 0.012f * (1.0f - (float) Math.exp(-angularDistance / 35.0f))) * 1.8f;
            gain += RNG.nextFloat() * 0.0028f;
            if (attackReady) {
                gain += 0.013f;
            }
            speedOsc += gain * (1.5f + noiseSm);
            if (speedOsc >= accelPeak) {
                accelerating = true;
                decelFloor = -0.02f - RNG.nextFloat() * 0.04f;
            }
        } else {
            float drop = (attackReady ? 0.042f : 0.012f) * (0.85f + RNG.nextFloat() * 0.3f);
            speedOsc -= drop * (2.0f + noiseSm);
            if (speedOsc <= decelFloor) {
                accelerating = false;
                accelPeak = 0.20f + RNG.nextFloat() * 0.12f;
            }
        }

        return Mth.clamp(0.65f + speedOsc * 2.0f, 0.40f, 1.35f);
    }

    private static void updateWanderOffset(Entity entity) {
        AABB box = entity.getBoundingBox();
        double lenX = box.getXsize();
        double lenY = box.getYsize();
        double lenZ = box.getZsize();

        if (wanderVel.lengthSqr() < 1.0e-6) {
            wanderVel = new Vec3(
                    MathUtil.randomInRange(-0.014f, 0.014f),
                    MathUtil.randomInRange(-0.010f, 0.010f),
                    MathUtil.randomInRange(-0.014f, 0.014f)
            );
        }
        if (wanderOffset.lengthSqr() < 1.0e-6) {
            wanderOffset = new Vec3(0.0, lenY * 0.12, 0.0);
        }

        wanderOffset = wanderOffset.add(wanderVel);

        double xLim = Math.max(0.02, (lenX - 0.12) / 2.0);
        double zLim = Math.max(0.02, (lenZ - 0.12) / 2.0);

        double minVx = 0.003, maxVx = 0.012;
        double minVy = 0.002, maxVy = 0.008;

        if (wanderOffset.x >= xLim) {
            wanderVel = new Vec3(-MathUtil.randomInRange((float) minVx, (float) maxVx), wanderVel.y, wanderVel.z);
        } else if (wanderOffset.x <= -xLim) {
            wanderVel = new Vec3(MathUtil.randomInRange((float) minVx, (float) maxVx), wanderVel.y, wanderVel.z);
        }

        if (wanderOffset.y >= lenY * 0.18) {
            wanderVel = new Vec3(wanderVel.x, -MathUtil.randomInRange((float) minVy, (float) maxVy), wanderVel.z);
        } else if (wanderOffset.y <= lenY * 0.07) {
            wanderVel = new Vec3(wanderVel.x, MathUtil.randomInRange((float) minVy, (float) maxVy), wanderVel.z);
        }

        if (wanderOffset.z >= zLim) {
            wanderVel = new Vec3(wanderVel.x, wanderVel.y, -MathUtil.randomInRange((float) minVx, (float) maxVx));
        } else if (wanderOffset.z <= -zLim) {
            wanderVel = new Vec3(wanderVel.x, wanderVel.y, MathUtil.randomInRange((float) minVx, (float) maxVx));
        }
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

    private static float postAttackLeadYaw() {
        long sinceAttack = System.currentTimeMillis() - lastAttackMs;
        float leadDuration = 320f;
        if (sinceAttack < 0L || sinceAttack > leadDuration) {
            return 0f;
        }

        float t = sinceAttack / leadDuration;
        float curve = (float) Math.sin(Math.PI * Math.pow(t, 0.75));

        float baseRight = 1.8f;
        float strafe = mc.player != null ? mc.player.xxa : 0f;
        float forward = mc.player != null ? mc.player.zza : 0f;

        float moveBonus = 0f;
        if (strafe < -0.05f) {
            moveBonus += 1.2f;
        } else if (strafe > 0.05f) {
            moveBonus -= 0.6f;
        }
        if (forward > 0.05f) {
            moveBonus += 0.35f;
        }

        return (baseRight + moveBonus) * curve;
    }

    private static float postAttackSpeedScale() {
        long sinceAttack = System.currentTimeMillis() - lastAttackMs;
        if (sinceAttack < 0L || sinceAttack >= attackDurationMs) return 1f;

        float t = (float) sinceAttack / attackDurationMs;
        float eased = 1f - (float) Math.pow(1f - t, 2.5);
        return attackSlowdown + (1f - attackSlowdown) * eased;
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
