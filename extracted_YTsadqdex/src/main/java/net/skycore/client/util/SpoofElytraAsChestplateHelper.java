package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.mixin.HudSetOverlayMessageTimeAccessor;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.module.player.AutoToolModule;

public final class SpoofElytraAsChestplateHelper {
   private static boolean freezeHotbar0;
   private static ItemStack frozenStack = ItemStack.EMPTY;
   private static int visualSlot = -1;
   private static float frozenHealth = -1.0F;
   private static int hiddenSelectedSlot = -1;
   private static int freezeHotbar0Ticks;
   private static boolean hudFreeze;
   private static ItemStack spoofElytra = ItemStack.EMPTY;
   private static int hiddenHotbarTicks;
   private static float frozenMaxHealth = -1.0F;
   private static int spoofElytraInvSlot = -1;
   private static boolean armorSpoof;
   private static ItemStack frozenHotbar0 = ItemStack.EMPTY;
   private static int frozenArmorPoints = -1;
   private static final ItemStack[] hiddenHotbar = new ItemStack[9];
   private static ItemStack spoofChestplate = ItemStack.EMPTY;
   private static int ticksLeft;

   public static void spoofElytraAsChestplate(ItemStack itemStack, ItemStack itemStack2, int value) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         armorSpoof = true;
         spoofChestplate = itemStack != null && !itemStack.isEmpty() ? itemStack.copy() : ItemStack.EMPTY;
         spoofElytra = itemStack2 != null && !itemStack2.isEmpty() ? itemStack2.copy() : new ItemStack(Items.ELYTRA);
         spoofElytraInvSlot = value >= 0 && value < 36 ? value : -1;
         suppressTooltip();
      }
   }

   public static ItemStack getHotbarSpoofStack(int slot, ItemStack itemStack) {
      if (freezeHotbar0 && slot == 0) {
         return frozenHotbar0;
      }

      if (visualSlot >= 0 && slot == visualSlot) {
         if (ticksLeft > 0) {
            if (!frozenStack.isEmpty()) {
               return frozenStack;
            }
         }
      }

      return getMainInvSpoofStack(slot, itemStack);
   }

   public static boolean shouldSuppressEquipAnimation() {
      if (hiddenHotbarTicks <= 0 && ticksLeft <= 0 && visualSlot < 0) {
         if (CrystalAuraModule.INSTANCE.visualActionSlot() < 0 && !freezeHotbar0) {
            if (!armorSpoof) {
               if (!AutoToolModule.shouldSuppressHeldItemTooltip()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public static void refresh(int value) {
      int value2 = 0;
      value2 = (byte)0;
      if (visualSlot >= 0) {
         value2 = Math.max(ticksLeft, Math.max(value, 1));
         ticksLeft = value2;
         suppressTooltip();
      }
   }

   public static boolean hasHudFreeze() {
      return hudFreeze;
   }

   public static ItemStack getSpoofElytra() {
      return spoofElytra;
   }

   public static int getVisualSlot() {
      if (CrystalAuraModule.INSTANCE.isVisualHidingEnabled()) {
         int result = CrystalAuraModule.INSTANCE.visualActionSlot();
         if (result >= 0) {
            return result;
         }

         int result2 = VisualSlotHelper.INSTANCE.visualSlot();
         if (result2 >= 0) {
            return result2;
         }
      }

      int visualSlot2 = AutoToolModule.getVisualSlot();
      return visualSlot2 >= 0 ? visualSlot2 : visualSlot;
   }

   public static void clearHiddenHotbar() {
      hiddenHotbarTicks = 0;
      hiddenSelectedSlot = -1;
      int value = 0;

      while (true) {
         if (value >= hiddenHotbar.length) {
            return;
         }

         hiddenHotbar[value] = ItemStack.EMPTY;
         value++;
      }
   }

   public static void freezeSwapBufferSlot() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         ItemStack item = instance.player.getInventory().getItem(0);
         ItemStack itemStack = item.copy();
         frozenHotbar0 = itemStack;
         freezeHotbar0 = true;
         freezeHotbar0Ticks = Math.max(freezeHotbar0Ticks, 8);
         suppressTooltip();
      }
   }

   public static boolean shouldSuppressTooltip() {
      if (hiddenHotbarTicks <= 0) {
         if (ticksLeft <= 0 && visualSlot < 0 && CrystalAuraModule.INSTANCE.visualActionSlot() < 0) {
            if (!freezeHotbar0) {
               if (!armorSpoof) {
                  if (!AutoToolModule.shouldSuppressHeldItemTooltip()) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public static int getSpoofElytraInvSlot() {
      return spoofElytraInvSlot;
   }

   public static boolean hasSwapBufferFreeze() {
      return freezeHotbar0;
   }

   public static void hideHotbar(int value) {
      int value2 = 0;
      value2 = (byte)0;
      value2 = (byte)0;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (hiddenHotbarTicks <= 0) {
            int value3 = 0;

            while (true) {
               if (value3 >= hiddenHotbar.length) {
                  value2 = instance.player.getInventory().getSelectedSlot();
                  hiddenSelectedSlot = value2;
                  break;
               }

               ItemStack[] hiddenHotbar2 = hiddenHotbar;
               Inventory inventory = instance.player.getInventory();
               ItemStack item = inventory.getItem(value3);
               hiddenHotbar2[value3] = item.copy();
               value3++;
            }
         }

         hiddenHotbarTicks = Math.max(hiddenHotbarTicks, Math.max(1, value));
         suppressTooltip();
      }
   }

   public static ItemStack getMainInvSpoofStack(int slot, ItemStack itemStack) {
      if (freezeHotbar0 && slot == 0) {
         return frozenHotbar0;
      }

      if (visualSlot >= 0) {
         if (slot == visualSlot) {
            if (ticksLeft > 0) {
               if (!frozenStack.isEmpty()) {
                  return frozenStack;
               }
            }
         }
      }

      if (armorSpoof) {
         if (!spoofElytra.isEmpty()) {
            if (slot == spoofElytraInvSlot) {
               return spoofElytra;
            }
         }
      }

      return itemStack;
   }

   public static ItemStack getPlayerScreenSlotSpoof(int slot, ItemStack itemStack) {
      if (freezeHotbar0 && slot == 36) {
         return frozenHotbar0;
      } else if (!armorSpoof) {
         return null;
      } else if (slot == 6) {
         return spoofChestplate.isEmpty() ? ItemStack.EMPTY : spoofChestplate;
      } else if (spoofElytraInvSlot < 0 || spoofElytra.isEmpty()) {
         return null;
      } else if (spoofElytraInvSlot <= 8) {
         return slot == 36 + spoofElytraInvSlot ? spoofElytra : null;
      } else {
         return slot == spoofElytraInvSlot ? spoofElytra : null;
      }
   }

   public static void clearSwapBufferFreeze() {
      freezeHotbar0 = false;
      freezeHotbar0Ticks = 0;
      frozenHotbar0 = ItemStack.EMPTY;
   }

   public static void clearHudFreeze() {
      hudFreeze = false;
      frozenArmorPoints = -1;
      frozenHealth = -1.0F;
      frozenMaxHealth = -1.0F;
   }

   public static ItemStack getSpoofChestplate() {
      return spoofChestplate;
   }

   public static void captureHudBaseline() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         frozenArmorPoints = instance.player.getArmorValue();
         float health = instance.player.getHealth();
         frozenHealth = health;
         frozenMaxHealth = instance.player.getMaxHealth();
         hudFreeze = true;
      }
   }

   public static float getVisualMaxHealth(float value) {
      return hudFreeze && frozenMaxHealth > 0.0F ? frozenMaxHealth : value;
   }

   public static ItemStack getVisualStack(ItemStack itemStack) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return itemStack;
      } else if (hiddenHotbarTicks > 0 && hiddenSelectedSlot >= 0 && hiddenSelectedSlot < hiddenHotbar.length) {
         return hiddenHotbar[hiddenSelectedSlot];
      } else if (freezeHotbar0 && instance.player.getInventory().getSelectedSlot() == 0) {
         return frozenHotbar0;
      } else if (armorSpoof
         && spoofElytraInvSlot >= 0
         && spoofElytraInvSlot <= 8
         && instance.player.getInventory().getSelectedSlot() == spoofElytraInvSlot
         && !spoofElytra.isEmpty()) {
         return spoofElytra;
      } else {
         int visualSlot2 = getVisualSlot();
         if (visualSlot2 < 0 || visualSlot2 > 8) {
            return itemStack;
         } else {
            return visualSlot == visualSlot2 && ticksLeft > 0 ? frozenStack : instance.player.getInventory().getItem(visualSlot2);
         }
      }
   }

   public static void clear() {
      visualSlot = -1;
      ticksLeft = 0;
      frozenStack = ItemStack.EMPTY;
   }

   public static void tick() {
      if (hiddenHotbarTicks > 0) {
         hiddenHotbarTicks--;
         suppressTooltip();
         if (hiddenHotbarTicks <= 0) {
            clearHiddenHotbar();
         }
      }

      if (freezeHotbar0Ticks > 0) {
         freezeHotbar0Ticks--;
         suppressTooltip();
         if (freezeHotbar0Ticks <= 0) {
            clearSwapBufferFreeze();
         }
      }

      if (ticksLeft > 0) {
         ticksLeft--;
         suppressTooltip();
         if (ticksLeft <= 0) {
            clear();
         }
      }
   }

   public static void clearArmorSpoof() {
      armorSpoof = false;
      spoofChestplate = ItemStack.EMPTY;
      spoofElytra = ItemStack.EMPTY;
      spoofElytraInvSlot = -1;
   }

   public static void hold(int slot, int value) {
      int value2 = 0;
      value2 = (byte)0;
      Minecraft instance = Minecraft.getInstance();
      if (slot >= 0 && slot <= 8 && instance.player != null) {
         visualSlot = slot;
         value2 = Math.max(value, 1);
         ticksLeft = value2;
         Inventory inventory = instance.player.getInventory();
         ItemStack item = inventory.getItem(slot).copy();
         frozenStack = item;
         suppressTooltip();
      }
   }

   public static ItemStack spoofEquippedVisual(EquipmentSlot equipmentSlot, ItemStack itemStack) {
      if (armorSpoof && equipmentSlot == EquipmentSlot.CHEST) {
         ItemStack itemStack2;
         if (spoofChestplate.isEmpty()) {
            itemStack2 = ItemStack.EMPTY;
         } else {
            itemStack2 = spoofChestplate;
         }

         return itemStack2;
      } else {
         return itemStack;
      }
   }

   public static boolean hasArmorSpoof() {
      return armorSpoof;
   }

   public static float getVisualHealth(float value, float value2) {
      return hudFreeze && frozenHealth >= 0.0F ? frozenHealth : value;
   }

   public static int getVisualArmorPoints(int value) {
      int value2 = 0;
      value2 = (byte)0;
      if (hudFreeze && frozenArmorPoints >= 0) {
         value2 = Math.clamp(frozenArmorPoints, 0, 20);
      } else {
         value2 = value;
      }

      return value2;
   }

   private SpoofElytraAsChestplateHelper() {
   }

   private static void suppressTooltip() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gui != null) {
         HudSetOverlayMessageTimeAccessor hudSetOverlayMessageTimeAccessor = (HudSetOverlayMessageTimeAccessor)instance.gui.hud;
         hudSetOverlayMessageTimeAccessor.skycore$setOverlayMessageTime(0);
         hudSetOverlayMessageTimeAccessor.skycore$setToolHighlightTimer(0);
      }
   }
}
