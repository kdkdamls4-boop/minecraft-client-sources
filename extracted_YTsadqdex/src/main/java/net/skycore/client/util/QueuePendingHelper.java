package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;

public final class QueuePendingHelper {
   private static final float SAFE_RETURN_PITCH_SPEED = 35.0F;
   private static float lastSentPitch = Float.NaN;
   private static float pendingYawTol;
   private static final float SAFE_RETURN_YAW_SPEED = 55.0F;
   private static float pendingPitchTol;
   private static int pendingTickCounter;
   private static float pendingPitchSpeed;
   private static QueuePendingHelper.Phase phase = QueuePendingHelper.Phase.IDLE;
   private static boolean pendingSilent;
   private static FromFreeLookHelper pendingTarget;
   private static final int CRYSTAL_AURA_PRIORITY = 50;
   private static float yawTolerance = 1.0E-4F;
   private static float pendingYawSpeed;
   private static boolean bodyOwnedBySkycore;
   private static float lastSentYaw = Float.NaN;
   private static int pendingPriority;
   private static long aimStartedMs;
   private static float pitchTolerance = 1.0E-4F;
   private static int yawBudgetTick = Integer.MIN_VALUE;
   private static float yawBudgetSpent;
   private static int activePriority;
   private static int aimIdleTicks;
   private static boolean silentAim;
   private static float yawSpeedLimit = 180.0F;
   private static int pendingTicks;
   private static float pitchSpeedLimit = 180.0F;
   private static FromFreeLookHelper returnTarget;

   private static void queuePending(FromFreeLookHelper fromFreeLookHelper, float yaw, float pitch, float yaw2, float pitch2, int value, boolean condition) {
      int value2 = pendingTarget != null && pendingTickCounter <= 1 && value < pendingPriority ? 0 : 1;
      if (value2 != 0) {
         pendingTarget = fromFreeLookHelper;
         pendingYawSpeed = yaw;
         pendingPitchSpeed = pitch;
         pendingYawTol = yaw2;
         pendingPitchTol = pitch2;
         pendingPriority = value;
         pendingSilent = condition;
         pendingTickCounter = 0;
      }
   }

   private static void clearPending() {
      pendingTarget = null;
      pendingPriority = 0;
      pendingTickCounter = 0;
   }

