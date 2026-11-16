package net.skycore.client.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AimMath implements AttackState {
   private static Vec3 aimPoint(LivingEntity entity) {
      return entity.position().add(0.0, entity.getBbHeight() * 0.85, 0.0);
   }

   @Override
   public void tick(LocalPlayer player, LivingEntity entity, Vec3 position, boolean condition) {
      if (player == null || entity == null || !entity.isAlive()) {
         this.reset();
      } else if (!condition) {
         if (RotationController.isActive()) {
            RotationController.disengage();
         }
      } else {
         Vec3 position2 = aimPoint(entity);
         Vec3 eyePosition = position2.subtract(player.getEyePosition());
         double squareRoot = Math.sqrt(eyePosition.x * eyePosition.x + eyePosition.z * eyePosition.z);
         float angle = (float)Math.toDegrees(Math.atan2(eyePosition.z, eyePosition.x)) - 90.0F;
         float angle2 = Mth.clamp((float)(-Math.toDegrees(Math.atan2(eyePosition.y, squareRoot))), -90.0F, 90.0F);
         RotationController.setRotation(angle, angle2);
      }
   }

   @Override
   public void reset() {
      if (RotationController.isActive()) {
         RotationController.disengage();
      }
   }
}
