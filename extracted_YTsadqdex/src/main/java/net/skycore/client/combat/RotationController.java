package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RotationController {
   private static float lastBodyYaw;
   private static boolean clientLook;
   private static float lastHeadYaw;
   private static float previousServerYaw;
   private static float lastPitch;
   private static float freeYaw;
   private static float visualBodyYaw;
   private static float serverYaw;
   private static float lastYaw;
   private static float freePitch;
   private static boolean visualBodyInitialized;
   private static float freeYawO;
   private static float freePitchO;
   private static boolean active;
   private static float previousVisualBodyYaw;
   private static float previousServerPitch;
   private static float serverPitch;
   private static boolean hasLastAngle;

   public static void apply(float yaw, float pitch) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (!active) {
            freeYaw = instance.getYRot();
            freePitch = instance.getXRot();
            freeYawO = freeYaw;
            freePitchO = freePitch;
            serverYaw = instance.getYRot();
            serverPitch = instance.getXRot();
            active = true;
         }

         rememberRenderAngles(instance);
         serverYaw += yaw;
         serverPitch = Mth.clamp(serverPitch + pitch, -90.0F, 90.0F);
         instance.setYRot(serverYaw);
         instance.setXRot(serverPitch);
      }
   }

   public static float getRenderServerPitch(float coordinate) {
      float clampedValue = Mth.clamp(coordinate, 0.0F, 1.0F);
      return Mth.lerp(clampedValue, previousServerPitch, serverPitch);
   }

   public static float getServerYaw() {
      return serverYaw;
   }

   public static boolean isClientLook() {
      return clientLook;
   }

   public static float getFreeYaw() {
      return freeYaw;
   }

   public static void clear() {
      LocalPlayer instance = Minecraft.getInstance().player;
      active = false;
      hasLastAngle = false;
      clientLook = false;
      if (instance != null) {
         if (freeYaw != 0.0F || freePitch != 0.0F) {
            instance.setYRot(freeYaw);
            instance.setXRot(freePitch);
         }

         serverYaw = instance.getYRot();
         serverPitch = instance.getXRot();
         freeYaw = instance.getYRot();
         freePitch = instance.getXRot();
         freeYawO = freeYaw;
         freePitchO = freePitch;
         visualBodyYaw = previousVisualBodyYaw = instance.yBodyRot;
         visualBodyInitialized = true;
      }
   }

   public static void beginTick() {
      freeYawO = freeYaw;
      freePitchO = freePitch;
      previousServerYaw = serverYaw;
      previousServerPitch = serverPitch;
      tickVisualBody();
   }

   public static void applyRenderInterpolation() {
      if (active) {
         if (hasLastAngle) {
            if (clientLook) {
               Minecraft instance = Minecraft.getInstance();
               LocalPlayer player = instance.player;
               if (player == null) {
                  hasLastAngle = false;
                  return;
               }

               player.yRotO = lastYaw;
               player.xRotO = lastPitch;
               player.yHeadRotO = lastHeadYaw;
               player.yBodyRotO = lastBodyYaw;
               return;
            }
         }
      }
   }

   public static void syncServerFromPlayer() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         serverYaw = instance.getYRot();
         serverPitch = instance.getXRot();
      }
   }

   public static float getFovToEntity(LocalPlayer player, Entity entity) {
      if (player != null && entity != null) {
         double x = entity.getX() - player.getX();
         double z = entity.getZ();
         double z2 = z - player.getZ();
         double angleRadians = Math.atan2(z2, x);
         float yaw = (float)(Math.toDegrees(angleRadians) - 90.0);
         float yRot = Mth.wrapDegrees(yaw - (active ? serverYaw : player.getYRot()));
         return Math.abs(yRot);
      } else {
         return 180.0F;
      }
   }

   public static void setClientLook(boolean clientLook2) {
      clientLook = clientLook2;
   }

   public static float getServerPitch() {
      return serverPitch;
   }

   private static void tickVisualBody() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         visualBodyInitialized = false;
      } else {
         float yRot = active && !clientLook ? serverYaw : instance.getYRot();
         if (!visualBodyInitialized) {
            visualBodyYaw = instance.yBodyRot;
            previousVisualBodyYaw = visualBodyYaw;
            visualBodyInitialized = true;
         } else {
            previousVisualBodyYaw = visualBodyYaw;
         }

         float computedValue = Mth.wrapDegrees(yRot - visualBodyYaw);
         visualBodyYaw += computedValue * 0.3F;
         float computedValue2 = Mth.wrapDegrees(yRot - visualBodyYaw);
         if (Math.abs(computedValue2) > 50.0F) {
            visualBodyYaw = visualBodyYaw + (computedValue2 - Math.copySign(50.0F, computedValue2));
         }

         visualBodyYaw = Mth.wrapDegrees(visualBodyYaw);
      }
   }

   public static void deactivateKeepingAim() {
      active = false;
      clientLook = false;
      hasLastAngle = false;
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         serverYaw = player.getYRot();
         float xRot = player.getXRot();
         serverPitch = xRot;
      }
   }

   public static void disengage() {
      if (active) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance == null) {
            clear();
         } else {
            float computedValue = Mth.wrapDegrees(freeYaw - serverYaw);
            float computedValue2 = freePitch - serverPitch;
            if (Math.abs(computedValue) < 2.0F && Math.abs(computedValue2) < 2.0F) {
               instance.setYRot(freeYaw);
               instance.setXRot(freePitch);
               clear();
            } else {
               rememberRenderAngles(instance);
               serverYaw += computedValue * 0.4F;
               serverPitch = Mth.clamp(serverPitch + computedValue2 * 0.4F, -90.0F, 90.0F);
               instance.setYRot(serverYaw);
               instance.setXRot(serverPitch);
            }
         }
      }
   }

   public static void setFreeYaw(float freeYaw2) {
      freeYaw = freeYaw2;
   }

   public static boolean isActive() {
      return active;
   }

   public static void syncFreeLook(float yaw, float pitch) {
      if (!active) {
         freeYawO = freeYaw;
         freePitchO = freePitch;
         freeYaw = yaw;
         freePitch = pitch;
      }
   }

   public static float getFreePitch() {
      return freePitch;
   }

   public static void resyncAfterTeleport() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         hasLastAngle = false;
      } else {
         hasLastAngle = false;
         if (active && !clientLook) {
            freeYawO = freeYaw;
            freePitchO = freePitch;
            instance.yRotO = freeYaw;
            instance.xRotO = freePitch;
         } else {
            float yRot = instance.getYRot();
            float xRot = instance.getXRot();
            freeYawO = yRot;
            freeYaw = yRot;
            freePitchO = xRot;
            freePitch = xRot;
            instance.yRotO = yRot;
            instance.xRotO = xRot;
         }

         instance.yBobO = instance.yBob;
         instance.xBobO = instance.xBob;
      }
   }

   public static float getRenderServerYaw(float coordinate) {
      return previousServerYaw + Mth.wrapDegrees(serverYaw - previousServerYaw) * Mth.clamp(coordinate, 0.0F, 1.0F);
   }

   public static float getFreeYaw(float value) {
      if (value == 1.0F) {
         return freeYaw;
      } else {
         float computedValue = freeYaw - freeYawO;
         return freeYawO + Mth.wrapDegrees(computedValue) * value;
      }
   }

   public static void activatePreservingFreeLook() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && !active) {
         freeYawO = freeYaw;
         freePitchO = freePitch;
         serverYaw = instance.getYRot();
         serverPitch = instance.getXRot();
         previousServerYaw = serverYaw;
         previousServerPitch = serverPitch;
         active = true;
         clientLook = false;
      }
   }

   public static void releaseToFreeLook() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && active) {
         float freeYaw2 = freeYaw;
         float freePitch2 = freePitch;
         instance.setYRot(freeYaw2);
         instance.setXRot(freePitch2);
         instance.yHeadRot = freeYaw2;
         instance.yBodyRot = freeYaw2;
         serverYaw = freeYaw2;
         serverPitch = freePitch2;
      }

      active = false;
      clientLook = false;
      hasLastAngle = false;
   }

   public static void setFreePitch(float freePitch2) {
      float clampedValue = Mth.clamp(freePitch2, -90.0F, 90.0F);
      freePitch = clampedValue;
   }

   public static void rotateTo(LocalPlayer player, Entity entity) {
      if (player != null && entity != null) {
         rotateToPosition(player, entity.getEyePosition());
      }
   }

   public static void activateFreeLook() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (!active) {
            freeYaw = player.getYRot();
            freePitch = player.getXRot();
            freeYawO = freeYaw;
            freePitchO = freePitch;
            float yRot = player.getYRot();
            serverYaw = yRot;
            serverPitch = player.getXRot();
            previousServerYaw = serverYaw;
            previousServerPitch = serverPitch;
            active = true;
            return;
         }
      }
   }

   private static void rememberRenderAngles(LocalPlayer player) {
      lastYaw = player.getYRot();
      float xRot = player.getXRot();
      lastPitch = xRot;
      lastHeadYaw = player.yHeadRot;
      lastBodyYaw = player.yBodyRot;
      hasLastAngle = true;
   }

   public static float getVisualBodyYaw(float value) {
      float computedValue = visualBodyYaw - previousVisualBodyYaw;
      float computedValue2 = Mth.wrapDegrees(computedValue);
      return previousVisualBodyYaw + computedValue2 * Mth.clamp(value, 0.0F, 1.0F);
   }

   private RotationController() {
   }

   public static float renderPitch(Entity entity, float pitch) {
      if (active) {
         Minecraft instance = Minecraft.getInstance();
         if (entity == instance.player) {
            return getFreePitch(pitch);
         }
      }

      return entity == null ? 0.0F : entity.getViewXRot(pitch);
   }

   public static void rotateToPosition(LocalPlayer player, Vec3 position) {
      if (player != null && position != null) {
         Vec3 eyePosition = player.getEyePosition();
         Vec3 position2 = position.subtract(eyePosition);
         double x = position2.x * position2.x + position2.z * position2.z;
         double squareRoot = Math.sqrt(x);
         float angle = (float)Math.toDegrees(Math.atan2(position2.z, position2.x)) - 90.0F;
         float angle2 = (float)(-Math.toDegrees(Math.atan2(position2.y, squareRoot)));
         setRotation(angle, angle2);
      }
   }

   public static void applyExact(float yaw, float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (!active) {
            freeYaw = instance.getYRot();
            freePitch = instance.getXRot();
            freeYawO = freeYaw;
            freePitchO = freePitch;
            previousServerYaw = instance.getYRot();
            previousServerPitch = instance.getXRot();
            active = true;
         }

         rememberRenderAngles(instance);
         serverYaw = yaw;
         serverPitch = Mth.clamp(value, -90.0F, 90.0F);
         instance.setYRot(serverYaw);
         instance.setXRot(serverPitch);
      }
   }

   public static void setRotation(float rotation, float rotation2) {
      applyExact(rotation, rotation2);
   }

   public static float renderYaw(Entity entity, float yaw) {
      if (active) {
         Minecraft instance = Minecraft.getInstance();
         if (entity == instance.player) {
            return getFreeYaw(yaw);
         }
      }

      float result;
      if (entity == null) {
         result = 0.0F;
      } else {
         result = entity.getViewYRot(yaw);
      }

      return result;
   }

   public static float getFreePitch(float pitch) {
      if (pitch == 1.0F) {
         return freePitch;
      }

      return Mth.lerp(pitch, freePitchO, freePitch);
   }

   public static void syncEntityRenderRotations(LocalPlayer player) {
      if (player != null && active) {
         if (!clientLook) {
            player.yRotO = freeYaw;
            player.xRotO = freePitch;
            hasLastAngle = false;
            return;
         }
      }
   }

   public static boolean onMouseTurn(double value, double value2) {
      if (active && !clientLook) {
         freeYaw += (float)(value * 0.15);
         float computedValue = freePitch + (float)(value2 * 0.15);
         float clampedValue = Mth.clamp(computedValue, -90.0F, 90.0F);
         freePitch = clampedValue;
         return true;
      } else {
         return false;
      }
   }
}
