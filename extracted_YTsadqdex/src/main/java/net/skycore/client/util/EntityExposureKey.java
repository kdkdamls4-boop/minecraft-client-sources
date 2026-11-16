package net.skycore.client.util;

public record EntityExposureKey(int id, long x, long y, long z) {
   public long y() {
      return this.y;
   }

   public int id() {
      return this.id;
   }

   public long z() {
      return this.z;
   }

   public long x() {
      return this.x;
   }
}
