package deluxe.client.setting;

import java.util.function.Supplier;

public class NumberSetting extends Setting<NumberSetting> {
   private float value;
   private float minimum = 0.0F;
   private float maximum = 1.0F;
   private float step = 0.01F;
   private boolean integral;
   private String suffix = "";
   private Supplier<Boolean> automatic = () -> Boolean.FALSE;

   public NumberSetting withStep(float newStep) {
      if (newStep > 0.0F) {
         this.step = newStep;
         this.value = this.clampAndSnap(this.value);
      }

      return this;
   }

   public NumberSetting withSuffix(String local6445_1) {
      this.suffix = local6445_1 == null ? "" : local6445_1;
      return this;
   }

   public double doubleValue() {
      return this.value;
   }

   public float getStep() {
      return this.step;
   }

   public float getFraction() {
      return this.maximum <= this.minimum ? 0.0F : (this.value - this.minimum) / (this.maximum - this.minimum);
   }

   public void setFraction(float local6449_1) {
      float local6449_2 = Math.max(0.0F, Math.min(1.0F, local6449_1));
      this.setValue(this.minimum + (this.maximum - this.minimum) * local6449_2);
   }

   private int decimalPlaces() {
      return !this.integral && !(this.step >= 1.0F) ? Math.max(1, (int)Math.ceil(-Math.log10(this.step))) : 0;
   }

   public int intValue() {
      return Math.round(this.value);
   }

   public String getSuffix() {
      return this.suffix;
   }

   public NumberSetting withIntegerStep(int newStep) {
      return this.withStep(newStep);
   }

   public float getMinimum() {
      return this.minimum;
   }

   public float getMaximum() {
      return this.maximum;
   }

   public NumberSetting(String name, String local6456_2) {
      super(name, local6456_2);
      this.value = 0.0F;
   }

   public NumberSetting automaticWhen(Supplier<Boolean> newAutomatic) {
      if (newAutomatic != null) {
         this.automatic = newAutomatic;
      }

      return this;
   }

   public boolean isAutomatic() {
      return Boolean.TRUE.equals(this.automatic.get());
   }

   public boolean isIntegral() {
      return this.integral;
   }

   public NumberSetting withIntegerRange(int newMinimum, int newMaximum) {
      this.minimum = newMinimum;
      this.maximum = newMaximum;
      this.integral = true;
      this.step = 1.0F;
      this.value = this.clampAndSnap(this.value);
      return this;
   }

   public NumberSetting withRange(float newMinimum, float newMaximum) {
      this.minimum = newMinimum;
      this.maximum = newMaximum;
      this.integral = false;
      this.value = this.clampAndSnap(this.value);
      return this;
   }

   private float clampAndSnap(float local6462_1) {
      float local6462_2 = Math.max(this.minimum, Math.min(this.maximum, local6462_1));
      if (this.step > 0.0F) {
         local6462_2 = this.minimum + Math.round((local6462_2 - this.minimum) / this.step) * this.step;
         local6462_2 = Math.max(this.minimum, Math.min(this.maximum, local6462_2));
      }

      return this.integral ? Math.round(local6462_2) : local6462_2;
   }

   public float floatValue() {
      return this.value;
   }

   public String getDisplayValue() {
      int local6465_1 = this.decimalPlaces();
      String local6465_2 = local6465_1 <= 0 ? Integer.toString(Math.round(this.value)) : String.format("%." + local6465_1 + "f", this.value);
      return this.suffix.isEmpty() ? local6465_2 : local6465_2 + this.suffix;
   }

   protected NumberSetting(
      String name, String local6466_2, float local6466_3, float initialMinimum, float initialMaximum, float local6466_6, boolean initialIntegral
   ) {
      super(name, local6466_2);
      this.minimum = initialMinimum;
      this.maximum = initialMaximum;
      this.step = local6466_6 <= 0.0F ? (initialIntegral ? 1.0F : 0.01F) : local6466_6;
      this.integral = initialIntegral;
      this.value = this.clampAndSnap(local6466_3);
   }

   public NumberSetting setValue(float local6467_1) {
      float currentValue = this.clampAndSnap(local6467_1);
      if (currentValue != this.value) {
         this.value = currentValue;
         this.notifyChanged();
      }

      return this;
   }

   public float getValue() {
      return this.value;
   }
}
