package net.skycore.client.util;

import net.minecraft.client.Minecraft;

public final class MinecraftContext {
   public static final Minecraft mc = Minecraft.getInstance();

   private MinecraftContext() {
   }
}
