package net.skycore.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.module.movement.NoPushModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerNoBlockPushMixin {
   @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
   private void skycore$noBlockPush(double value, double value2, CallbackInfo callbackInfo) {
      if (NoPushModule.blocksPush()) {
         callbackInfo.cancel();
      }
   }
}
