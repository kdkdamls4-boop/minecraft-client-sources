package net.skycore.client.util;

public final class FinishedHelper {
   private long lastMs = System.currentTimeMillis();

   public void reset() {
      long now = System.currentTimeMillis();
      this.lastMs = now;
   }

   public boolean hasTimeElapsed(long timestamp) {
      return this.finished(timestamp);
   }

   public boolean finished(long timestamp) {
      return System.currentTimeMillis() - this.lastMs >= timestamp;
   }
}
