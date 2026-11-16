package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class GrimRotation extends RotationMode {
    private static float prevYaw;
    private static float prevPitch;
    private static boolean initialized;

    public GrimRotation() {
        super("LonyGrief");
    }

    public static void reset() {
        initialized = false;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (targetRotation == null) return currentRotation;
        if (!initialized) {
            prevYaw = currentRotation.getYRot();
            prevPitch = currentRotation.getXRot();
            initialized = true;
        }

        float targetYaw = targetRotation.getYRot();
        float targetPitch = targetRotation.getXRot();

        float yawDelta = Mth.wrapDegrees(targetYaw - prevYaw);
        float pitchDelta = Mth.wrapDegrees(targetPitch - prevPitch);

        float yaw = prevYaw + (yawDelta > 0 ? Math.max(Math.abs(yawDelta), 0.0f) : -Math.max(Math.abs(yawDelta), 0.0f));
        float pitch = Mth.clamp(
                prevPitch + (pitchDelta > 0 ? Math.max(Math.abs(pitchDelta), 0.0f) : -Math.max(Math.abs(pitchDelta), 0.0f)),
                -89.0F, 90.0F
        );

        float gcd = getGcdValue();
        yaw -= (yaw - prevYaw) % gcd;
        pitch -= (pitch - prevPitch) % gcd;

        prevYaw = yaw;
        prevPitch = pitch;
        return new Rotation(yaw, pitch);
    }

    private static float getGcdValue() {
        Minecraft client = Minecraft.getInstance();
        double sensitivity = client.options.sensitivity().get();
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 1.2;
        return (float) (gcd * 0.15);
    }
}
