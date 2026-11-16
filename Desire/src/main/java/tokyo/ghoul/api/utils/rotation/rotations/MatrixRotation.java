package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class MatrixRotation extends RotationMode {
    private static float lastYaw, lastPitch;

    public MatrixRotation() {
        super("Matrix");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYRot();
        float pitchDelta = delta.getXRot();

        float yawSpeed = MathUtil.randomInRange(58, 60);
        float pitchSpeed = FunTimeRotation.attack || entity == null ? Math.abs(pitchDelta) : 0.333f;

        float pitchStep = Math.max(pitchDelta, (float) (2.0 + Math.random() * 2.0));
        pitchDelta = (pitchDelta > 0) ? pitchStep : -pitchStep;

        float jitterPower = 3f;
        if (Math.abs(yawDelta - lastYaw) >= jitterPower) {
            float jitterApplier = jitterPower + 0.1f;
            yawDelta += (yawDelta > 0) ? jitterApplier : -jitterApplier;
        }

        lastYaw = yawDelta;
        lastPitch = pitchDelta;

        return new Rotation(
                currentRotation.getYRot() + Mth.clamp(yawDelta, -yawSpeed, yawSpeed),
                currentRotation.getXRot() + Mth.clamp(pitchDelta, -pitchSpeed, pitchSpeed)
        );
    }
}
