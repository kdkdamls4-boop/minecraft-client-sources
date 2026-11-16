package tokyo.ghoul.api.utils.rotation.neuro;

public final class NeuroControl {
    private static final float MIN_GAIN = 0.25f;
    private static final float MAX_GAIN = 1.2f;
    private static final float OVERSHOOT = 1.5f;
    private static final float NOISE_FLOOR = 0.6f;
    private static final float NOISE_PER_DEGREE = 0.25f;
    private static final float BLIND_STEP = 5f;

    private NeuroControl() {
    }

    public static float step(float mu, float sigma, double noise, float temperature,
                             float err, float deadZone, boolean observed) {
        float step;
        float abs = Math.abs(err);
        if (!observed) {
            step = clamp(mu, -BLIND_STEP, BLIND_STEP);
        } else if (abs < 1e-4f) {
            step = clamp(mu, -OVERSHOOT, OVERSHOOT);
        } else {
            float sign = Math.signum(err);
            float toward = mu * sign;
            toward = abs > deadZone ? Math.max(toward, MIN_GAIN * abs) : Math.max(toward, -OVERSHOOT);
            toward = Math.min(toward, MAX_GAIN * abs + OVERSHOOT);
            step = toward * sign;
        }
        float sigmaLimit = NOISE_FLOOR + NOISE_PER_DEGREE * (observed ? abs : BLIND_STEP);
        return step + (float) (noise * Math.min(sigma, sigmaLimit) * temperature);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
