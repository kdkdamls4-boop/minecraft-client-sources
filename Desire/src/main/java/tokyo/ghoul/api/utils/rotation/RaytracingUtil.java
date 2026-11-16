package tokyo.ghoul.api.utils.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;

import java.util.function.Predicate;

@UtilityClass
public class RaytracingUtil implements QuickImports {
    private boolean shouldBlockHitCancel(Vec3 start, Vec3 entityPos, BlockHitResult blockHit) {
        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) return false;
        if (mc.level == null) return false;

        var blockState = mc.level.getBlockState(blockHit.getBlockPos());
        if (blockState == null) return false;

        if (!blockState.getCollisionShape(mc.level, blockHit.getBlockPos()).isEmpty()) {
            Vec3 blockPos = blockHit.getLocation();
            return blockPos.distanceToSqr(start) < entityPos.distanceToSqr(start);
        }

        return false;
    }

    public BlockHitResult raycast(
            Rotation rotation,
            double range,
            boolean includeFluids,
            float tickDelta
    ) {
        return raycast(range, includeFluids, mc.player.getEyePosition(tickDelta), rotation.getVector(), mc.getCameraEntity());
    }

    public BlockHitResult raycast(double range, boolean includeFluids, Vec3 start, Vec3 direction, Entity entity) {
        Vec3 end = start.add(direction.x * range, direction.y * range, direction.z * range);

        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, entity));
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls) {
        return raytraceEntity(range, rotation, ignoreWalls, entity -> true, 0f);
    }

    public boolean rayTraceBox(Rotation rotation, double range, AABB box) {
        Vec3 eye = mc.player.getEyePosition();
        return box.contains(eye) || box.clip(eye, eye.add(rotation.getVector().scale(range))).isPresent();
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls, float expand) {
        return raytraceEntity(range, rotation, ignoreWalls, entity -> true, expand);
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls, Predicate<Entity> filter, float expand) {
        Entity entity = mc.getCameraEntity();
        if (entity == null) return null;

        Vec3 cameraVec = entity.getEyePosition();
        Vec3 rotationVec = rotation.getVector();

        Vec3 vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        AABB box = entity.getBoundingBox().expandTowards(rotationVec.scale(range)).inflate(1f, 1f, 1f);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                entity, cameraVec, vec3d3, box,
                e -> e.isPickable() && e.isAlive() && !e.isSpectator() && filter.test(e),
                range * range
        );
        if (entityHit == null) return null;

        if (ignoreWalls) return entityHit;

        Vec3 entityPos = entityHit.getEntity().getBoundingBox().clip(cameraVec, vec3d3).orElse(entityHit.getEntity().position());
        BlockHitResult blockHit = mc.level != null ? mc.level.clip(new ClipContext(
                cameraVec, entityPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                mc.player
        )) : null;

        return shouldBlockHitCancel(cameraVec, entityPos, blockHit) ? null : entityHit;
    }
}