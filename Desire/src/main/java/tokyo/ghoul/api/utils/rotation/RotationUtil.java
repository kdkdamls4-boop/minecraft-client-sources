package tokyo.ghoul.api.utils.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;

@UtilityClass
public class RotationUtil implements QuickImports {
    public boolean inFov(Vec3 vec3d, float fov) {
        Rotation rotation = rotationAt(vec3d);
        float deltaYaw = Mth.wrapDegrees(rotation.getYRot() - mc.player.getYRot());
        float deltaPitch = Mth.wrapDegrees(rotation.getXRot() - mc.player.getXRot());

        return Math.abs(deltaYaw) <= fov && Math.abs(deltaPitch) <= fov;
    }

    public Rotation rotationAt(Vec3 position) {
        if (position == null) return Rotation.DEFAULT;
        Vec3 playerPos = mc.player.position().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
        float diffX = (float) (position.x - playerPos.x);
        float diffY = (float) (position.y - playerPos.y);
        float diffZ = (float) (position.z - playerPos.z);

        float dist = (float) Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(yaw, pitch);
    }

    public Rotation fromVec2f(Vec2 vector2f) {
        return new Rotation(vector2f.y, vector2f.x);
    }

    public Rotation fromVec3(Vec3 vector) {
        return new Rotation(
                Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90),
                Mth.wrapDegrees((float) Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
        );
    }

    public Rotation calculateDelta(Rotation start, Rotation end) {
        float deltaYaw = Mth.wrapDegrees(end.getYRot() - start.getYRot());
        float deltaPitch = Mth.wrapDegrees(end.getXRot() - start.getXRot());
        return new Rotation(deltaYaw, deltaPitch);
    }

    public Vec3 getSpot(Entity entity) {
        Vec3 eye = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        return new Vec3(
                Mth.clamp(eye.x, box.minX, box.maxX),
                Mth.clamp(eye.y, box.minY, box.maxY),
                Mth.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    public Vec3 rayCastBox(Entity entity, Vec3 end) {
        AABB box = entity.getBoundingBox();
        Vec3 start = mc.getCameraEntity().getEyePosition(1.0f);

        Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);

        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        Vec3 direction = end.normalize();

        for (int axis = 0; axis < 3; axis++) {
            double d, minVal, maxVal, startVal;

            switch (axis) {
                case 0: d = direction.x; minVal = min.x; maxVal = max.x; startVal = start.x; break;
                case 1: d = direction.y; minVal = min.y; maxVal = max.y; startVal = start.y; break;
                case 2: d = direction.z; minVal = min.z; maxVal = max.z; startVal = start.z; break;
                default: continue;
            }

            if (Math.abs(d) < 1e-7) {
                if (startVal < minVal || startVal > maxVal) {
                    return end;
                }
            } else {
                double t1 = (minVal - startVal) / d;
                double t2 = (maxVal - startVal) / d;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                if (tMin > tMax) {
                    return end;
                }
            }
        }

        double distance = start.distanceTo(end);
        if (tMin > distance || tMin < 0) {
            return end;
        }

        return start.add(direction.scale(tMin));
    }
}
