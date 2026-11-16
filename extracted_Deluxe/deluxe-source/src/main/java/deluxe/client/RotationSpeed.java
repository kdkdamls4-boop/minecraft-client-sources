package deluxe.client;


public final class RotationSpeed {
   private final float field08009;
   private final float field08010;
   private final float field08011;
   private final float field08012;

   public float method2282() {
      return this.field08011;
   }

   public float method2283() {
      return this.field08009;
   }

   public RotationSpeed(float local3565_1, float local3565_2, float local3565_3, float local3565_4) {
      this.field08009 = local3565_1;
      this.field08010 = local3565_2;
      this.field08011 = local3565_3;
      this.field08012 = local3565_4;
   }

   public float method2284() {
      return this.field08012;
   }

   public float method2285() {
      return this.field08010;
   }

   public RotationSpeed(float local3568_1, float local3568_2) {
      this(local3568_1, local3568_2, local3568_1, local3568_2);
   }
}
