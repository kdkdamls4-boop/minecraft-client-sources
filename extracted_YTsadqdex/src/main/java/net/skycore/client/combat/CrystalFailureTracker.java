package net.skycore.client.combat;

import net.minecraft.core.BlockPos;

public final class CrystalFailureTracker {
   public static boolean rejected(float value, float value2, float value3, float value4, float value5) {
      return value >= value5 || value > value4 || value2 > value4;
   }

   private CrystalFailureTracker() {
   }

   public static boolean sameSupportCoolingDown(BlockPos blockPos, BlockPos blockPos2, int value, int value2) {
      return blockPos.equals(blockPos2) && value - value2 < 2;
   }

   public static boolean lethal(float value, float value2) {
      return value >= value2;
   }
}
