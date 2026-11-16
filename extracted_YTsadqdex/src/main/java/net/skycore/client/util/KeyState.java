package net.skycore.client.util;

import net.skycore.client.setting.KeyBindSetting;

import net.minecraft.client.Minecraft;
import net.skycore.client.module.KeyBinds;

public final class KeyState {
   public static boolean isDown(int keyCode) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         if (instance.getWindow() != null && keyCode >= 0) {
            long windowHandle = instance.getWindow().handle();
            return KeyBinds.isPressed(windowHandle, keyCode);
         }
      }

      return false;
   }

   private KeyState() {
   }

   public static boolean isDown(KeyBindSetting keyBindSetting) {
      int value = 0;
      if (keyBindSetting != null) {
         value = keyBindSetting.get();
         if (isDown(value)) {
            return true;
         }
      }

      return false;
   }
}
