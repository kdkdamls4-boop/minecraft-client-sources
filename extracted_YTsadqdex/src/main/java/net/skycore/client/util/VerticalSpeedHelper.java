package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.RotationController;

public final class VerticalSpeedHelper {
   public static double verticalSpeed(AbstractClientPlayer player) {
      Vec3 deltaMovement = player.getDeltaMovement();
      return deltaMovement.y;
   }

   public static double health(AbstractClientPlayer player) {
      return player.getHealth();
   }

   private static boolean usesFreeLook(AbstractClientPlayer player) {
      if (player == Minecraft.getInstance().player) {
         if (RotationController.isActive() && !RotationController.isClientLook()) {
            return true;
         }
      }

      return false;
   }

   public static double z(AbstractClientPlayer player) {
      return player.getZ();
   }

   public static boolean using(AbstractClientPlayer player) {
      return player.isUsingItem();
   }

   public static boolean water(AbstractClientPlayer player) {
      return player.isInWater();
   }

   public static int ticks(AbstractClientPlayer player) {
      return player.tickCount;
   }

   public static boolean cooldown(ItemStack itemStack, AbstractClientPlayer player) {
      ItemCooldowns cooldowns = player.getCooldowns();
      return cooldowns.isOnCooldown(itemStack);
   }

   public static boolean swimming(AbstractClientPlayer player) {
      return player.isSwimming();
   }

   public static boolean crawling(AbstractClientPlayer player) {
      return player.isVisuallyCrawling();
   }

   public static ItemStack offhand(AbstractClientPlayer player) {
      return player.getOffhandItem();
   }

   public static double x(AbstractClientPlayer player) {
      return player.getX();
   }

   public static ItemStack mainhand(AbstractClientPlayer player) {
      return player.getMainHandItem();
   }

   public static boolean spinAttack(AbstractClientPlayer player) {
      return player.isAutoSpinAttack();
   }

   public static double pitch(AbstractClientPlayer player) {
      if (usesFreeLook(player)) {
         return RotationController.getFreePitch();
      } else {
         return player.getXRot();
      }
   }

   public static double yaw(AbstractClientPlayer player) {
      if (usesFreeLook(player)) {
         return RotationController.getFreeYaw();
      } else {
         return player.getYHeadRot();
      }
   }

   public static double speed(AbstractClientPlayer player) {
      return player.getDeltaMovement().length();
   }

   public static boolean onGround(AbstractClientPlayer player) {
      return player.onGround();
   }

   public static boolean climbing(AbstractClientPlayer player) {
      return player.onClimbable();
   }

   public static double y(AbstractClientPlayer player) {
      return player.getY();
   }

   public static InteractionHand usedHand(AbstractClientPlayer player) {
      return player.getUsedItemHand();
   }

   public static int swingCount(AbstractClientPlayer player) {
      return SwingCountHelper.swingCount();
   }

   public static boolean sneaking(AbstractClientPlayer player) {
      return player.isShiftKeyDown();
   }
}
