package net.skycore.client.combat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class ActionReservation {
   private static int activeTick = Integer.MIN_VALUE;
   private static final Map<BlockPos, Integer> reserved = new HashMap<>();

   public static void action() {
      RotationClaim.markAction();
   }

   public static void touch() {
      if (Minecraft.getInstance().player != null) {
         Minecraft instance = Minecraft.getInstance();
         activeTick = instance.player.tickCount;
      }
   }

   public static boolean reserved(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && blockPos != null) {
         Integer value = reserved.get(blockPos);
         if (value == null) {
            return false;
         } else if (player.tickCount > value) {
            reserved.remove(blockPos);
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static void reset() {
      reserved.clear();
      activeTick = Integer.MIN_VALUE;
   }

   public static void reserve(BlockPos blockPos) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (blockPos != null && instance != null) {
         if (reserved.size() > 64) {
            var values = reserved.values();
            values.removeIf(value -> {
               return instance.tickCount > value;
            });
         }

         var reserved2 = reserved;
         BlockPos blockPos2 = blockPos.immutable();
         int intermediate = instance.tickCount + 4;
         Integer value2 = intermediate;
         reserved2.put(blockPos2, value2);
      }
   }
}
