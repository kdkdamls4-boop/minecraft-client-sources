package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public class NumberSetting extends Setting<Double> {
   private final double min;
   private final String suffix;
   private final double max;
   private final double step;

   public NumberSetting(String text, double value, double value2, double value3, double value4) {
      this(text, "", value, value2, value3, value4, "");
   }

   public double getMin() {
      return this.min;
   }

   public void set(Double value) {
      Double value2;
      rangeValidation: {
         if (value != null) {
            if (Double.isFinite(value)) {
               value2 = value;
               break rangeValidation;
            }
         }

         value2 = this.getDefault();
      }

      double value3 = value2;
      double min2 = this.min;
      double minimum = Math.min(this.max, value3);
      value3 = Math.max(min2, minimum);
      if (this.step > 0.0) {
         minimum = value3 - this.min;
         minimum /= this.step;
         value3 = this.min + Math.round(minimum) * this.step;
         min2 = this.min;
         minimum = Math.min(this.max, value3);
         value3 = Math.max(min2, minimum);
      }

      Double value4 = value3 == -0.0 ? 0.0 : value3;
      super.set(value4);
   }

   public void setRatio(double ratio) {
      double min2 = this.min;
      double intermediate = Math.max(0.0, Math.min(1.0, ratio));
      this.set(min2 + intermediate * (this.max - this.min));
   }

   public double ratio() {
      double result;
      if (this.max == this.min) {
         result = 0.0;
      } else {
         double value = this.get();
         double computedValue = value - this.min;
         result = computedValue / (this.max - this.min);
      }

      return result;
   }

   public double getMax() {
      return this.max;
   }

   public NumberSetting(String text, String text2, double value, double min2, double max2, double step2, String suffix2) {
      super(text, text2, value);
      if (Double.isFinite(min2) && Double.isFinite(max2) && !(max2 < min2)) {
         this.min = min2;
         this.max = max2;
         this.step = step2 > 0.0 && Double.isFinite(step2) ? step2 : 0.0;
         this.suffix = suffix2 == null ? "" : suffix2;
         this.set(value);
      } else {
         throw new IllegalArgumentException("Invalid range");
      }
   }

   public double getStep() {
      return this.step;
   }

   public String getSuffix() {
      return this.suffix;
   }
}
