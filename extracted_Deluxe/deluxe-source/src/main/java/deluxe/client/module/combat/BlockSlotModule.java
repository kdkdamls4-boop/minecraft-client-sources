package deluxe.client.module.combat;

import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.SlotClickEvent;

import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "BlockSlot", description = "Не даёт выкинуть предмет из выбранных слотов хотбара.", category = ModuleCategory.COMBAT)
public final class BlockSlotModule extends Module {
   private static BlockSlotModule blockSlotModule;
   public MultiSelectSetting multiSelectSetting;
   private final EventListener<SlotClickEvent> field04153 = slotClickEvent -> {
      if (mc.player != null
         && slotClickEvent.method2988() == SlotActionType.THROW
         && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         if (slotClickEvent.method2983() >= 36 && slotClickEvent.method2983() <= 44 && this.method1378(slotClickEvent.method2983() - 36)) {
            slotClickEvent.method4423();
         }
      }
   };

   public boolean method1378(int local2017_1) {
      return this.isEnabled() && this.multiSelectSetting.isIndexSelected(local2017_1);
   }

   public static BlockSlotModule method1380() {
      if (blockSlotModule == null) {
         blockSlotModule = new BlockSlotModule();
      }

      return blockSlotModule;
   }

   private BlockSlotModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Слоты", "Какие слоты хотбара защищать от выброса.", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"})
      );
   }
}
