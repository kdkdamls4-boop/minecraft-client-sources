package net.skycore.client.module.combat;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ContainerClickEvent;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.setting.ActionSetting;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.util.UseItemHelper;

public final class ArrowSwapModule extends ClientModule {
   public final KeyBindSetting swapKey = this.add(new KeyBindSetting("Swap Key", -1));
   private ItemStack second;
   private int picking;
   public final ActionSetting firstArrow = this.add(new ActionSetting("First Arrow").actionLabel("Preview").action(() -> {
      byte flag = 0;
      this.openInventoryForPick(1);
   }));
   private ItemStack first;
   public final ActionSetting secondArrow = this.add(new ActionSetting("Second Arrow").actionLabel("Preview").action(() -> {
      byte flag2 = 0;
      this.openInventoryForPick(2);
   }));
   public static final ArrowSwapModule INSTANCE = new ArrowSwapModule();
   private boolean swapRequested;

   public void cancelPendingAction() {
      this.swapRequested = false;
   }

   private void performSwap() {
      int matchingSlot = findMatchingSlot(this.first, -1);
      int matchingSlot2 = findMatchingSlot(this.second, matchingSlot);
      if (matchingSlot == -1 || matchingSlot2 == -1) {
         SendDonationHelper.displayWarning("Arrow not found in inventory");
      } else if (matchingSlot != matchingSlot2) {
         int slot = toMenuSlot(matchingSlot);
         int slot2 = toMenuSlot(matchingSlot2);
         int slot3 = pickTempHotbar(matchingSlot, matchingSlot2);
         UseItemHelper.clickSlotId(slot, slot3, ContainerInput.SWAP, false);
         UseItemHelper.clickSlotId(slot2, slot3, ContainerInput.SWAP, false);
         UseItemHelper.clickSlotId(slot, slot3, ContainerInput.SWAP, false);
         UseItemHelper.syncCurrentPlayItem();
      }
   }

   @Override
   protected void onDisable() {
      this.picking = 0;
      this.swapRequested = false;
   }

   private static int toMenuSlot(int slot) {
      if (slot == 40) {
         return 45;
      }

      return UseItemHelper.toMenuSlot(slot);
   }

   private void requestSwap() {
      if (!this.swapRequested) {
         if (this.first.isEmpty() || this.second.isEmpty()) {
            SendDonationHelper.displayWarning("Select both arrows with Preview");
         } else if (Minecraft.getInstance().player != null) {
            if (findMatchingSlot(this.first, -1) != -1 && findMatchingSlot(this.second, -1) != -1) {
               this.swapRequested = true;
               ScheduleHelper.schedule(() -> {
                  if (this.isEnabled()) {
                     if (this.swapRequested) {
                        this.performSwap();
                        this.swapRequested = false;
                        return;
                     }
                  }
               });
            } else {
               SendDonationHelper.displayWarning("Arrow not found in inventory");
            }
         }
      }
   }

   private void onKey(KeyEvent keyEvent) {
      int value = 0;
      if (this.isEnabled() && keyEvent.pressed()) {
         if (keyEvent.client().gui.screen() == null) {
            Integer value2 = this.swapKey.get();
            if (value2 != -1) {
               net.minecraft.client.input.KeyEvent keyEvent2 = keyEvent.key();
               value = keyEvent2.key();
               Integer value3 = this.swapKey.get();
               if (value == value3) {
                  this.requestSwap();
               }
            }

            return;
         }
      }
   }

   private void returnToClickGui() {
      Minecraft instance = Minecraft.getInstance();
      instance.execute(() -> {
         var optional = SkycoreClient.modules().get(ClickGUIModule.class);
         Consumer<ClickGUIModule> consumer = clickGUIModule -> {
            if (!clickGUIModule.isEnabled()) {
               clickGUIModule.setEnabled(true);
            }
         };
         optional.ifPresent(consumer);
      });
   }

   private void openInventoryForPick(int picking2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (instance.level != null) {
            this.picking = picking2;
            instance.execute(() -> {
               if (instance.player != null) {
                  instance.setScreenAndShow(new InventoryScreen(instance.player));
               }
            });
            return;
         }
      }

      SendDonationHelper.displayWarning("Join a world to preview inventory");
   }

   private static boolean isArrow(ItemStack itemStack) {
      if (!itemStack.isEmpty()) {
         if (itemStack.is(Items.ARROW)) {
            return true;
         }

         if (itemStack.is(Items.SPECTRAL_ARROW) || itemStack.is(Items.TIPPED_ARROW)) {
            return true;
         }
      }

      return false;
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      if (this.isEnabled()) {
         if (mouseButtonEvent.pressed()) {
            if (mouseButtonEvent.client().gui.screen() == null) {
               MouseButtonInfo mouseButtonInfo = mouseButtonEvent.button();
               int computedValue = 1000 + mouseButtonInfo.button();
               if (this.swapKey.get() != -1) {
                  Integer value = this.swapKey.get();
                  if (computedValue == value) {
                     this.requestSwap();
                  }
               }

               return;
            }
         }
      }
   }

   private static int findMatchingSlot(ItemStack itemStack, int index) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && itemStack != null && !itemStack.isEmpty()) {
         for (int value = 0; value < 36; value++) {
            if (value != index) {
               ItemStack item = instance.getInventory().getItem(value);
               if (!item.isEmpty() && ItemStack.isSameItemSameComponents(item, itemStack)) {
                  return value;
               }
            }
         }

         return index != 40 && !instance.getOffhandItem().isEmpty() && ItemStack.isSameItemSameComponents(instance.getOffhandItem(), itemStack) ? 40 : -1;
      } else {
         return -1;
      }
   }

   @Override
   protected void onTick() {
      if (this.picking != 0) {
         Minecraft instance = Minecraft.getInstance();
         if (!(instance.gui.screen() instanceof InventoryScreen)) {
            this.picking = 0;
         }
      }
   }

   public boolean isBusy() {
      if (this.isEnabled()) {
         if (this.swapRequested) {
            return true;
         }
      }

      return false;
   }

   private ArrowSwapModule() {
      super("Arrow Swap", "Swaps two selected arrows in inventory on keybind", ModuleCategory.COMBAT);
      this.first = ItemStack.EMPTY;
      this.second = ItemStack.EMPTY;
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
      SkycoreClient.events().subscribe(ContainerClickEvent.class, this::onClickSlot);
   }

   private static int pickTempHotbar(int index, int index2) {

      for (int value = 0; value < 9; value++) {
         if (value != index && value != index2) {
            return value;
         }
      }

      return 0;
   }

   private void onClickSlot(ContainerClickEvent containerClickEvent) {
      if (this.picking != 0) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null && instance.gui.screen() instanceof InventoryScreen) {
            if (containerClickEvent.getSlotId() >= 0 && containerClickEvent.getSlotId() < player.containerMenu.slots.size()) {
               ItemStack item = player.containerMenu.getSlot(containerClickEvent.getSlotId()).getItem();
               if (!isArrow(item)) {
                  SendDonationHelper.displayWarning("Select an arrow");
                  containerClickEvent.cancel();
               } else {
                  ItemStack itemStack = item.copy();
                  itemStack.setCount(1);
                  if (this.picking == 1) {
                     this.first = itemStack;
                  } else {
                     this.second = itemStack;
                  }

                  this.picking = 0;
                  containerClickEvent.cancel();
                  this.returnToClickGui();
               }
            } else {
               containerClickEvent.cancel();
            }
         }
      }
   }
}
