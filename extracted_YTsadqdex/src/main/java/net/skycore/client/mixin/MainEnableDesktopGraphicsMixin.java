package net.skycore.client.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public abstract class MainEnableDesktopGraphicsMixin {
   @Inject(method = "<clinit>", at = @At("TAIL"))
   private static void skycore$enableDesktopGraphics(CallbackInfo callbackInfo) {
      if (System.getProperty("os.name", "").startsWith("Windows")) {
         System.setProperty("java.awt.headless", "false");
      }
   }
}
