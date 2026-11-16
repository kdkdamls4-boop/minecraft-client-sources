package net.skycore.client.event;

import net.minecraft.world.entity.player.Player;

public record PlayerUpdateEvent(Player player) {
   public Player player() {
      return this.player;
   }
}
