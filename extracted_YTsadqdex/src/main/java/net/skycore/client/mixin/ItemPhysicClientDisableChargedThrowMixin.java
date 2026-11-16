package net.skycore.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.itemphysic.ItemPhysic;
import team.creative.itemphysic.client.ItemPhysicClient;

@Mixin(value = ItemPhysicClient.class, remap = false)
public abstract class ItemPhysicClientDisableChargedThrowMixin {
   @Inject(method = "gameTick", at = @At("HEAD"), remap = false)
   private static void skycore$disableChargedThrow(CallbackInfo callbackInfo) {
      ItemPhysicClient.throwCharge = 0;
      if (ItemPhysic.CONFIG != null && ItemPhysic.CONFIG.throwConfig != null) {
         ItemPhysic.CONFIG.throwConfig.enabled = false;
      }
   }

   @Inject(method = "renderTooltip", at = @At("HEAD"), remap = false)
   private static void skycore$clearThrowHud(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo callbackInfo) {
      ItemPhysicClient.throwCharge = 0;
      if (ItemPhysic.CONFIG != null) {
         if (ItemPhysic.CONFIG.throwConfig != null) {
            ItemPhysic.CONFIG.throwConfig.enabled = false;
         }

         if (ItemPhysic.CONFIG.rendering != null) {
            ItemPhysic.CONFIG.rendering.disableThrowHUD = true;
         }
      }
   }
}
