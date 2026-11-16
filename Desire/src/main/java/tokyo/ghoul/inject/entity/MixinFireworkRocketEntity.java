package tokyo.ghoul.inject.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tokyo.ghoul.api.event.events.player.move.FireworkRocketEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void boostRedirect(LivingEntity entity, Vec3 velocity) {
        if (!entity.isFallFlying() || entity != SharedClass.player()) {
            entity.setDeltaMovement(velocity);
            return;
        }

        FireworkRocketEvent event = FireworkRocketEvent.getInstance();
        event.setSpeed(1.5f);
        event.setYSpeed(1.5f);
        if (event.call()) {
            entity.setDeltaMovement(velocity);
            return;
        }

        Vec3 silent = silentLook(entity);
        Vec3 dir = silent != null ? silent : entity.getLookAngle();
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.add(
                dir.x * 0.1 + (dir.x * event.getSpeed() - motion.x) * 0.5,
                dir.y * 0.1 + (dir.y * event.getYSpeed() - motion.y) * 0.5,
                dir.z * 0.1 + (dir.z * event.getSpeed() - motion.z) * 0.5
        ));
    }

    private static Vec3 silentLook(LivingEntity entity) {
        if (entity != SharedClass.player()) return null;
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager.getCurrentRotationPlan() == null) return null;
        return rotationManager.getRotation().getVector();
    }
}
