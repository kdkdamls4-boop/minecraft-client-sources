package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.animation.wrap.infinity.RotationAnimation;
import tokyo.ghoul.api.utils.combat.ClickScheduler;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.player.PlayerUtil;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

public class SpookyTimeRotation extends RotationMode {
    private final ClickScheduler clickScheduler;
    private final RotationAnimation interpolation = new RotationAnimation();

    private static float slowPitchTicks = 0, slowYawTicks = 0, someTicks = 0;
    private float lastYawDelta, lastPitchDelta;

    public SpookyTimeRotation(ClickScheduler clickScheduler) {
        super("Spooky Time");
        this.clickScheduler = clickScheduler;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYRot();
        float pitchDelta = delta.getXRot();

        float auraDistance = AuraModule.getInstance().getAttackDistance();
        float distanceToTarget = entity != null ? (float) mc.player.position().distanceTo(entity.position()) : 1f;
        float distanceFactor = Mth.clamp(0.3f + 0.7f * (distanceToTarget / auraDistance), 0f, 1f);

        boolean hasTarget = entity != null;
        EntityHitResult hitResult = RaytracingUtil.raytraceEntity(auraDistance, currentRotation, false);
        boolean hasTrace = hasTarget && hitResult != null && hitResult.getEntity() == entity;
        float raytrace = hasTrace ? 0.4f : 1;

        boolean check = hasTarget && PlayerUtil.hasCollisionWith(entity) && (stalin(entity)
                || PlayerUtil.getBlock(0, 2, 0) != Blocks.AIR && PlayerUtil.getBlock(0, -1, 0)
                != Blocks.AIR && PlayerUtil.getBlock(0, 2, 0) != Blocks.WATER && PlayerUtil.getBlock(0, -1, 0) != Blocks.WATER);
        if (check) yawDelta /= 30;

        float random = MathUtil.randomInRange(0.5f, 0.7f);
        float yawSpeed = MathUtil.randomInRange(25f, 31f);
        float pitchSpeed = MathUtil.randomInRange(6f, 7.5f);

        if (hasTrace) {
            slowYawTicks = MathUtil.interpolate(slowYawTicks, 0.7f, 0.6f);
            slowPitchTicks = MathUtil.interpolate(slowPitchTicks, 0.5f, 0.5f);
        } else {
            slowYawTicks = MathUtil.interpolate(slowYawTicks, 1f, 0.9f);
            slowPitchTicks = MathUtil.interpolate(slowPitchTicks, 1f, 0.2f);
        }

        yawSpeed *= slowYawTicks;
        pitchSpeed *= slowPitchTicks;
        yawDelta += (float) (Math.cos((double) System.currentTimeMillis() / 50) * (3f * distanceFactor));
        pitchDelta += (float) (Math.sin((double) System.currentTimeMillis() / 60) * (9f * distanceFactor));

        return new Rotation(
                currentRotation.getYRot() + Mth.clamp(yawDelta, -yawSpeed, yawSpeed) * random,
                currentRotation.getXRot() + Mth.clamp(pitchDelta, -pitchSpeed, pitchSpeed) * random / yawSpeed * 14 * raytrace
        );
    }

    public static boolean stalin(Entity target) {
        Vec3 pos = target.position();
        AABB hitbox = target.getBoundingBox();

        float off = 0.05f;

        return !isAir(hitbox.minX-off, pos.y, hitbox.minZ-off)
                || !isAir(hitbox.maxX+off, pos.y, hitbox.minZ-off)
                || !isAir(hitbox.minX-off, pos.y, hitbox.maxZ+off)
                || !isAir(hitbox.maxX+off, pos.y, hitbox.maxZ+off);
    }

    public static boolean isAir(double x, double y, double z) {
        return mc.level.getBlockState(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z))).getBlock() == Blocks.AIR;
    }
}
