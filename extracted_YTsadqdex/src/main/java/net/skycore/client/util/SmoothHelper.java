package net.skycore.client.util;

public final class SmoothHelper {
   public static float smooth(float value, float value2) {
      float clampedValue = Math.clamp(value, 0.0F, 1.0F);
      if (!(value2 <= 0.0F) && clampedValue != 0.0F && clampedValue != 1.0F) {
         double intermediate = clampedValue * Math.PI;
         float computedValue = (float)(0.5 - 0.5 * Math.cos(intermediate));
         return clampedValue + Math.clamp(value2, 0.0F, 1.0F) * (computedValue - clampedValue);
      } else {
         return clampedValue;
      }
   }

   private SmoothHelper() {
   }
}
