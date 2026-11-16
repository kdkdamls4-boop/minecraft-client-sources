package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.ContainerRenderEvent;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.setting.SectionSetting;
import deluxe.client.module.player.ServerHelperModule;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;

@ModuleInfo(name = "ItemHelper", description = "Использует предмет, пока зажата его клавиша.", category = ModuleCategory.COMBAT)
public final class ItemHelperModule extends Module {
   private static ItemHelperModule itemHelperModule;
   public BooleanSetting field12911;
   public NumberSetting numberSetting;
   public BooleanSetting field12913;
   public BooleanSetting field12914;
   public BooleanSetting field12915;
   public BooleanSetting field12916;
   public final EventListener<PlayerTickEvent> field12917 = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         for (ItemHelperModule.Nested0503 local6148_5 : ItemHelperModule.Nested0503.values()) {
            boolean local6148_6 = mc.currentScreen == null && local6148_5.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
            if (!local6148_6) {
               if (local6148_5.field12945) {
                  mc.options.useKey.setPressed(false);
                  local6148_5.field12945 = false;
               }

               this.method3718(local6148_5);
            } else {
               local6148_5.field12945 = true;
               int local6148_7 = local6148_5 == ItemHelperModule.Nested0503.HEAL_POTION
                  ? InventoryUtils.method0555(this::method3716)
                  : InventoryUtils.method0557(local6148_5.field12942);
               if (local6148_7 != -1 && local6148_5.field12948 < 0) {
                  if (local6148_7 <= 8) {
                     local6148_5.field12948 = InventoryUtils.method0554();
                     mc.player.getInventory().setSelectedSlot(local6148_7);
                  } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler && !InventoryActionQueue.method2656().method2665()) {
                     local6148_5.field12948 = InventoryUtils.method0554();
                     local6148_5.field12947 = local6148_7;
                     InventoryActionQueue.method2656().method2662(local6148_7, local6148_5.field12948, 1);
                  }
               }

               if (mc.player.getMainHandStack().getItem() == local6148_5.field12942) {
                  if (local6148_5 == ItemHelperModule.Nested0503.CROSSBOW
                     && mc.player.isUsingItem()
                     && (
                        CrossbowItem.isCharged(mc.player.getMainHandStack())
                           || mc.player.getItemUseTime() > CrossbowItem.getPullTime(mc.player.getMainHandStack(), mc.player) + 3
                     )) {
                     if (mc.player.isUsingItem()) {
                        mc.interactionManager.stopUsingItem(mc.player);
                     }

                     mc.options.useKey.setPressed(false);
                  } else {
                     mc.options.useKey.setPressed(true);
                     if (this.field12911.isChecked() && local6148_5 != ItemHelperModule.Nested0503.CROSSBOW && mc.player.getItemUseTime() >= 33) {
                        mc.interactionManager.stopUsingItem(mc.player);
                        mc.options.useKey.setPressed(false);
                        local6148_5.field12945 = false;
                     }
                  }
               }
            }
         }
      }
   };
   public final EventListener<ContainerRenderEvent> field12918 = containerRenderEvent -> {
      if (this.method3712()) {
         for (Slot slot : containerRenderEvent.method3212().getScreenHandler().slots) {
            if (slot.hasStack()) {
               int local6153_2 = this.method3707(slot.getStack());
               if (local6153_2 != -1) {
                  containerRenderEvent.method3213().fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -2147483648 | local6153_2);
               }
            }
         }
      }
   };

   public int method3707(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return -1;
      } else if (this.field12914.isChecked() && itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         return 58879;
      } else if (this.field12913.isChecked() && this.method3716(itemStack)) {
         return itemStack.get(DataComponentTypes.POTION_CONTENTS).getColor() & 16777215;
      } else if (this.field12915.isChecked() && itemStack.getItem() == Items.GOLDEN_APPLE) {
         return 14725168;
      } else {
         return this.field12916.isChecked() && itemStack.getItem() == Items.CHORUS_FRUIT ? 10049023 : -1;
      }
   }

   private ItemHelperModule() {
      this.addSetting(new SectionSetting("Предметы"));

      for (ItemHelperModule.Nested0503 local6146_4 : ItemHelperModule.Nested0503.values()) {
         local6146_4.keybindSetting = this.addSetting(new KeybindSetting(local6146_4.field12943, "Держи возьмёт предмет в руку и использует."));
      }

      this.addSetting(new SectionSetting("Подсвечивание"));
      this.numberSetting = this.addSetting(
         new NumberSetting("Подсвечивать при здоровье", "Подсвечивает лечащие предметы, когда здоровье не выше этого.")
            .withRange(0.0F, 20.0F)
            .withStep(1.0F)
            .setValue(8.0F)
      );
      this.field12913 = this.addSetting(new BooleanSetting("Зелье исцеление", "Подсвечивает зелья мгновенного исцеления.", false));
      this.field12914 = this.addSetting(new BooleanSetting("Чарка", "Подсвечивает зачарованные яблоки.", false));
      this.field12915 = this.addSetting(new BooleanSetting("Гепл", "Подсвечивает золотые яблоки.", false));
      this.field12916 = this.addSetting(new BooleanSetting("Хорус", "Подсвечивает плоды хоруса.", false));
      this.field12911 = this.addSetting(new BooleanSetting("Отжимать при выполнении", "Сам отпускает использование, когда предмет доеден.", true));
   }

   public int method3708() {
      if (this.isEnabled() && mc.player != null) {
         for (ItemHelperModule.Nested0503 local6147_4 : ItemHelperModule.Nested0503.values()) {
            if (local6147_4.field12945
               && local6147_4.field12947 == -1
               && local6147_4.field12948 != -1
               && local6147_4.field12948 != InventoryUtils.method0554()) {
               return local6147_4.field12948;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public boolean method3710(ItemStack itemStack) {
      return mc.world != null && itemStack.getItem() == Items.MACE
         ? EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.BREACH), itemStack)
               >= 3
            && EnchantmentHelper.getLevel(
                  mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.DENSITY), itemStack
               )
               >= 5
         : false;
   }

   public static ItemHelperModule method3711() {
      if (itemHelperModule == null) {
         itemHelperModule = new ItemHelperModule();
      }

      return itemHelperModule;
   }

   public boolean method3712() {
      return mc.player != null
         && (
            mc.player.getHealth() <= this.numberSetting.floatValue()
               || ServerHelperModule.method0707().isEnabled() && ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime")
         );
   }

   public void method3713(DrawContext drawContext) {
      if (this.method3712()) {
         int local6152_2 = drawContext.getScaledWindowWidth() / 2 - 91;

         for (int index = 0; index < 9; index++) {
            int local6152_3 = this.method3707(mc.player.getInventory().getStack(index));
            if (local6152_3 != -1) {
               drawContext.fill(
                  local6152_2 + 3 + index * 20,
                  drawContext.getScaledWindowHeight() - 19,
                  local6152_2 + 3 + index * 20 + 16,
                  drawContext.getScaledWindowHeight() - 3,
                  -2147483648 | local6152_3
               );
            }
         }
      }
   }

   public ItemHelperModule.Nested0503[] method3715() {
      return ItemHelperModule.Nested0503.values();
   }

   private boolean method3716(ItemStack itemStack) {
      if (itemStack.getItem() == Items.POTION && itemStack.get(DataComponentTypes.POTION_CONTENTS) != null) {
         for (StatusEffectInstance statusEffectInstance : itemStack.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
            if (statusEffectInstance.getEffectType() == StatusEffects.INSTANT_HEALTH) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean method3717() {
      if (!this.isEnabled()) {
         return false;
      }

      for (ItemHelperModule.Nested0503 local6156_4 : ItemHelperModule.Nested0503.values()) {
         if (local6156_4.field12945) {
            return true;
         }
      }

      return false;
   }

   private void method3718(ItemHelperModule.Nested0503 local6157_1) {
      if (local6157_1.field12948 != -1) {
         if (local6157_1.field12947 == -1) {
            mc.player.getInventory().setSelectedSlot(local6157_1.field12948);
            local6157_1.field12948 = -1;
         } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            InventoryActionQueue.method2656().method2662(local6157_1.field12947, local6157_1.field12948, 1);
            local6157_1.field12947 = -1;
            local6157_1.field12948 = -1;
         }
      }
   }

   public enum Nested0503 {
      CROSSBOW(Items.CROSSBOW, "Арбалет"),
      CHORUS_FRUIT(Items.CHORUS_FRUIT, "Плод хоруса"),
      GOLDEN_APPLE(Items.GOLDEN_APPLE, "Золотое яблоко"),
      HEAL_POTION(Items.POTION, "Зелье исцеления"),
      ENCHANTED_GOLDEN_APPLE(Items.ENCHANTED_GOLDEN_APPLE, "Зачар. яблоко");

      final Item field12942;
      final String field12943;
      KeybindSetting keybindSetting;
      boolean field12945;
      boolean field12946;
      int field12947 = -1;
      int field12948 = -1;

      Nested0503(Item item, String local6160_4) {
         this.field12942 = item;
         this.field12943 = local6160_4;
      }

      public boolean method3719(ItemStack itemStack) {
         if (itemStack.isEmpty()) {
            return false;
         } else {
            return this == HEAL_POTION ? ItemHelperModule.method3711().method3716(itemStack) : itemStack.getItem() == this.field12942;
         }
      }

         public Item method3720() {
         return this.field12942;
      }

         public KeybindSetting getKeybindSetting() {
         return this.keybindSetting;
      }
   }
}
