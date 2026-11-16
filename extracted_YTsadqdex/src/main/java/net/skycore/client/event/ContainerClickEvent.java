package net.skycore.client.event;

import net.minecraft.world.inventory.ContainerInput;

public final class ContainerClickEvent {
   private final int containerId;
   private boolean cancelled;
   private final int slotId;
   private final ContainerInput containerInput;
   private final int button;

   public int getSlotId() {
      return this.slotId;
   }

   public ContainerClickEvent(int containerId2, int slotId2, int button2, ContainerInput containerInput2) {
      this.containerId = containerId2;
      this.slotId = slotId2;
      this.button = button2;
      this.containerInput = containerInput2;
   }

   public int getButton() {
      return this.button;
   }

   public void cancel() {
      this.cancelled = true;
   }

   public ContainerInput getContainerInput() {
      return this.containerInput;
   }

   public int getContainerId() {
      return this.containerId;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }
}
