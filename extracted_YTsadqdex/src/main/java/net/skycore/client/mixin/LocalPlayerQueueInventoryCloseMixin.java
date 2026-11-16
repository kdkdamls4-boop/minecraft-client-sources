package net.skycore.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.module.movement.GuiMoveModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerQueueInventoryCloseMixin {
   @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
   private void skycore$queueInventoryClose(CallbackInfo callbackInfo) {
      if (GuiMoveModule.INSTANCE.interceptInventoryClose()) {
         callbackInfo.cancel();
      }
   }
}
