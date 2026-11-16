package deluxe.client.event;


public class CancellableEvent extends Event {
   private boolean field15088;

   public boolean method4421() {
      return this.field15088;
   }

   public void method4422() {
      this.field15088 = true;
   }

   public void method4423() {
      this.field15088 = true;
   }

   public void method4424(boolean local7442_1) {
      this.field15088 = local7442_1;
   }
}
