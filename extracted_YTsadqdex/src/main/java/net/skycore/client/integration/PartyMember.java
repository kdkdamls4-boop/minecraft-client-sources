package net.skycore.client.integration;

public final class PartyMember {
   private volatile double fromX;
   private static final long ANIM_MS = 1000L;
   private static final double SNAP_EPS_SQ = 1.0E-4;
   private volatile double toZ;
   private volatile double toY;
   private volatile long animStartMs;
   private final String username;
   private volatile boolean online;
   private volatile double fromY;
   private volatile float hp = 20.0F;
   private volatile boolean hasPos;
   private volatile double toX;
   private volatile double fromZ;

   public double getY() {
      return this.sample(this.fromY, this.toY);
   }

   public PartyMember(String username2) {
      this.online = true;
      this.username = username2;
   }

   public PartyMember(String username2, double value, double value2, double value3, float value4, boolean condition) {
      this.online = true;
      this.username = username2;
      this.applyNetwork(value, value2, value3, value4, condition);
   }

   public double getZ() {
      return this.sample(this.fromZ, this.toZ);
   }

   public void applyNetwork(double fromX2, double fromY2, double fromZ2, float hp2, boolean online2) {
      this.hp = hp2;
      this.online = online2;
      if (!this.hasPos) {
         this.fromX = fromX2;
         this.fromY = fromY2;
         this.fromZ = fromZ2;
         this.toX = fromX2;
         this.toY = fromY2;
         this.toZ = fromZ2;
         this.animStartMs = 0L;
         this.hasPos = true;
      } else {
         double computedValue = fromX2 - this.toX;
         double computedValue2 = fromY2 - this.toY;
         double computedValue3 = fromZ2 - this.toZ;
         if (!(computedValue * computedValue + computedValue2 * computedValue2 + computedValue3 * computedValue3 < 1.0E-4)) {
            this.fromX = this.getX();
            this.fromY = this.getY();
            this.fromZ = this.getZ();
            this.toX = fromX2;
            this.toY = fromY2;
            this.toZ = fromZ2;
            this.animStartMs = System.currentTimeMillis();
         }
      }
   }

   public double getX() {
      return this.sample(this.fromX, this.toX);
   }

   public float getHp() {
      return this.hp;
   }

   public String getUsername() {
      return this.username;
   }

   private double sample(double result, double result2) {
      if (!this.hasPos) {
         return result2;
      }

      long animStartMs2 = this.animStartMs;
      if (animStartMs2 <= 0L) {
         return result2;
      }

      double computedValue = (System.currentTimeMillis() - animStartMs2) / 1000.0;
      if (computedValue >= 1.0) {
         return result2;
      }

      if (computedValue <= 0.0) {
         return result;
      }

      double computedValue2 = computedValue * computedValue * (3.0 - 2.0 * computedValue);
      return result + (result2 - result) * computedValue2;
   }

   public boolean isOnline() {
      return this.online;
   }
}
