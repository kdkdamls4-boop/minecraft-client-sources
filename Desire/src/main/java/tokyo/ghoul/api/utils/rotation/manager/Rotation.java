package tokyo.ghoul.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;

@Getter
@Setter
public class Rotation {
    private float yaw;
    private float pitch;

    public static final Rotation DEFAULT = new Rotation(0f, 0f);

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYRot() {
        return yaw;
    }

    public float getXRot() {
        return pitch;
    }

    public Rotation adjustSensitivity() {
        double gcd = MouseUtil.getGCD();
        Rotation previousRotation = RotationManager.getInstance().getServerRotation();

        float adjustedYaw = adjustAxis(yaw, previousRotation.yaw, gcd);
        float adjustedPitch = adjustAxis(pitch, previousRotation.pitch, gcd);

        return new Rotation(adjustedYaw, Mth.clamp(adjustedPitch, -90f, 90f));
    }

    private float adjustAxis(float axisValue, float previousValue, double gcd) {
        float delta = axisValue - previousValue;
        return previousValue + Math.round(delta / gcd) * (float) gcd;
    }

    public Vec3 getVector() {
        float f = pitch * 0.017453292f;
        float g = -yaw * 0.017453292f;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public Rotation rotationDeltaTo(Rotation targetRotation) {
        return RotationUtil.calculateDelta(this, targetRotation);
    }

    @Override
    public String toString() {
        return "Rotation(yaw=" + yaw + ", pitch=" + pitch + ")";
    }

    public record VecRotation(Rotation rotation, Vec3 vec) {
        @Override
        public String toString() {
            return "VecRotation(rotation=" + rotation + ", vec=" + vec + ")";
        }
    }
}
