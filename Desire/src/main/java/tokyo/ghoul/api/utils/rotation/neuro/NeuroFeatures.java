package tokyo.ghoul.api.utils.rotation.neuro;

public final class NeuroFeatures {
    public static final String ARCH = "gru-v2";
    public static final int REACTION = 3;
    public static final int SIZE = 8;
    public static final int OUTPUTS = 4;
    public static final int MAX_SINCE_ATTACK = 40;

    private NeuroFeatures() {
    }

    public static float[] build(float errYaw, float errPitch, float tgtVelYaw, float tgtVelPitch,
                                float distance, float angularSize, int ticksSinceAttack, boolean observed) {
        return new float[]{
                errYaw, errPitch, tgtVelYaw, tgtVelPitch, distance, angularSize,
                Math.min(ticksSinceAttack, MAX_SINCE_ATTACK), observed ? 1f : 0f
        };
    }

    public static float[] angles(double ex, double ey, double ez, double px, double py, double pz) {
        double dx = px - ex;
        double dy = py - ey;
        double dz = pz - ez;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{yaw, pitch};
    }

    public static float wrap(float degrees) {
        float value = degrees % 360f;
        if (value >= 180f) value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }
}
