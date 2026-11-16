package net.skycore.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudRemoveVignetteMixin {
   @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
   private void skycore$removeVignette(GuiGraphicsExtractor guiGraphicsExtractor, Entity entity, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveVignette()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void skycore$removeScoreboard(GuiGraphicsExtractor guiGraphicsExtractor, Objective objective, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveScoreboard()) {
         callbackInfo.cancel();
      }
   }
}
