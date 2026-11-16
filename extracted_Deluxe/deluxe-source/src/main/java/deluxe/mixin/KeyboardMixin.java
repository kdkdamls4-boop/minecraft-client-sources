package deluxe.mixin;

import deluxe.client.util.InputBinding;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleManager;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("HEAD"))
   private void toggleBoundModules(long local8361_1, int local8361_3, KeyInput keyInput, CallbackInfo callbackInfo) {
      if (local8361_1 == MinecraftClient.getInstance().getWindow().getHandle() && keyInput.key() != -1 && local8361_3 != 2) {
         InputBinding.updatePressedKeys(keyInput, local8361_3 == 1);
      }

      if (local8361_3 == 1
         && keyInput.key() != -1
         && local8361_1 == MinecraftClient.getInstance().getWindow().getHandle()
         && MinecraftClient.getInstance().currentScreen == null
         && MinecraftClient.getInstance().player != null) {
         for (Module module : ModuleManager.getModules()) {
            if (module.getKeyCode() == keyInput.key()) {
               module.toggle();
            }
         }
      }
   }
}
