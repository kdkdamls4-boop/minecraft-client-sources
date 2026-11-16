package net.skycore.client.util;

import java.util.function.UnaryOperator;

public enum EasingCurve {
   LINEAR(value -> {
      return value;
   }),
   CUBIC_OUT(value2 -> {
      double computedValue = 1.0F - value2;
      return 1.0F - (float)Math.pow(computedValue, 3.0);
   });

   private final UnaryOperator<Float> fn;

   public float apply(float value) {
      UnaryOperator<Float> fn2 = this.fn;
      float minimum = Math.min(1.0F, value);
      Float maximum = Math.max(0.0F, minimum);
      return fn2.apply(maximum);
   }

   EasingCurve(UnaryOperator<Float> fn2) {
      this.fn = fn2;
   }
}
