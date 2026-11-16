package net.skycore.client.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public final class BlockPlaceEvent {
   private final Block block;
   private final BlockPos pos;

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockPlaceEvent(Block block2, BlockPos pos2) {
      this.block = block2;
      this.pos = pos2;
   }

   public Block getBlock() {
      return this.block;
   }
}
