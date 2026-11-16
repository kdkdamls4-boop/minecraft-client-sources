package net.skycore.client.util;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;

public final class UseItemHelper {
   public static void useItem(InteractionHand interactionHand) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (instance.gameMode != null && interactionHand != null) {
            instance.gameMode.useItem(player, interactionHand);
            return;
         }
      }
   }

   public static int getSlot(Item item) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && item != null) {
         for (int value = 0; value < 36; value++) {
            ItemStack item2 = player.getInventory().getItem(value);
            if (!item2.isEmpty() && item2.is(item)) {
               return value;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static int getChestPlateSlot() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return -1;
      }

      for (int value = 0; value < 36; value++) {
         ItemStack item = instance.getInventory().getItem(value);
         if (!item.isEmpty() && !item.is(Items.ELYTRA)) {
            Equippable equippable = item.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
               return value;
            }
         }
      }

      return -1;
   }

   public static void inventorySwapClick(Item item) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.gameMode != null && item != null) {
         if (player.getMainHandItem().is(item)) {
            useItem(InteractionHand.MAIN_HAND);
         } else if (player.getOffhandItem().is(item)) {
            useItem(InteractionHand.OFF_HAND);
         } else {
            int slot = getSlot(item);
            if (slot != -1) {
               inventorySwapClickSlot(slot);
            }
         }
      }
   }

   public static int antipoletToOffhand(Item item) {
      int slot = getSlot(item);
      if (slot == -1) {
         return -1;
      }

      int result = toMenuSlot(slot);
      moveItem(result, 45);
      return result;
   }

   public static int findHotbarSlot(Item item, int result) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return -1;
      }

      if (result >= 0 && result <= 8) {
         Inventory inventory = instance.getInventory();
         if (inventory.getItem(result).is(item)) {
            return result;
         }
      }

      for (int value = 0; value < 9; value++) {
         ItemStack item2 = instance.getInventory().getItem(value);
         if (item2.is(item)) {
            return value;
         }
      }

      return -1;
   }

   private UseItemHelper() {
   }

   public static void inventorySwapClickSlot(int slot) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.gameMode != null && slot >= 0) {
         int selectedSlot = player.getInventory().getSelectedSlot();
         if (slot < 9) {
            if (slot != selectedSlot) {
               switchToHotbarSlot(slot);
            }

            useItem(InteractionHand.MAIN_HAND);
            if (slot != selectedSlot) {
               switchToHotbarSlot(selectedSlot);
            }
         } else {
            int slot2 = toMenuSlot(slot);
            clickSlotId(slot2, selectedSlot, ContainerInput.SWAP, false);
            syncCurrentPlayItem();
            useItem(InteractionHand.MAIN_HAND);
            clickSlotId(slot2, selectedSlot, ContainerInput.SWAP, false);
            syncCurrentPlayItem();
         }
      }
   }

   public static void moveItem(int slot, int slot2) {
      if (slot != -1 && slot != slot2) {
         int slot3 = toMenuSlot(slot);
         if (slot2 == 45) {
            clickSlotId(slot3, 40, ContainerInput.SWAP, false);
         } else {
            clickSlotId(slot3, 0, ContainerInput.SWAP, false);
            clickSlotId(slot2, 0, ContainerInput.SWAP, false);
            clickSlotId(slot3, 0, ContainerInput.SWAP, false);
         }
      }
   }

   public static int getItemSlot(Item item) {
      int slot = getSlot(item);
      if (slot >= 0 && slot < 9) {
         slot += 36;
      }

      return slot;
   }

   public static int findEmptyInventorySlot() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return -1;
      }

      for (int value = 9; value < 36; value++) {
         if (instance.getInventory().getItem(value).isEmpty()) {
            return value;
         }
      }

      for (int value2 = 0; value2 < 9; value2++) {
         if (instance.getInventory().getItem(value2).isEmpty()) {
            return toMenuSlot(value2);
         }
      }

      return -1;
   }

   public static int findSlotByNameContains(String... stringValues) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && stringValues != null && stringValues.length != 0) {
         for (int value = 0; value < 36; value++) {
            ItemStack item = instance.getInventory().getItem(value);
            if (!item.isEmpty()) {
               String hoverName = item.getHoverName().getString().toLowerCase(Locale.ROOT);

               for (String text : stringValues) {
                  if (text != null && !text.isEmpty() && hoverName.contains(text.toLowerCase(Locale.ROOT))) {
                     return value;
                  }
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static void moveItemSwap(int slot, int slot2) {
      if (slot != -1 && slot != slot2) {
         int slot3 = toMenuSlot(slot);
         clickSlotId(slot3, 0, ContainerInput.SWAP, false);
         clickSlotId(slot2, 0, ContainerInput.SWAP, false);
         clickSlotId(slot3, 0, ContainerInput.SWAP, false);
      }
   }

   public static void closeInventoryQuietly() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && player.connection != null && Minecraft.getInstance().gui.screen() == null) {
         ClientPacketListener connection = player.connection;
         ServerboundContainerClosePacket serverboundContainerClosePacket = new ServerboundContainerClosePacket(player.containerMenu.containerId);
         connection.send(serverboundContainerClosePacket);
      }
   }

   public static int toMenuSlot(int value) {
      return value < 9 ? value + 36 : value;
   }

   public static void switchToHotbarSlot(int slot) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && slot >= 0 && slot <= 8) {
         Inventory inventory = player.getInventory();
         if (inventory.getSelectedSlot() != slot) {
            Inventory inventory2 = player.getInventory();
            inventory2.setSelectedSlot(slot);
            syncCurrentPlayItem();
         }
      }
   }

   public static void clickSlotId(int value, int value2, ContainerInput containerInput, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.gameMode != null) {
         RunWithBypassHelper.runWithBypass(() -> {
            instance.gameMode.handleContainerInput(player.containerMenu.containerId, value, value2, containerInput, player);
         });
      }
   }

   public static void moveToArmor(int slot, int slot2) {
      if (slot != -1) {
         int slot3 = toMenuSlot(slot);
         clickSlotId(slot3, 40, ContainerInput.SWAP, false);
         clickSlotId(slot2, 40, ContainerInput.SWAP, false);
         clickSlotId(slot3, 40, ContainerInput.SWAP, false);
      }
   }

   public static void pickupItem(int slot, int slot2) {
      clickSlotId(slot, slot2, ContainerInput.PICKUP, false);
   }

   public static void syncCurrentPlayItem() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (instance.gameMode != null) {
            if (instance.gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor) {
               multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$ensureHasSentCarriedItem();
            }

            return;
         }
      }
   }
}
