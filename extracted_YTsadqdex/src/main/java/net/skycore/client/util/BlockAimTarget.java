package net.skycore.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record BlockAimTarget(BlockPos block, Direction face, Vec3 aim) {
   public BlockPos block() {
      return this.block;
   }

   public Vec3 aim() {
      return this.aim;
   }

   public Direction face() {
      return this.face;
   }
}
