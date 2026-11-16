package net.skycore.client.combat;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;
import net.skycore.client.util.ScheduleHelper;

public final class ActionStop {
   public static int stopTicks;
   public static boolean stop;

   public static void reset() {
      stop = false;
      stopTicks = 0;
   }

   public static void apply(PlayerInputEvent playerInputEvent) {
      int value = 1;
      int value2 = 0;
      value = (byte)-1;
      value2 = (byte)0;
      value = (byte)1;
      value2 = (byte)0;
      if (!NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         value = (byte)1;
         reset();
      } else {
         value = (byte)1;
         if (isStop()) {
            playerInputEvent.setMoveVector(Vec2.ZERO);
            playerInputEvent.setKeyPresses(Input.EMPTY);
         }

         if (stopTicks > 0) {
            value2 = stopTicks - 1;
            value = stopTicks - 1;
            stopTicks = value;
            if (value2 == 0) {
               stop = false;
            }
         }
      }
   }

   public static boolean isStop() {
      if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         if (stop) {
            return true;
         }

         if (ScheduleHelper.isBlockingSprint()) {
            return true;
         }
      }

      return false;
   }

   private ActionStop() {
   }
}
