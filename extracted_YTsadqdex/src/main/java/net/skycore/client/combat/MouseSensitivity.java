package net.skycore.client.combat;

import net.minecraft.client.Minecraft;

public final class MouseSensitivity {
   public static float getGCDValue() {
      return (float)(getGCD() * 0.15);
   }

   public static float getSens(float delta) {
      return getDeltaMouse(delta) * getGCDValue();
   }

   public static float getDeltaMouse(float value) {
      float gCDValue = getGCDValue();
      float result;
      if (gCDValue == 0.0F) {
         result = 0.0F;
      } else {
         float computedValue = value / gCDValue;
         result = Math.round(computedValue);
      }

      return result;
   }

   private MouseSensitivity() {
   }

   public static float getGCD() {
      Double instance = Minecraft.getInstance().options.sensitivity().get();
      float computedValue = (float)(instance * 0.6 + 0.2);
      return computedValue * computedValue * computedValue * 8.0F;
   }
}
