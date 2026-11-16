package tokyo.ghoul.api.utils.rotation.misc;

import net.minecraft.util.Mth;

import java.util.Random;

public final class HumanAimPattern {
    private final Random random = new Random();
    private final float phaseA = random.nextFloat() * Mth.TWO_PI;
    private final float phaseB = random.nextFloat() * Mth.TWO_PI;
    private final float phaseC = random.nextFloat() * Mth.TWO_PI;
    private float time;
    private float reactionTicks;
    private float pauseTicks;
    private float overshootDeg;
    private boolean overshootUsed;

    public void reset() {
        reactionTicks = 0.0F;
        pauseTicks = 0.0F;
        overshootDeg = 0.0F;
        overshootUsed = false;
    }

    public void onTargetChanged() {
        reactionTicks = 3.0F + random.nextFloat() * 3.0F;
        pauseTicks = 0.0F;
        overshootDeg = 0.0F;
        overshootUsed = false;
    }

    public boolean isReacting(float frameScale) {
        if (reactionTicks <= 0.0F) return false;
        reactionTicks -= frameScale;
        return true;
    }

    public float yawBias(float realDeltaYaw, float angularError) {
        if (overshootDeg != 0.0F) {
            if (Math.signum(realDeltaYaw) == Math.signum(overshootDeg)) return overshootDeg;
            overshootDeg = 0.0F;
            return 0.0F;
        }
        if (overshootUsed || angularError < 10.0F) return 0.0F;

        overshootUsed = true;
        overshootDeg = Math.signum(realDeltaYaw) * (1.5F + random.nextFloat() * 4.0F);
        return overshootDeg;
    }

    public float speedScale(float frameScale) {
        time += frameScale;
        if (pauseTicks > 0.0F) {
            pauseTicks -= frameScale;
            return 0.2F;
        }
        if (random.nextFloat() < 0.012F * frameScale) {
            pauseTicks = 2.0F + random.nextFloat() * 4.0F;
            return 0.2F;
        }
        return 1.0F + 0.22F * (float) Math.sin(time * 0.045F + phaseA)
                + 0.08F * (float) Math.sin(time * 0.13F + phaseB);
    }

    public float[] jitter(float angularError, float frameScale) {
        float amplitude = Mth.clamp(angularError / 20.0F, 0.0F, 1.0F) * 0.12F;
        float yaw = amplitude * ((float) Math.sin(time * 0.9F + phaseC) + 0.5F * (random.nextFloat() * 2.0F - 1.0F));
        float pitch = amplitude * 0.6F * ((float) Math.sin(time * 0.7F + phaseA) + 0.5F * (random.nextFloat() * 2.0F - 1.0F));
        return new float[]{yaw * frameScale, pitch * frameScale};
    }
}
