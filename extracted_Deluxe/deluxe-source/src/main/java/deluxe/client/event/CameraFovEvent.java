package deluxe.client.event;


public class CameraFovEvent extends CancellableEvent {
   private float field12497;

   public float method3578() {
      return this.field12497;
   }

   public void method3579(float local5859_1) {
      this.field12497 = local5859_1;
   }
}
