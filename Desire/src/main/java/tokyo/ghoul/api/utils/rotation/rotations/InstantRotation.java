package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class InstantRotation extends RotationMode {
    public InstantRotation() {
        super("Instant");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        return targetRotation;
    }
}
