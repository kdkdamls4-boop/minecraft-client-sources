package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class GameplayKeyState {
   static boolean isPressed(int keyCode) {
      if (keyCode != -1 && keyCode != 0 && keyCode != 67) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.gui.screen() == null) {
            long windowHandle = instance.getWindow().handle();
            if (GLFW.glfwGetKey(windowHandle, keyCode) == 1) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
