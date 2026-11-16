package tokyo.ghoul.api.utils.rotation.misc;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.math.MouseUtil;

import java.util.Random;

public final class LegitAim implements QuickImports {
    private static final float YIELD_TICKS = 5.0F;
    private static final float YIELD_STRENGTH = 0.3F;

    private final Random random = new Random();
    private final HumanAimPattern humanPattern = new HumanAimPattern();

    private LivingEntity trackedTarget;
    private Vec3 pointStartOffset = Vec3.ZERO;
    private Vec3 pointControlOffset = Vec3.ZERO;
    private Vec3 currentPointOffset = Vec3.ZERO;
    private Vec3 targetPointOffset = Vec3.ZERO;
    private float pointTravelTick;
    private float pointTravelDuration;
    private float yawVelocity;
    private float pitchVelocity;
    private float yawMouseRemainder;
    private float pitchMouseRemainder;
    private float cadencePhase;
    private float cadenceMultiplier = 1.0F;
    private float targetCadenceMultiplier = 1.0F;
    private float cadenceTicks;
    private float yieldLeft;
    private float speedFactor = 1.0F;
    private float targetSpeedFactor = 1.0F;
    private float speedTicks;
    private float lastSetYaw;
    private float lastSetPitch;
    private boolean hasLastSet;
    private long lastFrameNs;

