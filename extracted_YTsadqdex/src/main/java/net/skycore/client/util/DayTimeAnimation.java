package net.skycore.client.util;

public final class DayTimeAnimation {
   private static final double DAY_LENGTH = 24000.0;

   public static long cycle(double value, double value2, double value3, double value4, double value5) {
      double maximum = Math.max(0.001, value4);
      double computedValue = Math.max(0.0, value3) * Math.max(0.0, value5) / maximum;
      double value6 = computedValue % 2.0;
      double computedValue2 = value6 <= 1.0 ? value6 : 2.0 - value6;
      double computedValue3 = computedValue2 * computedValue2 * (3.0 - 2.0 * computedValue2);
      return normalize(value + shortestDifference(value, value2) * computedValue3);
   }

   public static long advance(double value, double value2, double value3) {
      return normalize(value + Math.max(0.0, value3) * value2 * 20.0);
   }

   private DayTimeAnimation() {
   }

   public static long withCelestialRotation(double value, double value2) {
      double computedValue = value + value2 / 360.0 * 24000.0;
      return normalize(computedValue);
   }

   public static double shortestDifference(double value, double value2) {
      return (value2 - value % 24000.0 + 36000.0) % 24000.0 - 12000.0;
   }

   public static long normalize(double value) {
      long timestamp = (long)Math.floor(value) % 24000L;
      return timestamp < 0L ? timestamp + 24000L : timestamp;
   }
}
