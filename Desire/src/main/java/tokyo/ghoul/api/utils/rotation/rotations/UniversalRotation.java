package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.api.utils.predict.PredictUtil;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

public class UniversalRotation extends RotationMode {

    private static float randomYaw = 0f;
    private static float randomPitch = 0f;
    private static float targetRandomYaw = 0f;
    private static float targetRandomPitch = 0f;
    private static long lastRandomUpdateTime = 0L;
    private static long lastProcessTime = 0L;

    public UniversalRotation() {
        super("ReallyWorld");
    }

    public static void reset() {
        randomYaw = 0f;
        randomPitch = 0f;
        targetRandomYaw = 0f;
        targetRandomPitch = 0f;
        lastRandomUpdateTime = 0L;
        lastProcessTime = 0L;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (mc.player == null) return targetRotation;
        if (currentRotation == null) {
            currentRotation = RotationUtil.fromVec2f(mc.player.getRotationVector());
        }

        Rotation rot;
        if (entity instanceof LivingEntity target) {
            Vec3 center = target.getBoundingBox().getCenter();
            double aimY = Mth.clamp(center.y + 0.3, target.getBoundingBox().minY + 0.1, target.getBoundingBox().maxY - 0.1);
            Vec3 aimPos = new Vec3(center.x, aimY, center.z);

            if (target.isFallFlying()) {
                Vec3 pred = AuraModule.getInstance().getPredictedElytraPos();
                if (pred != null) {
                    aimPos = pred;
                } else {
                    aimPos = PredictUtil.realPredict(target);
                }
            } else if (mc.level != null) {
                Vec3 eyePos = mc.player.getEyePosition();
                HitResult trace = mc.level.clip(new ClipContext(
                        eyePos, aimPos,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        mc.player
                ));
                if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
                    aimPos = new Vec3(target.getX(), target.getBoundingBox().maxY - 0.15, target.getZ());
                }
            }

            rot = RotationUtil.rotationAt(aimPos);
        } else if (vec3d != null) {
            rot = RotationUtil.rotationAt(vec3d);
        } else if (targetRotation != null) {
            rot = targetRotation;
        } else {
            return currentRotation;
        }

        long now = System.currentTimeMillis();
        long deltaMs = lastProcessTime == 0L ? 5L : Math.max(1L, Math.min(50L, now - lastProcessTime));
        lastProcessTime = now;

        if (now - lastRandomUpdateTime >= 75L) {
            targetRandomYaw = MathUtil.randomInRange(-2.0f, 2.0f);
            targetRandomPitch = MathUtil.randomInRange(-1.5f, 1.5f);
            lastRandomUpdateTime = now;
        }

        float lerpFactor = (float) Mth.clamp(deltaMs / 50.0f, 0.05f, 0.5f);
        randomYaw = Mth.lerp(lerpFactor, randomYaw, targetRandomYaw);
        randomPitch = Mth.lerp(lerpFactor, randomPitch, targetRandomPitch);

        float yawWithRandom = rot.getYaw() + randomYaw;
        float pitchWithRandom = rot.getPitch() + randomPitch;

        float deltaYaw = Mth.wrapDegrees(yawWithRandom - currentRotation.getYaw());
        float deltaPitch = pitchWithRandom - currentRotation.getPitch();

        float absYaw = Math.abs(deltaYaw);
        float absPitch = Math.abs(deltaPitch);

        float timeScale = Math.max(0.5f, (float) deltaMs / 4.166f);
        float maxSpeed = 5.0f * timeScale;

        float speed = Mth.clamp(absYaw, 0f, maxSpeed);
        float speed2 = Mth.clamp(absPitch, 0f, maxSpeed);

        float yawStep = Mth.clamp(deltaYaw, -speed, speed);
        float pitchStep = Mth.clamp(deltaPitch, -speed2, speed2);

        float newYaw = currentRotation.getYaw() + yawStep;
        float newPitch = currentRotation.getPitch() + pitchStep;

        double gcd = MouseUtil.getGCD();
        if (gcd > 0.0) {
            newYaw -= (newYaw - currentRotation.getYaw()) % gcd;
            newPitch -= (newPitch - currentRotation.getPitch()) % gcd;
        }

        newPitch = Mth.clamp(newPitch, -90.0f, 90.0f);

        return new Rotation(newYaw, newPitch);
    }
}
