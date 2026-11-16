package net.skycore.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.skycore.client.module.render.FullBrightModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorFullBrightBrightnessMixin {
   @Inject(method = "extract", at = @At("TAIL"))
   private void skycore$fullBrightBrightness(LightmapRenderState lightmapRenderState, float value, CallbackInfo callbackInfo) {
      if (FullBrightModule.enabled() != null) {
         lightmapRenderState.brightness = FullBrightModule.getBrightnessForRendering(lightmapRenderState.brightness);
      }
   }
}
