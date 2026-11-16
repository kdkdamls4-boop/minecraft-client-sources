package net.skycore.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudHideEffectIconsMixin {
   @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
   private void skycore$hideEffectIcons(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveEffectIcons()) {
         callbackInfo.cancel();
      }
   }
}
