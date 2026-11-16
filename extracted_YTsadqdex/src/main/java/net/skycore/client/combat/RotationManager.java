package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.module.combat.AimbotModule;
import net.skycore.client.util.QueuePendingHelper;
import net.skycore.client.util.SynchronizeHelper;

public final class RotationManager {
   private RotationManager.RotationTask currentTask = RotationManager.RotationTask.IDLE;
   private int idleTicks;
   private Rotation targetRotation;
   private boolean clientLook;
   public static final RotationManager INSTANCE = new RotationManager();
   private float currentPitchReturnSpeed;
   private float currentPitchSpeed;
   private int currentPriority;
   private float lastPitch;
   private float currentYawReturnSpeed;
   public static final RotationManager instance = RotationManager.INSTANCE;
   private boolean sourceRotation;
   private float currentYawSpeed = 180.0F;
   private int currentTimeout;
   private float lastYaw;

   private void applyToPlayer(LocalPlayer player, float angle, float value) {
      float clampedValue = Mth.clamp(value, -90.0F, 90.0F);
      if (this.clientLook) {
         player.setYRot(angle);
         player.setXRot(clampedValue);
         RotationController.syncServerFromPlayer();
      } else {
         RotationController.applyExact(angle, clampedValue);
         preserveHandAngles(player);
      }
   }

   public static boolean canClaim(int value) {
      if (INSTANCE.currentTask == RotationManager.RotationTask.AIM) {
         if (INSTANCE.currentPriority > value) {
            return false;
         }
      }

      return true;
   }

   public Rotation targetRotation() {
      return this.targetRotation;
   }

   public boolean isRotating() {
      return this.currentTask != RotationManager.RotationTask.IDLE;
   }

   private RotationManager() {
      this.currentPitchSpeed = 180.0F;
      this.currentYawReturnSpeed = 180.0F;
      this.currentPitchReturnSpeed = 180.0F;
      this.currentTimeout = 5;
      this.targetRotation = Rotation.ZERO;
      ServerRotationManager.init();
   }

   public static void init() {
   }

   private static void preserveHandAngles(LocalPlayer player) {
      player.yRotO = RotationController.getFreeYaw();
      player.xRotO = RotationController.getFreePitch();
   }

   public void stopRotationIfPriority(int value) {
      if (this.currentTask == RotationManager.RotationTask.AIM && this.currentPriority == value) {
         this.stopRotation();
      }
   }

   private void applyReset() {
      float freeYaw = FreeLookController.getFreeYaw();
      Rotation rotation = new Rotation(freeYaw, FreeLookController.getFreePitch());
      if (this.updateRotationRelease(rotation, this.currentYawReturnSpeed, this.currentPitchReturnSpeed)) {
         this.stopRotation();
      }
   }

   public static void tickIdle() {
      INSTANCE.tickIdleInternal();
   }

   public void stopRotation() {
      this.sourceRotation = false;
      this.currentTask = RotationManager.RotationTask.IDLE;
      this.currentPriority = 0;
      this.clientLook = false;
      if (!ServerRotationManager.INSTANCE.isRotating()) {
         if (!AimbotModule.INSTANCE.isOtvodkaActive()) {
            if (!QueuePendingHelper.usesSilentBody()) {
               RotationController.releaseToFreeLook();
               LocalPlayer instance2 = Minecraft.getInstance().player;
               if (instance2 != null) {
                  preserveHandAngles(instance2);
               }
            }
         }
      }
   }

   private void tickIdleInternal() {
      if (!SynchronizeHelper.isActive()) {
         this.stopRotation();
      } else {
         Minecraft instance2 = Minecraft.getInstance();
         if (instance2.player != null) {
            if (this.currentTask == RotationManager.RotationTask.AIM && this.idleTicks > this.currentTimeout) {
               this.currentTask = RotationManager.RotationTask.RESET;
            }

            if (QueuePendingHelper.ownsRotationLoop()) {
               this.idleTicks++;
            } else {
               if (this.currentTask == RotationManager.RotationTask.RESET) {
                  this.applyReset();
               }

               this.idleTicks++;
            }
         }
      }
   }

   public static void update(Rotation rotation, float value, float value2, int value3, int value4) {
      update(rotation, value, value, value2, value2, value3, value4, false);
   }

   public static void update(Rotation rotation, float value, int value2, int value3) {
      update(rotation, value, value, 360.0F, 360.0F, value2, value3, false);
   }

