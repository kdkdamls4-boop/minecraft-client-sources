package net.skycore.client.util;

import net.minecraft.util.Mth;

public final class AssemblyHelper {
   public static float assembly(float value, int value2, int value3) {
      float computedValue = value3 <= 1 ? 0.0F : (float)value2 / (value3 - 1);
      float computedValue2 = computedValue * 0.22F;
      float intermediate = (value - computedValue2) / (1.0F - computedValue2);
      float clampedValue = Mth.clamp(intermediate, 0.0F, 1.0F);
      return clampedValue * clampedValue * (3.0F - 2.0F * clampedValue);
   }

   private AssemblyHelper() {
   }

   public static double scatter(int value, int value2, double value3, float value4) {
      int computedValue = value * -1640531527 + value2 * 2135587861;
      computedValue ^= computedValue >>> 16;
      computedValue *= -2048144789;
      computedValue ^= computedValue >>> 13;
      float computedValue2 = (computedValue >>> 8 & 65535) / 32767.5F - 1.0F;
      return computedValue2 * value3 * (1.0F - value4);
   }
}
