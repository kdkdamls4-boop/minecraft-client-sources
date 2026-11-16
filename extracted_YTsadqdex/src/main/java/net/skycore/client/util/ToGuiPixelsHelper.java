package net.skycore.client.util;

public final class ToGuiPixelsHelper {
   public static int toGuiPixels(int value, double value2) {
      int value3 = 0;
      value3 = (byte)0;
      validateGuiScale(value2);
      if (value == 0) {
         return 0;
      }

      double computedValue = value / value2;
      int roundedValue = (int)Math.round(computedValue);
      if (value > 0) {
         value3 = Math.max(1, roundedValue);
      } else {
         value3 = Math.min(-1, roundedValue);
      }

      return value3;
   }

   private static void validateGuiScale(double value) {
      if (!Double.isFinite(value) || value <= 0.0) {
         throw new IllegalArgumentException("GUI scale must be finite and positive");
      }
   }

   private ToGuiPixelsHelper() {
   }

   public static float toGuiPixels(float value, double value2) {
      validateGuiScale(value2);
      return (float)(value / value2);
   }
}
