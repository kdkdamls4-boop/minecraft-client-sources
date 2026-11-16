package tokyo.ghoul.client.features.modules.render.pets;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.render.pets.anim.AnimationValue;
import tokyo.ghoul.client.features.modules.render.pets.ScreenPoint;

public final class PetDragonController implements QuickImports {
    private static final float FLY_HEIGHT = 1.5F;
    private static final double CLEAR_RADIUS = 0.9D;
    private static final double CLEAR_HEIGHT = 1.4D;

    private Vec3 motion = Vec3.ZERO;
    private final AnimationValue x = new AnimationValue();
    private final AnimationValue y = new AnimationValue();
    private final AnimationValue z = new AnimationValue();
    private final AnimationValue bodyAnim = new AnimationValue();
    private final AnimationValue yawAnim = new AnimationValue();
    private final AnimationValue pitchAnim = new AnimationValue();

    private Player entity;
    private Vec3 pos;
    private float yaw;

    public void setEntity(Player entity) { this.entity = entity; }
    public void reset() { pos = null; motion = Vec3.ZERO; yaw = 0.0F; }
    public Vec3 getPos() {
        if (pos == null) return null;
        return new Vec3(x.get(), y.get(), z.get());
    }
    public float getYaw() { return yawAnim.get(); }
    public float getPitch() { return pitchAnim.get(); }
    public float getBody() { return bodyAnim.get(); }
    public boolean isFlying() { return true; }
    public boolean isPlayerMoving() { return entity != null && entity.getDeltaMovement().lengthSqr() > 0.01D; }

    public void onUpdate() {
        if (entity == null || mc.level == null) return;
        Vec3 entityPos = entity.position();
        if (pos == null || pos.distanceTo(entityPos) > 15.0D) {
            pos = findSafePosition(entityPos.add(0.0D, FLY_HEIGHT, 0.0D), entityPos);
            x.animate((float) pos.x, 1);
            y.animate((float) pos.y, 1);
            z.animate((float) pos.z, 1);
        }
        LivingEntity target = getAuraTarget();
        Vec3 desired;
        if (target != null && entity instanceof LocalPlayer) {
            desired = target.position().add(FLY_HEIGHT, FLY_HEIGHT, FLY_HEIGHT);
        } else {
            double bob = Math.sin(entity.tickCount * 0.1F) * 0.3D;
            desired = entityPos.add(2.0D, FLY_HEIGHT + bob, 0.0D);
        }
        Vec3 safeTarget = findSafePosition(desired, entityPos);
        Vec3 delta = safeTarget.subtract(pos);
        double followSpeed = target != null ? 0.2D : 0.15D;
        motion = delta.scale(followSpeed);
        Vec3 next = pos.add(motion);
        if (isAirSpace(next)) {
            pos = next;
        } else {
            Vec3 slide = pos.add(motion.x * 0.35D, 0.0D, motion.z * 0.35D);
            if (isAirSpace(slide)) pos = slide;
        }
        handleRotation(target);
        x.animate((float) pos.x, 100);
        y.animate((float) pos.y, 100);
        z.animate((float) pos.z, 100);
    }

    private void handleRotation(LivingEntity target) {
        if (target != null && entity instanceof LocalPlayer) {
            yaw = rotationToward(pos, target.position()).x;
        } else if (motion.x != 0.0D || motion.z != 0.0D) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float) Math.toDegrees(angle) - 90.0F;
            yaw %= 360.0F;
            if (yaw < 0.0F) yaw += 360.0F;
        }
        bodyAnim.animate(yaw, 80);
        yawAnim.animate(yaw, 100);
        pitchAnim.animate(0.0F, 100);
    }

    private Vec3 findSafePosition(Vec3 desired, Vec3 fallback) {
        if (isAirSpace(desired)) return desired;
        double[] radii = {0.75D, 1.25D, 1.75D, 2.5D, 3.25D};
        for (double radius : radii) {
            for (int step = 0; step < 8; step++) {
                double angle = Math.toRadians(step * 45.0D);
                Vec3 candidate = desired.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                if (isAirSpace(candidate)) return candidate;
            }
        }
        return fallback.add(0.0D, FLY_HEIGHT, 0.0D);
    }

    private boolean isAirSpace(Vec3 center) {
        double[] xOffsets = {0.0D, -CLEAR_RADIUS, CLEAR_RADIUS};
        double[] yOffsets = {0.0D, CLEAR_HEIGHT / 2.0D, CLEAR_HEIGHT};
        double[] zOffsets = {0.0D, -CLEAR_RADIUS, CLEAR_RADIUS};
        for (double offsetX : xOffsets) {
            for (double offsetY : yOffsets) {
                for (double offsetZ : zOffsets) {
                    if (isBlockSolid(center.x + offsetX, center.y + offsetY, center.z + offsetZ)) return false;
                }
            }
        }
        return true;
    }

    private static ScreenPoint rotationToward(Vec3 from, Vec3 to) {
        double deltaX = to.x - from.x;
        double deltaY = to.y - from.y;
        double deltaZ = to.z - from.z;
        double horizontal = Mth.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 57.29577951308232D) - 90.0F;
        float pitch = (float) -(Math.atan2(deltaY, horizontal) * 57.29577951308232D);
        return new ScreenPoint(yaw, pitch);
    }

    private static boolean isBlockSolid(double x, double y, double z) {
        if (mc.level == null) return false;
        BlockPos blockPos = BlockPos.containing(x, y, z);
        return !mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty();
    }

    private static LivingEntity getAuraTarget() {
        try {
            AuraModule aura = AuraModule.getInstance();
            return aura.isEnabled() ? aura.target : null;
        } catch (Exception e) { return null; }
    }
}
