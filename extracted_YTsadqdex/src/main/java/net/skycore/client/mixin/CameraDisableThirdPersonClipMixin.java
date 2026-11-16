package net.skycore.client.mixin;

import net.minecraft.client.Camera;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraDisableThirdPersonClipMixin {
   @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
   private void skycore$disableThirdPersonClip(float value, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      if (NoRenderModule.shouldDisableCameraClipping()) {
         callbackInfoReturnable.setReturnValue(value);
      }
   }
}
