package net.skycore.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class EasedFactorHelper {
   private static final int HURT_COLOR = -44459;
   private static final float HURT_TICKS = 10.0F;

   public static float scale(Entity entity, float value) {
      float value2 = easedFactor(entity);
      return 1.0F + value2 * Math.max(0.0F, value);
   }

   public static int blend(int alpha, float value, float alpha2) {
      float clampedValue = Mth.clamp(value, 0.0F, 1.0F);
      int result = WriteHexByteHelper.multiplyAlpha(alpha, alpha2);
      if (clampedValue <= 0.0F) {
         return result;
      }

      int computedValue = WriteHexByteHelper.multiplyAlpha(-44459, alpha2);
      return WriteHexByteHelper.lerp(result, computedValue, clampedValue);
   }

   public static float easedFactor(Entity progress) {
      float value = factor(progress);
      return 1.0F - (1.0F - value) * (1.0F - value);
   }

   public static int blend(int value, Entity entity, float value2) {
      float value3 = easedFactor(entity);
      return blend(value, value3, value2);
   }

   private EasedFactorHelper() {
   }

   public static float factor(Entity entity) {
      if (entity instanceof LivingEntity entity2 && entity2.hurtTime > 0) {
         return Mth.clamp(entity2.hurtTime / 10.0F, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }
}
