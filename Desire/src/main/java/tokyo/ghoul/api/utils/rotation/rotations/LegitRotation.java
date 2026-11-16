package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class LegitRotation extends RotationMode {

    public LegitRotation() {
        super("Legit");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        float yawDelta = Mth.wrapDegrees(targetRotation.getYRot() - currentRotation.getYRot());
        float pitchDelta = targetRotation.getXRot() - currentRotation.getXRot();
        float total = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float yawSpeed = Mth.clamp(Math.abs(yawDelta) * 0.225f + 6f, 6f, 27.5f);
        float pitchSpeed = Mth.clamp(Math.abs(pitchDelta) * 0.225f + 4.5f, 4.5f, 20f);

        float progress = Mth.clamp(total / 45f, 0.15f, 1f);
        float ease = 0.55f + 0.45f * progress;

        float stepYaw = Mth.clamp(yawDelta * 0.25f * ease, -yawSpeed, yawSpeed);
        float stepPitch = Mth.clamp(pitchDelta * 0.25f * ease, -pitchSpeed, pitchSpeed);

        float gcd = MouseUtil.getGCD();
        float newYaw = currentRotation.getYRot() + Math.round(stepYaw / gcd) * gcd;
        float newPitch = Mth.clamp(currentRotation.getXRot() + Math.round(stepPitch / gcd) * gcd, -90f, 90f);

        if (Math.abs(yawDelta) > 0.01f && Math.signum(stepYaw) != Math.signum(yawDelta)) {
            newYaw = currentRotation.getYRot();
        }
        if (Math.abs(pitchDelta) > 0.01f && Math.signum(stepPitch) != Math.signum(pitchDelta)) {
            newPitch = currentRotation.getXRot();
        }

        return new Rotation(newYaw, newPitch);
    }
}
