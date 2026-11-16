package deluxe.client.ui;

public final class AnimatedValue {
   private final DirectionalAnimation directionalAnimation = new DirectionalAnimation();
   private double field13437;
   private double field13438;

   public void method3869(double local6355_1) {
      if (!(Math.abs(local6355_1 - this.field13438) < 1.0E-6)) {
         this.field13437 = this.method3871();
         this.field13438 = local6355_1;
         this.directionalAnimation.field15215.method4471();
      }
   }

   public AnimatedValue method3870(double local6356_1) {
      this.field13437 = local6356_1;
      this.field13438 = local6356_1;
      this.directionalAnimation.field15215.method4471();
      return this;
   }

   public double method3871() {
      return this.field13437 + (this.field13438 - this.field13437) * this.directionalAnimation.method4467();
   }

   public double method3872() {
      return this.field13438;
   }

   public AnimatedValue(long local6359_1) {
      this.directionalAnimation.method4465(local6359_1);
      this.directionalAnimation.method4464(1.0);
      this.directionalAnimation.setAnimationDirection(AnimationDirection.IN);
   }
}
