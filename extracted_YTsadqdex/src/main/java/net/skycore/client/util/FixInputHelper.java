package net.skycore.client.util;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.skycore.client.event.PlayerInputEvent;

public final class FixInputHelper {
   private static boolean armed;
   private static Runnable current;
   private static int stopTicks;
   private static final ArrayDeque<Runnable> queued = new ArrayDeque<>();

   public static void reset() {
      queued.clear();
      current = null;
      stopTicks = 0;
      armed = false;
   }

   private FixInputHelper() {
   }

   public static void fixInput(PlayerInputEvent playerInputEvent) {
      if (stopTicks > 0) {
         playerInputEvent.clearMovement(false, false);
         playerInputEvent.setSprint(false);
      }
   }

   public static boolean busy() {
      if (current == null) {
         if (queued.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static void tick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         int value = stopTicks > 0 ? 1 : 0;
         if (stopTicks > 0) {
            stopTicks--;
         }

         if (armed) {
            armed = false;
            execute();
         } else {
            if (current == null) {
               current = queued.poll();
               if (current == null) {
                  return;
               }
            }

            if (value == 0) {
               stopTicks = 1;
            } else {
               stopTicks = Math.max(stopTicks, 1);
               armed = true;
            }
         }
      } else {
         reset();
      }
   }

   public static void beforePacket() {
      if (armed) {
         armed = false;
         execute();
      }
   }

   public static void submit(Runnable runnable) {
      queued.add(runnable);
   }

   private static void execute() {
      Runnable current2 = current;
      current = null;
      if (current2 != null) {
         current2.run();
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null) {
            player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
         }
      }
   }
}
