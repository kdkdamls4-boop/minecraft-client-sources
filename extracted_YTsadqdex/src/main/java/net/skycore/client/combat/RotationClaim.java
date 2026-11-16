package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.util.DirectionHelper;
import net.skycore.client.util.FromFreeLookHelper;
import net.skycore.client.util.MarkActionHelper;
import net.skycore.client.util.QueuePendingHelper;
import org.joml.Vector2f;

public final class RotationClaim {
   private static final int[] requested = new int[]{-100, -100};
   private static final int CRYSTAL_AURA_PRIORITY = 50;
   private static int moveFixTick = -1;
   private static final float ACTION_ROTATION_SPEED = 14400.0F;
   private static int lastTick = -1;
   private static final Object[] owners = new Object[2];
   private static final float SENT_ROTATION_EPSILON = 1.0F;
   private static int claimTick = -1;
   private static final int[] granted = new int[]{-100, -100};
   private static Object pairedOwner;
   private static final float[] grantedYaw = new float[]{Float.NaN, Float.NaN};

   private static int allocate(Object value) {

      for (int value2 = 0; value2 < 2; value2++) {
         if (owners[value2] == null) {
            owners[value2] = value;
            return value2;
         }
      }

      return -1;
   }

   public static float claimedYaw(Object value, int value2) {
      int yaw = slotFor(value);
      if (yaw >= 0) {
         if (claimedRecently(value, value2)) {
            return grantedYaw[yaw];
         }
      }

      return Float.NaN;
   }

   public static void onClaimTickMoveFix(PlayerInputEvent event) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         int value = instance.tickCount == moveFixTick ? 1 : 0;
         int value2 = QueuePendingHelper.ownsRotationLoop() && RotationController.isActive() && !RotationController.isClientLook() ? 1 : 0;
         if (value != 0 || value2 != 0) {
            DirectionHelper.apply(event, instance.getYRot());
         }
      }
   }

   private static boolean claim(Object value, Vector2f angles, float value2, float value3) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return false;
      }

      value2 = Math.min(value2, 14400.0F);
      value3 = Math.min(value3, 14400.0F);
      int value4 = instance.player.tickCount;
      if (value4 < lastTick) {
         reset();
      }

      lastTick = value4;
      int yaw = slotFor(value);
      if (yaw == -1) {
         yaw = allocate(value);
      }

      if (yaw != -1) {
         requested[yaw] = value4;
      }

      if (claimTick == value4) {
         return false;
      }

      if (QueuePendingHelper.blockedByPriority(50)) {
         return false;
      }

      Object value5 = yaw == -1 ? null : owners[1 - yaw];
      if (value5 != null && withinOneTick(yaw, value4) && pairedOwner != null && pairedOwner != value) {
         return false;
      }

      QueuePendingHelper.apply(new FromFreeLookHelper(angles.x, angles.y), value2, value3, 2.0F, 2.0F, 50, false);
      if (yaw != -1) {
         granted[yaw] = value4;
         grantedYaw[yaw] = angles.x;
      }

      claimTick = value4;
      pairedOwner = value5;
      moveFixTick = value4;
      return true;
   }

   private static int slotFor(Object value) {

      for (int value2 = 0; value2 < 2; value2++) {
         if (owners[value2] == value) {
            return value2;
         }
      }

      return -1;
   }

   public static float previousPitch() {
      return QueuePendingHelper.lastSentPitch();
   }

   public static void release(Object progress) {
      int yaw = slotFor(progress);
      if (yaw >= 0) {
         owners[yaw] = null;
         requested[yaw] = -100;
         granted[yaw] = -100;
         grantedYaw[yaw] = Float.NaN;
      }

      if (pairedOwner == progress) {
         pairedOwner = null;
      }
   }

   public static boolean claimMining(Object value, Vector2f angles) {
      return claim(value, angles, 14400.0F, 14400.0F);
   }

   public static void onClientTick() {
      QueuePendingHelper.onClientTick();
   }

   public static void reopenClaimForTick() {
      claimTick = -1;
   }

   public static float previousYaw() {
      return QueuePendingHelper.lastSentYaw();
   }

   public static void reset() {
      QueuePendingHelper.hardReset();
      MarkActionHelper.reset();
      owners[0] = owners[1] = null;
      pairedOwner = null;
      requested[0] = requested[1] = -100;
      granted[0] = granted[1] = -100;
      grantedYaw[0] = grantedYaw[1] = Float.NaN;
      moveFixTick = -1;
      lastTick = -1;
      claimTick = -1;
   }

   public static boolean claimedRecently(Object value, int value2) {
      LocalPlayer instance = Minecraft.getInstance().player;
      int value3 = slotFor(value);
      if (instance != null && value3 >= 0) {
         int computedValue = instance.tickCount - granted[value3];
         if (computedValue >= 0) {
            if (computedValue <= Math.max(0, value2)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void beforeMovementPacket() {
      MarkActionHelper.beforeMovementPacket();
   }

   private RotationClaim() {
   }

   public static void markAction() {
      MarkActionHelper.markAction();
   }

   private static boolean withinOneTick(int value, int value2) {
      int computedValue = requested[1 - value];
      int computedValue2 = value2 - computedValue;
      return computedValue2 >= 0 && computedValue2 <= 1;
   }

   public static void captureRotation() {
      QueuePendingHelper.recordSentRotation();
   }

   public static boolean claim(Object value, Vector2f angles) {
      return claim(value, angles, 14400.0F, 14400.0F);
   }

   public static boolean hasSentRotation(Vector2f angles) {
      float value = QueuePendingHelper.lastSentYaw();
      float value2 = QueuePendingHelper.lastSentPitch();
      if (Float.isFinite(value) && Float.isFinite(value2)) {
         if (Math.abs(Mth.wrapDegrees(angles.x - value)) <= 1.0F) {
            float y = angles.y - value2;
            if (Math.abs(y) <= 1.0F) {
               return true;
            }
         }
      }

      return false;
   }
}
