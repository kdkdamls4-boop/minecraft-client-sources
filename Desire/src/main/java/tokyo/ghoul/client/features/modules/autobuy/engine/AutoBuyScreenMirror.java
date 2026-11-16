package tokyo.ghoul.client.features.modules.autobuy.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class AutoBuyScreenMirror {
   public static final AutoBuyScreenMirror INSTANCE = new AutoBuyScreenMirror();

   private static final Snapshot EMPTY = new Snapshot(-1, 0, List.of(), null);

   private volatile Snapshot snapshot = EMPTY;

   private AutoBuyScreenMirror() {
   }

   public Snapshot snapshot() {
      return snapshot;
   }

   public void reset() {
      snapshot = EMPTY;
   }

   public boolean accept(Packet<?> packet) {
      if (packet instanceof ClientboundOpenScreenPacket open) {
         snapshot = new Snapshot(open.getContainerId(), 0, List.of(), open.getTitle());
         return false;
      }

      if (packet instanceof ClientboundContainerClosePacket close) {
         if (close.getContainerId() == snapshot.syncId()) {
            snapshot = EMPTY;
         }
         return false;
      }

      if (packet instanceof ClientboundContainerSetContentPacket inventory) {
         Snapshot previous = snapshot;
         if (!acceptsSyncId(previous, inventory.containerId())) {
            return false;
         }

         boolean sameScreen = inventory.containerId() == previous.syncId();
         snapshot = new Snapshot(
            inventory.containerId(),
            inventory.stateId(),
            List.copyOf(inventory.items()),
            sameScreen ? previous.title() : null
         );
         return true;
      }

      if (packet instanceof ClientboundContainerSetSlotPacket slotUpdate) {
         Snapshot previous = snapshot;
         int slot = slotUpdate.getSlot();
         if (slotUpdate.getContainerId() != previous.syncId() || slot < 0 || slot >= previous.slots().size()) {
            return false;
         }

         List<ItemStack> slots = new ArrayList<>(previous.slots());
         slots.set(slot, slotUpdate.getItem());
         snapshot = new Snapshot(previous.syncId(), slotUpdate.getStateId(), List.copyOf(slots), previous.title());
         return true;
      }

      return false;
   }

   private static boolean acceptsSyncId(Snapshot previous, int syncId) {
      return syncId != 0 || previous.syncId() <= 0;
   }

   public record Snapshot(int syncId, int revision, List<ItemStack> slots, Component title) {
      public boolean isOpen() {
         return syncId > 0 && !slots.isEmpty();
      }

      public ItemStack slot(int index) {
         return index >= 0 && index < slots.size() ? slots.get(index) : ItemStack.EMPTY;
      }

      public boolean isAuction() {
         return slots.size() == 90 && slot(49).getItem() == Items.NETHER_STAR;
      }

      public boolean isConfirm() {
         return slots.size() == 63 && slot(0).getItem() == Items.STAINED_GLASS_PANE.lime();
      }
   }
}
