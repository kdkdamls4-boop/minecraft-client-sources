package net.skycore.client.combat;

public final class ThornsTracker {
   private long lastThornsMillis = 0L;

   public void markDamage(long lastThornsMillis2) {
      this.lastThornsMillis = lastThornsMillis2;
   }

   public boolean consumeVelocity(long timestamp, boolean condition) {
      if (condition && this.lastThornsMillis >= 0L && timestamp - this.lastThornsMillis <= 500L) {
         this.lastThornsMillis = -1L;
         return true;
      } else {
         return false;
      }
   }
}
