package net.skycore.client.util;

public final class CameraYawNearBobHelper {
   public static float cameraYawNearBob(float value, float value2) {
      float computedValue = (value - value2) % 360.0F;
      if (computedValue >= 180.0F) {
         computedValue -= 360.0F;
      }

      if (computedValue < -180.0F) {
         computedValue += 360.0F;
      }

      return value2 + computedValue;
   }

   private CameraYawNearBobHelper() {
   }
}
