package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.module.player.AutoToolModule;

public final class VisualSlotHelper {
   private int restoreInventorySlot;
   private Item pendingItem;
   private double switchPenalty;
   private int pendingHotbarSlot;
   private boolean canSwitch;
   private int equippedSlot;
   private int restoreDelayTicks;
   private int pendingInventorySlot;
   private long lastSwitchTime;
   private boolean delayActive;
   private Item restoreItem;
   public static final VisualSlotHelper INSTANCE = new VisualSlotHelper();
   private int visualSlot;
   private int restoreHotbarSlot;
   private int originalSlot = -1;

   public int visualSlot() {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      if (silent()) {
         if (this.visualSlot >= 0) {
            value = this.visualSlot;
         } else {
            value = this.originalSlot;
         }
      } else {
         value = -1;
      }

      return value;
   }

   private void clearEquippedTool() {
      this.originalSlot = -1;
      this.equippedSlot = -1;
      this.restoreDelayTicks = 0;
      this.pendingHotbarSlot = -1;
      this.pendingInventorySlot = -1;
      this.pendingItem = null;
   }

   public void setVisualSlot(int visualSlot2) {
      this.visualSlot = visualSlot2;
   }

   private static double updateSwitchPenalty(double value, long timestamp, long timestamp2) {
      return timestamp >= timestamp2 ? value * 0.9 : value + (300.0 - timestamp);
   }

   public boolean isSilentSwitchEnabled() {
      return enabled() && silent();
   }

   public void tick() {
      if (Minecraft.getInstance().player != null
         && Minecraft.getInstance().level != null
         && Minecraft.getInstance().gameMode != null
         && !Minecraft.getInstance().player.isCreative()) {
         this.finishPendingSwap();
         if (this.originalSlot != -1 || this.restoreHotbarSlot != -1) {
            if (Minecraft.getInstance().gameMode.isDestroying() || Minecraft.getInstance().options.keyAttack.isDown()) {
               this.scheduleRestore();
            } else if (this.restoreDelayTicks > 0) {
               this.restoreDelayTicks--;
            } else {
               this.restoreSelectedSlot();
               this.restoreInventoryTool();
               this.clearEquippedTool();
            }
         }
      } else {
         this.clearEquippedTool();
         this.clearRestoreState();
      }
   }

   private static int findBestSlotAny(BlockState blockState) {
      if (Minecraft.getInstance().player == null) {
         return -1;
      }

      int index = -1;
      ItemStack eMPTY = ItemStack.EMPTY;
      float miningRate = getMiningRate(ItemStack.EMPTY, blockState);

      for (int value = 0; value < 36; value++) {
         ItemStack item = Minecraft.getInstance().player.getInventory().getItem(value);
         float miningRate2 = getMiningRate(item, blockState);
         if (isBetterTool(item, eMPTY, miningRate2, miningRate, blockState)) {
            miningRate = miningRate2;
            index = value;
            eMPTY = item;
         }
      }

      return index;
   }

   private void updateSwitchTiming() {
      long now = System.currentTimeMillis();
      long timestamp = this.lastSwitchTime == 0L ? 1000L : now - this.lastSwitchTime;
      this.switchPenalty = updateSwitchPenalty(this.switchPenalty, timestamp, 275L);
      this.canSwitch = isPenaltyWithinLimit(this.switchPenalty, 250.0, 600.0);
   }

