package net.skycore.client.event;

public final class CancellableEvent {
   private boolean cancelled;

   public void cancel() {
      this.cancelled = true;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancelled2) {
      this.cancelled = cancelled2;
   }
}
