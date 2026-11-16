package net.skycore.client.util;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record FirstDigHelper(BlockPos base, Vec3 blast, boolean obsidian, List<BlockPos> chain, float enemyDamage, float selfDamage) {
   public BlockPos firstDig() {
      BlockPos blockPos;
      if (this.chain.isEmpty()) {
         blockPos = null;
      } else {
         blockPos = this.chain.getFirst();
      }

      return blockPos;
   }

   public Vec3 blast() {
      return this.blast;
   }

   public BlockPos base() {
      return this.base;
   }

   public boolean obsidian() {
      return this.obsidian;
   }

   public float selfDamage() {
      return this.selfDamage;
   }

   public float enemyDamage() {
      return this.enemyDamage;
   }

   public List<BlockPos> chain() {
      return this.chain;
   }
}