    public void update(LivingEntity target, float speed) {
        long now = System.nanoTime();
        float frameDt = lastFrameNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastFrameNs) / 1e9f);
        lastFrameNs = now;

        if (target == null || mc.player == null) {
            reset();
            return;
        }
        float frameScale = frameDt * 20.0F;

        if (hasLastSet) {
            float userYaw = Mth.wrapDegrees(mc.player.getYRot() - lastSetYaw);
            float userPitch = mc.player.getXRot() - lastSetPitch;
            if (Math.hypot(userYaw, userPitch) > Math.max(0.35F, MouseUtil.getGCD() * 1.5F)) {
                yieldLeft = YIELD_TICKS;
                resetMotion();
            }
        }
        yieldLeft = Math.max(0.0F, yieldLeft - frameScale);

        if (target != humanTargetRef) {
            humanTargetRef = target;
            humanPattern.onTargetChanged();
        }
        if (humanPattern.isReacting(frameScale)) {
            resetMotion();
            remember();
            return;
        }

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetPos = getSmoothMultipoint(target, frameScale);
        float[] targetRot = rotationsTo(targetPos.subtract(eyePos));

        float deltaYaw = Mth.wrapDegrees(targetRot[0] - mc.player.getYRot());
        float deltaPitch = targetRot[1] - mc.player.getXRot();

        float angularError = (float) Math.hypot(deltaYaw, deltaPitch);

        deltaYaw = Mth.wrapDegrees(deltaYaw + humanPattern.yawBias(deltaYaw, angularError));
        angularError = (float) Math.hypot(deltaYaw, deltaPitch);
        float humanSpeed = humanPattern.speedScale(frameScale);
        float[] humanJitter = humanPattern.jitter(angularError, frameScale);

        float maxStep = Math.max(0.2F, speed * updateSpeedFactor(frameScale) * 0.4F);
        float assistStrength = yieldLeft > 0.0F ? YIELD_STRENGTH : 1.0F;
        float yawStep = nonLinearAxisStep(deltaYaw, angularError, maxStep, 0.82F) * assistStrength;
        float pitchStep = nonLinearAxisStep(deltaPitch, angularError, maxStep * 0.72F, 0.94F) * assistStrength;

        yawStep *= frameScale;
        pitchStep *= frameScale;

        updateCadence(speed, frameScale);
        float pulse = 0.965F + (float) Math.sin(cadencePhase) * 0.035F;
        yawStep *= cadenceMultiplier * pulse;
        pitchStep *= cadenceMultiplier * (2.0F - pulse) * 0.94F;

        yawStep = yawStep * humanSpeed + humanJitter[0];
        pitchStep = pitchStep * humanSpeed * 0.85F + humanJitter[1];

        float errorRatio = Mth.clamp(angularError / 45.0F, 0.0F, 1.0F);
        float accelerationCurve = 0.18F * (0.62F + 0.58F * (float) Math.pow(errorRatio, 0.63));
        float yawShare = angularError > 0.0001F ? Math.abs(deltaYaw) / angularError : 0.0F;
        float pitchShare = angularError > 0.0001F ? Math.abs(deltaPitch) / angularError : 0.0F;
        float yawAcceleration = (float) Math.pow(
                Mth.clamp(accelerationCurve * (0.91F + yawShare * 0.17F), 0.05F, 0.78F), frameScale);
        float pitchAcceleration = (float) Math.pow(
                Mth.clamp(accelerationCurve * (0.84F + pitchShare * 0.14F), 0.04F, 0.70F), frameScale);
        yawVelocity += (yawStep - yawVelocity) * yawAcceleration;
        pitchVelocity += (pitchStep - pitchVelocity) * pitchAcceleration;

        if (Math.signum(yawVelocity) != Math.signum(deltaYaw)) yawVelocity = 0.0F;
        if (Math.signum(pitchVelocity) != Math.signum(deltaPitch)) pitchVelocity = 0.0F;

        yawVelocity = Mth.clamp(yawVelocity, -Math.abs(deltaYaw), Math.abs(deltaYaw));
        pitchVelocity = Mth.clamp(pitchVelocity, -Math.abs(deltaPitch), Math.abs(deltaPitch));

        float gcd = MouseUtil.getGCD();
        float quantizedYaw = quantizeMouseStep(yawVelocity, gcd, true, deltaYaw);
        float quantizedPitch = quantizeMouseStep(pitchVelocity, gcd, false, deltaPitch);

        if (Math.abs(quantizedYaw) > 0.0001F || Math.abs(quantizedPitch) > 0.0001F) {
            float previousYaw = mc.player.getYRot();
            float previousPitch = mc.player.getXRot();
            float requestedYaw = previousYaw + quantizedYaw;
            float requestedPitch = Mth.clamp(previousPitch + quantizedPitch, -90.0F, 90.0F);

            float finalYaw = previousYaw + Math.round((requestedYaw - previousYaw) / gcd) * gcd;
            float finalPitch = Mth.clamp(previousPitch + Math.round((requestedPitch - previousPitch) / gcd) * gcd, -90.0F, 90.0F);

            mc.player.setYRot(finalYaw);
            mc.player.setXRot(finalPitch);
            mc.player.setYHeadRot(finalYaw);
        }
        remember();
    }

    private LivingEntity humanTargetRef;

    public void reset() {
        trackedTarget = null;
        humanTargetRef = null;
        humanPattern.reset();
        pointStartOffset = Vec3.ZERO;
        pointControlOffset = Vec3.ZERO;
        currentPointOffset = Vec3.ZERO;
        targetPointOffset = Vec3.ZERO;
        pointTravelTick = 0;
        pointTravelDuration = 0;
        cadencePhase = 0.0F;
        cadenceMultiplier = 1.0F;
        targetCadenceMultiplier = 1.0F;
        cadenceTicks = 0;
        yieldLeft = 0.0F;
        speedFactor = 1.0F;
        targetSpeedFactor = 1.0F;
        speedTicks = 0.0F;
        hasLastSet = false;
        resetMotion();
    }

    public void disable() {
        reset();
        lastFrameNs = 0L;
    }

    private void remember() {
        if (mc.player == null) return;
        lastSetYaw = mc.player.getYRot();
        lastSetPitch = mc.player.getXRot();
        hasLastSet = true;
    }

    private void resetMotion() {
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        yawMouseRemainder = 0.0F;
        pitchMouseRemainder = 0.0F;
    }

    private float nonLinearAxisStep(float delta, float totalError, float maxStep, float axisExponent) {
        float absoluteDelta = Math.abs(delta);
        if (absoluteDelta < 0.0001F || totalError < 0.0001F) return 0.0F;

        float axisShare = Mth.clamp(absoluteDelta / totalError, 0.0F, 1.0F);
        float directionCurve = (float) Math.pow(axisShare, axisExponent);
        float distanceCurve = 1.0F - (float) Math.exp(-Math.pow(totalError / 11.5F, 1.27));
        float closeControl = Mth.clamp(absoluteDelta / 2.5F, 0.08F, 1.0F);
        float magnitude = maxStep * directionCurve * distanceCurve * closeControl;
        return Math.copySign(Math.min(absoluteDelta, magnitude), delta);
    }

    private float updateSpeedFactor(float frameScale) {
        speedTicks -= frameScale;
        if (speedTicks <= 0.0F) {
            targetSpeedFactor = 0.55F + random.nextFloat() * 0.55F;
            speedTicks = 6.0F + random.nextInt(15);
        }
        speedFactor += (targetSpeedFactor - speedFactor) * Math.min(1.0F, 0.12F * frameScale);
        return speedFactor;
    }

    private void updateCadence(float speed, float frameScale) {
        cadenceTicks -= frameScale;
        if (cadenceTicks <= 0) {
            targetCadenceMultiplier = 0.86F + random.nextFloat() * 0.28F;
            cadenceTicks = (5 + random.nextInt(10)) * frameScale;
        }
        float cadenceEase = 0.12F + random.nextFloat() * 0.05F;
        cadenceMultiplier += (targetCadenceMultiplier - cadenceMultiplier) * cadenceEase;
        cadencePhase += (0.22F + Mth.clamp(speed / 20.0F, 0.0F, 1.0F) * 0.19F) * frameScale;
    }

    private Vec3 getSmoothMultipoint(LivingEntity target, float frameScale) {
        if (trackedTarget != target) {
            trackedTarget = target;
            currentPointOffset = Vec3.ZERO;
            chooseNextPoint(target, 12.0F);
        } else if (pointTravelTick >= pointTravelDuration) {
            chooseNextPoint(target, 12.0F);
        }

        pointTravelTick += frameScale;
        float t = Mth.clamp(pointTravelTick / Math.max(1, pointTravelDuration), 0.0F, 1.0F);
        float eased = smootherStep(t);
        double inverse = 1.0 - eased;
        currentPointOffset = pointStartOffset.scale(inverse * inverse)
                .add(pointControlOffset.scale(2.0 * inverse * eased))
                .add(targetPointOffset.scale(eased * eased));

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 center = target.getPosition(partial).add(0.0, target.getBbHeight() * 0.5, 0.0);
        return center.add(currentPointOffset);
    }

    private void chooseNextPoint(LivingEntity target, float baseDuration) {
        double width = target.getBbWidth();
        double height = target.getBbHeight();

        pointStartOffset = currentPointOffset;
        double x = (random.nextDouble() * 2.0 - 1.0) * width * 0.28;
        double y = (random.nextDouble() * 0.20 - 0.02) * height;
        double z = (random.nextDouble() * 2.0 - 1.0) * width * 0.28;
        targetPointOffset = new Vec3(x, y, z);

        Vec3 midpoint = pointStartOffset.lerp(targetPointOffset, 0.5);
        double curveX = (random.nextDouble() * 2.0 - 1.0) * width * 0.14;
        double curveY = (random.nextDouble() * 2.0 - 1.0) * height * 0.07;
        double curveZ = (random.nextDouble() * 2.0 - 1.0) * width * 0.14;
        pointControlOffset = new Vec3(
                Mth.clamp((float) (midpoint.x + curveX), (float) (-width * 0.34), (float) (width * 0.34)),
                Mth.clamp((float) (midpoint.y + curveY), (float) (-height * 0.10), (float) (height * 0.24)),
                Mth.clamp((float) (midpoint.z + curveZ), (float) (-width * 0.34), (float) (width * 0.34))
        );
        pointTravelDuration = baseDuration + random.nextInt(12);
        pointTravelTick = 0;
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private float quantizeMouseStep(float step, float gcd, boolean yawAxis, float remainingDelta) {
        if (!(gcd > 0.0F) || !Float.isFinite(gcd)) return step;
        if (Math.abs(remainingDelta) < gcd * 0.55F) {
            if (yawAxis) {
                yawMouseRemainder = 0.0F;
                yawVelocity = 0.0F;
            } else {
                pitchMouseRemainder = 0.0F;
                pitchVelocity = 0.0F;
            }
            return 0.0F;
        }

        float remainder = yawAxis ? yawMouseRemainder : pitchMouseRemainder;
        float accumulated = step + remainder;
        float quantized = Math.round(accumulated / gcd) * gcd;
        if (Math.abs(quantized) > Math.abs(remainingDelta) + gcd * 0.5F) {
            quantized = (float) ((int) (remainingDelta / gcd)) * gcd;
        }

        float nextRemainder = Mth.clamp(accumulated - quantized, -gcd, gcd);
        if (yawAxis) {
            yawMouseRemainder = nextRemainder;
        } else {
            pitchMouseRemainder = nextRemainder;
        }
        return quantized;
    }

    private static float[] rotationsTo(Vec3 delta) {
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * 57.29577951308232 - 90.0);
        float pitch = (float) (-Mth.atan2(delta.y, horizontal) * 57.29577951308232);
        return new float[]{yaw, pitch};
    }
}
