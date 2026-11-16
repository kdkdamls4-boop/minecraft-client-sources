package net.skycore.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraRemoveBadEffectSkyBlockMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void skycore$removeBadEffectSkyBlock(CameraRenderState cameraRenderState, float value, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveBadEffectsVisuals()) {
         cameraRenderState.entityRenderState.doesMobEffectBlockSky = false;
      }
   }
}
