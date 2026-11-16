package deluxe.mixin;

import deluxe.client.event.EventBus;
import deluxe.client.module.render.FreeCameraModule;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.module.movement.MovementPauseController;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void stripFreeCameraKeys(CallbackInfo callbackInfo) {
      if (FreeCameraModule.method0910().isEnabled()) {
         this.playerInput = PlayerInput.DEFAULT;
         this.movementVector = Vec2f.ZERO;
      }
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void stripFrozenMoveKeys(CallbackInfo callbackInfo) {
      if (MovementPauseController.getInstance().isPaused()) {
         this.playerInput = new PlayerInput(false, false, false, false, false, this.playerInput.sneak(), this.playerInput.sprint());
         this.movementVector = Vec2f.ZERO;
      }
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void emitMoveInput(CallbackInfo callbackInfo) {
      if (EventBus.getInstance().hasListeners(MovementInputEvent.class)) {
         MovementInputEvent movementInputEvent = new MovementInputEvent(
            this.playerInput,
            movementImpulse(this.playerInput.forward(), this.playerInput.backward()),
            movementImpulse(this.playerInput.left(), this.playerInput.right()),
            this.playerInput.jump(),
            this.playerInput.sneak()
         );
         EventBus.getInstance().post(movementInputEvent);
         if (!movementInputEvent.method4421()) {
            this.movementVector = new Vec2f(movementInputEvent.method4447(), movementInputEvent.method4441()).normalize();
            this.playerInput = new PlayerInput(
               movementInputEvent.method4441() > 0.0F,
               movementInputEvent.method4441() < 0.0F,
               movementInputEvent.method4447() > 0.0F,
               movementInputEvent.method4447() < 0.0F,
               movementInputEvent.method4445(),
               movementInputEvent.method4440(),
               movementInputEvent.method4443().sprint()
            );
         }
      }
   }

   @Unique
   private static float movementImpulse(boolean local8138_0, boolean local8138_1) {
      return local8138_0 == local8138_1 ? 0.0F : (local8138_0 ? 1.0F : -1.0F);
   }

   static {
      VMBridge.identifyClass(KeyboardInputMixin.class, "C9cYwOv6");
   }
}
