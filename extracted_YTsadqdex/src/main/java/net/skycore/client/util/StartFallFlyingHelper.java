package net.skycore.client.util;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class StartFallFlyingHelper {
   private StartFallFlyingHelper() {
   }

   public static void startFallFlying() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (player.connection != null) {
            ClientPacketListener connection = player.connection;
            ServerboundPlayerCommandPacket serverboundPlayerCommandPacket = new ServerboundPlayerCommandPacket(player, Action.START_FALL_FLYING);
            connection.send(serverboundPlayerCommandPacket);
            player.startFallFlying();
            return;
         }
      }
   }

   public static ServerboundContainerClickPacket createClickSlotPacket(int value, int value2, int value3, ContainerInput containerInput, Player player) {
      if (player == null) {
         return null;
      }

      AbstractContainerMenu containerMenu = player.containerMenu;
      if (value != containerMenu.containerId) {
         return null;
      }

      NonNullList<Slot> slots = containerMenu.slots;
      int size = slots.size();
      var objects = Lists.newArrayListWithCapacity(size);

      for (Slot slot : slots) {
         objects.add(slot.getItem().copy());
      }

      containerMenu.clicked(value2, value3, containerInput, player);
      Int2ObjectOpenHashMap lookup = new Int2ObjectOpenHashMap();

      for (int value4 = 0; value4 < size; value4++) {
         ItemStack itemStack = (ItemStack)objects.get(value4);
         ItemStack item = ((Slot)slots.get(value4)).getItem();
         if (!ItemStack.matches(itemStack, item)) {
            lookup.put(value4, item.copy());
         }
      }

      return new ServerboundContainerClickPacket(
         value, containerMenu.getStateId(), (short)value2, (byte)value3, containerInput, HashStacksHelper.hashStacks(lookup), HashStacksHelper.hash(containerMenu.getCarried().copy())
      );
   }
}
