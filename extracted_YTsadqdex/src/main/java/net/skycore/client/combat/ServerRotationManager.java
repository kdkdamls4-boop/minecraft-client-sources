package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.util.QueuePendingHelper;
import net.skycore.client.util.SynchronizeHelper;

public final class ServerRotationManager {
   public static final ServerRotationManager INSTANCE = new ServerRotationManager();
   private int priority;
   public static final ServerRotationManager instance = ServerRotationManager.INSTANCE;
   private ServerRotationManager.RotationTask currentTask = ServerRotationManager.RotationTask.IDLE;
   private float pitchReturnSpeed;
   private int idleTicks;
   private float yawReturnSpeed;
   private int timeout;

   public static void init() {
   }

   public static void update(Rotation rotation, float angle, float angle2, float yaw, float pitch, int elapsed, int value, boolean condition) {
      if (SynchronizeHelper.isActive()) {
         ServerRotationManager iNSTANCE = INSTANCE;
         if (iNSTANCE.priority <= value) {
            if (iNSTANCE.currentTask == ServerRotationManager.RotationTask.IDLE && !condition) {
               FreeLookController.setActive(true);
            }

            RotationController.setClientLook(condition);
            iNSTANCE.yawReturnSpeed = yaw;
            iNSTANCE.pitchReturnSpeed = pitch;
            iNSTANCE.timeout = elapsed;
            iNSTANCE.priority = value;
            iNSTANCE.currentTask = ServerRotationManager.RotationTask.AIM;
            iNSTANCE.updateRotation(rotation, angle, angle2);
         }
      }
   }

   public static void update(Rotation rotation, float value, float value2, int value3, int value4) {
      update(rotation, value, value, value2, value2, value3, value4, false);
   }

   public void stopRotation() {
      this.currentTask = ServerRotationManager.RotationTask.IDLE;
      this.priority = 0;
      if (!RotationManager.INSTANCE.isRotating()) {
         RotationController.releaseToFreeLook();
      }
   }

   public boolean isRotating() {
      return this.currentTask != ServerRotationManager.RotationTask.IDLE;
   }

   private float smoothRotation(float value, float result, float value2) {
      float computedValue = result - value;
      float computedValue2 = Mth.wrapDegrees(computedValue);
      if (value2 == 0.0F) {
         return result;
      }

      float computedValue3 = computedValue2 / value2;
      float computedValue4 = Math.abs(computedValue3) * Math.signum(computedValue2);
      if (Math.abs(computedValue4) > Math.abs(computedValue2)) {
         computedValue4 = computedValue2;
      }

      float sens = MouseSensitivity.getSens(computedValue4);
      float gCDValue = MouseSensitivity.getGCDValue();
      return value + sens - (gCDValue == 0.0F ? 0.0F : sens % gCDValue);
   }

   private void tickIdleInternal() {
      if (!SynchronizeHelper.isActive()) {
         this.stopRotation();
      } else if (!CrystalAuraModule.INSTANCE.isEnabled() && !QueuePendingHelper.ownsRotationLoop()) {
         if (this.currentTask == ServerRotationManager.RotationTask.AIM && this.idleTicks > this.timeout) {
            this.currentTask = ServerRotationManager.RotationTask.RESET;
         }

         if (this.currentTask == ServerRotationManager.RotationTask.RESET) {
            Rotation rotation = new Rotation(FreeLookController.getFreeYaw(), FreeLookController.getFreePitch());
            if (this.updateRotation(rotation, this.yawReturnSpeed, this.pitchReturnSpeed)) {
               this.stopRotation();
            }
         }

         this.idleTicks++;
      }
   }

   public static void tickIdle() {
      INSTANCE.tickIdleInternal();
   }

   private boolean updateRotation(Rotation rotation, float yaw, float pitch) {
      LocalPlayer instance2 = Minecraft.getInstance().player;
      if (instance2 == null) {
         return false;
      }

      float yRot = RotationController.isActive() ? RotationController.getServerYaw() : instance2.getYRot();
      float xRot = RotationController.isActive() ? RotationController.getServerPitch() : instance2.getXRot();
      float yaw2 = this.smoothRotation(yRot, rotation.getYaw(), yaw);
      float pitch2 = Mth.clamp(this.smoothRotation(xRot, rotation.getPitch(), pitch), -90.0F, 90.0F);
      if (RotationController.isClientLook()) {
         instance2.setYRot(yaw2);
         instance2.setXRot(pitch2);
         RotationController.syncServerFromPlayer();
      } else {
         RotationController.applyExact(yaw2, pitch2);
      }

      this.idleTicks = 0;
      return new Rotation(
               RotationController.isActive() ? RotationController.getServerYaw() : instance2.getYRot(),
               RotationController.isActive() ? RotationController.getServerPitch() : instance2.getXRot()
            )
            .getDelta(rotation)
         < 1.0F;
   }

   private ServerRotationManager() {
   }

   enum RotationTask {
      AIM,
      RESET,
      IDLE;
   }
}
