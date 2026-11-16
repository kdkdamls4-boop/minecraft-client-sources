package net.skycore.client.util;

public final class ClampByteHelper {
   public static long clamp(long timestamp, long timestamp2, long timestamp3) {
      long minimum = Math.min(timestamp3, timestamp);
      return Math.max(timestamp2, minimum);
   }

   public static int clampByte(int value) {
      return clamp(value, 0, 255);
   }

   public static double clamp01(double value) {
      return clamp(value, 0.0, 1.0);
   }

   public static float easeOutCubic(float progress) {
      float value = clamp01(progress);
      return 1.0F - (float)Math.pow(1.0F - value, 3.0);
   }

   public static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   public static double clamp(double value, double value2, double value3) {
      double minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public static int clamp(int value, int value2, int value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   public static double lerp(double value, double value2, double value3) {
      return value + (value2 - value) * value3;
   }

   public static double easeOutCubic(double progress) {
      double value = clamp01(progress);
      double computedValue = 1.0 - value;
      return 1.0 - Math.pow(computedValue, 3.0);
   }

   private ClampByteHelper() {
   }

   public static float clamp01(float value) {
      return clamp(value, 0.0F, 1.0F);
   }

   public static float lerp(float value, float value2, float value3) {
      return value + (value2 - value) * value3;
   }
}
