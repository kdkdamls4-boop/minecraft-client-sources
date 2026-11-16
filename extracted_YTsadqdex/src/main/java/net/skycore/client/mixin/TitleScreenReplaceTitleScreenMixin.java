package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.skycore.client.util.MouseScrolledHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenReplaceTitleScreenMixin {
   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void skycore$replaceTitleScreen(CallbackInfo callbackInfo) {
      Minecraft instance = Minecraft.getInstance();
      if (!(instance.gui.screen() instanceof MouseScrolledHelper)) {
         instance.setScreenAndShow(new MouseScrolledHelper());
      }

      callbackInfo.cancel();
   }
}
