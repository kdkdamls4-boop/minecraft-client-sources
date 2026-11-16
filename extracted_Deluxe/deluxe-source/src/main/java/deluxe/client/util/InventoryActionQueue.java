package deluxe.client.util;

import deluxe.client.module.movement.MovementPauseController;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PlayerTickEvent;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryActionQueue implements MinecraftAccess {
   private static InventoryActionQueue inventoryActionQueue;
   private final ArrayList<InventoryActionQueue.Nested0447> field09169 = new ArrayList<>();
   private boolean field09170;
   private final EventListener<PlayerTickEvent> field09171 = playerTickEvent -> {
      if (!this.field09169.isEmpty() && mc.player != null && mc.interactionManager != null) {
         InventoryActionQueue.Nested0447 local4148_2 = this.field09169.get(0);
         if (MovementPauseController.getInstance().getRemainingTicks() < local4148_2.field09174) {
            this.field09170 = false;
            int local4148_3 = !local4148_2.field09176.isEmpty()
                  && !ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local4148_2.field09172), local4148_2.field09176)
               ? InventoryUtils.method0556(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, local4148_2.field09176), 0, 35)
               : local4148_2.field09172;
            if (local4148_3 != -1 && (local4148_3 != local4148_2.field09173 || local4148_2.field09175)) {
               int local4148_4 = this.method2663(local4148_3);
               int local4148_5 = local4148_2.field09175 ? local4148_2.field09173 : this.method2663(local4148_2.field09173);
               if (local4148_2.field09177 != -1) {
                  this.method2658(local4148_2, local4148_4, local4148_5);
               } else if (mc.player.playerScreenHandler.getSlot(local4148_4).getStack().get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
                  this.method2667(local4148_4, 1, SlotActionType.PICKUP);
                  this.method2667(local4148_5, 0, SlotActionType.PICKUP);
                  if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                     this.method2667(local4148_4, 0, SlotActionType.PICKUP);
                  }
               } else {
                  int local4148_6 = this.method2661(local4148_2.field09173, local4148_5);
                  if (local4148_6 != -1) {
                     this.method2667(local4148_4, local4148_6, SlotActionType.SWAP);
                  } else {
                     int local4148_7 = this.method2661(local4148_3, local4148_4);
                     if (local4148_7 != -1) {
                        this.method2667(local4148_5, local4148_7, SlotActionType.SWAP);
                     } else if (local4148_4 != local4148_5) {
                        this.method2667(local4148_4, 0, SlotActionType.SWAP);
                        this.method2667(local4148_5, 0, SlotActionType.SWAP);
                        this.method2667(local4148_4, 0, SlotActionType.SWAP);
                     }
                  }
               }

               mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
               this.method2655(local4148_2);
            } else {
               this.method2655(local4148_2);
            }
         }
      }
   };

   private void method2655(InventoryActionQueue.Nested0447 local4136_1) {
      this.field09169.remove(local4136_1);
      if (!this.field09169.isEmpty()) {
         MovementPauseController.getInstance().pauseForTicks(this.field09169.get(0).field09174);
      }
   }

   public static InventoryActionQueue method2656() {
      if (inventoryActionQueue == null) {
         inventoryActionQueue = new InventoryActionQueue();
         EventBus.getInstance().subscribe(inventoryActionQueue);
      }

      return inventoryActionQueue;
   }

   private void method2657(InventoryActionQueue.Nested0447 local4138_1) {
      if (local4138_1.field09172 != -1 && local4138_1.field09173 != -1) {
         if (this.field09169.isEmpty() && local4138_1.field09174 > 0) {
            MovementPauseController.getInstance().pauseForTicks(local4138_1.field09174);
         }

         this.field09169.add(local4138_1);
      }
   }

   private InventoryActionQueue() {
   }

   private void method2658(InventoryActionQueue.Nested0447 local4140_1, int local4140_2, int local4140_3) {
      int local4140_4 = this.method2663(local4140_1.field09177);
      int local4140_5 = this.method2661(local4140_1.field09173, local4140_3);
      if (local4140_1.field09178 == -1) {
         this.method2667(local4140_4, local4140_5, SlotActionType.SWAP);
      } else if (mc.player.getInventory().getStack(local4140_1.field09177).isEmpty()
         && mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
         if (BundleItem.getSelectedStackIndex(mc.player.playerScreenHandler.getSlot(local4140_2).getStack()) != local4140_1.field09178) {
            mc.player.playerScreenHandler.selectBundleStack(local4140_2, local4140_1.field09178);
            mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(local4140_2, local4140_1.field09178));
         }

         this.method2667(local4140_2, 1, SlotActionType.PICKUP);
         this.method2667(local4140_4, 0, SlotActionType.PICKUP);
         this.method2667(local4140_4, local4140_5, SlotActionType.SWAP);
      }
   }

   public void method2659(int local4141_1, int local4141_2, int local4141_3, int local4141_4, int local4141_5) {
      if (local4141_3 != -1 && local4141_4 != -1) {
         this.method2657(new InventoryActionQueue.Nested0447(local4141_1, local4141_2, local4141_3, local4141_4, local4141_5));
      }
   }

   public void method2660(int local4142_1, int local4142_2, int local4142_3) {
      this.method2657(
         new InventoryActionQueue.Nested0447(
            local4142_1,
            local4142_2,
            local4142_3,
            false,
            mc.player != null && local4142_1 >= 0 && local4142_1 <= 35
               ? mc.player.getInventory().getStack(local4142_1).copy()
               : ItemStack.EMPTY
         )
      );
   }

   private int method2661(int local4143_1, int local4143_2) {
      if (local4143_1 == 40 || local4143_1 == 45 || local4143_2 == 45) {
         return 40;
      } else {
         return local4143_2 >= 36 && local4143_2 <= 44 ? local4143_2 - 36 : -1;
      }
   }

   public void method2662(int local4144_1, int local4144_2, int local4144_3) {
      this.method2657(new InventoryActionQueue.Nested0447(local4144_1, local4144_2, local4144_3, false, ItemStack.EMPTY));
   }

   private int method2663(int local4145_1) {
      return local4145_1 >= 0 && local4145_1 <= 8 ? local4145_1 + 36 : local4145_1;
   }

   public void method2664(int local4146_1, int local4146_2, int local4146_3, int local4146_4) {
      if (local4146_3 != -1) {
         this.method2657(new InventoryActionQueue.Nested0447(local4146_1, local4146_2, local4146_3, -1, local4146_4));
      }
   }

   public boolean method2665() {
      return !this.field09169.isEmpty();
   }

   private void method2667(int local4149_1, int local4149_2, SlotActionType slotActionType) {
      if (local4149_1 != -1 && local4149_2 != -1) {
         if (this.field09170) {
            mc.player.playerScreenHandler.nextRevision();
         }

         this.field09170 = true;
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, local4149_1, local4149_2, slotActionType, mc.player);
      }
   }

   public void method2668(int local4150_1, int local4150_2, int local4150_3) {
      if (mc.player != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         this.method2657(new InventoryActionQueue.Nested0447(local4150_1, local4150_2, local4150_3, true, ItemStack.EMPTY));
      }
   }

   static final class Nested0447 {
      private final int field09172;
      private final int field09173;
      private final int field09174;
      private final boolean field09175;
      private final ItemStack field09176;
      private final int field09177;
      private final int field09178;

      private Nested0447(int local4151_1, int local4151_2, int local4151_3, boolean local4151_4, ItemStack itemStack) {
         this.field09172 = local4151_1;
         this.field09173 = local4151_2;
         this.field09174 = local4151_3;
         this.field09175 = local4151_4;
         this.field09176 = itemStack;
         this.field09177 = -1;
         this.field09178 = -1;
      }

      private Nested0447(int local4152_1, int local4152_2, int local4152_3, int local4152_4, int local4152_5) {
         this.field09172 = local4152_1;
         this.field09173 = local4152_2;
         this.field09174 = local4152_5;
         this.field09175 = false;
         this.field09176 = ItemStack.EMPTY;
         this.field09177 = local4152_3;
         this.field09178 = local4152_4;
      }
   }
}
