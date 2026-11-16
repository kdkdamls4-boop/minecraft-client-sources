package net.skycore.client.event;

import net.minecraft.client.player.LocalPlayer;

public record PlayerTickEndEvent(LocalPlayer player) {
   public LocalPlayer player() {
      return this.player;
   }
}
