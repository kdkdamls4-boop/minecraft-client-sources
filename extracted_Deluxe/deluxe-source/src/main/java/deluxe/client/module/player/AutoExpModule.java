package deluxe.client.module.player;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@ModuleInfo(name = "AutoExp", description = "Пока зажат бинд смотрит в пол, кидает пузырьки опыта и чинит броню мендингом.", category = ModuleCategory.PLAYER)
public final class AutoExpModule extends Module {
   private static AutoExpModule autoExpModule;
   private static final EquipmentSlot[] field13888 = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   public KeybindSetting keybindSetting;
   public BooleanSetting booleanSetting;
   private boolean field13891;
   private int field13892 = -1;
   private int field13893 = -1;
   private int field13894 = -1;
   private boolean field13895;
   private EquipmentSlot field13896;
   private int field13897;
   private final boolean[] field13898 = new boolean[4];
   private final Item[] field13899 = new Item[4];
   private final EventListener<PlayerTickEvent> field13900 = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         boolean local6755_2 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         if (!local6755_2) {
            if (this.field13891) {
               this.method4037();
               this.method4036();
            } else if (!InventoryActionQueue.method2656().method2665()
               && !InventorySlotManager.method4176().method4179()
               && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
               if (this.field13894 != -1) {
                  this.method4039(this.field13893);
               }

               if (this.field13898[0] || this.field13898[1] || this.field13898[2] || this.field13898[3]) {
                  this.method4036();
               }
            }

            this.field13891 = false;
         } else {
            if (!this.field13891) {
               for (int index = 0; index < 4; index++) {
                  if (this.field13899[index] == null) {
                     this.field13899[index] = mc.player.getEquippedStack(field13888[index]).getItem();
                  }
               }
            }

            this.field13891 = true;
            RotationManager.method0658()
               .method0652(new RotationAngles(RotationAngles.method4366(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
            if (this.booleanSetting.isChecked()) {
               this.method4042();
            }

            if (mc.player.getPitch() > 85.0F) {
               this.method4038();
            }
         }
      }
   };

   public static AutoExpModule method4034() {
      if (autoExpModule == null) {
         autoExpModule = new AutoExpModule();
      }

      return autoExpModule;
   }

   private void method4035(int local6758_1, Item item) {
      if (mc.player != null && mc.interactionManager != null && !this.field13891) {
         if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            if (mc.player.getEquippedStack(field13888[local6758_1]).getItem() != item) {
               if (item == Items.AIR) {
                  int local6758_3 = InventoryUtils.method0546(9, 35);
                  if (local6758_3 == -1) {
                     return;
                  }

                  InventoryActionQueue.method2656().method2668(local6758_3, 5 + local6758_1, 1);
               } else {
                  int local6758_3 = InventoryUtils.method0556(itemStack -> itemStack.getItem() == item, 0, 35);
                  if (local6758_3 != -1) {
                     InventoryActionQueue.method2656().method2668(local6758_3, 5 + local6758_1, 1);
                  }
               }
            }

            this.field13898[local6758_1] = false;
            this.field13899[local6758_1] = null;
         }
      }
   }

   private void method4036() {
      this.field13896 = null;

      for (int index = 0; index < 4; index++) {
         if (!this.field13898[index]) {
            this.field13899[index] = null;
         } else if (this.field13899[index] == null) {
            this.field13898[index] = false;
         } else {
            this.method4035(index, this.field13899[index]);
         }
      }
   }

   private void method4037() {
      this.field13895 = false;
      if (this.field13892 != -1) {
         InventoryUtils.method0553(this.field13892, false);
         this.field13892 = -1;
      }

      if (this.field13894 != -1) {
         this.method4039(this.field13893);
      } else {
         this.field13893 = -1;
      }
   }