   public static void recordSentRotation() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         lastSentYaw = Float.NaN;
         lastSentPitch = Float.NaN;
      } else {
         float yRot = player.getYRot();
         lastSentYaw = yRot;
         float xRot = player.getXRot();
         lastSentPitch = xRot;
      }
   }

   public static boolean blockedByPriority(int value) {
      return activePriority > value;
   }

   private QueuePendingHelper() {
   }

   public static void finish() {
      pendingTarget = null;
      if (bodyOwnedBySkycore) {
         if (FreeLookController.isActive()) {
            phase = QueuePendingHelper.Phase.RESET;
            returnTarget = FromFreeLookHelper.fromFreeLook();
            aimIdleTicks = 0;
            return;
         }
      }

      completeReset();
   }

   public static float lastSentPitch() {
      return lastSentPitch;
   }

   public static void onClientTick() {
      SnapBodyToFreeLookHelper.tickIdleFollow();
      if (pendingTarget != null) {
         pendingTickCounter++;
      }

      if (phase == QueuePendingHelper.Phase.AIM && ++aimIdleTicks > 3) {
         phase = QueuePendingHelper.Phase.RESET;
         returnTarget = FromFreeLookHelper.fromFreeLook();
      }

      if (phase == QueuePendingHelper.Phase.RESET) {
         if (pendingTarget != null && pendingPriority > 0 && pendingTickCounter <= 1) {
            FromFreeLookHelper pendingTarget2 = pendingTarget;
            float pendingYawSpeed2 = pendingYawSpeed;
            float pendingPitchSpeed2 = pendingPitchSpeed;
            float pendingYawTol2 = pendingYawTol;
            float pendingPitchTol2 = pendingPitchTol;
            int pendingPriority2 = pendingPriority;
            boolean pendingSilent2 = pendingSilent;
            clearPending();
            startAim(pendingTarget2, pendingYawSpeed2, pendingPitchSpeed2, pendingYawTol2, pendingPitchTol2, pendingPriority2, pendingSilent2, true);
         } else {
            returnTarget = FromFreeLookHelper.fromFreeLook();
            if (stepToward(returnTarget, 55.0F, 35.0F, 1.0F, 1.0F)) {
               completeReset();
            }
         }
      }
   }

   public static void abandon() {
      finish();
   }

   public static boolean apply(FromFreeLookHelper fromFreeLookHelper, float value, int value2, boolean condition) {
      return apply(fromFreeLookHelper, value, value, value, value, value2, condition);
   }

   private static void ensureSilentFreeLook() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (!FreeLookController.isActive()) {
            int yRot = !(Math.abs(Mth.wrapDegrees(instance.getYRot() - RotationController.getFreeYaw())) > 1.0F)
                  && !(Math.abs(instance.getXRot() - RotationController.getFreePitch()) > 1.0F)
               ? 0
               : 1;
            if (yRot != 0 && (RotationController.getFreeYaw() != 0.0F || RotationController.getFreePitch() != 0.0F || SnapBodyToFreeLookHelper.hasCaptured())) {
               if (SnapBodyToFreeLookHelper.hasCaptured()) {
                  FreeLookController.setFreeYaw(SnapBodyToFreeLookHelper.getYaw());
                  FreeLookController.setFreePitch(SnapBodyToFreeLookHelper.getPitch());
               }

               RotationController.activatePreservingFreeLook();
            } else {
               FreeLookController.setFreeYaw(instance.getYRot());
               FreeLookController.setFreePitch(instance.getXRot());
               FreeLookController.setActive(true);
            }
         }

         RotationController.setClientLook(false);
         bodyOwnedBySkycore = true;
         SnapBodyToFreeLookHelper.setRotationLock(true);
      }
   }

   private static void completeReset() {
      phase = QueuePendingHelper.Phase.IDLE;
      activePriority = 0;
      silentAim = false;
      bodyOwnedBySkycore = false;
      returnTarget = null;
      aimIdleTicks = 0;
      SnapBodyToFreeLookHelper.captureFromFreeLook();
      SnapBodyToFreeLookHelper.setRotationLock(false);
      RotationManager.INSTANCE.stopRotationIfPriority(50);
      if (RotationController.isActive()) {
         if (!RotationController.isClientLook()) {
            RotationController.releaseToFreeLook();
         }
      }
   }

   public static boolean ownsRotationLoop() {
      if (phase == QueuePendingHelper.Phase.IDLE) {
         if (pendingTarget == null) {
            return false;
         }
      }

      return true;
   }

   public static void convergeAim(float value, float value2) {
      ensureSilentFreeLook();
      if (activePriority < 50) {
         activePriority = 50;
         phase = QueuePendingHelper.Phase.AIM;
      }

      aimStartedMs = System.currentTimeMillis();
      aimIdleTicks = 0;
      phase = QueuePendingHelper.Phase.AIM;
      FromFreeLookHelper fromFreeLookHelper = new FromFreeLookHelper(value, value2);
      int value3 = 0;

      while (value3 < 256 && !stepToward(fromFreeLookHelper, 180.0F, 180.0F, 0.05F, 0.05F)) {
         value3++;
      }
   }

   public static float lastSentYaw() {
      return lastSentYaw;
   }

   public static boolean usesSilentBody() {
      return FreeLookController.isActive() && (activePriority >= 50 || ownsRotationLoop());
   }

   public static boolean apply(FromFreeLookHelper fromFreeLookHelper, float value, float value2, float value3, float value4, int value5, boolean condition) {
      if (activePriority > value5) {
         queuePending(fromFreeLookHelper, value, value2, value3, value4, value5, condition);
         return false;
      } else {
         return startAim(fromFreeLookHelper, value, value2, value3, value4, value5, condition, false);
      }
   }

   private static float spendYawBudget(float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return 0.0F;
      }

      int tickCount = instance.tickCount;
      if (tickCount != yawBudgetTick) {
         yawBudgetTick = tickCount;
         yawBudgetSpent = 0.0F;
      }

      float computedValue = 180.0F - yawBudgetSpent;
      if (computedValue <= 0.0F) {
         return 0.0F;
      }

      float clampedValue = Mth.clamp(value, -computedValue, computedValue);
      float yaw = yawBudgetSpent + Math.abs(clampedValue);
      yawBudgetSpent = yaw;
      return clampedValue;
   }

   private static boolean startAim(FromFreeLookHelper fromFreeLookHelper, float yaw, float pitch, float yaw2, float pitch2, int value, boolean condition, boolean condition2) {
      silentAim = condition;
      if (value >= 50 && !condition) {
         ensureSilentFreeLook();
      }

      yawSpeedLimit = yaw;
      pitchSpeedLimit = pitch;
      yawTolerance = yaw2;
      pitchTolerance = pitch2;
      activePriority = value;
      aimStartedMs = System.currentTimeMillis();
      aimIdleTicks = 0;
      phase = QueuePendingHelper.Phase.AIM;
      return stepToward(fromFreeLookHelper, yaw, pitch, yaw2, pitch2);
   }

   private static boolean stepToward(FromFreeLookHelper fromFreeLookHelper, float value, float value2, float value3, float value4) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return false;
      }

      float yRot = instance.getYRot();
      float xRot = instance.getXRot();
      if (FreeLookController.isActive()) {
         yRot = RotationController.getServerYaw();
         xRot = RotationController.getServerPitch();
      }

      float y = Mth.wrapDegrees(fromFreeLookHelper.yaw() - yRot);
      float computedValue = fromFreeLookHelper.pitch() - xRot;
      float computedValue2 = Math.abs(y) + Math.abs(computedValue);
      float maximum = computedValue2 == 0.0F ? 0.0F : Math.abs(y / computedValue2) * Math.max(0.0F, value);
      float maximum2 = computedValue2 == 0.0F ? 0.0F : Math.abs(computedValue / computedValue2) * Math.max(0.0F, value2);
      float computedValue3 = Math.min(Math.abs(y), maximum);
      float computedValue4 = Math.min(Math.abs(computedValue), maximum2);
      float computedValue5 = Mth.clamp(xRot + TruncateGcdHelper.applyGcd(Mth.clamp(computedValue, -computedValue4, computedValue4)), -90.0F, 90.0F);
      float computedValue6 = yRot + TruncateGcdHelper.applyGcd(Mth.clamp(y, -computedValue3, computedValue3));
      if (!applyBody(computedValue6, computedValue5)) {
         return false;
      }

      float serverYaw = RotationController.getServerYaw();
      float serverPitch = RotationController.getServerPitch();
      float y2 = Math.abs(Mth.wrapDegrees(fromFreeLookHelper.yaw() - serverYaw));
      float computedValue7 = Math.abs(fromFreeLookHelper.pitch() - serverPitch);
      return y2 <= value3 && computedValue7 <= value4;
   }

   public static void hardReset() {
      yawBudgetTick = Integer.MIN_VALUE;
      phase = QueuePendingHelper.Phase.IDLE;
      activePriority = 0;
      silentAim = false;
      bodyOwnedBySkycore = false;
      aimIdleTicks = 0;
      returnTarget = null;
      lastSentYaw = Float.NaN;
      lastSentPitch = Float.NaN;
      clearPending();
      SnapBodyToFreeLookHelper.setRotationLock(false);
      RotationManager.INSTANCE.stopRotationIfPriority(50);
      if (RotationController.isActive() && !RotationController.isClientLook()) {
         RotationController.releaseToFreeLook();
      }

      SnapBodyToFreeLookHelper.reset();
   }

   public static boolean isAiming() {
      return phase == QueuePendingHelper.Phase.AIM;
   }

   private static boolean applyBody(float angle, float value) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return false;
      }

      float pitch;
      if (Float.isFinite(value)) {
         pitch = Mth.clamp(value, -90.0F, 90.0F);
      } else if (Float.isFinite(lastSentPitch)) {
         pitch = lastSentPitch;
      } else {
         pitch = player.getXRot();
      }

      float angle2 = pitch;
      ensureSilentFreeLook();
      Rotation rotation = new Rotation(angle, angle2);
      RotationManager.update(rotation, 2500.0F, 2500.0F, 2500.0F, 2500.0F, 2, 50, false);
      return RotationManager.INSTANCE.isAimingAtPriority(50);
   }

   enum Phase {
      IDLE,
      AIM,
      RESET;
   }
}
