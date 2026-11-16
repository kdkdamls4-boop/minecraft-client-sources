package net.skycore.client.util;

import java.util.UUID;

public final class WaitingHelper {
   private int fullSince;
   private int lastTick = -1;
   private UUID target;
   private int touchingSince = -1;
   private boolean ready;

   public void update(UUID target2, int lastTick2, boolean condition, boolean condition2) {
      if (target2 == null || !target2.equals(this.target) || lastTick2 < this.lastTick) {
         this.reset();
      }

      this.target = target2;
      this.lastTick = lastTick2;
      if (target2 != null && condition2) {
         if (this.touchingSince < 0) {
            this.touchingSince = lastTick2;
         }

         if (!this.ready) {
            if (condition) {
               if (this.fullSince < 0) {
                  this.fullSince = lastTick2;
               }

               this.ready = ((lastTick2 > this.fullSince ? 1 : 0) != 0);
            } else {
               this.fullSince = -1;
            }

            this.ready |= lastTick2 - this.touchingSince >= 8;
         }
      } else {
         this.touchingSince = this.fullSince = -1;
         this.ready = false;
      }
   }

   public WaitingHelper() {
      this.fullSince = -1;
   }

   public boolean waiting(UUID uUID) {
      if (!this.ready && this.touchingSince >= 0 && uUID != null) {
         if (uUID.equals(this.target)) {
            return true;
         }
      }

      return false;
   }

   public void reset() {
      int intermediate = -1;
      intermediate = (byte)1;
      this.target = null;
      intermediate = this.touchingSince = this.fullSince = -1;
      this.lastTick = intermediate;
      this.ready = false;
   }

   public boolean ready(UUID uUID) {
      return this.ready && uUID != null && uUID.equals(this.target);
   }
}
