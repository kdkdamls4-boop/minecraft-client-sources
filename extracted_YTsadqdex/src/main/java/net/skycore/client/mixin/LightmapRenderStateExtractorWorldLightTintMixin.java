package net.skycore.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.skycore.client.module.render.WorldTweaksModule;
import net.skycore.client.util.WriteHexByteHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorWorldLightTintMixin {
   @Inject(method = "extract", at = @At("TAIL"))
   private void skycore$worldLightTint(LightmapRenderState lightmapRenderState, float value, CallbackInfo callbackInfo) {
      WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
      if (worldTweaksModule != null && worldTweaksModule.usesWorldColor()) {
         int value2 = worldTweaksModule.resolvedWorldColor();
         Vector3f vector = new Vector3f(WriteHexByteHelper.red(value2) / 255.0F, WriteHexByteHelper.green(value2) / 255.0F, WriteHexByteHelper.blue(value2) / 255.0F);
         lightmapRenderState.blockLightTint = new Vector3f(lightmapRenderState.blockLightTint).mul(vector);
         lightmapRenderState.skyLightColor = new Vector3f(lightmapRenderState.skyLightColor).mul(vector);
         lightmapRenderState.ambientColor = new Vector3f(lightmapRenderState.ambientColor).mul(vector);
      }
   }
}
