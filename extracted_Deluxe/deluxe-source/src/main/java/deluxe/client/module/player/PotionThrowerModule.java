package deluxe.client.module.player;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.event.EventListener;
import deluxe.client.util.InputBinding;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.ui.PotionWheelScreen;
import deluxe.client.util.InventoryUtils;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.storage.AutoBuyItem;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "PotionThrower", description = "Быстрое метание бафов через колесо.", category = ModuleCategory.PLAYER)
public final class PotionThrowerModule extends Module {
   private static PotionThrowerModule potionThrowerModule;
   public KeybindSetting keybindSetting;
   public LegacyNumberSetting legacyNumberSetting;
   public NumericTextSetting numericTextSetting;
   private final ArrayList<String> field09095 = new ArrayList<>();
   private final PotionWheelScreen potionWheelScreen = new PotionWheelScreen();
   private boolean field09097;
   private final EventListener<PlayerTickEvent> field09098 = playerTickEvent -> {
      if (mc.player != null) {
         this.method2643();
      }
   };

   public boolean method2630() {
      return this.keybindSetting.isBound() && this.method2640();
   }

   public void method2631() {
      String local4101_1 = String.join(";", this.field09095);
      ArrayList<String> local4101_2 = new ArrayList<>(this.field09095);
      this.numericTextSetting.setValue(local4101_1);
      this.field09095.clear();
      this.field09095.addAll(local4101_2);
   }

   public void method2633(AutoBuyItem autoBuyItem) {
      if (autoBuyItem != null
         && mc.player != null
         && mc.interactionManager != null
         && !InventorySlotManager.method4176().method4179()
         && !InventoryActionQueue.method2656().method2665()) {
         if (InventoryUtils.method0555(itemStack -> AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) != -1) {
            ServerHelperModule.method0707().method0686(autoBuyItem.name());
            InventorySlotManager.method4176()
               .method4195(
                  mc.player
                     .getInventory()
                     .getStack(InventoryUtils.method0555(itemStack2 -> AutoBuyItem.CHECKER.test(itemStack2, autoBuyItem)))
                     .copy(),
                  true
               );
         } else if (InventoryUtils.method0555(itemStack3 -> this.method2637(itemStack3, autoBuyItem) != -1) != -1
            && InventoryUtils.method0546(0, 35) != -1) {
            ServerHelperModule.method0707().method0686(autoBuyItem.name());
            InventorySlotManager.method4176()
               .method4195(
                  mc.player
                     .getInventory()
                     .getStack(InventoryUtils.method0555(itemStack4 -> this.method2637(itemStack4, autoBuyItem) != -1))
                     .get(DataComponentTypes.BUNDLE_CONTENTS)
                     .get(
                        this.method2637(
                           mc.player
                              .getInventory()
                              .getStack(InventoryUtils.method0555(itemStack5 -> this.method2637(itemStack5, autoBuyItem) != -1)),
                           autoBuyItem
                        )
                     )
                     .copy(),
                  true
               );
         }
      }
   }

   public AutoBuyItem method2634(int local4104_1) {
      if (local4104_1 >= 0 && local4104_1 < this.method2636().size() && !this.method2636().get(local4104_1).isEmpty()) {
         for (AutoBuyItem autoBuyItem : AuctionItemCatalog.method1077()) {
            if (autoBuyItem.name().equals(this.method2636().get(local4104_1))) {
               return autoBuyItem;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public ArrayList<String> method2636() {
      if (this.field09095.isEmpty()) {
         for (String local4106_4 : this.numericTextSetting.getValue().split(";", -1)) {
            this.field09095.add(local4106_4);
         }
      }

      int local4106_1 = Math.max(2, Math.min(7, this.legacyNumberSetting.intValue()));

      while (this.field09095.size() < local4106_1) {
         this.field09095.add("");
      }

      while (this.field09095.size() > local4106_1) {
         this.field09095.remove(this.field09095.size() - 1);
      }

      return this.field09095;
   }

   private int method2637(ItemStack itemStack, AutoBuyItem autoBuyItem) {
      if (itemStack.get(DataComponentTypes.BUNDLE_CONTENTS) == null) {
         return -1;
      }

      for (int index = 0; index < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index++) {
         if (AutoBuyItem.CHECKER.test(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index), autoBuyItem)) {
            return index;
         }
      }

      return -1;
   }

   public ArrayList<AutoBuyItem> method2639() {
      ArrayList<AutoBuyItem> local4109_1 = new ArrayList<>();
      AuctionItemCatalog.method1056();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.method1077()) {
         if (autoBuyItem.item() == Items.SPLASH_POTION) {
            local4109_1.add(autoBuyItem);
         }
      }

      return local4109_1;
   }

   private boolean method2640() {
      return this.keybindSetting.getBinding().getDevice() == InputBinding.DeviceType.MOUSE
         ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), this.keybindSetting.getBinding().getCode()) == 1
         : GLFW.glfwGetKey(mc.getWindow().getHandle(), this.keybindSetting.getBinding().getCode()) == 1;
   }

   public static PotionThrowerModule method2641() {
      if (potionThrowerModule == null) {
         potionThrowerModule = new PotionThrowerModule();
      }

      return potionThrowerModule;
   }

   private PotionThrowerModule() {
      this.keybindSetting = this.addSetting(
         new KeybindSetting("Открыть колесо", "Удерживай клавишу, наводи на зелье и отпускай, чтобы кинуть.", InputBinding.keyboardKey(86))
      );
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Количество зелий", "Сколько секторов в колесе.", 3.0, 2.0, 7.0, 1.0));
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Слоты колеса", "", "", 512));
      this.numericTextSetting.withVisibility(() -> false);
      this.numericTextSetting.onChange(this.field09095::clear);
   }

   private void method2643() {
      if (this.keybindSetting.isBound()) {
         boolean local4114_1 = this.method2640() && (mc.currentScreen == null || mc.currentScreen == this.potionWheelScreen);
         if (local4114_1 && !this.field09097 && (mc.currentScreen == null || this.potionWheelScreen.closing())) {
            mc.setScreen(this.potionWheelScreen);
         }

         if (!local4114_1 && this.field09097 && mc.currentScreen == this.potionWheelScreen && !this.potionWheelScreen.pickerOpen() && !this.potionWheelScreen.closing()) {
            if (this.potionWheelScreen.draggedSlot() != -1) {
               this.potionWheelScreen.releaseHeldSlot(this.potionWheelScreen.hoveredSlot());
            } else if (this.potionWheelScreen.hoveredSlot() != -1) {
               this.method2633(this.method2634(this.potionWheelScreen.hoveredSlot()));
            }

            this.potionWheelScreen.startClosing();
         }

         this.field09097 = local4114_1;
      }
   }
}
