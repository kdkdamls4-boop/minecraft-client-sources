package net.skycore.client.util;

import net.minecraft.client.Minecraft;

public final class TruncateGcdHelper {
   public static float step() {
      Minecraft instance = Minecraft.getInstance();
      Double value = instance.options.sensitivity().get();
      double value2 = value;
      float computedValue = (float)(Math.pow(value2 * 0.6 + 0.2, 3.0) * 8.0);
      return (float)(computedValue * 0.15);
   }

   public static float truncateGcd(float value) {
      float value2 = step();
      return value2 == 0.0F ? value : value - value % value2;
   }

   public static float quantize(float value) {
      float computedValue = value / step();
      return Math.round(computedValue);
   }

   private TruncateGcdHelper() {
   }

   public static float gcdStep() {
      return applyGcd(1.0F);
   }

   public static float applyGcd(float value) {
      return quantize(value) * step();
   }
}
