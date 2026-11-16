package net.skycore.client.util;

public final class SpringRotation {
   private double angle;
   private double velocity;

   public float update(float angle2, double value) {
      double computedValue = Math.max(0.0, Math.min(0.05, value));
      double angle3 = (this.angle - angle2) % 360.0;
      if (angle3 >= 180.0) {
         angle3 -= 360.0;
      }

      if (angle3 < -180.0) {
         angle3 += 360.0;
      }

      double computedValue2 = this.angle - angle3;
      double value2 = 4.0;
      double computedValue3 = Math.exp(-value2 * computedValue);
      double computedValue4 = (this.velocity + value2 * angle3) * computedValue;
      this.angle = computedValue2 + (angle3 + computedValue4) * computedValue3;
      this.velocity = (this.velocity - value2 * computedValue4) * computedValue3;
      return (float)this.angle;
   }

   public void reset(float value) {
      double angle2 = value;
      this.angle = angle2;
      this.velocity = 0.0;
   }
}
