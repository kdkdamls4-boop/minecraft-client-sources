package net.skycore.client.mixin;

import net.minecraft.world.level.Level;
import net.skycore.client.module.render.AtmosphereModule;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelRemoveRainMixin {
   @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
   private void skycore$removeRain(float value, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      if (NoRenderModule.shouldRemoveWeather() || AtmosphereModule.INSTANCE.shouldHideVanillaWeather()) {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }

   @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
   private void skycore$removeThunder(float value, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      if (NoRenderModule.shouldRemoveWeather() || AtmosphereModule.INSTANCE.shouldHideVanillaWeather()) {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }
}
