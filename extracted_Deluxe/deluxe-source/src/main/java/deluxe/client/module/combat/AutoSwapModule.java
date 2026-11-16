package deluxe.client.module.combat;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.util.InputBinding;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.event.SlotClickEvent;
import deluxe.client.ui.SwapWheelScreen;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "AutoSwap", description = "Свапает выбранные предметы в левую руку по клавише.", category = ModuleCategory.COMBAT)
public final class AutoSwapModule extends Module {
   private static AutoSwapModule autoSwapModule;
   public ModeSetting field08701;
   public ModeSetting field08702;
   public ModeSetting field08703;
   public KeybindSetting field08704;
   public KeybindSetting field08705;
   public KeybindSetting field08706;
   public KeybindSetting field08707;
   public BooleanSetting booleanSetting;
   private boolean field08709;
   private boolean field08710;
   private Item field08711;
   private ItemStack field08712 = ItemStack.EMPTY;
   private boolean field08713;
   private boolean field08714;
   private boolean field08715;
   private int field08716 = -1;
   private final ItemStack[] field08717 = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   private final EventListener<PlayerTickEvent> field08718 = playerTickEvent -> {
      if (mc.player != null) {
         boolean local4049_2 = this.field08701.isSelected("Двойной свап")
            && this.field08707.isBound()
            && this.field08707.getBinding().method2749(mc.getWindow().getHandle());
         if (local4049_2 && !this.field08709) {
            this.method2595();
         }

         this.field08709 = local4049_2;
         boolean local4049_3 = this.field08704.isBound() && this.field08704.getBinding().method2749(mc.getWindow().getHandle());
         if (local4049_3 && !this.field08713) {
            this.method2581("цербер");
         }

         this.field08713 = local4049_3;
         boolean local4049_4 = this.field08705.isBound() && this.field08705.getBinding().method2749(mc.getWindow().getHandle());
         if (local4049_4 && !this.field08714) {
            this.method2581("вихр");
         }

         this.field08714 = local4049_4;
         boolean local4049_5 = this.field08701.isSelected("Тройной свап")
            && this.field08706.isBound()
            && this.method2582()
            && (mc.currentScreen == null || mc.currentScreen instanceof SwapWheelScreen);
         if (local4049_5
            && !this.field08715
            && (mc.currentScreen == null || mc.currentScreen instanceof SwapWheelScreen swapWheelScreen && swapWheelScreen.closing())) {
            mc.setScreen(new SwapWheelScreen(this.field08717, local4021_1 -> this.method2597(local4021_1)));
         }

         if (!local4049_5 && this.field08715 && mc.currentScreen instanceof SwapWheelScreen swapWheelScreen2) {
            if (!swapWheelScreen2.pickerOpen() && swapWheelScreen2.hoveredSlot() != -1 && !this.field08717[swapWheelScreen2.hoveredSlot()].isEmpty()) {
               this.method2581(Formatting.strip(this.field08717[swapWheelScreen2.hoveredSlot()].getName().getString()).toLowerCase());
            }

            if (!swapWheelScreen2.pickerOpen()) {
               swapWheelScreen2.startClosing();
            }
         }

         this.field08715 = local4049_5;
      }
   };
   private final EventListener<SlotClickEvent> field08719 = slotClickEvent -> {
      if (this.field08716 != -1
         && mc.player != null
         && slotClickEvent.method2983() >= 0
         && slotClickEvent.method2983() < mc.player.currentScreenHandler.slots.size()) {
         slotClickEvent.method4424(true);
         if (!mc.player.currentScreenHandler.getSlot(slotClickEvent.method2983()).getStack().isEmpty()) {
            this.field08717[this.field08716] = mc.player.currentScreenHandler.getSlot(slotClickEvent.method2983()).getStack().copy();
         }

         this.field08716 = -1;
         mc.execute(() -> mc.setScreen(null));
      }
   };

