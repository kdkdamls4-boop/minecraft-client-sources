package net.skycore.client.event;

import net.minecraft.network.protocol.Packet;

public final class PacketReceiveEvent {
   private final Packet<?> packet;
   private boolean cancelled;

   public PacketReceiveEvent(Packet<?> packet2) {
      this.packet = packet2;
   }

   public void cancel() {
      this.cancelled = true;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
