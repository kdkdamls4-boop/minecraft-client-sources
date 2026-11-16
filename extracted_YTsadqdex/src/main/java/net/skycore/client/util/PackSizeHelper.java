package net.skycore.client.util;

public final class PackSizeHelper {
   static final float MAX_DUAL_RADIUS = 255.9375F;
   static final float SIZE_SCALE = 8.0F;
   static final float RADIUS_SCALE = 16.0F;
   private static final int MODE_FLAG = 4096;

   public static int packSize(float value) {
      float computedValue = value * 8.0F;
      return ClampByteHelper.clamp(Math.round(computedValue), 0, 32767);
   }

   public static float packZ(float value, int value2, boolean condition) {
      float computedValue = value * 16.0F;
      int computedValue2 = ClampByteHelper.clamp(Math.round(computedValue), 0, 4095);
      if (condition) {
         computedValue2 |= 4096;
      }

      return computedValue2 * 256.0F + ClampByteHelper.clampByte(value2);
   }

   public static float snormChannel(int value) {
      return value / 127.5F - 1.0F;
   }

   private PackSizeHelper() {
   }

   public static int packRadius(float value) {
      return ClampByteHelper.clamp(Math.round(value * 16.0F), 0, 32767);
   }

   public static int packU8Pair(int value, int value2) {
      int value3 = 0;
      value3 = ClampByteHelper.clampByte(value);
      return value3 | ClampByteHelper.clampByte(value2) << 8;
   }

   public static float packDual12(float value, float value2) {
      int computedValue = ClampByteHelper.clamp(Math.round(value * 16.0F), 0, 4095);
      int computedValue2 = ClampByteHelper.clamp(Math.round(value2 * 16.0F), 0, 4095);
      return computedValue * 4096.0F + computedValue2;
   }

   public static float packDual12Raw(int value, int value2) {
      return ClampByteHelper.clamp(value, 0, 4095) * 4096.0F + ClampByteHelper.clamp(value2, 0, 4095);
   }

   public static int packSignedSize(float value) {
      int value2 = 0;
      float computedValue = value * 8.0F;
      value2 = Math.round(computedValue);
      return ClampByteHelper.clamp(value2, -32768, 32767);
   }
}
