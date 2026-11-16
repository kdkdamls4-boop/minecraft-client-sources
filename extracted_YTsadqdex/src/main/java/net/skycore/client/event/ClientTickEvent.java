package net.skycore.client.event;

import net.minecraft.client.Minecraft;

public record ClientTickEvent(Minecraft client) {
   public Minecraft client() {
      return this.client;
   }
}
