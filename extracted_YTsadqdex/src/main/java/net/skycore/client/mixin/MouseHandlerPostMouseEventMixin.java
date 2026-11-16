package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerPostMouseEventMixin {
   @Inject(method = "onButton", at = @At("HEAD"))
   private void skycore$postMouseEvent(long timestamp, MouseButtonInfo mouseButtonInfo, int value, CallbackInfo callbackInfo) {
      if (SkycoreClient.interfaceRenderer() != null) {
         SkycoreClient.interfaceRenderer().onMouseButton(mouseButtonInfo, value);
      }

      SkycoreClient.events().post(new MouseButtonEvent(Minecraft.getInstance(), mouseButtonInfo, value));
   }
}
