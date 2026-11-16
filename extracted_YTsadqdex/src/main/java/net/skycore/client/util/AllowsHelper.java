package net.skycore.client.util;

import net.minecraft.core.BlockPos;

public final class AllowsHelper {
   private static BlockPos expected;

   public void begin(BlockPos blockPos) {
      expected = blockPos;
   }

   public static boolean allows(BlockPos blockPos) {
      if (expected != null) {
         if (!expected.equals(blockPos)) {
            return false;
         }
      }

      return true;
   }

   public void end() {
      expected = null;
   }

   public static boolean active() {
      return expected != null;
   }
}
