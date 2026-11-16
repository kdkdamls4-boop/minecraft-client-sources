package tokyo.ghoul.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

@UtilityClass
public class MultipointUtil implements QuickImports {
    private Entity cachedTarget;
    private Vec3 cachedEye;
    private AABB cachedBox;
    private double cachedDistance;
    private int cachedTick = -1;
    private Vec3 cachedResult;

    public Vec3 getNearestPoint(Entity target, double distance) {
        Vec3 eye = mc.player.getEyePosition(1.0f);
        double maxDistSq = distance * distance;
        AABB box = target.getBoundingBox();
        int tick = mc.player.tickCount;

        if (cachedResult != null && cachedTarget == target && cachedTick == tick
                && cachedDistance == distance && eye.equals(cachedEye) && box.equals(cachedBox)) {
            return cachedResult;
        }

        Vec3 center = box.getCenter();
        double step = 0.1;
        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
                for (double z = box.minZ; z <= box.maxZ; z += step) {
                    Vec3 point = new Vec3(x, y, z);
                    if (eye.distanceToSqr(point) > maxDistSq) continue;

                    HitResult result = mc.level.clip(new ClipContext(
                            eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                    boolean visible = result.getType() == HitResult.Type.MISS || result.getType() != HitResult.Type.BLOCK;
                    if (!visible) continue;

                    double score = center.distanceToSqr(point);
                    if (score < bestScore) {
                        bestScore = score;
                        best = point;
                    }
                }
            }
        }

        Vec3 result = best == null ? box.getCenter() : best;
        cachedTarget = target;
        cachedEye = eye;
        cachedBox = box;
        cachedDistance = distance;
        cachedTick = tick;
        cachedResult = result;
        return result;
    }
}
