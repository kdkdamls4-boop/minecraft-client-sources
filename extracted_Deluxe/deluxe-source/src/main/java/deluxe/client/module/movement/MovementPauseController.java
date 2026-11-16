package deluxe.client.module.movement;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.event.EventPriority;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PlayerTickEvent;

public class MovementPauseController implements MinecraftAccess {
   private static MovementPauseController movementPauseController;
   private int remainingTicks = -1;
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<PlayerTickEvent> field10258 = playerTickEvent -> {
      if (this.remainingTicks > 0) {
         this.remainingTicks--;
      } else if (this.remainingTicks == 0) {
         this.remainingTicks = -1;
      }
   };
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<MovementInputEvent> field10259 = movementInputEvent -> {
      if (this.remainingTicks >= 0) {
         movementInputEvent.method4446(0.0F);
         movementInputEvent.method4449(0.0F);
         movementInputEvent.method4442(false);
      }
   };

   public boolean isPaused() {
      return this.remainingTicks != -1;
   }

   private MovementPauseController() {
   }

   public void pauseForTicks(int newRemainingTicks) {
      this.remainingTicks = newRemainingTicks;
   }

   public static MovementPauseController getInstance() {
      if (movementPauseController == null) {
         movementPauseController = new MovementPauseController();
         EventBus.getInstance().subscribe(movementPauseController);
      }

      return movementPauseController;
   }

   public int getRemainingTicks() {
      return this.remainingTicks;
   }
}
