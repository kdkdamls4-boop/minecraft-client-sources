package net.skycore.client.module.combat;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.InventoryHelper;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class AutoSwapModule extends ClientModule {
   private AutoSwapModule.Pending pending;
   private long actionGeneration;
   private boolean hadAttackSpeedRecently;
   private final BooleanSetting headMode;
   private final BooleanSetting attackSpeedSwap;
   public static final AutoSwapModule INSTANCE = new AutoSwapModule();
   private static final int STOP_TICKS = 3;
   private boolean lastAttackSpeedResult;
   private boolean hotbarSwapPending;
   private long attackSpeedChangedAt;
   private final ModeSetting handSecond;
   private ItemStack lastOffhandStack;
   private final ModeSetting headFirst;
   private final BooleanSetting handMode = this.add(new BooleanSetting("Рука", true));
   private final KeyBindSetting swapKeyHead;
   private final BooleanSetting notifications;
   private final ModeSetting headSecond;
   private int storedAxeSlot;
   private long lastSwapAt;
   private final KeyBindSetting swapKey;
   private final ModeSetting handFirst;

   private void handleHandSwap() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null && !InventoryHelper.isServerScreen()) {
         Item item = this.itemFor(this.handFirst.get());
         Item item2 = this.itemFor(this.handSecond.get());
         if (item != null && item2 != null) {
            Item offhandItem = instance.player.getOffhandItem().is(item) ? item2 : item;
            int inventorySlot = this.getInventorySlot(offhandItem);
            if (inventorySlot < 0) {
               this.warnMissing(offhandItem);
            } else {
               Slot slot = instance.player.containerMenu.getSlot(UseItemHelper.toMenuSlot(inventorySlot));
               ItemStack item3 = slot.getItem().copy();
               InventoryHelper.swapHand(slot, InteractionHand.OFF_HAND, true);
               this.finishSwap(item3);
            }
         }
      }
   }

   private void beginPending(AutoSwapModule.Pending pending2) {
      this.pending = pending2;
      long timestamp = this.actionGeneration + 1L;
      this.actionGeneration = timestamp;
      long timestamp2 = timestamp;
      ScheduleHelper.schedule(() -> {
         if (timestamp2 == this.actionGeneration) {
            this.flushPendingSwap();
         }
      });
   }

   private int getInventorySlot(Item item) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && item != null && item != Items.AIR) {
         for (int value = 0; value < 36; value++) {
            if (player.getInventory().getItem(value).is(item)) {
               return value;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public void cancelPendingAction() {
      this.actionGeneration++;
      this.pending = null;
      this.hotbarSwapPending = false;
   }

   private void flushPendingSwap() {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (this.pending != null) {
            if (instance.player != null && instance.level != null) {
               AutoSwapModule.Pending pending2 = this.pending;
               this.pending = null;
               if (pending2 == AutoSwapModule.Pending.HAND) {
                  this.handleHandSwap();
               } else {
                  this.handleHelmetSwap();
               }

               return;
            }
         }
      }
   }

   private boolean hasAttackSpeedDescription(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (itemStack.getHoverName().getString().contains("Кобры")) {
            return true;
         }

         ItemLore itemLore = itemStack.get(DataComponents.LORE);
         if (itemLore == null) {
            return false;
         }

         for (Component text : itemLore.lines()) {
            String string = text.getString();
            if (string.contains("Скорости Атаки") || string.contains("Скорость атаки") || string.contains("Attack Speed")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void handleAttackSpeedLogic() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         if (!this.attackSpeedSwap.get()) {
            this.hadAttackSpeedRecently = false;
            this.lastAttackSpeedResult = false;
            this.storedAxeSlot = -1;
         } else {
            ItemStack offhandItem = instance.player.getOffhandItem();
            boolean attackSpeedDescription = this.hasAttackSpeedDescription(offhandItem);
            long now = System.currentTimeMillis();
            if (this.lastOffhandStack == null || !ItemStack.matches(this.lastOffhandStack, offhandItem) || attackSpeedDescription != this.lastAttackSpeedResult) {
               this.attackSpeedChangedAt = now;
               this.lastOffhandStack = offhandItem.copy();
               this.lastAttackSpeedResult = attackSpeedDescription;
            }

            if (this.pending == null && !this.hotbarSwapPending && now - this.lastSwapAt >= 150L && now - this.attackSpeedChangedAt >= 150L) {
               ItemStack mainHandItem = instance.player.getMainHandItem();
               if (attackSpeedDescription && mainHandItem.is(ItemTags.SWORDS)) {
                  int taggedHotbarSlot = this.findTaggedHotbarSlot(ItemTags.AXES);
                  if (taggedHotbarSlot >= 0) {
                     this.storedAxeSlot = instance.player.getInventory().getSelectedSlot();
                     this.hadAttackSpeedRecently = true;
                     this.scheduleHotbarSwap(taggedHotbarSlot);
                  }
               } else if (!attackSpeedDescription && this.hadAttackSpeedRecently) {
                  int storedAxeSlot2 = this.storedAxeSlot;
                  if (storedAxeSlot2 < 0 || storedAxeSlot2 >= 9 || !instance.player.getInventory().getItem(storedAxeSlot2).is(ItemTags.SWORDS)) {
                     storedAxeSlot2 = this.findTaggedHotbarSlot(ItemTags.SWORDS);
                  }

                  if (storedAxeSlot2 >= 0) {
                     this.scheduleHotbarSwap(storedAxeSlot2);
                  }

                  this.storedAxeSlot = -1;
                  this.hadAttackSpeedRecently = false;
               }
            }
         }
      }
   }

   public void onTickNew() {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null && instance.level != null && instance.gui.screen() == null) {
         this.handleAttackSpeedLogic();
      } else {
         this.cancelPendingAction();
      }
   }

   private AutoSwapModule() {
      super("Auto Swap", "Swaps configured offhand and helmet items", ModuleCategory.COMBAT);
      this.headMode = this.add(new BooleanSetting("Голова", false));
      this.swapKey = this.add(new KeyBindSetting("Свап (рука)", -1));
      this.handFirst = this.add(new ModeSetting("Свапать с", "Щит", "Щит", "Геплы", "Тотем", "Шар"));
      this.handSecond = this.add(new ModeSetting("Свапать на", "Геплы", "Геплы", "Щит", "Тотем", "Шар"));
      this.swapKeyHead = this.add(new KeyBindSetting("Свап (голова)", -1));
      this.headFirst = this.add(new ModeSetting("Свапать с (голова)", "Шар", "Шар", "Шлем"));
      this.headSecond = this.add(new ModeSetting("Свапать на (голова)", "Шлем", "Шлем", "Шар"));
      this.notifications = this.add(new BooleanSetting("Уведомления", true));
      this.attackSpeedSwap = this.add(new BooleanSetting("Свап топора на меч", true));
      this.lastOffhandStack = ItemStack.EMPTY;
      this.storedAxeSlot = -1;
      this.swapKey.visibleWhen(this.handMode::get);
      this.handFirst.visibleWhen(this.handMode::get);
      this.handSecond.visibleWhen(this.handMode::get);
      this.swapKeyHead.visibleWhen(this.headMode::get);
      this.headFirst.visibleWhen(this.headMode::get);
      this.headSecond.visibleWhen(this.headMode::get);
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
   }

   private void handleBind(int value) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled()
         && instance.player != null
         && instance.level != null
         && instance.gui.screen() == null
         && this.pending == null
         && !this.hotbarSwapPending
         && System.currentTimeMillis() - this.lastSwapAt >= 300L) {
         if (this.handMode.get() && this.swapKey.get() != -1 && value == this.swapKey.get()) {
            this.beginPending(AutoSwapModule.Pending.HAND);
         } else if (this.headMode.get() && this.swapKeyHead.get() != -1 && value == this.swapKeyHead.get()) {
            this.beginPending(AutoSwapModule.Pending.HEAD);
         }
      }
   }

   private void warnMissing(Item item) {
      if (this.notifications.get() && item != null && item != Items.AIR) {
         Component hoverName = item.getDefaultInstance().getHoverName();
         String string = "Item not found: " + hoverName.getString();
         SendDonationHelper.displayWarning(string);
      }
   }

   private int findTaggedHotbarSlot(TagKey<Item> tagKey) {
      LocalPlayer instance = Minecraft.getInstance().player;

      for (int value = 0; value < 9; value++) {
         Inventory inventory = instance.getInventory();
         if (inventory.getItem(value).is(tagKey)) {
            return value;
         }
      }

      return -1;
   }

   private void onKey(KeyEvent keyEvent) {
      if (keyEvent.pressed()) {
         net.minecraft.client.input.KeyEvent keyEvent2 = keyEvent.key();
         this.handleBind(keyEvent2.key());
      }
   }

   @Override
   protected void onTick() {
      this.onTickNew();
   }

   private void finishSwap(ItemStack itemStack) {
      long now = System.currentTimeMillis();
      this.lastSwapAt = now;
      Boolean get = this.notifications.get();
      if (get) {
         if (!itemStack.isEmpty()) {
            String string = itemStack.getHoverName().getString();
            String text = "Swapped to " + string;
            SendDonationHelper.displayInfo(text);
         }
      }
   }

   private Item itemFor(String text) {
      return switch (text == null ? "" : text.toLowerCase(Locale.ROOT)) {
         case "щит" -> Items.SHIELD;
         case "геплы" -> Items.GOLDEN_APPLE;
         case "тотем" -> Items.TOTEM_OF_UNDYING;
         case "шар" -> Items.PLAYER_HEAD;
         case "шлем" -> Items.NETHERITE_HELMET;
         default -> Items.AIR;
      };
   }

   private void scheduleHotbarSwap(int slot) {
      this.hotbarSwapPending = true;
      long timestamp = ++this.actionGeneration;
      ScheduleHelper.schedule(() -> {
         if (timestamp == this.actionGeneration) {
            this.hotbarSwapPending = false;
            if (this.isEnabled()) {
               Minecraft instance = Minecraft.getInstance();
               if (instance.player != null) {
                  UseItemHelper.switchToHotbarSlot(slot);
                  this.lastSwapAt = System.currentTimeMillis();
               }
            }
         }
      });
   }

   private void handleHelmetSwap() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && !InventoryHelper.isServerScreen()) {
         Item item = this.itemFor(this.headFirst.get());
         Item item2 = this.itemFor(this.headSecond.get());
         ItemStack itemBySlot = instance.getItemBySlot(EquipmentSlot.HEAD);
         Item item3 = itemBySlot.is(item) ? item2 : item;
         int inventorySlot = this.getInventorySlot(item3);
         if (inventorySlot < 0) {
            this.warnMissing(item3);
         } else {
            ItemStack inventory = instance.getInventory().getItem(inventorySlot).copy();
            UseItemHelper.moveToArmor(inventorySlot, 5);
            this.finishSwap(inventory);
         }
      }
   }

   @Override
   protected void onDisable() {
      this.cancelPendingAction();
      this.storedAxeSlot = -1;
      this.hadAttackSpeedRecently = false;
      this.lastAttackSpeedResult = false;
      this.lastOffhandStack = ItemStack.EMPTY;
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      int intermediate = -1;
      intermediate = (byte)-1;
      if (mouseButtonEvent.pressed()) {
         intermediate = 1000 + mouseButtonEvent.button().button();
         this.handleBind(intermediate);
      }
   }

   enum Pending {
      HAND,
      HEAD;
   }
}
