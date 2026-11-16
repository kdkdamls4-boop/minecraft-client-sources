package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ClientTickEvent;
import net.skycore.client.event.EventBus;

public final class FreeLookController {
   private static boolean tickHooked;
   private static float prevVisualBodyYaw;
   private static float visualBodyYaw;

   public static void tickVisualBody() {
      prevVisualBodyYaw = visualBodyYaw;
      float freeYaw = getFreeYaw();
      float computedValue = freeYaw - visualBodyYaw;
      float computedValue2 = Mth.wrapDegrees(computedValue);
      visualBodyYaw += computedValue2 * 0.3F;
      float computedValue3 = freeYaw - visualBodyYaw;
      float computedValue4 = Mth.wrapDegrees(computedValue3);
      if (Math.abs(computedValue4) > 50.0F) {
         visualBodyYaw = visualBodyYaw + (computedValue4 - Math.signum(computedValue4) * 50.0F);
      }

      float yaw = Mth.wrapDegrees(visualBodyYaw);
      visualBodyYaw = yaw;
   }

   public static float getVisualBodyYaw(float yaw) {
      return Mth.rotLerp(yaw, prevVisualBodyYaw, visualBodyYaw);
   }

   private FreeLookController() {
   }

   private static void ensureTick() {
      if (!tickHooked) {
         tickHooked = true;
         EventBus eventBus = SkycoreClient.events();
         eventBus.subscribe(ClientTickEvent.class, clientTickEvent -> {
            tick();
         });
      }
   }

   public static boolean isActive() {
      if (RotationController.isActive()) {
         if (!RotationController.isClientLook()) {
            return true;
         }
      }

      return false;
   }

   private static void tick() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (!isActive()) {
            float yBodyRot = instance.yBodyRot;
            visualBodyYaw = instance.yBodyRot;
            prevVisualBodyYaw = yBodyRot;
         } else {
            tickVisualBody();
         }
      }
   }

   public static float getVisualBodyYaw() {
      return visualBodyYaw;
   }

   public static void setActive(boolean active) {
      ensureTick();
      if (active) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && !RotationController.isActive()) {
            RotationController.syncFreeLook(instance.getYRot(), instance.getXRot());
            RotationController.activateFreeLook();
            prevVisualBodyYaw = visualBodyYaw = instance.yBodyRot;
         }
      } else {
         RotationController.releaseToFreeLook();
         LocalPlayer instance2 = Minecraft.getInstance().player;
         if (instance2 != null) {
            prevVisualBodyYaw = visualBodyYaw = instance2.yBodyRot;
         }
      }
   }

   public static float getFreePitch() {
      return RotationController.getFreePitch();
   }

   public static void setFreePitch(float freePitch) {
      ensureTick();
      RotationController.setFreePitch(Mth.clamp(freePitch, -90.0F, 90.0F));
   }

   public static float getFreeYaw() {
      return RotationController.getFreeYaw();
   }

   public static void setFreeYaw(float freeYaw) {
      ensureTick();
      RotationController.setFreeYaw(freeYaw);
   }
}
