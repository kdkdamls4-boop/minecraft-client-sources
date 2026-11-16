package net.skycore.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.skycore.client.module.render.AtmosphereModule;
import net.skycore.client.module.render.FogModule;
import net.skycore.client.module.render.WorldTweaksModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererApplyWorldTweaksFogMixin {
   @Inject(method = "setupFog", at = @At("RETURN"))
   private void skycore$applyWorldTweaksFog(Camera camera, int value, DeltaTracker deltaTracker, float value2, ClientLevel level, CallbackInfoReturnable<FogData> callbackInfoReturnable) {
      if (camera.getFluidInCamera() == FogType.NONE) {
         FogModule fogModule = FogModule.enabled();
         if (fogModule != null && callbackInfoReturnable.getReturnValue() != null) {
            fogModule.applyFog((FogData)callbackInfoReturnable.getReturnValue());
         } else {
            AtmosphereModule atmosphereModule = AtmosphereModule.enabled();
            if (atmosphereModule == null || !atmosphereModule.ownsFog()) {
               WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
               if (worldTweaksModule != null && worldTweaksModule.modifiesVanillaFog()) {
                  FogData returnValue = (FogData)callbackInfoReturnable.getReturnValue();
                  if (returnValue != null) {
                     worldTweaksModule.applyVanillaFog(returnValue, value * 16.0F);
                  }
               }
            }
         }
      }
   }
}
