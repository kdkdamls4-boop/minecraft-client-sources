package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.skycore.client.mixin.MinecraftGetRightClickDelayAccessor;

public final class ItemUseCooldown {
   private ItemUseCooldown() {
   }

   public static void setItemUseCooldown(int itemUseCooldown) {
      int value = 1;
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         MinecraftGetRightClickDelayAccessor minecraftGetRightClickDelayAccessor = (MinecraftGetRightClickDelayAccessor)instance;
         value = Math.max(0, itemUseCooldown);
         minecraftGetRightClickDelayAccessor.skycore$setRightClickDelay(value);
      }
   }
}
