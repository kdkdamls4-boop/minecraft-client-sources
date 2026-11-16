package net.skycore.client.util;

import java.util.Set;
import net.minecraft.core.BlockPos;

public final class MiningPlanOptions {
   public float progress;
   public int maxBlocks = 4;
   public BlockPos standBlock;
   public Set<BlockPos> blacklist;
   public BlockPos currentBlock;
   public int footY;
   public BlockPos preferredBase;
   public boolean fastExposure;
   public int maxAttempts = 16;
   public BlockPos stickyBase;
   public float preferredDamage;
   public boolean enabled;
   public boolean preferMaximumDamage;

   public MiningPlanOptions() {
      this.footY = Integer.MIN_VALUE;
   }
}
