package deluxe.client.module.player;

import deluxe.client.util.TargetHudStyle;

import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.ContainerComponentMixin;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.util.ItemCooldownAccess;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.GuiScale;
import deluxe.client.util.WaypointManager;
import deluxe.client.render.FontText;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.module.movement.MovementPauseController;
import deluxe.client.util.TargetHudStyle;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.ui.hud.CooldownsHud;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.module.movement.ScreenWalkModule;
import deluxe.client.setting.SectionSetting;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(name = "ServerHelper", description = "Бинды предметов для SpookyTime, FunTime и HolyWorld.", category = ModuleCategory.PLAYER)
public final class ServerHelperModule extends Module {
   private static ServerHelperModule serverHelperModule;
   public ModeSetting modeSetting;
   public BooleanSetting field01015;
   public MultiSelectSetting multiSelectSetting;
   public BooleanSetting field01017;
   public BooleanSetting field01018;
   public BooleanSetting field01019;
   private static final Pattern field01020 = Pattern.compile("(?m)\\|\\|\\|\\s+\\[([^\\]]+)]|^\\s*\\[\\d+]([^:\\n]+):");
   private static final Pattern field01021 = Pattern.compile("(?:Координаты: \\[|/gps )(-?\\d{1,7})[ ,]+(-?\\d{1,7})[ ,]+(-?\\d{1,7})");
   private static final Pattern field01022 = Pattern.compile("Призван игроком: ([^\\n|]+)");
   private static final Pattern field01023 = Pattern.compile("Уровень лута: ([^\\n|]+)");
   private final ArrayList<ServerHelperModule.Nested0360> field01024 = new ArrayList<>();
   private long field01025;
   private long field01026;
   private ServerHelperModule.Nested0360 field01027;
   private int field01028;
   private int field01029;
   private int field01030;
   private final EventListener<PlayerTickEvent> field01031 = playerTickEvent -> this.method0689();
   private final EventListener<HudRenderEvent> field01032 = hudRenderEvent -> this.method0689();
   private final EventListener<PacketEvent> field01033 = packetEvent -> {
      if (packetEvent.isOutgoing()
         && mc.player != null
         && ScreenWalkModule.method0991().method0988()
         && System.currentTimeMillis() < this.field01026
         && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket) {
         packetEvent.setPacket(new PlayerInteractItemC2SPacket(playerInteractItemC2SPacket.getHand(), playerInteractItemC2SPacket.getSequence(), playerInteractItemC2SPacket.getYaw(), 90.0F));
         this.field01026 = 0L;
      } else {
         if (packetEvent.isIncoming() && this.field01019.isChecked() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
            Matcher matcher = field01020.matcher(Formatting.strip(gameMessageS2CPacket.content().getString()));
            Matcher matcher2 = field01021.matcher(Formatting.strip(gameMessageS2CPacket.content().getString() + gameMessageS2CPacket.content().toString()));
            Matcher matcher3 = field01022.matcher(Formatting.strip(gameMessageS2CPacket.content().getString()));
            Matcher matcher4 = field01023.matcher(Formatting.strip(gameMessageS2CPacket.content().getString()));
            if (matcher.find()) {
               mc.execute(
                  () -> this.method0679(
                     (matcher.group(1) == null ? matcher.group(2) : matcher.group(1)).trim(),
                     matcher2.find()
                        ? new BlockPos(Integer.parseInt(matcher2.group(1)), Integer.parseInt(matcher2.group(2)), Integer.parseInt(matcher2.group(3)))
                        : null,
                     matcher3.find() ? "Призван: " + matcher3.group(1).trim() : null,
                     matcher4.find() ? "Лут: " + matcher4.group(1).trim() : null
                  )
               );
            }
         }

         if (packetEvent.isIncoming()
            && packetEvent.getPacket() instanceof OverlayMessageS2CPacket overlayMessageS2CPacket
            && overlayMessageS2CPacket.text().getString().contains("будет доступн")) {
            Matcher matcher5 = Pattern.compile("(.*через )(\\d[\\d.,]*)( ?сек.*)").matcher(Formatting.strip(overlayMessageS2CPacket.text().getString()));
            if (matcher5.matches() && System.currentTimeMillis() - this.field01025 >= 2000L) {
               this.field01025 = System.currentTimeMillis();
               mc.execute(
                  () -> {
                     NotificationHud.method2691(
                        this.category.getIcon(),
                        "Кулдаун",
                        Text.literal(matcher5.group(1))
                           .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                           .append(Text.literal(matcher5.group(2) + matcher5.group(3)).setStyle(Style.EMPTY.withColor(Formatting.RED))),
                        true
                     );
                     CooldownsHud.method4553("Исцел/Святая вода", Items.SPLASH_POTION, Float.parseFloat(matcher5.group(2).replace(',', '.')));
                  }
               );
            }
         }
      }
   };
   private final EventListener<HudRenderEvent> field01034 = hudRenderEvent2 -> {
      if (mc.player != null && this.field01018.isChecked()) {
         int local905_2 = 0;

         for (int index = 0; index < mc.player.getInventory().size(); index++) {
            if (mc.player.getInventory().getStack(index).isIn(ItemTags.ARROWS)) {
               local905_2 += mc.player.getInventory().getStack(index).getCount();
            }
         }

         String local905_3 = String.valueOf(local905_2);
         float local905_4 = GuiScale.method0797() / GuiScale.method0794();
         float local905_5 = 6.0F * local905_4;
         float local905_6 = 0.7F * local905_4;

         for (int index2 = 0; index2 < 9; index2++) {
            if (mc.player.getInventory().getStack(index2).isOf(Items.BOW)
               || mc.player.getInventory().getStack(index2).isOf(Items.CROSSBOW)) {
               float local905_8 = (hudRenderEvent2.method4462() / 2.0F - 96.0F + index2 * 20.0F + 16.0F) * local905_4
                  - FontText.method1396(FontFace.BOLD, local905_3, local905_5);
               float local905_9 = (hudRenderEvent2.method4460() - 19.0F + 6.0F) * local905_4 - local905_5;
               FontText.method1397(FontFace.BOLD, local905_3, local905_8 - local905_6, local905_9, local905_5, -16777216);
               FontText.method1397(FontFace.BOLD, local905_3, local905_8 + local905_6, local905_9, local905_5, -16777216);
               FontText.method1397(FontFace.BOLD, local905_3, local905_8, local905_9 - local905_6, local905_5, -16777216);
               FontText.method1397(FontFace.BOLD, local905_3, local905_8, local905_9 + local905_6, local905_5, -16777216);
               FontText.method1397(FontFace.BOLD, local905_3, local905_8, local905_9, local905_5, -1);
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field01035 = playerTickEvent2 -> {
      if (this.field01027 != null && mc.player != null && mc.interactionManager != null) {
         this.field01030++;
         switch (this.field01029) {
            case 0:
               if (MovementPauseController.getInstance().getRemainingTicks() >= 1) {
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler
                  || !this.method0704(mc.player.getInventory().getStack(this.field01028), this.field01027)) {
                  this.field01027 = null;
                  return;
               }

               mc.interactionManager
                  .clickSlot(
                     mc.player.playerScreenHandler.syncId,
                     this.field01028 < 9 ? this.field01028 + 36 : this.field01028,
                     1,
                     SlotActionType.PICKUP,
                     mc.player
                  );
               this.field01029 = 1;
               this.field01030 = 0;
               break;
            case 1: {
               ScreenHandler screenHandler = mc.player.currentScreenHandler;
               if (screenHandler != mc.player.playerScreenHandler && screenHandler.slots.size() > 36) {
                  if (!mc.player.playerScreenHandler.getCursorStack().isEmpty() && mc.player.getInventory().getStack(this.field01028).isEmpty()
                     )
                   {
                     mc.player.getInventory().setStack(this.field01028, mc.player.playerScreenHandler.getCursorStack());
                     mc.player.playerScreenHandler.setCursorStack(ItemStack.EMPTY);
                  }

                  if (this.method0683(screenHandler) == -1) {
                     if (this.field01030 <= 5) {
                        return;
                     }

                     mc.player.closeHandledScreen();
                     this.field01027 = null;
                     return;
                  }

                  MovementPauseController.getInstance().pauseForTicks(1);
                  this.field01029 = 2;
                  break;
               }

               if (this.field01030 <= 20) {
                  return;
               }

               if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                  mc.interactionManager
                     .clickSlot(
                        mc.player.playerScreenHandler.syncId,
                        this.field01028 < 9 ? this.field01028 + 36 : this.field01028,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                     );
               }

               NotificationHud.method2691(
                  this.category.getIcon(), "Шалкер", Text.literal("не открылся").setStyle(Style.EMPTY.withColor(Formatting.RED)), true
               );
               this.field01027 = null;
               return;
            }
            case 2: {
               if (MovementPauseController.getInstance().getRemainingTicks() >= 1) {
                  return;
               }

               ScreenHandler screenHandler2 = mc.player.currentScreenHandler;
               int local913_3 = screenHandler2.slots.size() - 36;
               int local913_4 = this.method0683(screenHandler2);
               int local913_5 = InventoryUtils.method0546(9, 35) != -1 ? InventoryUtils.method0546(9, 35) : InventoryUtils.method0546(0, 8);
               if (screenHandler2 != mc.player.playerScreenHandler && local913_4 != -1 && local913_5 != -1 && screenHandler2.getCursorStack().isEmpty()) {
                  boolean local913_6 = this.field01027.method0712(screenHandler2.getSlot(local913_4).getStack());
                  int local913_7 = local913_6 ? -1 : this.method0678(screenHandler2.getSlot(local913_4).getStack(), this.field01027);
                  if (!local913_6 && BundleItem.getSelectedStackIndex(screenHandler2.getSlot(local913_4).getStack()) != local913_7) {
                     screenHandler2.selectBundleStack(local913_4, local913_7);
                     mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(local913_4, local913_7));
                  }

                  mc.interactionManager.clickSlot(screenHandler2.syncId, local913_4, local913_6 ? 0 : 1, SlotActionType.PICKUP, mc.player);
                  screenHandler2.nextRevision();
                  mc.interactionManager
                     .clickSlot(
                        screenHandler2.syncId,
                        local913_5 < 9 ? local913_3 + 27 + local913_5 : local913_3 + local913_5 - 9,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                     );
                  mc.player.closeHandledScreen();
                  this.field01029 = 3;
                  break;
               }

               if (screenHandler2 != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
               }

               this.field01027 = null;
               return;
            }
            default: {
               ServerHelperModule.Nested0360 local913_2 = this.field01027;
               this.field01027 = null;
               this.method0701(local913_2);
            }
         }
      }
   };

   private int method0678(ItemStack itemStack, ServerHelperModule.Nested0360 local903_2) {
      if (itemStack.get(DataComponentTypes.BUNDLE_CONTENTS) == null) {
         return -1;
      }

      for (int index = 0; index < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index++) {
         if (local903_2.method0712(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index))) {
            return index;
         }
      }

      return -1;
   }

   private void method0679(String local904_1, BlockPos blockPos, String stringArray, String local904_4) {
      if (blockPos == null) {
         switch (local904_1.toLowerCase()) {
            case "сундук смерти":
               WaypointManager.method0947().method0951(local904_1, new BlockPos(-155, 64, 205), 300, 0, stringArray, local904_4);
               break;
            case "адская резня":
               WaypointManager.method0947().method0951(local904_1, new BlockPos(48, 87, 73), 180, 120, stringArray, local904_4);
         }
      } else {
         switch (local904_1.toLowerCase()) {
            case "мистический сундук":
               WaypointManager.method0947().method0951(local904_1, blockPos, 300, 0, stringArray, local904_4);
               break;
            case "вулкан":
               WaypointManager.method0947().method0951(local904_1, blockPos, 300, 120, stringArray, local904_4);
               break;
            case "метеоритный дождь":
               WaypointManager.method0947().method0951(local904_1, blockPos, 360, 0, stringArray, local904_4);
               break;
            case "маяк убийца":
               WaypointManager.method0947().method0951(local904_1, blockPos, 360, 0, stringArray, local904_4);
               break;
            case "мистический алтарь":
               WaypointManager.method0947().method0951(local904_1, blockPos, 360, 0, stringArray, local904_4);
               break;
            case "загадочный маяк":
               WaypointManager.method0947().method0951(local904_1, blockPos, 60, 180, stringArray, local904_4);
         }
      }
   }

   public int method0681(int local906_1, int local906_2) {
      if (this.isEnabled() && this.field01015.isChecked()) {
         int local906_3 = Math.floorMod(local906_2 - local906_1, 9) <= 4 ? 1 : -1;
         int local906_4 = local906_2;

         for (int index = 0; index < 9; index++) {
            if (!this.multiSelectSetting.isIndexSelected(local906_4)) {
               return local906_4;
            }

            local906_4 = Math.floorMod(local906_4 + local906_3, 9);
         }

         return local906_1;
      } else {
         return local906_2;
      }
   }

   private void method0682(ItemStack itemStack) {
      NotificationHud.method2691(
         this.category.getIcon(),
         Formatting.strip(itemStack.getName().getString()),
         Text.literal("в кд ещё ")
            .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
            .append(
               Text.literal(
                     String.format(
                        Locale.US, "%.1fс", ((ItemCooldownAccess)mc.player.getItemCooldownManager()).remainingCooldownTicks(itemStack) / 20.0F
                     )
                  )
                  .setStyle(Style.EMPTY.withColor(Formatting.RED))
            ),
         true
      );
   }

   private int method0683(ScreenHandler screenHandler) {
      for (int index = 0; index < screenHandler.slots.size() - 36; index++) {
         if (this.field01027.method0712(screenHandler.getSlot(index).getStack())
            || this.method0678(screenHandler.getSlot(index).getStack(), this.field01027) != -1) {
            return index;
         }
      }

      return -1;
   }

   public void method0686(String local911_1) {
      if (this.isEnabled() && this.field01017.isChecked() && mc.player != null && !local911_1.toLowerCase().matches(".*(радиац|снотворн).*")) {
         this.field01026 = System.currentTimeMillis() + (ScreenWalkModule.method0991().method0988() ? 500 : 200);
         if (!ScreenWalkModule.method0991().method0988()) {
            RotationManager.method0658()
               .method0648(new RotationAngles(mc.player.getYaw(), 90.0F), new RotationSpeed(355.0F, 355.0F, 28.0F, 28.0F), 300L);
         }
      }
   }

   private void method0687(ServerHelperModule.Nested0360 local912_1) {
      if (local912_1.field01173 != null && local912_1.field01171 == null) {
         this.method0686(local912_1.field01173);
      }
   }

   private void method0689() {
      if (mc.player != null && mc.interactionManager != null && mc.currentScreen == null) {
         for (ServerHelperModule.Nested0360 local914_2 : this.field01024) {
            if (local914_2.keybindSetting.isVisible()
               && local914_2.keybindSetting.getBinding().method2749(mc.getWindow().getHandle())
               && !local914_2.field01175) {
               this.method0701(local914_2);
            }

            local914_2.field01175 = local914_2.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         }
      } else {
         for (ServerHelperModule.Nested0360 local914_2 : this.field01024) {
            local914_2.field01175 = local914_2.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         }
      }
   }

   public ArrayList<ServerHelperModule.Nested0360> method0690() {
      return this.field01024;
   }

   private void method0691(ItemStack itemStack) {
      NotificationHud.method2691(
         this.category.getIcon(),
         Formatting.strip(itemStack.getName().getString()),
         Text.literal("использовано").setStyle(Style.EMPTY.withColor(Formatting.GREEN)),
         true
      );
   }

   private void method0697(String newName, String... stringArray) {
      KeybindSetting currentKeybindSetting = this.addSetting(new KeybindSetting(newName, "Использует предмет по нажатию кнопки."));
      currentKeybindSetting.withVisibility(() -> !this.modeSetting.isSelected("HolyWorld"));
      this.field01024.add(new ServerHelperModule.Nested0360(newName, currentKeybindSetting, stringArray));
   }

   private void method0699(String newName, Item item, String local925_3) {
      KeybindSetting currentKeybindSetting = this.addSetting(new KeybindSetting(newName, "Использует предмет по нажатию кнопки."));
      currentKeybindSetting.withVisibility(() -> this.modeSetting.isSelected("HolyWorld"));
      this.field01024.add(new ServerHelperModule.Nested0360(local925_3, item, currentKeybindSetting));
   }

   private void method0700(String newName, Item item, String local926_3) {
      KeybindSetting currentKeybindSetting = this.addSetting(new KeybindSetting(newName, "Использует предмет по нажатию кнопки."));
      currentKeybindSetting.withVisibility(() -> !this.modeSetting.isSelected("HolyWorld"));
      this.field01024.add(new ServerHelperModule.Nested0360(item, currentKeybindSetting));
   }

   public void method0701(ServerHelperModule.Nested0360 local927_1) {
      if (!InventorySlotManager.method4176().method4179() && !InventoryActionQueue.method2656().method2665()) {
         int local927_2 = InventoryUtils.method0555(itemStack -> local927_1.method0712(itemStack));
         if (local927_2 != -1) {
            if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local927_2))) {
               this.method0682(mc.player.getInventory().getStack(local927_2));
            } else {
               this.method0687(local927_1);
               TargetHudStyle.method3842(mc.player.getInventory().getStack(local927_2).getItem());
               this.method0691(mc.player.getInventory().getStack(local927_2));
               InventorySlotManager.method4176().method4195(mc.player.getInventory().getStack(local927_2).copy(), true);
            }
         } else {
            int local927_3 = InventoryUtils.method0555(itemStack2 -> this.method0678(itemStack2, local927_1) != -1);
            if (local927_3 == -1) {
               int local927_4 = InventoryUtils.method0555(itemStack3 -> this.method0704(itemStack3, local927_1));
               if (local927_4 != -1) {
                  if (InventoryUtils.method0546(0, 35) == -1) {
                     NotificationHud.method2691(
                        this.category.getIcon(),
                        "Шалкер",
                        Text.literal("Нет свободного слота под предмет из мешка").setStyle(Style.EMPTY.withColor(Formatting.RED)),
                        true
                     );
                  } else if (this.field01027 == null && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
                     this.field01027 = local927_1;
                     this.field01028 = local927_4;
                     this.field01029 = 0;
                     this.field01030 = 0;
                     MovementPauseController.getInstance().pauseForTicks(1);
                  }
               } else {
                  NotificationHud.method2691(
                     this.category.getIcon(),
                     local927_1.getKeybindSetting().name,
                     Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
                     true
                  );
               }
            } else {
               ItemStack itemStack4 = mc.player
                  .getInventory()
                  .getStack(local927_3)
                  .get(DataComponentTypes.BUNDLE_CONTENTS)
                  .get(this.method0678(mc.player.getInventory().getStack(local927_3), local927_1));
               if (mc.player.getItemCooldownManager().isCoolingDown(itemStack4)) {
                  this.method0682(itemStack4);
               } else if (InventoryUtils.method0546(0, 35) == -1) {
                  NotificationHud.method2691(
                     this.category.getIcon(),
                     "Мешок",
                     Text.literal("Нет свободного слота под предмет из мешка").setStyle(Style.EMPTY.withColor(Formatting.RED)),
                     true
                  );
               } else {
                  this.method0687(local927_1);
                  TargetHudStyle.method3842(itemStack4.getItem());
                  this.method0691(itemStack4);
                  InventorySlotManager.method4176().method4195(itemStack4.copy(), true);
               }
            }
         }
      }
   }

   public void method0703() {
      if (mc.player != null) {
         this.field01026 = System.currentTimeMillis() + (ScreenWalkModule.method0991().method0988() ? 500 : 200);
         if (!ScreenWalkModule.method0991().method0988()) {
            RotationManager.method0658()
               .method0648(new RotationAngles(mc.player.getYaw(), 90.0F), new RotationSpeed(355.0F, 355.0F, 28.0F, 28.0F), 300L);
         }
      }
   }

   private boolean method0704(ItemStack itemStack, ServerHelperModule.Nested0360 local930_2) {
      if (Block.getBlockFromItem(itemStack.getItem()) instanceof ShulkerBoxBlock && itemStack.get(DataComponentTypes.CONTAINER) != null) {
         for (ItemStack itemStack2 : ((ContainerComponentMixin)(Object)itemStack.get(DataComponentTypes.CONTAINER)).getItems()) {
            if (local930_2.method0712(itemStack2) || this.method0678(itemStack2, local930_2) != -1) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private ServerHelperModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Сервер", "Для какого сервера предметы.", "SpookyTime", "SpookyTime", "FunTime", "HolyWorld"));
      this.field01015 = this.addSetting(
         new BooleanSetting("Умный скролл", "Скролл колёсиком пропускает выбранные слоты, выбор цифрами работает как обычно.", false)
      );
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Слоты без скролла", "Какие слоты хотбара пропускать при скролле.", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"})
      );
      this.multiSelectSetting.withVisibility(() -> this.field01015.isChecked());
      this.field01017 = this.addSetting(
         new BooleanSetting("Авто питч", "Кидает зелья по бинду себе под ноги, глядя вниз. Радиацию и Снотворное не трогает.", false)
      );
      this.field01018 = this.addSetting(new BooleanSetting("Показывать стрелы", "У лука и арбалета в хотбаре рисует общее число стрел в инвентаре.", false));
      this.field01019 = this.addSetting(new BooleanSetting("Точка на ивент", "Ставит метку по координатам ивента из чата.", false));
      this.addSetting(new SectionSetting("Предметы"));
      this.method0700("Снежок", Items.SNOWBALL, "snowball");
      this.method0700("Божья аура", Items.PHANTOM_MEMBRANE, "phantom membrane");
      this.method0700("Трапка", Items.NETHERITE_SCRAP, "netherite scrap");
      this.method0700("Пласт", Items.DRIED_KELP, "dried kelp");
      this.method0700("Явная пыль", Items.SUGAR, "sugar");
      this.method0700("Огненный смерч", Items.FIRE_CHARGE, "fire charge");
      this.method0700("Дезориентация", Items.ENDER_EYE, "ender eye");
      this.method0699("Взрывная трапка", Items.PRISMARINE_SHARD, "взрывная трапка");
      this.method0699("Обычная трапка", Items.POPPED_CHORUS_FRUIT, "трапка");
      this.method0699("Стан", Items.NETHER_STAR, "стан");
      this.method0699("Взрывная штучка", Items.FIRE_CHARGE, "взрывная штучка");
      this.method0699("Ком снега", Items.SNOWBALL, "ком снега");
      this.addSetting(new SectionSetting("Зелья")).withVisibility(() -> !this.modeSetting.isSelected("HolyWorld"));
      this.method0697("Зелье Палладина", "minecraft:resistance", "minecraft:fire_resistance", "minecraft:health_boost", "minecraft:invisibility");
      this.method0697("Зелье Гнева", "minecraft:strength", "minecraft:slowness");
      this.method0697("Хлопушка", "minecraft:slowness", "minecraft:speed", "minecraft:blindness", "minecraft:glowing");
      this.method0697("Святая вода", "minecraft:regeneration", "minecraft:invisibility", "minecraft:instant_health");
      this.method0697("Зелье Ассасина", "minecraft:strength", "minecraft:speed", "minecraft:haste", "minecraft:instant_damage");
      this.method0697("Зелье Радиации", "minecraft:poison", "minecraft:wither", "minecraft:slowness", "minecraft:hunger", "minecraft:glowing");
      this.method0697("Снотворное", "minecraft:weakness", "minecraft:mining_fatigue", "minecraft:wither", "minecraft:blindness");
      this.method0697("Зелье Прыгучести");
   }

   public static ServerHelperModule method0707() {
      if (serverHelperModule == null) {
         serverHelperModule = new ServerHelperModule();
      }

      return serverHelperModule;
   }

   public static final class Nested0360 {
      private final Item field01171;
      private final KeybindSetting keybindSetting;
      private final String field01173;
      private String[] field01174;
      private boolean field01175;

      private Nested0360(Item item, KeybindSetting initialKeybindSetting) {
         this.field01171 = item;
         this.keybindSetting = initialKeybindSetting;
         this.field01173 = null;
      }

         public Item method0710() {
         return this.field01171;
      }

         public KeybindSetting getKeybindSetting() {
         return this.keybindSetting;
      }

      public boolean method0712(ItemStack itemStack) {
         if (itemStack.isEmpty()) {
            return false;
         } else if (this.field01171 != null && itemStack.getItem() != this.field01171) {
            return false;
         } else {
            return this.field01174 != null && this.field01174.length > 0
               ? this.method0713(itemStack)
               : this.field01173 == null || Formatting.strip(itemStack.getName().getString()).toLowerCase().contains(this.field01173);
         }
      }

      private Nested0360(String local941_1, Item item, KeybindSetting initialKeybindSetting) {
         this.field01171 = item;
         this.keybindSetting = initialKeybindSetting;
         this.field01173 = local941_1.toLowerCase();
      }

      private boolean method0713(ItemStack itemStack) {
         for (String local942_5 : this.field01174) {
            boolean local942_6 = false;

            for (StatusEffectInstance statusEffectInstance : itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) {
               if (statusEffectInstance.getEffectType().getIdAsString().equals(local942_5)) {
                  local942_6 = true;
               }
            }

            if (!local942_6) {
               return false;
            }
         }

         return true;
      }

      private Nested0360(String local943_1, KeybindSetting initialKeybindSetting, String... stringArray) {
         this.field01171 = null;
         this.keybindSetting = initialKeybindSetting;
         this.field01173 = local943_1.toLowerCase();
         this.field01174 = stringArray;
      }
   }
}
