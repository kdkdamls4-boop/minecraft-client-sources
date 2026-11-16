package net.skycore.client.util;

public final class ResetSpeedHelper {
   private static volatile float speed = 1.0F;

   public static void setSpeed(float speed2) {
      float speed3;
      if (Float.isFinite(speed2)) {
         speed3 = Math.max(0.05F, speed2);
      } else {
         speed3 = 1.0F;
      }

      speed = speed3;
   }

   private ResetSpeedHelper() {
   }

   public static float getSpeed() {
      return speed;
   }

   public static void resetSpeed() {
      speed = 1.0F;
   }
}
