package net.skycore.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayRemoveBossBarMixin {
   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void skycore$removeBossBar(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveBossBar()) {
         callbackInfo.cancel();
      }
   }
}
