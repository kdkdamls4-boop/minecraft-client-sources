package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class SmoothRotation extends RotationMode {
    public SmoothRotation() {
        super("Smooth");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYRot();
        float pitchDelta = delta.getXRot();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (180.0f / 9f);
        float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (90.0f / 9f);

        return new Rotation(
                currentRotation.getYRot() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
                currentRotation.getXRot() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
        );
    }
}