   public static boolean updateExact(Rotation rotation, int value, int elapsed) {
      if (SynchronizeHelper.isActive() && canClaim(value)) {
         LocalPlayer instance2 = Minecraft.getInstance().player;
         if (instance2 == null) {
            return false;
         }

         RotationManager iNSTANCE = INSTANCE;
         if (!FreeLookController.isActive()) {
            FreeLookController.setFreeYaw(instance2.getYRot());
            FreeLookController.setFreePitch(instance2.getXRot());
            FreeLookController.setActive(true);
         }

         iNSTANCE.sourceRotation = true;
         iNSTANCE.clientLook = false;
         RotationController.setClientLook(false);
         iNSTANCE.targetRotation = rotation;
         iNSTANCE.currentPriority = value;
         iNSTANCE.currentTimeout = elapsed;
         iNSTANCE.currentYawReturnSpeed = iNSTANCE.currentPitchReturnSpeed = 180.0F;
         iNSTANCE.currentTask = RotationManager.RotationTask.AIM;
         iNSTANCE.idleTicks = 0;
         iNSTANCE.applyToPlayer(instance2, rotation.getYaw(), rotation.getPitch());
         iNSTANCE.lastYaw = RotationController.getServerYaw();
         iNSTANCE.lastPitch = RotationController.getServerPitch();
         return true;
      } else {
         return false;
      }
   }

   public static void updateHolyworld(Rotation rotation, float value, float value2, float value3, float value4, int value5, int value6, boolean condition) {
      update(rotation, value, value2, value3, value4, value5, value6, condition);
   }

   public boolean isAimingAtPriority(int value) {
      return this.currentTask == RotationManager.RotationTask.AIM && this.currentPriority == value;
   }

   public static void update(Rotation rotation, float value, float value2, float value3, int value4, int value5) {
      update(rotation, value, value2, value3, value3, value4, value5, false);
   }

   public boolean isSourceRotation() {
      return this.sourceRotation;
   }

   public static void update(Rotation rotation, float yaw, float pitch, float yaw2, float pitch2, int elapsed, int value, boolean condition) {
      if (SynchronizeHelper.isActive()) {
         RotationManager iNSTANCE = INSTANCE;
         if (canClaim(value)) {
            if (!condition && !FreeLookController.isActive()) {
               LocalPlayer instance2 = Minecraft.getInstance().player;
               if (instance2 != null) {
                  FreeLookController.setFreeYaw(instance2.getYRot());
                  FreeLookController.setFreePitch(instance2.getXRot());
                  iNSTANCE.lastYaw = instance2.getYRot();
                  iNSTANCE.lastPitch = instance2.getXRot();
               }

               FreeLookController.setActive(true);
            }

            iNSTANCE.sourceRotation = false;
            iNSTANCE.clientLook = condition;
            RotationController.setClientLook(condition);
            iNSTANCE.targetRotation = rotation;
            iNSTANCE.currentYawSpeed = yaw;
            iNSTANCE.currentPitchSpeed = pitch;
            iNSTANCE.currentYawReturnSpeed = yaw2;
            iNSTANCE.currentPitchReturnSpeed = pitch2;
            iNSTANCE.currentTimeout = elapsed;
            iNSTANCE.currentPriority = value;
            iNSTANCE.currentTask = RotationManager.RotationTask.AIM;
            iNSTANCE.idleTicks = 0;
            iNSTANCE.updateRotationRelease(iNSTANCE.targetRotation, iNSTANCE.currentYawSpeed, iNSTANCE.currentPitchSpeed);
         }
      }
   }

   private boolean updateRotationRelease(Rotation progress, float progress2, float progress3) {
      LocalPlayer instance2 = Minecraft.getInstance().player;
      if (instance2 == null) {
         return false;
      }

      float serverYaw = this.clientLook ? instance2.getYRot() : RotationController.getServerYaw();
      float serverPitch = this.clientLook ? instance2.getXRot() : RotationController.getServerPitch();
      float yaw = Mth.wrapDegrees(progress.getYaw() - serverYaw);
      float pitch = progress.getPitch() - serverPitch;
      float computedValue = Math.min(Math.abs(yaw), progress2);
      float computedValue2 = Math.min(Math.abs(pitch), progress3);
      float gCDValue = MouseSensitivity.getGCDValue();
      float sens = MouseSensitivity.getSens(Mth.clamp(yaw, -computedValue, computedValue));
      float computedValue3 = serverYaw + sens - (gCDValue == 0.0F ? 0.0F : sens % gCDValue);
      float sens2 = MouseSensitivity.getSens(Mth.clamp(pitch, -computedValue2, computedValue2));
      float clampedValue = Mth.clamp(serverPitch + sens2, -90.0F, 90.0F);
      float computedValue4 = clampedValue - (gCDValue == 0.0F ? 0.0F : clampedValue % gCDValue);
      this.applyToPlayer(instance2, computedValue3, computedValue4);
      this.lastYaw = this.clientLook ? instance2.getYRot() : RotationController.getServerYaw();
      this.lastPitch = this.clientLook ? instance2.getXRot() : RotationController.getServerPitch();
      this.idleTicks = 0;
      return new Rotation(this.lastYaw, this.lastPitch).getDelta(progress) < 1.0F;
   }

   public enum RotationTask {
      AIM,
      RESET,
      IDLE;
   }
}