   private MutableText method2572(ItemStack itemStack) {
      MutableText mutableText = Text.empty();
      mutableText.append(
         itemStack.getName()
            .copy()
            .styled(style -> style.getColor() == null ? style.withColor(itemStack.getRarity().getFormatting()) : style)
      );
      if (itemStack.getCount() > 1) {
         mutableText.append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
         mutableText.append(Text.literal("x" + itemStack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
         mutableText.append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }

      return mutableText;
   }

   public static AutoSwapModule method2577() {
      if (autoSwapModule == null) {
         autoSwapModule = new AutoSwapModule();
      }

      return autoSwapModule;
   }

   private int method2578() {
      int local4024_1 = InventoryUtils.method0556(
         itemStack -> EntityUtils.method2795(itemStack) && !this.method2580(itemStack) && !this.method2590(itemStack), 0, 35
      );
      if (local4024_1 != -1) {
         return local4024_1;
      }

      int local4024_2 = InventoryUtils.method0556(
         itemStack2 -> itemStack2.getItem() == Items.TOTEM_OF_UNDYING
            && itemStack2.hasEnchantments()
            && !this.method2580(itemStack2)
            && !this.method2590(itemStack2),
         0,
         35
      );
      return local4024_2 != -1
         ? local4024_2
         : InventoryUtils.method0556(
            itemStack3 -> itemStack3.getItem() == Items.TOTEM_OF_UNDYING && !this.method2580(itemStack3) && !this.method2590(itemStack3), 0, 35
         );
   }

   public boolean method2580(ItemStack itemStack) {
      AuctionItemCatalog.method1056();
      return Formatting.strip(itemStack.getName().getString()).toLowerCase().contains("цербер")
         || itemStack.getItem() == Items.TOTEM_OF_UNDYING
            && AuctionItemCatalog.method1055("Талисман Вихря") != null
            && AutoBuyItem.CHECKER.test(itemStack, AuctionItemCatalog.method1055("Талисман Вихря"));
   }

   private void method2581(String local4027_1) {
      if (!this.method2583()) {
         int local4027_2 = InventoryUtils.method0556(
            itemStack -> Formatting.strip(itemStack.getName().getString()).toLowerCase().contains(local4027_1), 0, 35
         );
         if (local4027_2 != -1) {
            this.field08711 = mc.player.getInventory().getStack(local4027_2).getItem();
            if (this.booleanSetting.isChecked()) {
               NotificationHud.method2691(
                  this.category.getIcon(), "Свап", this.method2572(mc.player.getInventory().getStack(local4027_2)), true
               );
               ClientChat.method2261(
                  Text.literal("Свап ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(this.method2572(mc.player.getInventory().getStack(local4027_2)))
               );
            }

            if (!this.method2580(mc.player.getOffHandStack())) {
               this.field08712 = mc.player.getOffHandStack().copy();
            }

            InventoryActionQueue.method2656().method2660(local4027_2, 40, 1);
         }
      }
   }

   private boolean method2582() {
      return this.field08706.getBinding().getDevice() == InputBinding.DeviceType.MOUSE
         ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), this.field08706.getBinding().getCode()) == 1
         : GLFW.glfwGetKey(mc.getWindow().getHandle(), this.field08706.getBinding().getCode()) == 1;
   }

   private boolean method2583() {
      return mc.player == null
         || AutoTotemModule.method3667().method3670()
         || InventoryActionQueue.method2656().method2665()
         || InventorySlotManager.method4176().method4179();
   }

   private int method2584(String local4030_1) {
      return switch (local4030_1) {
         case "Золотое яблоко" -> InventoryUtils.method0556(
            itemStack -> itemStack.getItem() == Items.GOLDEN_APPLE && !this.method2580(itemStack), 0, 35
         );
         case "Зачарованное золотое яблоко" -> InventoryUtils.method0556(
            itemStack2 -> itemStack2.getItem() == Items.ENCHANTED_GOLDEN_APPLE && !this.method2580(itemStack2), 0, 35
         );
         case "Тотем" -> this.method2578();
         case "Щит" -> InventoryUtils.method0556(itemStack3 -> itemStack3.getItem() == Items.SHIELD && !this.method2580(itemStack3), 0, 35);
         case "Шар" -> this.method2593();
         default -> -1;
      };
   }

   public void method2587(int local4033_1, ItemStack itemStack) {
      this.field08717[local4033_1] = itemStack;
   }

   public ItemStack method2589(int local4035_1) {
      return this.field08717[local4035_1];
   }

   private boolean method2590(ItemStack itemStack) {
      return AutoTotemModule.method3667().field12778.isChecked()
         && !AutoTotemModule.method3667().numericTextSetting.getText().isEmpty()
         && AutoTotemModule.method3667().method3669(itemStack);
   }

   private AutoSwapModule() {
      this.field08701 = this.addSetting(
         new ModeSetting("Режим свапа", "Двойной цикл из двух предметов, тройной колесо на три слота.", "Двойной свап", "Двойной свап", "Тройной свап")
      );
      this.field08702 = this.addSetting(
         new ModeSetting(
            "Первый предмет", "Первый предмет в цикле свапа.", "Золотое яблоко", "Золотое яблоко", "Зачарованное золотое яблоко", "Тотем", "Щит", "Шар"
         )
      );
      this.field08703 = this.addSetting(
         new ModeSetting(
            "Второй предмет", "Второй предмет в цикле свапа.", "Тотем", "Золотое яблоко", "Зачарованное золотое яблоко", "Тотем", "Щит", "Шар"
         )
      );
      this.field08707 = this.addSetting(new KeybindSetting("Свап", "Клавиша свапа предмета."));
      this.field08704 = this.addSetting(new KeybindSetting("Церберка", "Свапает голову цербера в левую руку."));
      this.field08705 = this.addSetting(new KeybindSetting("Вихрь", "Свапает вихрь в левую руку."));
      this.field08706 = this.addSetting(new KeybindSetting("Колесо свапа", "Удерживай, наводи на слот и отпускай, чтобы свапнуть выбранный предмет."));
      this.field08702.withVisibility(() -> this.field08701.isSelected("Двойной свап"));
      this.field08703.withVisibility(() -> this.field08701.isSelected("Двойной свап"));
      this.field08707.withVisibility(() -> this.field08701.isSelected("Двойной свап"));
      this.field08706.withVisibility(() -> this.field08701.isSelected("Тройной свап"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Уведомлять о свапе", "Показывать нотификацию с именем предмета, на который свапнулся.", false));
   }

   private int method2593() {
      int local4040_1 = InventoryUtils.method0556(
         itemStack -> itemStack.getItem() == Items.PLAYER_HEAD
            && itemStack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)
            && !itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
            && !this.method2580(itemStack)
            && !Formatting.strip(itemStack.getName().getString()).toLowerCase().contains("голова"),
         0,
         35
      );
      return local4040_1 != -1
         ? local4040_1
         : InventoryUtils.method0556(
            itemStack2 -> itemStack2.getItem() == Items.PLAYER_HEAD
               && itemStack2.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)
               && !itemStack2.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
               && !this.method2580(itemStack2),
            0,
            35
         );
   }

   private void method2595() {
      if (!this.method2583()) {
         if (this.method2580(mc.player.getOffHandStack()) && !this.field08712.isEmpty()) {
            int local4042_1 = InventoryUtils.method0556(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, this.field08712), 0, 35);
            if (local4042_1 != -1) {
               this.field08712 = ItemStack.EMPTY;
               this.field08711 = mc.player.getInventory().getStack(local4042_1).getItem();
               if (this.booleanSetting.isChecked()) {
                  NotificationHud.method2691(
                     this.category.getIcon(), "Свап", this.method2572(mc.player.getInventory().getStack(local4042_1)), true
                  );
                  ClientChat.method2261(
                     Text.literal("Свап ")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                        .append(this.method2572(mc.player.getInventory().getStack(local4042_1)))
                  );
               }

               InventoryActionQueue.method2656().method2660(local4042_1, 40, 1);
               return;
            }
         }

         if (mc.player.getOffHandStack().getItem() != this.field08711) {
            this.field08710 = this.method2584(this.field08702.getSelectedOption()) == -1;
         }

         int local4042_1 = this.method2584(this.field08710 ? this.field08703.getSelectedOption() : this.field08702.getSelectedOption());
         if (local4042_1 != -1) {
            this.field08710 = !this.field08710;
            this.field08711 = mc.player.getInventory().getStack(local4042_1).getItem();
            if (this.booleanSetting.isChecked()) {
               NotificationHud.method2691(
                  this.category.getIcon(), "Свап", this.method2572(mc.player.getInventory().getStack(local4042_1)), true
               );
               ClientChat.method2261(
                  Text.literal("Свап ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(this.method2572(mc.player.getInventory().getStack(local4042_1)))
               );
            }

            InventoryActionQueue.method2656().method2660(local4042_1, 40, 1);
         }
      }
   }

   public void method2597(int local4044_1) {
      this.field08716 = local4044_1;
      mc.setScreen(new InventoryScreen(mc.player));
   }
}
