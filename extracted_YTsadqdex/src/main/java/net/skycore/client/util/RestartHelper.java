package net.skycore.client.util;

public final class RestartHelper {
   private float startValue;
   private final long duration;
   private float targetValue;
   private final EasingCurve easing;
   private long startTime;
   private float value;

   public boolean isDone() {
      if (this.value == this.targetValue) {
         long now = System.currentTimeMillis();
         if (now - this.startTime >= this.duration) {
            return true;
         }
      }

      return false;
   }

   public float getValue() {
      return this.value;
   }

   public float update(float targetValue2) {
      long now = System.currentTimeMillis();
      if (targetValue2 != this.targetValue) {
         this.startValue = this.value;
         this.targetValue = targetValue2;
         this.startTime = now;
      }

      long timestamp = now - this.startTime;
      if (timestamp >= this.duration) {
         this.value = this.targetValue;
         return this.value;
      } else {
         float computedValue = (float)timestamp / (float)this.duration;
         float intermediate = this.targetValue - this.startValue;
         float computedValue2 = this.startValue + intermediate * this.easing.apply(computedValue);
         this.value = computedValue2;
         return this.value;
      }
   }

   public void restart() {
      long now = System.currentTimeMillis();
      this.value = 0.0F;
      this.startValue = 0.0F;
      this.targetValue = 0.0F;
      this.startTime = now;
   }

   public RestartHelper(long duration2, EasingCurve easing2) {
      this.duration = duration2;
      this.easing = easing2 == null ? EasingCurve.LINEAR : easing2;
   }
}
