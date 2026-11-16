package net.skycore.client.module.player;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.player.Inventory;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.MultiSelectSetting;

public final class LockSlotModule extends ClientModule {
   private final MultiSelectSetting slots = this.add(
      new MultiSelectSetting("Slots", List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), Set.of())
   );
   public static final LockSlotModule INSTANCE = new LockSlotModule();

   private LockSlotModule() {
      super("Lock Slot", "Prevents dropping items from selected slots", ModuleCategory.PLAYER);
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::onPacket);
   }

   private void onPacket(PacketSendEvent packetSendEvent) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null && packetSendEvent.getPacket() instanceof ServerboundPlayerActionPacket serverboundPlayerActionPacket) {
         if (serverboundPlayerActionPacket.getAction() == Action.DROP_ITEM || serverboundPlayerActionPacket.getAction() == Action.DROP_ALL_ITEMS) {
            MultiSelectSetting slots2 = this.slots;
            Inventory inventory = instance.player.getInventory();
            if (slots2.isSelected(Integer.toString(inventory.getSelectedSlot() + 1))) {
               packetSendEvent.cancel();
            }
         }
      }
   }
}
