package net.skycore.client.util;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;

public final class InventoryHelper {
   private static final int PLAYER_MENU_SLOT_COUNT = 46;

   public static void closeScreen(boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (condition) {
            ClientPacketListener connection = player.connection;
            ServerboundContainerClosePacket serverboundContainerClosePacket = new ServerboundContainerClosePacket(player.containerMenu.containerId);
            connection.send(serverboundContainerClosePacket);
         } else {
            player.closeContainer();
         }
      }
   }

   public static boolean isServerScreen() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (instance.containerMenu.slots.size() == 46) {
            return false;
         }
      }

      return true;
   }

   private InventoryHelper() {
   }

   public static Slot getSlot(ItemStack itemStack) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && itemStack != null && !itemStack.isEmpty()) {
         for (Slot slot : instance.containerMenu.slots) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty() && item.getItem() == itemStack.getItem() && item.getHoverName().equals(itemStack.getHoverName())) {
               return slot;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void swapHand(Slot slot, InteractionHand interactionHand, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (slot != null && player != null && instance.gameMode != null && slot.index >= 0) {
         int slot2;
         if (interactionHand == InteractionHand.MAIN_HAND) {
            slot2 = player.getInventory().getSelectedSlot();
         } else {
            slot2 = 40;
         }

         int value = slot2;
         instance.gameMode.handleContainerInput(player.containerMenu.containerId, slot.index, value, ContainerInput.SWAP, player);
         if (condition) {
            player.containerMenu.broadcastChanges();
         }
      }
   }

   public static void selectHotbarSlot(int slot) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && slot >= 0 && slot <= 8) {
         if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
            if (player.connection != null) {
               player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            }
         }

         if (instance.gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor) {
            multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$ensureHasSentCarriedItem();
         }
      }
   }

   public static Slot getSlot(Item item, Predicate<Slot> predicate) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return null;
      }


      for (Slot slot : instance.containerMenu.slots) {
         ItemStack item2 = slot.getItem();
         if (item2.getItem() == item && predicate.test(slot)) {
            return slot;
         }
      }

      return null;
   }
}
