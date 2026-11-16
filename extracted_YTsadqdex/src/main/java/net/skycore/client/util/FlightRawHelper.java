package net.skycore.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.EntityGetResolvedYawAccessor;
import net.skycore.client.mixin.LivingEntityGetEffectiveGravityAccessor;

public final class FlightRawHelper {
   public static Vec3 flightRaw(LocalPlayer player, Vec3 position, double value, double value2) {
      Vec3 lookAngle = player.getLookAngle();
      float xRot = player.getXRot() * (float) (Math.PI / 180.0);
      double squareRoot = Math.sqrt(lookAngle.x * lookAngle.x + lookAngle.z * lookAngle.z);
      double value3 = position.horizontalDistance();
      double gravity = player.getDeltaMovement().y <= 0.0 && player.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(player.getGravity(), 0.01) : player.getGravity();
      double computedValue = Mth.square(Math.cos(xRot));
      position = position.add(0.0, gravity * (-1.0 + computedValue * 0.75), 0.0);
      if (position.y < 0.0 && squareRoot > 0.0) {
         double y = position.y * -0.1 * computedValue;
         position = position.add(lookAngle.x * y / squareRoot, y, lookAngle.z * y / squareRoot);
      }

      if (xRot < 0.0F && squareRoot > 0.0) {
         double computedValue2 = value3 * -Mth.sin(xRot) * value2;
         position = position.add(-lookAngle.x * computedValue2 / squareRoot, computedValue2 * value, -lookAngle.z * computedValue2 / squareRoot);
      }

      if (squareRoot > 0.0) {
         position = position.add((lookAngle.x / squareRoot * value3 - position.x) * 0.1, 0.0, (lookAngle.z / squareRoot * value3 - position.z) * 0.1);
      }

      return position;
   }

   public static float yaw(LivingEntity entity) {
      EntityGetResolvedYawAccessor entityGetResolvedYawAccessor = (EntityGetResolvedYawAccessor)entity;
      return entityGetResolvedYawAccessor.skycore$getResolvedYaw();
   }

   public static float pitch(LivingEntity entity) {
      EntityGetResolvedYawAccessor entityGetResolvedYawAccessor = (EntityGetResolvedYawAccessor)entity;
      return entityGetResolvedYawAccessor.skycore$getResolvedPitch();
   }

   public static Vec3 flightStep(LocalPlayer player, Vec3 position, double value) {
      Vec3 position2 = flightRaw(player, position, value, 0.04);
      return position2.multiply(0.99F, 0.98F, 0.99F);
   }

   public static Vec3 simulate(Vec3 position, Vec3 position2, float value, double value2, boolean condition) {
      float computedValue = value * (float) (Math.PI / 180.0);
      double squareRoot = Math.sqrt(position2.x * position2.x + position2.z * position2.z);
      int value3 = squareRoot > 0.0 ? 1 : 0;
      double x = value3 != 0 ? position2.x / squareRoot : 0.0;
      double z = value3 != 0 ? position2.z / squareRoot : 0.0;
      double computedValue2 = Mth.square(Math.cos(computedValue));
      double computedValue3 = -1.0 + computedValue2 * 0.75;
      double computedValue4 = value2 * computedValue3;
      double computedValue5 = Math.min(value2, 0.01) * computedValue3;
      double computedValue6 = -0.1 * computedValue2;
      int value4 = computedValue < 0.0F && value3 != 0 ? 1 : 0;
      double computedValue7 = -Mth.sin(computedValue) * 0.04;
      double x2 = position.x;
      double y = position.y;
      double z2 = position.z;
      double value5 = 0.0;
      double value6 = 0.0;
      double value7 = 0.0;

      for (int value8 = 0; value8 < 8; value8++) {
         double squareRoot2 = Math.sqrt(x2 * x2 + z2 * z2);
         y += condition && y <= 0.0 ? computedValue5 : computedValue4;
         if (value3 != 0 && y < 0.0) {
            double computedValue8 = y * computedValue6;
            x2 += x * computedValue8;
            y += computedValue8;
            z2 += z * computedValue8;
         }

         if (value4 != 0) {
            double computedValue9 = squareRoot2 * computedValue7;
            x2 -= x * computedValue9;
            y += computedValue9 * 3.2;
            z2 -= z * computedValue9;
         }

         if (value3 != 0) {
            x2 += (x * squareRoot2 - x2) * 0.1;
            z2 += (z * squareRoot2 - z2) * 0.1;
         }

         value5 += x2 *= 0.99F;
         value6 += y = y * 0.98F;
         value7 += z2 *= 0.99F;
      }

      return new Vec3(value5, value6, value7);
   }

   public static Vec3 predictOffset(LivingEntity entity, float value) {
      if (entity != null && Float.isFinite(value) && value != 0.0F) {
         float yaw = pitch(entity);
         Vec3 position = entity.calculateViewVector(yaw, yaw(entity));
         Vec3 deltaMovement = simulate(
            entity.getDeltaMovement(),
            position,
            yaw,
            ((LivingEntityGetEffectiveGravityAccessor)entity).skycore$getEffectiveGravity(),
            entity.hasEffect(MobEffects.SLOW_FALLING)
         );
         double lengthSquared = deltaMovement.lengthSqr();
         if (Double.isFinite(lengthSquared) && !(lengthSquared < 1.0E-4)) {
            double squareRoot = Math.sqrt(lengthSquared);
            float computedValue = (float)(squareRoot / 8.0);
            return deltaMovement.scale(1.0 / squareRoot).scale(value * computedValue);
         } else {
            return Vec3.ZERO;
         }
      } else {
         return Vec3.ZERO;
      }
   }

   public static Vec3 closest(Vec3 position, AABB bounds) {
      double x = position.x();
      x = Mth.clamp(x, bounds.minX, bounds.maxX);
      double y = position.y();
      y = Mth.clamp(y, bounds.minY, bounds.maxY);
      double z = position.z();
      return new Vec3(x, y, Mth.clamp(z, bounds.minZ, bounds.maxZ));
   }

   private FlightRawHelper() {
   }

   public static boolean exceedsPredictThreshold(double value, double value2) {
      return value * 20.0 > value2;
   }
}
