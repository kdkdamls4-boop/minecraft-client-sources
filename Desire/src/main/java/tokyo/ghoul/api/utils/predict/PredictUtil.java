package tokyo.ghoul.api.utils.predict;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import net.minecraft.world.entity.player.Player;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

@UtilityClass
public class PredictUtil implements QuickImports {

    public enum ElytraPredictType {
        DEFAULT,
        LIMIT
    }

    public Vec3 predictElytra(LivingEntity entity, float minusTicks, ElytraPredictType type) {
        if (!(entity instanceof Player player)) return entity.position();

        Vec3 basePos = player.position();
        Vec3 velocity = new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);

        float ticks = (float) (mc.player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) * minusTicks);

        int ping = 50;
        if (mc.player != null && mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
            ping = mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency();
        }
        ticks += (float) ping / 15.0F;

        if (type == ElytraPredictType.LIMIT) {
            ticks = Mth.clamp(ticks, 0.0F, 2.7F);
        }

        float deltaYaw = Mth.wrapDegrees(player.getYRot() - player.yRotO);
        float deltaPitch = Mth.wrapDegrees(player.getXRot() - player.xRotO);

        Vec3 predictedPos = basePos;
        int intTicks = (int) ticks;
        float frac = ticks - intTicks;

        for (int i = 0; i < intTicks; i++) {
            double speed = velocity.length();
            if (speed > 0.001) {
                double velYaw = Math.atan2(-velocity.x, velocity.z);
                double velPitch = -Math.atan2(velocity.y, Math.hypot(velocity.x, velocity.z));

                velYaw += Math.toRadians(deltaYaw);
                velPitch += Math.toRadians(deltaPitch);

                velPitch = Mth.clamp(velPitch, -Math.PI / 2.0, Math.PI / 2.0);

                velocity = new Vec3(
                        -Math.sin(velYaw) * Math.cos(velPitch) * speed,
                        -Math.sin(velPitch) * speed,
                        Math.cos(velYaw) * Math.cos(velPitch) * speed
                );
            }
            predictedPos = predictedPos.add(velocity);
        }

        if (frac > 0) {
            double speed = velocity.length();
            if (speed > 0.001) {
                double velYaw = Math.atan2(-velocity.x, velocity.z);
                double velPitch = -Math.atan2(velocity.y, Math.hypot(velocity.x, velocity.z));

                velYaw += Math.toRadians(deltaYaw * frac);
                velPitch += Math.toRadians(deltaPitch * frac);

                velPitch = Mth.clamp(velPitch, -Math.PI / 2.0, Math.PI / 2.0);

                velocity = new Vec3(
                        -Math.sin(velYaw) * Math.cos(velPitch) * speed,
                        -Math.sin(velPitch) * speed,
                        Math.cos(velYaw) * Math.cos(velPitch) * speed
                );
            }
            predictedPos = predictedPos.add(velocity.scale(frac));
        }

        return predictedPos;
    }

    public Vec3 realPredict(LivingEntity entity) {
        if (entity == null || mc.player == null) return Vec3.ZERO;
        ElytraPredictType type = ElytraPredictType.DEFAULT;
        if (AuraModule.getInstance().getElytraPredictMode() != null && AuraModule.getInstance().getElytraPredictMode().is("Легитный")) {
            type = ElytraPredictType.LIMIT;
        }
        Vec3 predict = predictElytra(entity, 0.0F, type);
        float minusTicks = (float) Mth.clamp(mc.player.getEyePosition().distanceTo(predict) / 100.0, 0.0, 12.0);
        minusTicks = Math.max(minusTicks, 0.0F);
        return predictElytra(entity, minusTicks, type);
    }

    public Vec3 getPredicted(LivingEntity entity, float ticks) {
        return predict(entity, entity.position(), ticks);
    }

    public Vec3 predict(LivingEntity entity, Vec3 pos, float ticks) {
        if (Math.hypot(entity.getX() - entity.xo, entity.getZ() - entity.zo) * 20.0 <= 5.0
                && (entity.getY() - entity.yo) * 20.0 <= 5.0) {
            return pos;
        }

        float f2 = (entity.getXRot() + (entity.xRotO - entity.getXRot())) * 0.017453292F;
        float g = -(entity.getYRot() + (entity.yHeadRotO - entity.getYRot())) * 0.017453292F;
        float h2 = Mth.cos(g);
        float i2 = Mth.sin(g);
        float j = Mth.cos(f2);
        float k = Mth.sin(f2);

        Vec3 oldVelocity = entity.getDeltaMovement();
        Vec3 lookVec = new Vec3(i2 * j, -k, h2 * j);

        float f = (float) (entity.getXRot() * 0.017453293005625408);
        double d = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double e = oldVelocity.horizontalDistance();
        boolean slowFalling = entity.getDeltaMovement().y <= 0.0;
        double gravity = slowFalling && entity.hasEffect(MobEffects.SLOW_FALLING)
                ? Math.min(entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY), 0.01)
                : entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);

        double h = Mth.square((float) Math.cos(f));
        oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + h * 0.75), 0.0);

        double i;
        if (oldVelocity.y < 0.0 && d > 0.0) {
            i = oldVelocity.y * -0.1 * h;
            oldVelocity = oldVelocity.add(lookVec.x * i / d, i, lookVec.z * i / d);
        }

        if (f < 0.0F && d > 0.0) {
            i = e * (-Mth.sin(f)) * 0.04;
            oldVelocity = oldVelocity.add(-lookVec.x * i / d, i * 3.2, -lookVec.z * i / d);
        }

        if (d > 0.0) {
            oldVelocity = oldVelocity.add((lookVec.x / d * e - oldVelocity.x) * 0.1, 0.0, (lookVec.z / d * e - oldVelocity.z) * 0.1);
        }

        Vec3 totalMotion = oldVelocity.multiply(0.9900000095367432, 0.9800000190734863, 0.9900000095367432);
        return pos.add(totalMotion.scale(ticks)).add(0.0, 0.3499999940395355, 0.0);
    }

    public Vec3 getMovement(LivingEntity entity) {
        if (entity == null) return Vec3.ZERO;

        Vec3 tickMotion = new Vec3(
                entity.getX() - entity.xo,
                entity.getY() - entity.yo,
                entity.getZ() - entity.zo);
        Vec3 velocity = entity.getDeltaMovement();
        if (tickMotion.lengthSqr() < 1.0E-6) {
            return velocity;
        }
        if (velocity.lengthSqr() > 1.0E-6) {
            return tickMotion.scale(0.72).add(velocity.scale(0.28));
        }
        return tickMotion;
    }
}