   private void method4038() {
      if (!mc.player.isUsingItem()) {
         if (this.field13893 != -1) {
            if (this.field13894 == -1) {
               if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
                  this.field13893 = -1;
               }
            } else if (InventoryUtils.method0554() != this.field13894) {
               InventoryUtils.method0553(this.field13894, false);
            } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
               if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
                  this.method4037();
               }
            } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
               InventoryUtils.method0542(Hand.MAIN_HAND);
            }
         } else if (this.field13892 == -1) {
            if (InventoryUtils.method0539(Items.EXPERIENCE_BOTTLE) == -1) {
               if (InventoryUtils.method0551(Items.EXPERIENCE_BOTTLE) == -1) {
                  if (!InventoryUtils.method0534(Items.EXPERIENCE_BOTTLE) && !this.field13895) {
                     this.field13895 = true;
                     NotificationHud.method2691(
                        this.category.getIcon(),
                        Items.EXPERIENCE_BOTTLE.getDefaultStack().getName().getString(),
                        Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
                        true
                     );
                  }
               } else {
                  this.field13893 = InventoryUtils.method0551(Items.EXPERIENCE_BOTTLE);
                  this.field13894 = InventoryUtils.method0554();
                  InventoryActionQueue.method2656().method2662(this.field13893, this.field13894, 1);
               }
            } else {
               this.field13892 = InventoryUtils.method0554();
               InventoryUtils.method0553(InventoryUtils.method0539(Items.EXPERIENCE_BOTTLE), false);
            }
         } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            InventoryUtils.method0553(this.field13892, false);
            this.field13892 = -1;
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.method0542(Hand.MAIN_HAND);
         }
      }
   }

   private AutoExpModule() {
      this.keybindSetting = this.addSetting(
         new KeybindSetting("Кидать опыт", "Пока зажат смотрим в пол и кидаем пузырьки опыта из мешка, инвентаря или хотбара, отпустил всё вернулось.")
      );
      this.booleanSetting = this.addSetting(
         new BooleanSetting("Быстрая починка", "Оставляет надетым только самый поломанный элемент брони и меняет его по мере починки.", false)
      );
   }

   private void method4039(int local6763_1) {
      if (this.field13894 == -1) {
         this.field13893 = -1;
      } else {
         InventoryActionQueue.method2656().method2662(local6763_1, this.field13894, 1);
         this.field13894 = -1;
         this.field13893 = -1;
      }
   }

   private void method4040() {
      int local6764_1 = 0;

      for (int index = 9; index <= 35; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            local6764_1++;
         }
      }

      boolean[] booleanArray = new boolean[4];

      while (local6764_1 > 0) {
         int local6764_4 = -1;
         float local6764_5 = -1.0F;

         for (int index2 = 0; index2 < 4; index2++) {
            if (!booleanArray[index2] && field13888[index2] != this.field13896) {
               ItemStack equippedStack = mc.player.getEquippedStack(field13888[index2]);
               if (!equippedStack.isEmpty()) {
                  float local6764_6 = (this.method4043(equippedStack) ? 10.0F : 0.0F)
                     + (float)(equippedStack.getMaxDamage() - equippedStack.getDamage()) / Math.max(1, equippedStack.getMaxDamage());
                  if (local6764_6 > local6764_5) {
                     local6764_5 = local6764_6;
                     local6764_4 = index2;
                  }
               }
            }
         }

         if (local6764_4 == -1) {
            return;
         }

         booleanArray[local6764_4] = true;
         local6764_1--;
         this.field13898[local6764_4] = true;
         this.method4044(local6764_4);
      }
   }

   private void method4042() {
      if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
         if (this.field13896 != null) {
            ItemStack equippedStack = mc.player.getEquippedStack(this.field13896);
            if (!this.method4043(equippedStack)
               || equippedStack.getMaxDamage() - equippedStack.getDamage() >= Math.max(this.field13897 * 1.3F, this.field13897 + equippedStack.getMaxDamage() * 0.1F)
               )
             {
               this.field13896 = null;
            }
         }

         if (this.field13896 == null) {
            int local6766_1 = -1;
            int local6766_2 = -1;
            float local6766_4 = Float.MAX_VALUE;

            for (int index = 0; index < 4; index++) {
               ItemStack equippedStack2 = mc.player.getEquippedStack(field13888[index]);
               int local6766_3 = -1;
               if (!this.method4043(equippedStack2)) {
                  Item item = this.field13899[index];
                  if (!this.field13898[index] || item == null || item == Items.AIR) {
                     continue;
                  }

                  local6766_3 = InventoryUtils.method0556(itemStack -> itemStack.getItem() == item && this.method4043(itemStack), 0, 35);
                  if (local6766_3 == -1) {
                     continue;
                  }

                  equippedStack2 = mc.player.getInventory().getStack(local6766_3);
               }

               float local6766_5 = (float)(equippedStack2.getMaxDamage() - equippedStack2.getDamage()) / Math.max(1, equippedStack2.getMaxDamage());
               if (local6766_5 < local6766_4) {
                  local6766_4 = local6766_5;
                  local6766_1 = index;
                  local6766_2 = local6766_3;
               }
            }

            if (local6766_1 == -1) {
               return;
            }

            this.field13896 = field13888[local6766_1];
            if (local6766_2 != -1) {
               this.field13897 = mc.player.getInventory().getStack(local6766_2).getMaxDamage()
                  - mc.player.getInventory().getStack(local6766_2).getDamage();
               this.field13898[local6766_1] = true;
               InventoryActionQueue.method2656().method2668(local6766_2, 5 + local6766_1, 1);
               return;
            }

            this.field13897 = mc.player.getEquippedStack(this.field13896).getMaxDamage()
               - mc.player.getEquippedStack(this.field13896).getDamage();
         }

         this.method4040();
      }
   }

   @Override
   protected void onDisable() {
      if (this.field13891) {
         this.method4037();
         this.method4036();
      }

      this.field13891 = false;
   }

   private boolean method4043(ItemStack itemStack) {
      return !itemStack.isEmpty()
         && itemStack.isDamaged()
         && EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.REPAIR_WITH_XP);
   }

   private void method4044(int local6769_1) {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         int local6769_2 = InventoryUtils.method0546(9, 35);
         if (local6769_2 != -1 && !mc.player.getEquippedStack(field13888[local6769_1]).isEmpty()) {
            InventoryActionQueue.method2656().method2668(local6769_2, 5 + local6769_1, 1);
         }
      }
   }
}
