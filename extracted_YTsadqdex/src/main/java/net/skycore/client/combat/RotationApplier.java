package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class RotationApplier {
   public static void applyBodyYaw(float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         instance.setYRot(value);
         instance.yHeadRot = value;
         instance.yBodyRot = value;
         instance.yRotO = value;
         instance.yHeadRotO = value;
         instance.yBodyRotO = value;
      }
   }

   public static float aimModulo360(float value, float value2) {
      float computedValue = value2 - value;
      return value + Mth.wrapDegrees(computedValue);
   }

   private RotationApplier() {
   }

   public static float applyGcdStep(float value, float result) {
      float computedValue = result - value;
      float gCDValue = MouseSensitivity.getGCDValue();
      if (gCDValue <= 0.0F) {
         return result;
      }

      computedValue = Math.round(computedValue / gCDValue) * gCDValue;
      return value + computedValue;
   }
}
