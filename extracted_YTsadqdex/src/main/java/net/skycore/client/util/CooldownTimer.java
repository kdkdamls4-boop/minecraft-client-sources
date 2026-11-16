package net.skycore.client.util;

public final class CooldownTimer {
   private long last = System.currentTimeMillis();

   public void reset() {
      long now = System.currentTimeMillis();
      this.last = now;
   }

   public boolean elapsed(long timestamp) {
      return System.currentTimeMillis() - this.last > timestamp;
   }
}
