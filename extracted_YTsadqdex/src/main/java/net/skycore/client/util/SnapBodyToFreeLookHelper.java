package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.combat.RotationController;

public final class SnapBodyToFreeLookHelper {
   private static float freePitch;
   private static float freeYaw;
   private static boolean captured;
   private static boolean rotationLock;

   private static void snapBodyToFreeLook() {
   }

   public static void captureFromFreeLook() {
      float freeYaw2 = RotationController.getFreeYaw();
      freeYaw = freeYaw2;
      freePitch = RotationController.getFreePitch();
      captured = true;
   }

   public static void captureFromPlayer() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (RotationController.isActive() && !RotationController.isClientLook()) {
            captureFromFreeLook();
         } else {
            float yRot = instance.getYRot();
            freeYaw = yRot;
            freePitch = instance.getXRot();
            captured = true;
         }
      }
   }

   public static boolean hasCaptured() {
      return captured;
   }

   public static void reset() {
      rotationLock = false;
      captured = false;
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         freeYaw = player.getYRot();
         float xRot = player.getXRot();
         freePitch = xRot;
         captured = true;
      }
   }

   private SnapBodyToFreeLookHelper() {
   }

   public static float getPitch() {
      if (RotationController.isActive()) {
         if (!RotationController.isClientLook()) {
            return RotationController.getFreePitch();
         }
      }

      return freePitch;
   }

   public static float getYaw() {
      float yaw;
      if (RotationController.isActive() && !RotationController.isClientLook()) {
         yaw = RotationController.getFreeYaw();
      } else {
         yaw = freeYaw;
      }

      return yaw;
   }

   public static void tickIdleFollow() {
      if (!rotationLock && !QueuePendingHelper.ownsRotationLoop()) {
         if (RotationController.isActive() && !RotationController.isClientLook()) {
            freeYaw = RotationController.getFreeYaw();
            freePitch = RotationController.getFreePitch();
            captured = true;
         } else {
            LocalPlayer instance = Minecraft.getInstance().player;
            if (instance != null) {
               freeYaw = instance.getYRot();
               freePitch = instance.getXRot();
               captured = true;
            }
         }
      }
   }

   public static FromFreeLookHelper snapshot() {
      float yaw = getYaw();
      float pitch = getPitch();
      return new FromFreeLookHelper(yaw, pitch);
   }

   public static void setRotationLock(boolean rotationLock2) {
      if (rotationLock != rotationLock2) {
         rotationLock = rotationLock2;
         if (rotationLock2) {
            snapBodyToFreeLook();
         }
      }
   }
}
