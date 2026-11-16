package deluxe.client.event;

import net.minecraft.util.PlayerInput;

public final class MovementInputEvent extends CancellableEvent {
   private PlayerInput field15151;
   private float field15152;
   private float field15153;
   private boolean field15154;
   private boolean field15155;

   public MovementInputEvent(PlayerInput playerInput, float local7487_2, float local7487_3, boolean local7487_4, boolean local7487_5) {
      this.field15151 = playerInput;
      this.field15152 = local7487_2;
      this.field15153 = local7487_3;
      this.field15154 = local7487_4;
      this.field15155 = local7487_5;
   }

   public boolean method4440() {
      return this.field15155;
   }

   public float method4441() {
      return this.field15152;
   }

   public void method4442(boolean local7490_1) {
      this.field15154 = local7490_1;
   }

   public PlayerInput method4443() {
      return this.field15151;
   }

   public void method4444(PlayerInput playerInput) {
      this.field15151 = playerInput;
   }

   public boolean method4445() {
      return this.field15154;
   }

   public void method4446(float local7494_1) {
      this.field15152 = local7494_1;
   }

   public float method4447() {
      return this.field15153;
   }

   public void method4448(boolean local7496_1) {
      this.field15155 = local7496_1;
   }

   public void method4449(float local7497_1) {
      this.field15153 = local7497_1;
   }
}
