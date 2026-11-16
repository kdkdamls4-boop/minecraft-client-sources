package deluxe.mixin;

import deluxe.client.ui.WelcomeScreen;
import deluxe.client.ui.MainMenuScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
   @Unique
   private static boolean deluxe$loadingShown;

   @ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), argsOnly = true)
   private Screen replaceTitleScreen(Screen screen) {
      if (!(screen instanceof TitleScreen) || screen instanceof MainMenuScreen) {
         return screen;
      } else if (!deluxe$loadingShown) {
         deluxe$loadingShown = true;
         return new WelcomeScreen();
      } else {
         return new MainMenuScreen();
      }
   }

   @ModifyVariable(method = "setScreenAndRender(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), argsOnly = true)
   private Screen replaceQuitWorldTitleScreen(Screen screen) {
      return screen instanceof TitleScreen && !(screen instanceof MainMenuScreen) ? new MainMenuScreen() : screen;
   }
}
