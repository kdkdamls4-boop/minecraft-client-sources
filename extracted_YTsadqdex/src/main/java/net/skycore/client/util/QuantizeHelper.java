package net.skycore.client.util;

public final class QuantizeHelper {
   public static float quantize(float value, double value2) {
      return (float)(Math.round(value / (value2 * 0.15F)) * value2) * 0.15F;
   }

   private QuantizeHelper() {
   }
}
