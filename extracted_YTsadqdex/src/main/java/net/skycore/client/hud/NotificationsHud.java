package net.skycore.client.hud;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.SelectAndSaveHelper;
import net.skycore.client.util.TextHeightHelper;
import org.joml.Matrix3x2fStack;

public final class NotificationsHud {
   private static final float SEPARATOR_HEIGHT = 9.5F;
   private static final float PAD_X = 8.0F;
   private boolean wasPreview;
   private static final long HOLD_MS = 2400L;
   private final List<NotificationsHud.PendingBreak> pendingBreaks;
   private static final long BREAK_CONFIRM_MS = 250L;
   private float animatedWidth;
   private static final long EXIT_MS = 240L;
   private static final float ICON_SIZE = 10.75F;
   private static final float SEPARATOR_WIDTH = 1.0F;
   private long lastFrameAt;
   private final EnumMap<EquipmentSlot, NotificationsHud.ArmorState> armorStates;
   private boolean suppress;
   private static final float HEIGHT = 23.5F;
   private static final int ARMOR_WARNING_DURABILITY = 50;
   private static final float TEXT_SIZE = 9.0F;
   private Player trackedPlayer;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final float TOGGLE_KNOB = 4.5F;
   private static final float TOGGLE_INSET = 2.0F;
   private static final float TOGGLE_WIDTH = 12.0F;
   private static final long ENTER_MS = 260L;
   private final List<NotificationsHud.Entry> entries;
   private static final float STACK_GAP = 6.5F;
   private static final float TOGGLE_HEIGHT = 8.5F;
   private final Draggable draggable = new Draggable("notifications", 0.5F, 0.58F).lockAxis(Draggable.AxisLock.VERTICAL);
   private static final float RADIUS = 10.0F;
   private static final float SLIDE_DISTANCE = 12.0F;
   private static final float WIDTH_SPEED = 18.0F;
   private static final float CONTENT_GAP = 5.5F;

