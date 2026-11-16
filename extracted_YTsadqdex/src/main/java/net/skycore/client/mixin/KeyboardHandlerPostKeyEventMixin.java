package net.skycore.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.skycore.client.SkycoreClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerPostKeyEventMixin {
   @Inject(method = "keyPress", at = @At("HEAD"))
   private void skycore$postKeyEvent(long timestamp, int keyCode, KeyEvent keyEvent, CallbackInfo callbackInfo) {
      SkycoreClient.events().post(new net.skycore.client.event.KeyEvent(Minecraft.getInstance(), keyEvent, keyCode));
   }
}
