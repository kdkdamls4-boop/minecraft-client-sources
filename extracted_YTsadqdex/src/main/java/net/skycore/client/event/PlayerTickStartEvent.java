package net.skycore.client.event;

import net.minecraft.client.player.LocalPlayer;

public record PlayerTickStartEvent(LocalPlayer player) {
   public LocalPlayer player() {
      return this.player;
   }
}
