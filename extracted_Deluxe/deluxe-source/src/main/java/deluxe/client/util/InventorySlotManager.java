package deluxe.client.util;

import deluxe.mixin.ClientPlayerInteractionManagerMixin2;
import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class InventorySlotManager implements MinecraftAccess {
   private static InventorySlotManager inventorySlotManager;
   private final ArrayList<InventorySlotManager.Nested0520> field14368 = new ArrayList<>();
   private int field14369 = -1;
   private ItemStack field14370 = ItemStack.EMPTY;
   private int field14371;
   private final EventListener<PlayerTickEvent> field14372 = playerTickEvent -> {
      if (!this.field14368.isEmpty() && mc.player != null && mc.interactionManager != null) {
         InventorySlotManager.Nested0520 local6992_2 = this.field14368.get(0);
         int local6992_3 = this.method4178(local6992_2.field14374);
         int local6992_4 = this.method4190(local6992_2.field14374);
         if (local6992_2.field14382 != -1
            || (local6992_3 != -1 || local6992_4 != -1) && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.field14374)) {
            if (local6992_2.field14382 < 0 || !InventoryActionQueue.method2656().method2665()) {
               local6992_2.field14382++;
               if (local6992_2.field14382 == 0) {
                  local6992_2.field14376 = InventoryUtils.method0554();
                  if (local6992_3 != -1) {
                     local6992_2.field14377 = local6992_3;
                     if (local6992_3 != local6992_2.field14376) {
                        InventoryUtils.method0553(local6992_3, false);
                     }
                  } else if (local6992_4 != -1) {
                     int local6992_5 = this.method4196(
                        mc.player.getInventory().getStack(local6992_4), local6992_2.field14374, this.method4186(local6992_2.field14374)
                     );
                     if (local6992_5 == -1) {
                        local6992_2.field14377 = local6992_4;
                        if (!local6992_2.__) {
                           InventoryActionQueue.method2656().method2662(local6992_4, local6992_2.field14376, 1);
                        } else {
                           local6992_2.field14379 = this.method4177(local6992_2.field14376, local6992_4, -1);
                           this.method4194(local6992_2.field14379);
                           InventoryActionQueue.method2656().method2662(local6992_4, local6992_2.field14379, 1);
                        }
                     } else {
                        local6992_2.field14378 = InventoryUtils.method0546(9, 35);
                        if (local6992_2.field14378 == -1) {
                           local6992_2.field14378 = InventoryUtils.method0546(0, 8);
                        }

                        if (local6992_2.field14378 == -1) {
                           this.field14368.remove(local6992_2);
                        } else {
                           local6992_2.field14380 = local6992_4;
                           if (!local6992_2.__) {
                              if (local6992_4 == local6992_2.field14376 && InventoryUtils.method0546(0, 8) != -1) {
                                 local6992_2.field14381 = local6992_2.field14376;
                                 local6992_2.field14376 = InventoryUtils.method0546(0, 8);
                                 InventoryUtils.method0553(local6992_2.field14376, false);
                              }

                              local6992_2.field14377 = local6992_2.field14376;
                              local6992_2.field14375 = mc.player.getInventory().getStack(local6992_2.field14376).copy();
                              InventoryActionQueue.method2656().method2659(local6992_4, local6992_2.field14376, local6992_2.field14378, local6992_5, 1);
                           } else {
                              local6992_2.field14379 = this.method4177(local6992_2.field14376, local6992_4, local6992_2.field14378);
                              this.method4194(local6992_2.field14379);
                              local6992_2.field14377 = local6992_2.field14379;
                              local6992_2.field14375 = mc.player.getInventory().getStack(local6992_2.field14379).copy();
                              InventoryActionQueue.method2656().method2659(local6992_4, local6992_2.field14379, local6992_2.field14378, local6992_5, 1);
                           }
                        }
                     }
                  }
               } else if (local6992_2.field14382 == 1) {
                  if (local6992_2.field14379 != -1) {
                     if (this.method4184(mc.player.getInventory().getStack(local6992_2.field14379), local6992_2.field14374)
                        && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.field14374)) {
                        InventoryUtils.method0553(local6992_2.field14379, true);
                        ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                           .invokeStartPrediction(
                              mc.world,
                              local7000_0 -> new PlayerInteractItemC2SPacket(
                                 Hand.MAIN_HAND, local7000_0, mc.player.getYaw(), mc.player.getPitch()
                              )
                           );
                        InventoryUtils.method0553(local6992_2.field14376, true);
                     }

                     if (local6992_2.field14380 != -1) {
                        if (!ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local6992_2.field14379), local6992_2.field14375)) {
                           InventoryActionQueue.method2656().method2664(local6992_2.field14380, local6992_2.field14379, local6992_2.field14378, 1);
                        }
                     } else {
                        InventoryActionQueue.method2656().method2662(local6992_2.field14379, local6992_2.field14377, 1);
                     }

                     this.field14368.remove(local6992_2);
                  } else {
                     if (this.method4184(mc.player.getMainHandStack(), local6992_2.field14374)
                        && !mc.player.getItemCooldownManager().isCoolingDown(local6992_2.field14374)) {
                        ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                           .invokeStartPrediction(
                              mc.world,
                              local6981_0 -> new PlayerInteractItemC2SPacket(
                                 Hand.MAIN_HAND, local6981_0, mc.player.getYaw(), mc.player.getPitch()
                              )
                           );
                     }

                     if (local6992_2.field14380 != -1) {
                        if (!ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(local6992_2.field14376), local6992_2.field14375)) {
                           InventoryActionQueue.method2656().method2664(local6992_2.field14380, local6992_2.field14376, local6992_2.field14378, 1);
                        }

                        if (local6992_2.field14381 != -1) {
                           InventoryUtils.method0553(local6992_2.field14381, false);
                        }
                     } else if (local6992_2.field14377 > 8) {
                        InventoryActionQueue.method2656().method2662(local6992_2.field14376, local6992_2.field14377, 1);
                     } else if (local6992_2.field14376 != InventoryUtils.method0554()) {
                        InventoryUtils.method0553(local6992_2.field14376, false);
                     }

                     this.field14368.remove(local6992_2);
                  }
               }
            }
         } else {
            this.field14368.remove(local6992_2);
         }
      }
   };
   private final EventListener<PlayerTickEvent> field14373 = playerTickEvent2 -> {
      if (this.field14369 != -1) {
         if (this.field14368.isEmpty() && !InventoryActionQueue.method2656().method2665()) {
            if (--this.field14371 <= 0) {
               this.field14369 = -1;
            }
         } else {
            this.field14371 = 4;
         }
      }
   };

   public void method4174(ItemStack itemStack) {
      this.field14368.add(new InventorySlotManager.Nested0520(itemStack, false));
   }

   public static InventorySlotManager method4176() {
      if (inventorySlotManager == null) {
         inventorySlotManager = new InventorySlotManager();
         EventBus.getInstance().subscribe(inventorySlotManager);
      }

      return inventorySlotManager;
   }

   private int method4177(int local6985_1, int local6985_2, int local6985_3) {
      if (mc.player == null) {
         return -1;
      }

      for (int index = 8; index >= 0; index--) {
         if (index != local6985_1
            && index != local6985_2
            && index != local6985_3
            && mc.player.getInventory().getStack(index).isEmpty()) {
            return index;
         }
      }

      for (int index2 = 8; index2 >= 0; index2--) {
         if (index2 != local6985_1 && index2 != local6985_2 && index2 != local6985_3) {
            return index2;
         }
      }

      return -1;
   }

   private int method4178(ItemStack itemStack) {
      return InventoryUtils.method0556(itemStack2 -> this.method4184(itemStack2, itemStack), 0, 8);
   }

   public boolean method4179() {
      return !this.field14368.isEmpty();
   }

   public ItemStack method4181(int local6989_1, ItemStack itemStack) {
      return this.field14369 == local6989_1 ? this.field14370 : itemStack;
   }

   private InventorySlotManager() {
   }

   private boolean method4184(ItemStack itemStack, ItemStack itemStack2) {
      return this.method4186(itemStack2) ? ItemStack.areItemsAndComponentsEqual(itemStack, itemStack2) : itemStack.getItem() == itemStack2.getItem();
   }

   public boolean method4185(ItemStack itemStack) {
      return this.method4178(itemStack) != -1 || this.method4190(itemStack) != -1;
   }

   private boolean method4186(ItemStack itemStack) {
      return itemStack.getItem() == Items.SPLASH_POTION || itemStack.getItem() == Items.LINGERING_POTION;
   }

   private boolean method4187(ItemStack itemStack) {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         int local6996_2 = this.method4178(itemStack);
         if (local6996_2 != -1 && !mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
            int local6996_3 = InventoryUtils.method0554();
            if (local6996_2 != local6996_3) {
               InventoryUtils.method0553(local6996_2, true);
            }

            ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
               .invokeStartPrediction(
                  mc.world,
                  local6991_0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, local6991_0, mc.player.getYaw(), mc.player.getPitch())
               );
            if (local6996_2 != local6996_3) {
               InventoryUtils.method0553(local6996_3, true);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int method4190(ItemStack itemStack) {
      int local6999_2 = InventoryUtils.method0556(itemStack2 -> ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack), 9, 35);
      return local6999_2 != -1
         ? local6999_2
         : InventoryUtils.method0556(
            itemStack3 -> this.method4184(itemStack3, itemStack) || this.method4196(itemStack3, itemStack, this.method4186(itemStack)) != -1, 0, 35
         );
   }

   public void method4192(ItemStack itemStack) {
      this.method4195(itemStack, false);
   }

   public boolean method4193() {
      return ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime");
   }

   private void method4194(int local7003_1) {
      if (mc.player != null && local7003_1 != -1) {
         this.field14369 = local7003_1;
         this.field14370 = mc.player.getInventory().getStack(local7003_1).copy();
         this.field14371 = 4;
      }
   }

   public void method4195(ItemStack itemStack, boolean new__) {
      if (!this.method4193() || !this.method4187(itemStack)) {
         this.field14368.add(new InventorySlotManager.Nested0520(itemStack, new__));
      }
   }

   private int method4196(ItemStack itemStack, ItemStack itemStack2, boolean local7005_3) {
      if (itemStack.get(DataComponentTypes.BUNDLE_CONTENTS) == null) {
         return -1;
      }

      for (int index = 0; index < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index++) {
         if (ItemStack.areItemsAndComponentsEqual(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index), itemStack2)) {
            return index;
         }
      }

      if (local7005_3) {
         return -1;
      }

      for (int index2 = 0; index2 < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index2++) {
         if (ItemStack.areItemsEqual(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index2), itemStack2)) {
            return index2;
         }
      }

      return -1;
   }

   static final class Nested0520 {
      private final ItemStack field14374;
      private final boolean __;
      private ItemStack field14375 = ItemStack.EMPTY;
      private int field14376;
      private int field14377;
      private int field14378 = -1;
      private int field14379 = -1;
      private int field14380 = -1;
      private int field14381 = -1;
      private int field14382 = -1;

      private Nested0520(ItemStack itemStack, boolean initial__) {
         this.field14374 = itemStack;
         this.__ = initial__;
      }
   }
}
