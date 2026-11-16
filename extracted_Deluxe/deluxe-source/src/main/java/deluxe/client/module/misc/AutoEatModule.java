package deluxe.client.module.misc;

import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;

@ModuleInfo(name = "AutoEat", description = "Сам ест из хотбара, когда голод падает ниже порога.", category = ModuleCategory.MISC)
public final class AutoEatModule extends Module {
   private static AutoEatModule autoEatModule;
   public LegacyNumberSetting legacyNumberSetting;
   private int field13429 = -1;
   private boolean field13430;
   private final EventListener<PlayerTickEvent> field13431 = playerTickEvent -> {
      if (mc.player != null
         && mc.world != null
         && mc.interactionManager != null
         && !mc.player.isCreative()
         && !mc.player.isSpectator()) {
         boolean local6349_2 = mc.player.getHungerManager().getFoodLevel() <= this.legacyNumberSetting.intValue();
         if (!this.field13430) {
            if (!local6349_2) {
               return;
            }

            int local6349_3 = this.method3866();
            if (local6349_3 == -1) {
               return;
            }

            this.field13429 = InventoryUtils.method0554();
            if (local6349_3 != this.field13429) {
               InventoryUtils.method0553(local6349_3, false);
            }

            this.field13430 = true;
         }

         if (!mc.player.getInventory().getStack(InventoryUtils.method0554()).contains(DataComponentTypes.FOOD)) {
            int local6349_3 = this.method3866();
            if (local6349_3 == -1) {
               this.method3867();
               return;
            }

            InventoryUtils.method0553(local6349_3, false);
         }

         if (!mc.player.isUsingItem()
            && mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND) instanceof Success success
            && success.swingSource() == SwingSource.CLIENT) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         if (!local6349_2 && !mc.player.isUsingItem()) {
            this.method3867();
         }
      } else {
         this.method3867();
      }
   };

   private int method3866() {
      int local6350_1 = -1;
      int local6350_2 = 0;

      for (int index = 0; index < 9; index++) {
         ItemStack stack = mc.player.getInventory().getStack(index);
         FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
         if (foodComponent != null
            && !stack.isOf(Items.ROTTEN_FLESH)
            && !stack.isOf(Items.SPIDER_EYE)
            && !stack.isOf(Items.POISONOUS_POTATO)
            && !stack.isOf(Items.PUFFERFISH)
            && !stack.isOf(Items.CHORUS_FRUIT)
            && foodComponent.nutrition() > local6350_2) {
            local6350_2 = foodComponent.nutrition();
            local6350_1 = index;
         }
      }

      return local6350_1;
   }

   @Override
   protected void onDisable() {
      this.method3867();
   }

   private void method3867() {
      if (this.field13430) {
         if (mc.player != null && mc.interactionManager != null && mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
         }

         int local6352_1 = this.field13429;
         if (local6352_1 != -1 && local6352_1 != InventoryUtils.method0554()) {
            InventoryUtils.method0553(local6352_1, false);
         }
      }

      this.field13430 = false;
      this.field13429 = -1;
   }

   private AutoEatModule() {
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Голод", "Есть, когда уровень голода не выше этого.", 16.0, 1.0, 19.0, 1.0));
   }

   public static AutoEatModule method3868() {
      if (autoEatModule == null) {
         autoEatModule = new AutoEatModule();
      }

      return autoEatModule;
   }
}
