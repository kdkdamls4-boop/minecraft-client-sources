package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class MarkActionHelper {
   private static boolean pendingDelta;
   private static int nudge;
   private static float actionDelta;
   private static float lastPitch = Float.NaN;
   private static boolean actionFlag;
   private static float lastDelta;
   private static float lastYaw = Float.NaN;

   private MarkActionHelper() {
   }

   public static void markAction() {
      actionFlag = true;
   }

   public static void beforeMovementPacket() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         reset();
      } else {
         if (actionFlag && pendingDelta) {
            actionDelta = lastDelta;
            pendingDelta = false;
         }

         actionFlag = false;
         float yRot = instance.getYRot();
         float xRot = instance.getXRot();
         if (Float.isNaN(lastYaw)) {
            lastYaw = yRot;
            lastPitch = xRot;
         } else {
            if (yRot != lastYaw || xRot != lastPitch) {
               float delta = Math.abs(yRot - lastYaw);
               lastDelta = delta;
               pendingDelta = true;
               lastYaw = instance.getYRot();
               lastPitch = xRot;
            }
         }
      }
   }

   public static void reset() {
      lastYaw = Float.NaN;
      lastPitch = Float.NaN;
      actionDelta = 0.0F;
      lastDelta = 0.0F;
      actionFlag = false;
      pendingDelta = false;
   }
}
