package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionStop;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.module.combat.AutoWebTrapModule;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.module.misc.ElytraHelperModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputAutoTakeoffJumpHeadMixin extends ClientInput {
   @Inject(method = "tick", at = @At("HEAD"))
   private void skycore$autoTakeoffJumpHead(CallbackInfo callbackInfo) {
      if (ElytraHelperModule.INSTANCE.shouldForceAutoTakeoffJump()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.options != null) {
            instance.options.keyJump.setDown(true);
         }
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void skycore$postPlayerInput(CallbackInfo callbackInfo) {
      PlayerInputEvent playerInputEvent = new PlayerInputEvent((KeyboardInput)(Object)this, this.keyPresses, this.getMoveVector());
      SkycoreClient.events().post(playerInputEvent);
      CrystalAuraModule.INSTANCE.applyFinalMovementCorrection(playerInputEvent);
      AutoWebTrapModule.INSTANCE.applyFinalMovementCorrection(playerInputEvent);
      ActionStop.apply(playerInputEvent);
      this.keyPresses = playerInputEvent.keyPresses();
      this.moveVector = playerInputEvent.moveVector();
   }
}
