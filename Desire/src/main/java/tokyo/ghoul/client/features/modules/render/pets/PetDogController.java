package tokyo.ghoul.client.features.modules.render.pets;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.render.pets.anim.MSTimer;
import tokyo.ghoul.client.features.modules.render.pets.anim.AnimationValue;
import tokyo.ghoul.client.features.modules.render.pets.ScreenPoint;

public final class PetDogController implements QuickImports {
    private Vec3 motion = Vec3.ZERO;
    private float direction = (float) ThreadLocalRandom.current().nextDouble(0.0D, 360.0D);
    private final AnimationValue x = new AnimationValue();
    private final AnimationValue y = new AnimationValue();
    private final AnimationValue z = new AnimationValue();
    private final AnimationValue bodyAnim = new AnimationValue();
    private final AnimationValue yawAnim = new AnimationValue();
    private final AnimationValue pitchAnim = new AnimationValue();
    private final MSTimer staying = new MSTimer();

    private Player entity;
    private Vec3 pos;
    private float yaw;
    private float body;
    private boolean lay;
    private long trickStartedAt;

    public float limbSwing;
    public float limbSwingAmount;
    public float prevLimbSwingAmount;

    public void setEntity(Player entity) { this.entity = entity; }

    public void reset() {
        pos = null;
        motion = Vec3.ZERO;
        direction = (float) ThreadLocalRandom.current().nextDouble(0.0D, 360.0D);
        yaw = 0.0F;
        body = 0.0F;
        lay = false;
        trickStartedAt = 0L;
        prevLimbSwingAmount = 0.0F;
        limbSwingAmount = 0.0F;
        limbSwing = 0.0F;
    }

    public Vec3 getPos() {
        if (pos == null) return null;
        return new Vec3(x.get(), y.get(), z.get());
    }

    public float getYaw() { return yawAnim.get(); }
    public float getPitch() { return pitchAnim.get(); }
    public float getBody() { return bodyAnim.get(); }
    public boolean isLay() { return lay; }
    public boolean isFlying() {
        return getTrickYOffset() > 0.08F || (pos != null && !isBlockSolid(pos.x, pos.y - 0.15D, pos.z));
    }

    public void startTrick() { trickStartedAt = System.currentTimeMillis(); staying.reset(); }
    public boolean isTrickActive() { return trickStartedAt > 0L && System.currentTimeMillis() - trickStartedAt < 4700L; }
    public float getLayProgress() { return Math.max(lay ? 1.0F : 0.0F, getTrickLayProgress()); }

    public float getTrickYOffset() {
        if (trickStartedAt == 0L) return 0.0F;
        float elapsed = (System.currentTimeMillis() - trickStartedAt) / 1000.0F;
        if (elapsed >= 4.7F) { trickStartedAt = 0L; return 0.0F; }
        if (elapsed < 0.85F) return easeOutCubic(elapsed / 0.85F) * 1.65F;
        if (elapsed < 2.35F) return 1.65F + (float) Math.sin(elapsed * 9.0F) * 0.05F;
        if (elapsed < 3.25F) return (1.0F - easeInOutCubic((elapsed - 2.35F) / 0.9F)) * 1.65F;
        return 0.0F;
    }

    public float getTrickLayProgress() {
        if (trickStartedAt == 0L) return 0.0F;
        float elapsed = (System.currentTimeMillis() - trickStartedAt) / 1000.0F;
        if (elapsed < 3.05F || elapsed > 4.25F) return 0.0F;
        if (elapsed < 3.45F) return easeInOutCubic((elapsed - 3.05F) / 0.4F);
        if (elapsed < 3.85F) return 1.0F;
        return 1.0F - easeInOutCubic((elapsed - 3.85F) / 0.4F);
    }

    public float getTrickWakeProgress() {
        if (trickStartedAt == 0L) return 0.0F;
        float elapsed = (System.currentTimeMillis() - trickStartedAt) / 1000.0F;
        if (elapsed < 3.75F || elapsed > 4.65F) return 0.0F;
        float progress = (elapsed - 3.75F) / 0.9F;
        return (float) Math.sin(progress * Math.PI);
    }

