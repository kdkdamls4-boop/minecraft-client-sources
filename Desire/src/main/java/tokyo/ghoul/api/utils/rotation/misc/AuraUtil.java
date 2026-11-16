package tokyo.ghoul.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

@UtilityClass
public class AuraUtil implements QuickImports {
    private Vec3 dvdPoint = Vec3.ZERO;
    private Vec3 dvdMotion = Vec3.ZERO;

    private float hitCount = 0;

    public void onAttack(String mode) {
        hitCount += 1;
        if (hitCount >= 3) {
            hitCount = 0;
        }
    }

    public Vec3 getAimpoint(LivingEntity entity, String mode) {
        switch (mode) {
            case "Holy World" -> {
                return getDvDPoint(entity);
            }

            case "Fun Time" -> {
                return getDistanceBasedPoint(entity);
            }

            default -> {
                return RotationUtil.getSpot(entity);
            }
        }
    }

    public Vec3 getBestVector(Entity entity, float jitterOnBoxValue) {
        double yExpand = Mth.clamp(mc.player.getEyeHeight(mc.player.getPose()) - entity.getEyeHeight(entity.getPose()), entity.getBbHeight() / 2, entity.getBbHeight())
                / (mc.player.isFallFlying() ? 10 : !mc.options.keyJump.isDown() && mc.player.onGround() ?
                entity.isShiftKeyDown() ? 0.8F : 0.6f : 1F);

        Vec3 finalVector = entity.position().add(0, yExpand, 0);
        return finalVector.add(jitterOnBoxValue, jitterOnBoxValue / 2, jitterOnBoxValue);
    }

    public Vec3 getDistanceBasedPoint(Entity entity) {
        Vec3 eye = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();

        float attackDistance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        float distanceFactor = (float) (mc.player.position().distanceTo(entity.position()) / attackDistance);

        float minY = (float) (box.maxY - box.minY);
        float clampedY = (float) Math.max(box.minY + minY * distanceFactor, box.minY + minY * 0.3f);

        float safePoint = entity.getBbWidth() * 0.4f;

        Vec3 basePoint = new Vec3(
                Mth.clamp(eye.x, box.minX + safePoint, box.maxX - safePoint),
                Mth.clamp(eye.y, box.minY, clampedY),
                Mth.clamp(eye.z, box.minZ + safePoint, box.maxZ - safePoint)
        );

        return basePoint;
    }

    public Vec3 getDvDPoint(Entity entity) {
        float minMotionXZ = 0.003f;
        float maxMotionXZ = 0.04f;

        float minMotionY = 0.001f;
        float maxMotionY = 0.03f;

        double lengthX = entity.getBoundingBox().getXsize();
        double lengthY = entity.getBoundingBox().getYsize() * 0.8f;
        double lengthZ = entity.getBoundingBox().getZsize();

        if (dvdMotion.equals(Vec3.ZERO))
            dvdMotion = new Vec3(MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f));

        dvdPoint = dvdPoint.add(dvdMotion);

        if (dvdPoint.x >= (lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3(-MathUtil.randomInRange(minMotionXZ, maxMotionXZ), dvdMotion.y(), dvdMotion.z());

        if (dvdPoint.y >= lengthY)
            dvdMotion = new Vec3(dvdMotion.x(), -MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.z());

        if (dvdPoint.z >= (lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3(dvdMotion.x(), dvdMotion.y(), -MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        if (dvdPoint.x <= -(lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3(MathUtil.randomInRange(minMotionXZ, 0.03f), dvdMotion.y(), dvdMotion.z());

        if (dvdPoint.y <= 0.05)
            dvdMotion = new Vec3(dvdMotion.x(), MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.z());

        if (dvdPoint.z <= -(lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3(dvdMotion.x(), dvdMotion.y(), MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        dvdPoint.add(MathUtil.randomInRange(-0.03f, 0.03f), 0f, MathUtil.randomInRange(-0.03f, 0.03f));

        Vec3 dvdPointed = entity.position().add(dvdPoint);
        AABB box = entity.getBoundingBox();
        return new Vec3(
                Mth.clamp(dvdPointed.x, box.minX, box.maxX),
                Mth.clamp(dvdPointed.y, box.minY, box.maxY),
                Mth.clamp(dvdPointed.z, box.minZ, box.maxZ)
        );
    }
}