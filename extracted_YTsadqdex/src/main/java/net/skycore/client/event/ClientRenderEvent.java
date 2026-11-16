package net.skycore.client.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public record ClientRenderEvent(Minecraft client, DeltaTracker deltaTracker) {
   public Minecraft client() {
      return this.client;
   }

   public DeltaTracker deltaTracker() {
      return this.deltaTracker;
   }
}
