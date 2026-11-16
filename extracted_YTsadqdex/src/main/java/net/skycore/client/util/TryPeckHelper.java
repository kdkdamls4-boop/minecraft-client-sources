package net.skycore.client.util;

public final class TryPeckHelper {
   public static final float ALL_EGGS_LAID_AT = 37.0F;
   private float lastPeck = -10.0F;
   private int damage;

   public boolean tryPeck(float lastPeck2, double value, double value2) {
      if (!(lastPeck2 < 37.0F) && !(lastPeck2 >= 60.0F) && !(value >= 2.5600000000000005)) {
         if (!(Math.abs(value2) > 1.8)) {
            if (!(lastPeck2 - this.lastPeck < 1.2F)) {
               this.lastPeck = lastPeck2;
               this.damage = Math.min(1000, this.damage + 2);
               return true;
            }
         }
      }

      return false;
   }

   public float peckAmount(float value) {
      float computedValue = value - this.lastPeck;
      float result;
      if (!(computedValue < 0.0F) && !(computedValue >= 0.5F)) {
         double computedValue2 = computedValue * Math.PI / 0.5;
         result = (float)Math.sin(computedValue2);
      } else {
         result = 0.0F;
      }

      return result;
   }

   public void clear() {
      this.damage = 0;
      this.lastPeck = -10.0F;
   }

   public int displayedHealth(int damage2) {
      int damage3 = -1;
      int value = 0;
      if (damage2 <= 0) {
         value = damage2;
      } else {
         damage3 = damage2 - this.damage;
         value = Math.max(1, damage3);
      }

      return value;
   }
}