   private void restoreSelectedSlot() {
      int value = 0;
      if (this.originalSlot != -1) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            if (this.equippedSlot != -1) {
               Minecraft instance2 = Minecraft.getInstance();
               value = instance2.player.getInventory().getSelectedSlot();
               if (value != this.equippedSlot) {
                  return;
               }
            }

            Minecraft.getInstance().player.getInventory().setSelectedSlot(this.originalSlot);
         }
      }
   }

   private void restoreInventoryTool() {
      if (this.restoreHotbarSlot != -1) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            instance = Minecraft.getInstance();
            if (instance.gameMode != null) {
               AbstractContainerMenu instance2 = Minecraft.getInstance().player.containerMenu;
               Minecraft instance3 = Minecraft.getInstance();
               if (instance2 == instance3.player.inventoryMenu) {
                  instance = Minecraft.getInstance();
                  Inventory inventory = instance.player.getInventory();
                  int restoreHotbarSlot2 = this.restoreHotbarSlot;
                  int restoreInventorySlot2 = this.restoreInventorySlot;
                  Item restoreItem2 = this.restoreItem;
                  this.clearRestoreState();
                  if (inventory.getItem(restoreHotbarSlot2).getItem() == restoreItem2) {
                     this.swapInventorySlots(restoreInventorySlot2, restoreHotbarSlot2);
                  }
               }
            }
         }
      }
   }

   private static boolean isPenaltyWithinLimit(double value, double value2, double value3) {
      return value + value2 <= value3;
   }

   private void clearRestoreState() {
      this.restoreHotbarSlot = -1;
      this.restoreInventorySlot = -1;
      this.restoreItem = null;
   }

   private static boolean isUnenchantedShears(ItemStack itemStack) {
      if (!itemStack.isEmpty()) {
         if (itemStack.getItem() == Items.SHEARS) {
            if (getEfficiency(itemStack) <= 0.0F) {
               return true;
            }
         }
      }

      return false;
   }

   public void clearVisualSlot() {
      this.visualSlot = -1;
   }

   private int equipBestTool(BlockState blockState) {
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().gameMode != null) {
         Inventory inventory = Minecraft.getInstance().player.getInventory();
         int bestSlotAny = findBestSlotAny(blockState);
         if (bestSlotAny == -1) {
            return -1;
         }

         int selectedSlot = inventory.getSelectedSlot();
         ItemStack item = inventory.getItem(selectedSlot);
         ItemStack item2 = inventory.getItem(bestSlotAny);
         if (isBetterTool(item2, item, getMiningRate(item2, blockState), getMiningRate(item, blockState), blockState)) {
            if (this.originalSlot == -1) {
               this.originalSlot = selectedSlot;
            }

            int slot = bestSlotAny < 9 ? bestSlotAny : this.swapInventoryNow(bestSlotAny, blockState);
            if (slot < 0) {
               return -1;
            }

            if (slot != inventory.getSelectedSlot()) {
               this.selectToolSlot(slot);
            }

            MultiPlayerGameModeEnsureHasSentCarriedItemAccessor instance = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode;
            instance.skycore$ensureHasSentCarriedItem();
            instance.skycore$setDestroyingItem(Minecraft.getInstance().player.getMainHandItem().copy());
            return slot;
         } else {
            return selectedSlot != bestSlotAny && !this.alreadyHoldingSwapped(bestSlotAny, selectedSlot) ? -1 : selectedSlot;
         }
      } else {
         return -1;
      }
   }

   private boolean alreadyHoldingSwapped(int value, int value2) {
      return this.restoreHotbarSlot == value2 && this.restoreInventorySlot == value;
   }

   private void method436Now(int value, int value2) {
      int instance = Minecraft.getInstance().player.containerMenu.containerId;
      Minecraft instance2 = Minecraft.getInstance();
      MultiPlayerGameMode gameMode = instance2.gameMode;
      ContainerInput sWAP = ContainerInput.SWAP;
      Minecraft instance3 = Minecraft.getInstance();
      gameMode.handleContainerInput(instance, value, value2, sWAP, instance3.player);
   }

   private int swapInventoryNow(int restoreInventorySlot2, BlockState blockState) {
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().gameMode != null) {
         if (Minecraft.getInstance().player.containerMenu != Minecraft.getInstance().player.inventoryMenu) {
            return -1;
         }

         if (this.restoreHotbarSlot != -1) {
            if (this.restoreInventorySlot == restoreInventorySlot2) {
               return this.restoreHotbarSlot;
            }

            this.restoreInventoryTool();
         }

         int replacementSlot = this.findReplacementSlot(blockState);
         if (replacementSlot == -1) {
            replacementSlot = Minecraft.getInstance().player.getInventory().getSelectedSlot();
         }

         this.method436Now(restoreInventorySlot2, replacementSlot);
         this.restoreHotbarSlot = replacementSlot;
         this.restoreInventorySlot = restoreInventorySlot2;
         this.restoreItem = Minecraft.getInstance().player.getInventory().getItem(replacementSlot).getItem();
         return replacementSlot;
      } else {
         return -1;
      }
   }

   private void selectToolSlot(int equippedSlot2) {
      Minecraft instance = Minecraft.getInstance();
      Inventory inventory = instance.player.getInventory();
      inventory.setSelectedSlot(equippedSlot2);
      this.equippedSlot = equippedSlot2;
   }

   public VisualSlotHelper() {
      this.equippedSlot = -1;
      this.visualSlot = -1;
      this.pendingHotbarSlot = -1;
      this.pendingInventorySlot = -1;
      this.restoreHotbarSlot = -1;
      this.restoreInventorySlot = -1;
      this.canSwitch = true;
   }

   private static boolean noDelay() {
      return false;
   }

   private static boolean silent() {
      return CrystalAuraModule.INSTANCE.isVisualHidingEnabled();
   }

   private static ItemStack getBestHotbarTool(BlockState blockState) {
      int bestHotbarToolSlot = findBestHotbarToolSlot(blockState);
      ItemStack itemStack;
      if (bestHotbarToolSlot == -1) {
         Minecraft instance = Minecraft.getInstance();
         itemStack = instance.player.getMainHandItem();
      } else {
         Inventory inventory = Minecraft.getInstance().player.getInventory();
         itemStack = inventory.getItem(bestHotbarToolSlot);
      }

      return itemStack;
   }

   private static float getMiningRate(ItemStack itemStack, BlockState blockState) {
      return EfficiencyHelper.rate(itemStack, blockState);
   }

   private void finishPendingSwap() {
      if (this.pendingHotbarSlot != -1 && !FixInputHelper.busy()) {
         int pendingHotbarSlot2 = this.pendingHotbarSlot;
         int pendingInventorySlot2 = this.pendingInventorySlot;
         Item pendingItem2 = this.pendingItem;
         this.pendingHotbarSlot = -1;
         this.pendingInventorySlot = -1;
         this.pendingItem = null;
         if (Minecraft.getInstance().player.getInventory().getItem(pendingHotbarSlot2).getItem() == pendingItem2) {
            this.restoreHotbarSlot = pendingHotbarSlot2;
            this.restoreInventorySlot = pendingInventorySlot2;
            this.restoreItem = pendingItem2;
            if (Minecraft.getInstance().gameMode.isDestroying()) {
               Inventory inventory = Minecraft.getInstance().player.getInventory();
               if (pendingHotbarSlot2 != inventory.getSelectedSlot()) {
                  BlockPos instance = ((MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode).skycore$getDestroyBlockPos();
                  BlockState blockState = Minecraft.getInstance().level.getBlockState(instance);
                  float item = getMiningRate(inventory.getItem(pendingHotbarSlot2), blockState);
                  float item2 = getMiningRate(inventory.getItem(inventory.getSelectedSlot()), blockState);
                  if (item <= item2 + 1.0E-4F) {
                     return;
                  }

                  if (this.originalSlot == -1) {
                     this.originalSlot = inventory.getSelectedSlot();
                  }

                  this.selectToolSlot(pendingHotbarSlot2);
               }

               ((MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode)
                  .skycore$setDestroyingItem(Minecraft.getInstance().player.getMainHandItem().copy());
               this.scheduleRestore();
            }
         }
      }
   }

   private static boolean isBetterTool(ItemStack itemStack, ItemStack itemStack2, float value, float value2, BlockState blockState) {
      if (blockState != null && blockState.is(Blocks.COBWEB)) {
         int value3 = !itemStack.isEmpty() && itemStack.is(Items.SHEARS) ? 1 : 0;
         int value4 = !itemStack2.isEmpty() && itemStack2.is(Items.SHEARS) ? 1 : 0;
         if (value3 != value4) {
            return (value3 != 0);
         }
      }

      if (value > value2 + 1.0E-4F) {
         return true;
      }

      if (value + 1.0E-4F < value2) {
         return false;
      }

      boolean correctToolForDrops = itemStack.isCorrectToolForDrops(blockState);
      boolean correctToolForDrops2 = !itemStack2.isEmpty() && itemStack2.isCorrectToolForDrops(blockState);
      if (correctToolForDrops != correctToolForDrops2) {
         return (boolean)correctToolForDrops;
      }

      float efficiency = getEfficiency(itemStack);
      float efficiency2 = getEfficiency(itemStack2);
      if (efficiency != efficiency2) {
         return efficiency > efficiency2;
      }

      boolean unenchantedShears = isUnenchantedShears(itemStack);
      boolean unenchantedShears2 = isUnenchantedShears(itemStack2);
      return unenchantedShears != unenchantedShears2 ? !unenchantedShears : false;
   }

   private int findReplacementSlot(BlockState blockState) {
      int value = 0;
      value = (byte)0;
      int result = emptyHotbar(this.originalSlot);
      if (result != -1) {
         return result;
      }

      int bestHotbarToolSlot = findBestHotbarToolSlot(blockState);
      if (bestHotbarToolSlot != -1 && bestHotbarToolSlot != this.originalSlot) {
         value = bestHotbarToolSlot;
      } else {
         value = replaceableHotbar(this.originalSlot);
      }

      return value;
   }

   private int equipToolForBlock(BlockState blockState, BlockPos blockPos) {
      return this.equipBestTool(blockState);
   }

   private static float getEfficiency(ItemStack itemStack) {
      return EfficiencyHelper.efficiency(itemStack);
   }

   public static boolean enabled() {
      if (!AutoToolModule.INSTANCE.isEnabled()) {
         if (!CrystalAuraModule.INSTANCE.isEnabled()) {
            return false;
         }
      }

      return true;
   }

   private static boolean protectedItem(ItemStack itemStack) {
      Item item = itemStack.getItem();
      if (item != Items.NETHERITE_SWORD) {
         if (item != Items.DIAMOND_SWORD) {
            if (item != Items.IRON_SWORD) {
               if (item != Items.MACE) {
                  if (item != Items.ELYTRA) {
                     if (item != Items.TOTEM_OF_UNDYING) {
                        if (item != Items.END_CRYSTAL && item != Items.OBSIDIAN) {
                           if (item != Items.CRYING_OBSIDIAN) {
                              return false;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return true;
   }

   private static boolean fromInventory() {
      return false;
   }

   public boolean clearDelay(int value) {
      if (value <= 0) {
         this.delayActive = false;
      } else {
         this.scheduleRestore();
         if (!this.delayActive) {
            this.delayActive = true;
            long now = System.currentTimeMillis();
            this.lastSwitchTime = now;
            if (noDelay()) {
               if (this.canSwitch) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static int getAutoToolSlot(BlockState blockState) {
      VisualSlotHelper iNSTANCE = INSTANCE;
      if (iNSTANCE != null) {
         if (enabled()) {
            return findBestHotbarToolSlot(blockState);
         }
      }

      return -1;
   }

   public void beforeStart(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && Minecraft.getInstance().level != null && Minecraft.getInstance().gameMode != null) {
         if (!Minecraft.getInstance().player.isCreative()) {
            Minecraft instance2 = Minecraft.getInstance();
            BlockState blockState = instance2.level.getBlockState(blockPos);
            if (!blockState.isAir()) {
               this.updateSwitchTiming();
               int value = this.equipBestTool(blockState);
               if (value != -1) {
                  if (this.originalSlot != -1) {
                     this.scheduleRestore();
                  }
               } else {
                  this.prepareInventoryTool(blockState);
               }
            }
         }
      }
   }

   private static int findBestInventoryToolSlot(BlockState blockState, ItemStack itemStack) {
      float miningRate = getMiningRate(itemStack, blockState);
      int index = -1;

      for (int value = 9; value < 36; value++) {
         Minecraft instance = Minecraft.getInstance();
         Inventory inventory = instance.player.getInventory();
         ItemStack item = inventory.getItem(value);
         float miningRate2 = getMiningRate(item, blockState);
         if (isBetterTool(item, itemStack, miningRate2, miningRate, blockState)) {
            miningRate = miningRate2;
            index = value;
            itemStack = item;
         }
      }

      return index;
   }

   private void prepareInventoryTool(BlockState blockState) {
      if (fromInventory()
         && this.pendingHotbarSlot == -1
         && !FixInputHelper.busy()
         && Minecraft.getInstance().player.containerMenu == Minecraft.getInstance().player.inventoryMenu) {
         int value = findBestInventoryToolSlot(blockState, getBestHotbarTool(blockState));
         if (value == -1) {
            if (this.restoreHotbarSlot != -1 && Minecraft.getInstance().player.getInventory().getSelectedSlot() != this.restoreHotbarSlot) {
               this.restoreInventoryTool();
            }
         } else {
            int restoreHotbarSlot2 = this.restoreHotbarSlot;
            if (restoreHotbarSlot2 != -1) {
               int value2 = value == this.restoreInventorySlot ? 1 : 0;
               this.restoreInventoryTool();
               if (value2 != 0) {
                  return;
               }
            } else {
               restoreHotbarSlot2 = this.findReplacementSlot(blockState);
            }

            if (restoreHotbarSlot2 != -1) {
               this.swapInventorySlots(value, restoreHotbarSlot2);
               this.pendingHotbarSlot = restoreHotbarSlot2;
               this.pendingInventorySlot = value;
               this.pendingItem = Minecraft.getInstance().player.getInventory().getItem(value).getItem();
            }
         }
      }
   }

   public void restoreAndReset() {
      this.restoreSelectedSlot();
      this.restoreInventoryTool();
      this.clearEquippedTool();
      this.clearRestoreState();
   }

   private void swapInventorySlots(int value, int value2) {
      Minecraft instance = Minecraft.getInstance();
      int value3 = instance.player.containerMenu.containerId;
      FixInputHelper.submit(() -> {
         Minecraft.getInstance().gameMode.handleContainerInput(value3, value, value2, ContainerInput.SWAP, Minecraft.getInstance().player);
      });
   }

   private static int emptyHotbar(int index) {
      Inventory inventory = Minecraft.getInstance().player.getInventory();

      for (int value = 0; value < 9; value++) {
         if (value != index && value != inventory.getSelectedSlot()) {
            if (inventory.getItem(value).isEmpty()) {
               return value;
            }
         }
      }

      return -1;
   }

   private static int replaceableHotbar(int index) {
      Minecraft instance = Minecraft.getInstance();
      Inventory inventory = instance.player.getInventory();

      for (int value = 0; value < 9; value++) {
         ItemStack item = inventory.getItem(value);
         item.getItem();
         if (value != index && value != inventory.getSelectedSlot()) {
            if (!protectedItem(item)) {
               return value;
            }
         }
      }

      return -1;
   }

   public static int findBestHotbarToolSlot(BlockState blockState) {
      if (Minecraft.getInstance().player == null) {
         return -1;
      }

      int index = -1;
      ItemStack eMPTY = ItemStack.EMPTY;
      float miningRate = getMiningRate(ItemStack.EMPTY, blockState);

      for (int value = 0; value < 9; value++) {
         ItemStack item = Minecraft.getInstance().player.getInventory().getItem(value);
         float miningRate2 = getMiningRate(item, blockState);
         if (isBetterTool(item, eMPTY, miningRate2, miningRate, blockState)) {
            miningRate = miningRate2;
            index = value;
            eMPTY = item;
         }
      }

      return index;
   }

   private void scheduleRestore() {
      this.restoreDelayTicks = this.restoreHotbarSlot == -1 ? 2 : 3;
   }

   public static ItemStack getBestAvailableTool(BlockState blockState) {
      if (Minecraft.getInstance().player == null) {
         return ItemStack.EMPTY;
      }

      ItemStack bestHotbarTool = getBestHotbarTool(blockState);
      VisualSlotHelper iNSTANCE = INSTANCE;
      if (iNSTANCE != null) {
         if (enabled() && fromInventory()) {
            int bestInventoryToolSlot = findBestInventoryToolSlot(blockState, bestHotbarTool);
            ItemStack itemStack;
            if (bestInventoryToolSlot == -1) {
               itemStack = bestHotbarTool;
            } else {
               itemStack = Minecraft.getInstance().player.getInventory().getItem(bestInventoryToolSlot);
            }

            return itemStack;
         }
      }

      return bestHotbarTool;
   }
}
