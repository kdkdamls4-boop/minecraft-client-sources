package deluxe.client.module.render;

import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(name = "Chams", description = "Показывает выбранных существ сквозь стены вместе с экипировкой.", category = ModuleCategory.RENDER)
public final class ChamsModule extends Module {
   private static ChamsModule chamsModule;
   public MultiSelectSetting multiSelectSetting = this.addSetting(
      new MultiSelectSetting("Кого показывать", "Кого видно сквозь стены.", new String[]{"Игроков", "Мобов", "Голых игроков", "Игроков в незерите"}, "Игроков")
   );

   private ChamsModule() {
   }

   public static ChamsModule method2783() {
      if (chamsModule == null) {
         chamsModule = new ChamsModule();
      }

      return chamsModule;
   }

   public boolean method2784(LivingEntity livingEntity) {
      if (this.isEnabled() && livingEntity != mc.player && !(livingEntity instanceof ArmorStandEntity)) {
         if (!(livingEntity instanceof PlayerEntity playerEntity)) {
            return this.multiSelectSetting.matchesOption("Мобов");
         } else {
            if (this.multiSelectSetting.matchesOption("Игроков")) {
               return true;
            }

            int local4381_3 = 0;
            int local4381_4 = 0;

            for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
               ItemStack equippedStack = playerEntity.getEquippedStack(equipmentSlot);
               if (!equippedStack.isEmpty()) {
                  local4381_3++;
                  if (equippedStack.isOf(Items.NETHERITE_HELMET)
                     || equippedStack.isOf(Items.NETHERITE_CHESTPLATE)
                     || equippedStack.isOf(Items.NETHERITE_LEGGINGS)
                     || equippedStack.isOf(Items.NETHERITE_BOOTS)) {
                     local4381_4++;
                  }
               }
            }

            return this.multiSelectSetting.matchesOption("Голых игроков") && local4381_3 == 0 || this.multiSelectSetting.matchesOption("Игроков в незерите") && local4381_4 > 0;
         }
      } else {
         return false;
      }
   }
}
