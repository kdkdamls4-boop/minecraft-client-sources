package deluxe.client.ui;

public class DirectionalAnimation {
   public final DirectionalAnimation.Nested0525 field15215 = new DirectionalAnimation.Nested0525();
   private long field15216 = 200L;
   private double field15217 = 1.0;
   private AnimationDirection animationDirection = AnimationDirection.IN;

   public boolean method4463() {
      return this.field15215.method4469() >= this.field15216;
   }

   public void method4464(double local7542_1) {
      this.field15217 = local7542_1;
   }

   public void method4465(long local7543_1) {
      this.field15216 = local7543_1;
   }

   public void setAnimationDirection(AnimationDirection newAnimationDirection) {
      this.animationDirection = newAnimationDirection;
   }

   public double method4467() {
      double local7545_1 = this.field15215.method4469();
      double local7545_3 = this.field15216 > 0L ? Math.min(local7545_1 / this.field15216, 1.0) : 1.0;
      double local7545_5 = 1.0 - (1.0 - local7545_3) * (1.0 - local7545_3);
      return this.animationDirection == AnimationDirection.OUT ? this.field15217 * (1.0 - local7545_5) : this.field15217 * local7545_5;
   }

   public AnimationDirection getAnimationDirection() {
      return this.animationDirection;
   }

   public static class Nested0525 {
      private long field15220 = System.nanoTime();

      public long method4469() {
         return (System.nanoTime() - this.field15220) / 1000000L;
      }

      public void method4470(long local7549_1) {
         this.field15220 = local7549_1;
      }

      public void method4471() {
         this.field15220 = System.nanoTime();
      }
   }
}
