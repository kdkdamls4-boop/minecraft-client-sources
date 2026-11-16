package deluxe.client.event;


public final class MouseLookEvent extends CancellableEvent {
   private double field13824;
   private double field13825;
   private double field13826;
   private boolean field13827;

   public MouseLookEvent(double local6709_1, double local6709_3, double local6709_5, boolean local6709_7) {
      this.field13824 = local6709_1;
      this.field13825 = local6709_3;
      this.field13826 = local6709_5;
      this.field13827 = local6709_7;
   }

   public double method4016() {
      return this.field13826;
   }

   public void method4017(double local6711_1) {
      this.field13826 = local6711_1;
   }

   public double method4018() {
      return this.field13824;
   }

   public void method4019(double local6713_1) {
      this.field13824 = local6713_1;
   }

   public void method4020(double local6714_1) {
      this.field13825 = local6714_1;
   }

   public void method4021(boolean local6715_1) {
      this.field13827 = local6715_1;
   }

   public boolean method4022() {
      return this.field13827;
   }

   public double method4023() {
      return this.field13825;
   }
}
