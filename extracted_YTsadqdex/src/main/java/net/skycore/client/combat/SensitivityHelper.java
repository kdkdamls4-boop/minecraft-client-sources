package net.skycore.client.combat;

import net.minecraft.client.Minecraft;

public final class SensitivityHelper {
   public static float getDeltaMouse(float value) {
      return Math.round(value / getGCDValue());
   }

   public static float getFixRotate(float delta) {
      float deltaMouse = getDeltaMouse(delta);
      return deltaMouse * getGCDValue();
   }

   public static float getGCDValue() {
      return getGCD() * 0.15F;
   }

   private SensitivityHelper() {
   }

   public static float getGCD() {
      Minecraft instance = Minecraft.getInstance();
      Double value = instance.options.sensitivity().get();
      double computedValue = value / 0.15 / 8.0;
      double computedValue2 = Math.cbrt(computedValue);
      float computedValue3 = (float)((computedValue2 - 0.2F) / 0.6F * 0.6F + 0.2F);
      return computedValue3 * computedValue3 * computedValue3 * 8.0F;
   }
}