    public void onUpdate() {
        if (entity == null || mc.level == null) return;

        Vec3 entityPos = entity.position();
        if (pos == null || pos.distanceTo(entityPos) > 10.0D) {
            pos = entityPos;
            x.animate((float) pos.x, 1);
            y.animate((float) pos.y, 1);
            z.animate((float) pos.z, 1);
        }

        if (isTrickActive()) {
            motion = Vec3.ZERO;
            handleRotation();
            x.animate((float) pos.x, 80);
            y.animate((float) pos.y, 80);
            z.animate((float) pos.z, 80);
            limbSwingAmount += (0.0F - limbSwingAmount) * 0.55F;
            limbSwing += limbSwingAmount;
            lay = false;
            return;
        }

        motion = motion.add(0.0D, -0.2D, 0.0D);
        Vec3 next = pos.add(motion);
        if (isBlockSolid(next.x, next.y, next.z)) {
            int blockY = Mth.floor(next.y);
            double surface = blockY + 1.0D + 0.1D;
            next = new Vec3(next.x, surface, next.z);
            motion = new Vec3(motion.x, 0.0D, motion.z);
        }
        motion = new Vec3(motion.x, 0.0D, motion.z);

        LivingEntity target = getAuraTarget();
        if (target != null && entity instanceof LocalPlayer) {
            if (isBlockSolid(next.x, next.y - 0.1D, next.z)) {
                motion = new Vec3(motion.x, 0.62D, motion.z);
            }
            Vec3 current = getPos();
            if (current != null) {
                AABB dogBox = new AABB(current.subtract(0.4D, 0.0D, 0.4D), current.add(0.4D, 0.4D, 0.4D));
                AABB targetBox = target.getBoundingBox().deflate(0.1D);
                motion = motion.add(target.position().subtract(next).normalize());
                if (dogBox.intersects(targetBox)) {
                    motion = motion.multiply(-1.0D, 1.0D, -1.0D);
                }
            } else {
                motion = motion.add(target.position().subtract(next).normalize());
            }
        } else if (next.distanceTo(entityPos) > 2.0D) {
            motion = motion.add(entityPos.subtract(next).normalize());
        }

        handleRotation();
        pos = next;

        if (pos.distanceTo(entityPos) < 0.1D) {
            direction = (float) ThreadLocalRandom.current().nextDouble(0.0D, 360.0D);
            double wanderX = -Math.sin(Math.toRadians(direction)) * 0.1D;
            double wanderZ = Math.cos(Math.toRadians(direction)) * 0.1D;
            motion = motion.add(wanderX, 0.0D, wanderZ);
        }
        motion = motion.scale(0.5D);

        x.animate((float) pos.x, 150);
        y.animate((float) pos.y, 150);
        z.animate((float) pos.z, 150);
        limbTick();

        if (Math.abs(pos.x - x.get()) > 0.1F || Math.abs(pos.z - z.get()) > 0.1F) {
            staying.reset();
        }
        lay = !isTrickActive() && staying.getElapsedTime() >= 1000L;
    }

    public void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double deltaX = x.get() - pos.x;
        double deltaZ = z.get() - pos.z;
        float speed = Mth.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ)) * 4.0F;
        if (speed > 1.0F) speed = 1.0F;
        limbSwingAmount += (speed - limbSwingAmount) * 0.4F;
        limbSwing += limbSwingAmount;
    }

    private void handleRotation() {
        if (motion.x != 0.0D || motion.z != 0.0D) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float) Math.toDegrees(angle) - 90.0F;
            yaw %= 360.0F;
            if (yaw < 0.0F) yaw += 360.0F;
        }
        Vec3 current = getPos();
        if (current == null) return;
        Vec3 ownerEyes = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D);
        ScreenPoint rotation = rotationToward(current, ownerEyes);
        if (isTrickActive()) yaw = rotation.x;
        LivingEntity target = getAuraTarget();
        if (!isTrickActive() && target != null && entity instanceof LocalPlayer) {
            rotation = rotationToward(current, target.position());
        }
        float snapLimit = lay ? 200.0F : 150.0F;
        float headLimit = lay ? 100.0F : 50.0F;
        if (rotation.x - yaw < -snapLimit || rotation.x - yaw > snapLimit) yaw = rotation.x;
        float bodyDelta = ((yaw - body) % 360.0F + 540.0F) % 360.0F - 180.0F;
        if (!lay) bodyAnim.animate(body + bodyDelta, 150);
        yawAnim.animate(Mth.clamp(rotation.x - yaw, -headLimit, headLimit), 150);
        pitchAnim.animate(rotation.y, 150);
        body += bodyDelta;
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

    private static float easeOutCubic(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (float) Math.pow(1.0F - clamped, 3.0D);
    }

    private static float easeInOutCubic(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped < 0.5F ? 4.0F * clamped * clamped * clamped : 1.0F - (float) Math.pow(-2.0F * clamped + 2.0F, 3.0D) / 2.0F;
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