   private void notifyArmorBroken(Player player, String text) {
      if (!this.suppress) {
         NotificationsHud.Type type = NotificationsHud.Type.ARMOR_BROKEN;
         String text2 = SelectAndSaveHelper.isRussian() ? "Сломалось: " : "Broken: ";
         long nowNanos = System.nanoTime();
         NotificationsHud.Entry entry = new NotificationsHud.Entry(type, text2, text, "", nowNanos);
         this.push(entry);
         player.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 0.7F);
         player.playSound(SoundEvents.TOTEM_USE, 0.35F, 1.35F);
      }
   }

   private void confirmPendingBreaks(Player player, long timestamp) {
      Iterator iterator = this.pendingBreaks.iterator();

      while (iterator.hasNext()) {
         NotificationsHud.PendingBreak element = (NotificationsHud.PendingBreak)iterator.next();
         if (containsStack(player, element.item, element.damage)) {
            iterator.remove();
         } else {
            if (timestamp >= element.confirmAt) {
               this.notifyArmorBroken(player, element.name);
               iterator.remove();
            }
         }
      }
   }

   private void push(NotificationsHud.Entry entry) {
      if (!this.suppress && entry != null) {
         if (entry.preview) {
            if (this.entries.stream().noneMatch(entry2 -> {
               return entry2.preview;
            })) {
               long nowNanos = System.nanoTime();
               entry.activate(nowNanos);
               this.entries.add(entry);
            }
         } else {
            var entries2 = this.entries;
            Predicate<net.skycore.client.hud.NotificationsHud.Entry> predicate = entry3 -> {
               return entry3.preview;
            };
            entries2.removeIf(predicate);
            entry.activate(System.nanoTime());
            this.entries.add(0, entry);
         }
      }
   }

   public void onConfigLoaded(String text) {
      if (!this.suppress) {
         String text2;
         if (text != null && !text.isBlank()) {
            text2 = text.trim();
         } else {
            text2 = "Config";
         }

         String text3 = text2;
         NotificationsHud.Type type = NotificationsHud.Type.CONFIG;
         String text4 = "Конфиг " + text3 + " загружен";
         long nowNanos = System.nanoTime();
         this.push(new NotificationsHud.Entry(type, text4, "", "", nowNanos));
      }
   }

   private static int withAlpha(int value, float value2) {
      float computedValue = (value >>> 24 & 0xFF) * clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(computedValue);
      return value & 16777215 | roundedValue << 24;
   }

   public void onItemUsed(String text, ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         NotificationsHud.Type type = NotificationsHud.Type.ITEM_USED;
         long nowNanos = System.nanoTime();
         NotificationsHud.Entry entry = new NotificationsHud.Entry(type, text, "", "", nowNanos);
         ItemStack itemStack2 = itemStack.copy();
         entry.item = itemStack2;
         entry.item.setCount(1);
         this.push(entry);
      }
   }

   private void drawEntry(NotificationsHud.Entry entry, int coordinate, float coordinate2, float height, float width, long timestamp) {
      float alpha = entry.progress(timestamp);
      float computedValue = entry.slideOffset(alpha, 12.0F * width);
      float alpha2 = entry.alpha(alpha);
      if (!(alpha2 <= 0.01F)) {
         float value = entry.width(width);
         float computedValue2 = Math.max(0.0F, (coordinate - value) * 0.5F);
         float height2 = coordinate2 + computedValue;
         ThemeManager themeManager = SkycoreClient.themes();
         int computedValue3 = withAlpha(themeManager.textPrimary(), 0.75F * alpha2);
         int color = withAlpha(-1, 0.03F * alpha2);
         int color2 = withAlpha(themeManager.accent(), alpha2);
         drawBlock(computedValue2, height2, value, height, 10.0F * width, width, alpha2);
         float height3 = computedValue2 + 8.0F * width;
         if (entry.type == NotificationsHud.Type.MODULE_ON || entry.type == NotificationsHud.Type.MODULE_OFF) {
            int value2 = entry.type == NotificationsHud.Type.MODULE_ON ? 1 : 0;
            float computedValue4 = 12.0F * width;
            float computedValue5 = 8.5F * width;
            float computedValue6 = height2 + (height - computedValue5) * 0.5F;
            int color3 = value2 != 0 ? color2 : withAlpha(-1, 0.05F * alpha2);
            int color4 = value2 != 0 ? withAlpha(-1, alpha2) : withAlpha(-1, 0.15F * alpha2);
            BeginFrameHelper.hudIconGlow(height3 + computedValue4 * 0.5F, computedValue6 + computedValue5 * 0.5F, computedValue5, alpha2, 0.7F, 0.75F);
            BeginFrameHelper.rect(height3, computedValue6, computedValue4, computedValue5).color(color3).radius(computedValue5 * 0.5F).hudGlow(false).draw();
            float computedValue7 = 4.5F * width;
            float computedValue8 = value2 != 0 ? height3 + computedValue4 - 2.0F * width - computedValue7 : height3 + 2.0F * width;
            float computedValue9 = computedValue6 + (computedValue5 - computedValue7) * 0.5F;
            BeginFrameHelper.rect(computedValue8, computedValue9, computedValue7, computedValue7).color(color4).radius(computedValue7 * 0.5F).hudGlow(false).draw();
            height3 += computedValue4 + 5.5F * width;
         } else if (!entry.item.isEmpty()) {
            float computedValue10 = 16.0F * width;
            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor != null) {
               BeginFrameHelper.flush();
               float value3 = height3;
               float computedValue11 = height2 + (height - computedValue10) * 0.5F;
               CaptureHelper.withAlpha(alpha2, () -> {
                  Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
                  matrix3x2fStack.pushMatrix();
                  matrix3x2fStack.translate(value3, computedValue11);
                  float intermediate = computedValue10 / 16.0F;
                  matrix3x2fStack.scale(intermediate);
                  guiGraphicsExtractor.item(entry.item, 0, 0);
                  matrix3x2fStack.popMatrix();
               });
            }

            height3 += computedValue10 + 5.5F * width;
         } else {
            String text = entry.type == NotificationsHud.Type.CONFIG ? "\ue006" : "k";
            float width2 = 10.75F * width;
            float width3;
            if (entry.type == NotificationsHud.Type.CONFIG) {
               width3 = InterBoldHelper.csGuiIcons().getWidth(text, width2);
               BeginFrameHelper.text(height3, height2 + (height - InterBoldHelper.csGuiIcons().textHeight(width2)) * 0.5F, width2, text)
                  .font(InterBoldHelper.csGuiIcons())
                  .color(color2)
                  .draw();
            } else {
               width3 = InterBoldHelper.clickGuiIcons().getWidth(text, width2);
               BeginFrameHelper.text(height3, height2 + (height - width2) * 0.5F - 1.0F * width - 4.5F, width2, text)
                  .font(InterBoldHelper.clickGuiIcons())
                  .color(color2)
                  .draw();
            }

            height3 += width3 + 5.5F * width;
         }

         float computedValue12 = 9.5F * width;
         BeginFrameHelper.rect(height3 - 1.0F, height2 + (height - computedValue12) * 0.5F, 1.0F, computedValue12).color(color).radius(1.0F * width).hudGlow(false).draw();
         height3 += 1.0F * width + 5.5F * width;
         float computedValue13 = 9.0F * width;
         float computedValue14 = height2 + (height - computedValue13) * 0.5F - 0.8F * width;
         height3 = drawSegment(height3, computedValue14, computedValue13, entry.prefix, computedValue3);
         int computedValue15 = entry.type == NotificationsHud.Type.MODULE_OFF ? withAlpha(-1, 0.25F * alpha2) : color2;
         height3 = drawSegment(height3, computedValue14, computedValue13, entry.highlight, computedValue15);
         drawSegment(height3, computedValue14, computedValue13, entry.suffix, computedValue3);
      }
   }

   public void onModuleToggle(ClientModule clientModule, boolean condition) {
      if (!this.suppress && clientModule != null && !(clientModule instanceof ClickGUIModule)) {
         NotificationsHud.Entry entry;
         NotificationsHud.Type type;
         if (condition) {
            type = NotificationsHud.Type.MODULE_ON;
         } else {
            type = NotificationsHud.Type.MODULE_OFF;
         }

         String name = clientModule.getName();
         String text = condition ? " активирована" : " деактивирована";
         long nowNanos = System.nanoTime();
         entry = new NotificationsHud.Entry(type, "Функция ", name, text, nowNanos);
         this.push(entry);
      }
   }

   private void queuePossibleBreak(EquipmentSlot equipmentSlot, NotificationsHud.ArmorState armorState, long timestamp) {
      this.pendingBreaks.removeIf(pendingBreak -> {
         return pendingBreak.slot == equipmentSlot;
      });
      var pendingBreaks2 = this.pendingBreaks;
      long timestamp2 = timestamp + 250000000L;
      NotificationsHud.PendingBreak pendingBreak2 = new NotificationsHud.PendingBreak(equipmentSlot, armorState.item, armorState.damage, armorState.name, timestamp2);
      pendingBreaks2.add(pendingBreak2);
   }

   public NotificationsHud() {
      this.entries = new ArrayList<>();
      this.armorStates = new EnumMap<>(EquipmentSlot.class);
      this.pendingBreaks = new ArrayList<>();
   }

   private static String armorAlmostBrokenSuffix(EquipmentSlot equipmentSlot, ItemStack itemStack) {
      return switch (equipmentSlot) {
         case HEAD, CHEST -> {
            yield itemStack.is(Items.ELYTRA) ? " почти сломаны" : " почти сломан";
         }
         case LEGS, FEET -> " почти сломаны";
         default -> " почти сломана";
      };
   }

   private static boolean matchesStack(ItemStack itemStack, Item item, int damage) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (itemStack.getItem() == item && itemStack.getDamageValue() == damage) {
            return true;
         }
      }

      return false;
   }

   public void render(float width, boolean wasPreview2, int coordinate) {
      long nowNanos = System.nanoTime();
      float computedValue = this.lastFrameAt == 0L ? 0.016F : Math.min(0.05F, (float)(nowNanos - this.lastFrameAt) / 1.0E9F);
      this.lastFrameAt = nowNanos;
      if (wasPreview2) {
         if (!this.wasPreview && this.entries.stream().noneMatch(entry -> {
            return entry.preview;
         })) {
            this.push(previewEntry(nowNanos));
         } else if (this.entries.isEmpty()) {
            this.push(previewEntry(nowNanos));
         }
      } else if (this.wasPreview) {
         for (NotificationsHud.Entry entry2 : this.entries) {
            if (entry2.preview && !entry2.exiting) {
               entry2.startExit(nowNanos);
            }
         }
      }

      this.wasPreview = wasPreview2;

      for (NotificationsHud.Entry entry3 : this.entries) {
         if (!entry3.preview && !entry3.exiting && entry3.shouldExit(nowNanos)) {
            entry3.startExit(nowNanos);
         }
      }

      this.entries.removeIf(entry4 -> {
         return entry4.finished(nowNanos);
      });
      if (wasPreview2 && this.entries.isEmpty()) {
         this.push(previewEntry(nowNanos));
      }

      float computedValue2 = 23.5F * width;
      float computedValue3 = 6.5F * width;
      float value = 30.0F;

      for (int value2 = 0; value2 < this.entries.size(); value2++) {
         this.entries.get(value2).updateStack(value2 * value, computedValue);
      }

      float width2 = 0.0F;

      for (NotificationsHud.Entry entry5 : this.entries) {
         float value3 = entry5.alpha(entry5.progress(nowNanos));
         width2 = Math.max(width2, entry5.width(width) * Math.max(value3, 0.15F));
      }

      if (width2 <= 0.0F && wasPreview2) {
         width2 = previewEntry(nowNanos).width(width);
      }

      if (this.animatedWidth <= 0.0F) {
         this.animatedWidth = width2;
      } else {
         this.animatedWidth = moveTowards(this.animatedWidth, width2, 1080.0F * computedValue);
      }

      float maximum = Math.max(this.animatedWidth, 0.0F);
      int size = this.entries.size();
      float computedValue4 = size <= 0 ? 0.0F : size * computedValue2 + Math.max(0, size - 1) * computedValue3;
      int value4 = size > 0 ? 1 : 0;
      this.draggable.setBounds(value4 == 0 && !wasPreview2 ? 0.0F : Math.max(maximum, 1.0F), value4 == 0 && !wasPreview2 ? 0.0F : Math.max(computedValue4, computedValue2));
      float computedValue5 = Math.max(0.0F, (coordinate - Math.max(maximum, 1.0F)) * 0.5F);
      this.draggable.setTarget(computedValue5, this.draggable.targetY());
      if (Math.abs(this.draggable.x() - computedValue5) > 0.5F && !this.draggable.dragging()) {
         this.draggable.snapX(computedValue5);
      }

      float y = this.draggable.y();

      for (NotificationsHud.Entry entry6 : this.entries) {
         this.drawEntry(entry6, coordinate, y + entry6.stackY * width, computedValue2, width, nowNanos);
      }
   }

   private static float moveTowards(float value, float value2, float value3) {
      if (value < value2) {
         return Math.min(value2, value + value3);
      }

      float intermediate = value - value3;
      return Math.max(value2, intermediate);
   }

   private void resetArmorTracking() {
      this.trackedPlayer = null;
      this.armorStates.clear();
      this.pendingBreaks.clear();
   }

   private static String armorName(EquipmentSlot equipmentSlot, ItemStack itemStack) {
      boolean russian = SelectAndSaveHelper.isRussian();

      return switch (equipmentSlot) {
         case HEAD -> russian ? "Шлем" : "Helmet";
         case CHEST -> itemStack.is(Items.ELYTRA) ? (russian ? "Элитры" : "Elytra") : (russian ? "Нагрудник" : "Chestplate");
         case LEGS -> russian ? "Поножи" : "Leggings";
         case FEET -> russian ? "Ботинки" : "Boots";
         default -> russian ? "Броня" : "Armor";
      };
   }

   private static float drawSegment(float width, float coordinate, float width2, String text, int color) {
      if (text != null && !text.isEmpty()) {
         BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(width, coordinate, width2, text).weight(500);
         BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.color(color);
         textBuilder2.draw();
         TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
         return width + textHeightHelper.getWidth(text, width2);
      } else {
         return width;
      }
   }

   public void setSuppress(boolean suppress2) {
      this.suppress = suppress2;
   }

   private void notifyArmorWarning(Player player, EquipmentSlot equipmentSlot, ItemStack itemStack) {
      if (!this.suppress) {
         String text = armorName(equipmentSlot, itemStack);
         boolean russian = SelectAndSaveHelper.isRussian();
         NotificationsHud.Entry entry;
         NotificationsHud.Type type = NotificationsHud.Type.ARMOR_WARNING;
         String text2;
         if (russian) {
            text2 = armorAlmostBrokenSuffix(equipmentSlot, itemStack);
         } else {
            text2 = " is almost broken";
         }

         long nowNanos = System.nanoTime();
         entry = new NotificationsHud.Entry(type, "", text, text2, nowNanos);
         this.push(entry);
         player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.55F);
      }
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   public void tick(Player trackedPlayer2) {
      if (trackedPlayer2 == null) {
         this.resetArmorTracking();
      } else {
         if (this.trackedPlayer != trackedPlayer2) {
            this.resetArmorTracking();
            this.trackedPlayer = trackedPlayer2;
         }

         long nowNanos = System.nanoTime();
         this.confirmPendingBreaks(trackedPlayer2, nowNanos);

         for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack itemBySlot = trackedPlayer2.getItemBySlot(equipmentSlot);
            NotificationsHud.ArmorState armorState = this.armorStates.get(equipmentSlot);
            if (!itemBySlot.isEmpty() && itemBySlot.isDamageableItem()) {
               int maxDamage = Math.max(0, itemBySlot.getMaxDamage() - itemBySlot.getDamageValue());
               int item = armorState != null && armorState.item == itemBySlot.getItem() ? 1 : 0;
               if (item == 0 && armorState != null) {
                  this.queuePossibleBreak(equipmentSlot, armorState, nowNanos);
               }

               int value = item != 0 && armorState.warned ? 1 : 0;
               if (maxDamage > 50) {
                  value = 0;
               } else if (value == 0) {
                  this.notifyArmorWarning(trackedPlayer2, equipmentSlot, itemBySlot);
                  value = 1;
               }

               this.armorStates.put(equipmentSlot, new NotificationsHud.ArmorState(itemBySlot.getItem(), itemBySlot.getDamageValue(), maxDamage, (value != 0), armorName(equipmentSlot, itemBySlot)));
            } else if (armorState != null) {
               this.queuePossibleBreak(equipmentSlot, armorState, nowNanos);
               this.armorStates.remove(equipmentSlot);
            }
         }
      }
   }

   private static boolean containsStack(Player player, Item item, int value) {
      ItemStack carried = player.containerMenu.getCarried();
      if (matchesStack(carried, item, value)) {
         return true;
      }

      Iterator iterator = player.containerMenu.slots.iterator();

      while (iterator.hasNext()) {
         Slot element = (Slot)iterator.next();
         if (matchesStack(element.getItem(), item, value)) {
            return true;
         }
      }

      return false;
   }

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float alpha) {
      int color2 = -1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color2 = HudStyle.background(alpha);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(color2).radius(color);
         rectBuilder2.hudGlow(false).blur(HudStyle.blurRadius(radius), alpha).draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder3 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color2 = withAlpha(-15856114, alpha);
         BeginFrameHelper.RectBuilder rectBuilder4 = rectBuilder3.color(color2);
         rectBuilder4.radius(color).hudGlow(false).draw();
      }
   }

   private static NotificationsHud.Entry previewEntry(long timestamp) {
      NotificationsHud.Entry entry = new NotificationsHud.Entry(NotificationsHud.Type.PREVIEW, "Обычное уведомление", "", "", timestamp);
      entry.preview = true;
      return entry;
   }

   public Draggable draggable() {
      return this.draggable;
   }

   static final class ArmorState {
      private final int damage;
      private final Item item;
      private final int remaining;
      private final boolean warned;
      private final String name;

      private ArmorState(Item item2, int damage2, int remaining2, boolean warned2, String name2) {
         this.item = item2;
         this.damage = damage2;
         this.remaining = remaining2;
         this.warned = warned2;
         this.name = name2;
      }
   }

   static final class Entry {
      private final String suffix;
      private long exitAt;
      private final NotificationsHud.Type type;
      private final String prefix;
      private final String highlight;
      private float stackY;
      private ItemStack item = ItemStack.EMPTY;
      private boolean exiting;
      private float stackVelocity;
      private long bornAt;
      private boolean preview;

      private float progress(long timestamp) {
         if (this.exiting) {
            float computedValue = (float)(timestamp - this.exitAt) / 2.4E8F;
            return NotificationsHud.clamp(computedValue, 0.0F, 1.0F);
         } else {
            return NotificationsHud.clamp((float)(timestamp - this.bornAt) / 2.6E8F, 0.0F, 1.0F);
         }
      }

      private NotificationsHud.Entry startExit(long exitAt2) {
         if (!this.exiting) {
            this.exiting = true;
            this.exitAt = exitAt2;
         }

         return this;
      }

      private NotificationsHud.Entry activate(long bornAt2) {
         this.bornAt = bornAt2;
         this.exitAt = 0L;
         this.exiting = false;
         return this;
      }

      private void updateStack(float stackY2, float value) {
         float maximum = Math.max(value, 0.0F);
         float minimum = Math.min(maximum, 0.033333335F);
         float computedValue = (stackY2 - this.stackY) * 170.0F;
         float computedValue2 = computedValue - this.stackVelocity * 23.0F;
         float computedValue3 = this.stackVelocity + computedValue2 * minimum;
         this.stackVelocity = computedValue3;
         this.stackY = this.stackY + this.stackVelocity * minimum;
         float computedValue4 = stackY2 - this.stackY;
         if (Math.abs(computedValue4) < 0.01F) {
            if (Math.abs(this.stackVelocity) < 0.02F) {
               this.stackY = stackY2;
               this.stackVelocity = 0.0F;
            }
         }
      }

      private boolean shouldExit(long timestamp) {
         if (!this.exiting) {
            if (timestamp - this.bornAt >= 2660000000L) {
               return true;
            }
         }

         return false;
      }

      private float slideOffset(float value, float value2) {
         if (this.exiting) {
            return -value2 * smooth(value);
         }

         return value2 * (1.0F - smooth(value));
      }

      private static float smooth(float value) {
         float clampedValue = NotificationsHud.clamp(value, 0.0F, 1.0F);
         return clampedValue * clampedValue * (3.0F - 2.0F * clampedValue);
      }

      private float alpha(float value) {
         if (this.exiting) {
            return 1.0F - smooth(value);
         } else {
            return smooth(value);
         }
      }

      private Entry(NotificationsHud.Type type2, String prefix2, String highlight2, String suffix2, long bornAt2) {
         this.type = type2;
         this.prefix = prefix2 == null ? "" : prefix2;
         this.highlight = highlight2 == null ? "" : highlight2;
         this.suffix = suffix2 == null ? "" : suffix2;
         this.bornAt = bornAt2;
      }

      private boolean finished(long timestamp) {
         if (this.exiting) {
            if (timestamp - this.exitAt >= 240000000L) {
               return true;
            }
         }

         return false;
      }

      private float width(float value) {
         float width = 9.0F * value;
         float width2 = InterBoldHelper.forWeight(500).getWidth(this.prefix + this.highlight + this.suffix, width);
         float width3;
         if (!this.item.isEmpty()) {
            width3 = 16.0F * value;
         } else if (this.type == NotificationsHud.Type.MODULE_ON || this.type == NotificationsHud.Type.MODULE_OFF) {
            width3 = 12.0F * value;
         } else if (this.type == NotificationsHud.Type.CONFIG) {
            width3 = InterBoldHelper.csGuiIcons().getWidth("\ue006", 10.75F * value);
         } else {
            width3 = InterBoldHelper.clickGuiIcons().getWidth("k", 10.75F * value);
         }

         return 16.0F * value + width3 + 5.5F * value + 1.0F * value + 5.5F * value + width2 + 1.0F;
      }
   }

   static final class PendingBreak {
      private final Item item;
      private final EquipmentSlot slot;
      private final String name;
      private final int damage;
      private final long confirmAt;

      private PendingBreak(EquipmentSlot slot2, Item item2, int damage2, String name2, long confirmAt2) {
         this.slot = slot2;
         this.item = item2;
         this.damage = damage2;
         this.name = name2;
         this.confirmAt = confirmAt2;
      }
   }

   enum Type {
      PREVIEW,
      CONFIG,
      MODULE_ON,
      MODULE_OFF,
      ARMOR_WARNING,
      ARMOR_BROKEN,
      ITEM_USED;
   }
}
