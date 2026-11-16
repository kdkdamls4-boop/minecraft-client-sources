package net.skycore.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class CurrentHelper {
   private AABB to;
   private long startedAt;
   private BlockPos position;
   private AABB from;

   public AABB update(BlockPos position2, AABB to2, long startedAt2) {
      if (this.position == null) {
         this.position = position2.immutable();
         this.from = this.to = to2;
         this.startedAt = startedAt2 - 170L;
      } else {
         if (!this.position.equals(position2)) {
            AABB bounds = this.current(startedAt2);
            this.from = bounds;
            this.to = to2;
            BlockPos blockPos = position2.immutable();
            this.position = blockPos;
            this.startedAt = startedAt2;
         } else {
            this.to = to2;
         }
      }

      return this.current(startedAt2);
   }

   private AABB current(long timestamp) {
      double computedValue = Math.clamp((timestamp - this.startedAt) / 170.0, 0.0, 1.0);
      double computedValue2 = 1.0 - computedValue;
      double computedValue3 = 1.0 - Math.pow(computedValue2, 3.0);
      double intermediate = this.from.minX + (this.to.minX - this.from.minX) * computedValue3;
      double intermediate2 = this.from.minY + (this.to.minY - this.from.minY) * computedValue3;
      double computedValue4 = this.from.minZ + (this.to.minZ - this.from.minZ) * computedValue3;
      double computedValue5 = this.from.maxX + (this.to.maxX - this.from.maxX) * computedValue3;
      double computedValue6 = this.from.maxY + (this.to.maxY - this.from.maxY) * computedValue3;
      double computedValue7 = this.from.maxZ + (this.to.maxZ - this.from.maxZ) * computedValue3;
      return new AABB(intermediate, intermediate2, computedValue4, computedValue5, computedValue6, computedValue7);
   }

   public void clear() {
      this.position = null;
      this.to = null;
      this.from = null;
   }
}
