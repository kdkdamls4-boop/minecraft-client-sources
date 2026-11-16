package net.skycore.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.mixin.ItemCooldownsCooldownInstanceGetEndTimeAccessor;
import net.skycore.client.mixin.ItemCooldownsGetCooldownsAccessor;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import org.joml.Matrix3x2fStack;

public final class CooldownsHud {
   private static final float ITEM_TEXT_GAP = 5.0F;
   private static final float RADIUS = 9.0F;
   private static final float BODY_PAD_Y = 7.0F;
   private static final float PAD_X = 9.0F;
   private float visibilityProgress;
   private static final float ROW_HEIGHT = 11.0F;
   private final Map<Identifier, ItemStack> representativeStacks;
   private float visualWidth;
   private static final float HEADER_TEXT_SIZE = 10.0F;
   private static final float TIME_CHIP_MIN_WIDTH = 28.0F;
   private static final float RING_SIZE = 7.0F;
   private final Map<Identifier, CooldownsHud.RowState> rows;
   private static final float TIME_RING_GAP = 3.0F;
   private static final float MIN_WIDTH = 116.0F;
   private static final float BODY_REVEAL_BASE = 8.0E-5F;
   private final Draggable draggable = new Draggable("cooldowns", 0.0065F, 0.205F);
   private static final float TIME_CHIP_RADIUS = 5.5F;
   private long lastFrameAt;
   private static final float TIME_LEFT_PAD = 4.0F;
   private static final float ITEM_SIZE = 10.0F;
   private static final float TIME_CHIP_HEIGHT = 11.0F;
   private static final float ROW_TEXT_SIZE = 10.0F;
   private static final float HEADER_BODY_GAP = 4.0F;
   private static final float RING_THICKNESS = 1.0F;
   private static final float TIME_TEXT_SIZE = 7.5F;
   private static final float HEADER_ICON_SIZE = 12.0F;
   private float visualBodyHeight;
   private static final float TIME_RIGHT_PAD = 3.0F;
   private static final float HEADER_HEIGHT = 26.0F;
   private static final float ROW_PITCH = 17.0F;
   private boolean representativeStacksReady;
   private static final float VISIBILITY_BASE = 1.0E-5F;
   private static final float MOTION_BASE = 4.0E-6F;

   private List<CooldownsHud.CooldownSnapshot> collectActiveCooldowns(Minecraft minecraft) {
      if (minecraft.player == null) {
         return List.of();
      }

      ItemCooldowns cooldowns = minecraft.player.getCooldowns();
      ItemCooldownsGetCooldownsAccessor itemCooldownsGetCooldownsAccessor = (ItemCooldownsGetCooldownsAccessor)cooldowns;
      var lookup = itemCooldownsGetCooldownsAccessor.skycore$getCooldowns();
      if (lookup.isEmpty()) {
         return List.of();
      }

      this.ensureRepresentativeStacks(itemCooldownsGetCooldownsAccessor);
      HashMap lookup2 = new HashMap();
      var keys = lookup.keySet();

      for (int value = 0; value < 36; value++) {
         this.registerPreferredStack(itemCooldownsGetCooldownsAccessor, keys, lookup2, minecraft.player.getInventory().getItem(value));
      }

      this.registerPreferredStack(itemCooldownsGetCooldownsAccessor, keys, lookup2, minecraft.player.getOffhandItem());
      int elapsed = itemCooldownsGetCooldownsAccessor.skycore$getTickCount();
      ArrayList<CooldownsHud.CooldownSnapshot> cooldownSnapshots = new ArrayList<>(lookup.size());

      for (Entry entry : lookup.entrySet()) {
         if (entry.getValue() instanceof ItemCooldownsCooldownInstanceGetEndTimeAccessor itemCooldownsCooldownInstanceGetEndTimeAccessor) {
            int computedValue = Math.max(0, itemCooldownsCooldownInstanceGetEndTimeAccessor.skycore$getEndTime() - elapsed);
            if (computedValue > 0) {
               ItemStack key = (ItemStack)lookup2.get(entry.getKey());
               if (key == null || key.isEmpty()) {
                  key = this.representativeStacks.get(entry.getKey());
               }

               if (key != null && !key.isEmpty()) {
                  cooldownSnapshots.add(new CooldownsHud.CooldownSnapshot((Identifier)entry.getKey(), key.copy(), computedValue));
               }
            }
         }
      }

      cooldownSnapshots.sort(Comparator.comparing(cooldownSnapshot -> {
         return cooldownSnapshot.stack().getHoverName().getString();
      }, String.CASE_INSENSITIVE_ORDER));
      return cooldownSnapshots;
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   private void ensureRepresentativeStacks(ItemCooldownsGetCooldownsAccessor itemCooldownsGetCooldownsAccessor) {
      if (!this.representativeStacksReady) {
         this.representativeStacksReady = true;

         for (Item item : BuiltInRegistries.ITEM) {
            ItemStack defaultInstance = item.getDefaultInstance();
            if (!defaultInstance.isEmpty()) {
               Identifier texture = itemCooldownsGetCooldownsAccessor.skycore$invokeGetCooldownGroup(defaultInstance);
               if (texture != null) {
                  this.representativeStacks.putIfAbsent(texture, defaultInstance.copy());
               }
            }
         }
      }
   }

   private float targetWidth(List<CooldownsHud.RowState> rowStates, float value) {
      float width = 18.0F * value
         + InterBoldHelper.forWeight(600).getWidth("Cooldowns", 10.0F * value)
         + 28.0F * value
         + InterBoldHelper.skycoreIcons().getWidth("g", 12.0F * value);
      float maximum = Math.max(116.0F * value, width);
      float width2 = 7.5F * value;
      float computedValue = 7.0F * value;

      for (CooldownsHud.RowState rowState : rowStates) {
         if (rowState.snapshot != null) {
            String text = rowState.currentDuration != null && !rowState.currentDuration.isEmpty() ? rowState.currentDuration : durationText(rowState.snapshot.remainingTicks());
            float width3 = InterBoldHelper.forWeight(600).getWidth(text, width2);
            float maximum2 = Math.max(28.0F * value, 4.0F * value + width3 + 3.0F * value + computedValue + 3.0F * value);
            float width4 = 9.0F * value
               + 10.0F * value
               + 5.0F * value
               + InterBoldHelper.forWeight(500).getWidth(rowState.snapshot.stack().getHoverName().getString(), 10.0F * value)
               + 10.0F * value
               + maximum2
               + 9.0F * value;
            maximum = Math.max(maximum, width4);
         }
      }

      return maximum;
   }

   private void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float alpha, float alpha2) {
      int value = 1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = HudStyle.background(alpha2);
         value = shade(value, alpha);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(value);
         BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.radius(color).hudGlow(false).blur(HudStyle.blurRadius(radius), alpha2);
         rectBuilder3.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder4 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         BeginFrameHelper.RectBuilder rectBuilder5 = rectBuilder4.color(shade(alpha(-15856114, alpha2), alpha)).radius(color);
         BeginFrameHelper.RectBuilder rectBuilder6 = rectBuilder5.hudGlow(false);
         rectBuilder6.draw();
      }
   }

   public void render(float width, float alpha, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      ThemeManager themeManager = SkycoreClient.themes();
      float width2 = this.deltaSeconds();
      var activeCooldowns = this.collectActiveCooldowns(instance);
      int value = !activeCooldowns.isEmpty() ? 1 : 0;
      this.syncRows(activeCooldowns, width2);
      var rowStates = this.orderedRows();
      int value2 = !condition && value == 0 ? 0 : 1;
      this.visibilityProgress = approach(this.visibilityProgress, value2 != 0 ? 1.0F : 0.0F, width2, 1.0E-5F);
      float alpha2 = smoothstep(this.visibilityProgress);
      if (value2 == 0 && alpha2 <= 0.001F) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         float width3 = this.draggable.x();
         float width4 = this.draggable.y();
         float width5 = this.targetWidth(rowStates, width);
         this.visualWidth = approach(this.visualWidth, width5, width2, 4.0E-6F);
         if (this.visualWidth <= 0.01F) {
            this.visualWidth = width5;
         }

         int value3 = 0;

         for (CooldownsHud.RowState rowState : rowStates) {
            if (rowState.target) {
               value3++;
            }
         }

         int value4 = value != 0 && value3 > 0 ? 1 : 0;
         float width6 = 26.0F * width;
         float height = 4.0F * width;
         float height2 = value4 != 0 ? (25.0F + Math.max(0, value3 - 1) * 17.0F) * width : 0.0F;
         this.visualBodyHeight = approach(this.visualBodyHeight, height2, width2, 8.0E-5F);
         if (Math.abs(this.visualBodyHeight - height2) < 0.01F) {
            this.visualBodyHeight = height2;
         }

         float width7 = width6 + (this.visualBodyHeight > 0.01F ? height + this.visualBodyHeight : 0.0F);
         this.draggable.setBounds(this.visualWidth, width7);
         int color = shade(alpha(themeManager.textPrimary(), 0.75F * alpha2), alpha);
         int color2 = shade(alpha(themeManager.accent(), alpha2), alpha);
         int alpha3 = shade(alpha(-1, 0.5F * alpha2), alpha);
         int alpha4 = shade(alpha(-1, 0.5F * alpha2), alpha);
         int alpha5 = shade(alpha(-1, 0.02F * alpha2), alpha);
         int alpha6 = shade(alpha(-1, 0.03F * alpha2), alpha);
         this.drawBlock(width3, width4, this.visualWidth, width6, 9.0F * width, width, alpha, alpha2);
         float computedValue = width4 + 8.5F * width - 0.9F;
         BeginFrameHelper.text(width3 + 9.0F * width, computedValue, 10.0F * width, "Cooldowns").weight(600).color(color).draw();
         String text = "g";
         float width8 = 12.0F * width;
         float width9 = InterBoldHelper.skycoreIcons().getWidth(text, width8);
         BeginFrameHelper.text(width3 + this.visualWidth - 9.0F * width - width9, width4 + 7.0F * width, width8, text)
            .font(InterBoldHelper.skycoreIcons())
            .color(color2)
            .draw();
         if (!(this.visualBodyHeight <= 0.01F)) {
            float width10 = width4 + width6 + height;
            this.drawBlock(width3, width10, this.visualWidth, this.visualBodyHeight, 9.0F * width, width, alpha, alpha2);
            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor != null && rowStates.stream().anyMatch(rowState2 -> {
               return rowState2.progress > 0.001F && rowState2.snapshot != null;
            })) {
               BeginFrameHelper.flush();
            }

            float computedValue2 = 10.0F * width;
            float computedValue3 = 11.0F * width;
            float computedValue4 = width3 + this.visualWidth - 9.0F * width;

            for (CooldownsHud.RowState rowState3 : rowStates) {
               if (!(rowState3.progress <= 0.001F) && rowState3.snapshot != null) {
                  float value5 = smoothstep(rowState3.progress);
                  float alpha7 = value5 * alpha2;
                  float computedValue5 = width10 + (7.0F + rowState3.visualIndex * 17.0F) * width - 1.5F;
                  float computedValue6 = (1.0F - value5) * (rowState3.target ? 2.5F : -2.0F) * width;
                  float computedValue7 = computedValue5 + 1.0F * width + computedValue6 + 1.0F;
                  float computedValue8 = width3 + 9.0F * width - 1.0F;
                  float computedValue9 = computedValue5 + 1.7F * width + computedValue6 + 0.3F;
                  if (guiGraphicsExtractor != null && alpha7 > 0.001F) {
                     CaptureHelper.withAlpha(alpha7, () -> {
                        drawItem(guiGraphicsExtractor, rowState3.snapshot.stack(), computedValue8, computedValue9, 10.0F * width);
                     });
                  }

                  String string = rowState3.snapshot.stack().getHoverName().getString();
                  float computedValue10 = computedValue8 + 15.0F * width;
                  BeginFrameHelper.text(computedValue10, computedValue5 + 1.5F * width + computedValue6, computedValue2, string).weight(500).color(alpha(alpha3, alpha7)).draw();
                  float width11 = 7.5F * width;
                  String currentDuration = rowState3.currentDuration;
                  float width12 = InterBoldHelper.forWeight(600).getWidth(currentDuration, width11);
                  float computedValue11 = 7.0F * width;
                  float maximum = Math.max(28.0F * width, 4.0F * width + width12 + 3.0F * width + computedValue11 + 3.0F * width);
                  float computedValue12 = computedValue4 - maximum;
                  BeginFrameHelper.rect(computedValue12, computedValue7, maximum, computedValue3).color(alpha(alpha5, alpha7)).radius(5.5F * width).hudGlow(false).draw();
                  BeginFrameHelper.text(computedValue12 + 4.0F * width, computedValue7 + 2.15F * width - 0.7F, width11, currentDuration).weight(600).color(alpha(alpha4, alpha7)).draw();
                  float computedValue13 = computedValue4 - 3.0F * width - computedValue11;
                  float computedValue14 = computedValue7 + (computedValue3 - computedValue11) * 0.5F;
                  BeginFrameHelper.ring(computedValue13, computedValue14, computedValue11).thickness(Math.max(1.0F, 1.0F * width)).color(alpha(alpha6, alpha7)).progress(1.0F).draw();
                  float color3 = rowState3.cooldownProgress();
                  if (color3 > 0.0F) {
                     BeginFrameHelper.ring(computedValue13, computedValue14, computedValue11).thickness(Math.max(1.0F, 1.0F * width)).color(alpha(color2, alpha7)).progress(color3).draw();
                  }
               }
            }
         }
      }
   }

   private void syncRows(List<CooldownsHud.CooldownSnapshot> cooldownSnapshots, float progress) {
      for (CooldownsHud.RowState rowState : this.rows.values()) {
         rowState.target = false;
      }

      int value = 0;

      for (CooldownsHud.CooldownSnapshot cooldownSnapshot : cooldownSnapshots) {
         CooldownsHud.RowState rowState2 = this.rows.get(cooldownSnapshot.key());
         if (rowState2 == null) {
            rowState2 = new CooldownsHud.RowState(value, cooldownSnapshot);
            this.rows.put(cooldownSnapshot.key(), rowState2);
         }

         rowState2.target = true;
         rowState2.targetIndex = value++;
         rowState2.snapshot = cooldownSnapshot;
         rowState2.currentDuration = durationText(cooldownSnapshot.remainingTicks());
         rowState2.maxRemainingTicks = Math.max(rowState2.maxRemainingTicks, cooldownSnapshot.remainingTicks());
      }

      Iterator iterator = this.rows.values().iterator();

      while (iterator.hasNext()) {
         CooldownsHud.RowState element = (CooldownsHud.RowState)iterator.next();
         element.progress = approach(element.progress, element.target ? 1.0F : 0.0F, progress, 4.0E-6F);
         element.visualIndex = approach(element.visualIndex, element.targetIndex, progress, 3.0E-5F);
         if (!element.target && element.progress < 0.005F) {
            iterator.remove();
         }
      }
   }

   private static float approach(float value, float value2, float value3, float value4) {
      if (value == 0.0F && value2 > 0.0F) {
         return Math.min(value2, 0.08F);
      }

      float computedValue = 1.0F - (float)Math.pow(value4, value3);
      float computedValue2 = value + (value2 - value) * computedValue;
      float computedValue3 = value2 - computedValue2;
      return Math.abs(computedValue3) < 0.001F ? value2 : computedValue2;
   }

   public Draggable draggable() {
      return this.draggable;
   }

   private void registerPreferredStack(ItemCooldownsGetCooldownsAccessor itemCooldownsGetCooldownsAccessor, Set<Identifier> identifiers, Map<Identifier, ItemStack> lookup, ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         Identifier texture = itemCooldownsGetCooldownsAccessor.skycore$invokeGetCooldownGroup(itemStack);
         if (texture != null) {
            if (identifiers.contains(texture)) {
               ItemStack itemStack2 = itemStack.copy();
               lookup.putIfAbsent(texture, itemStack2);
            }
         }
      }
   }

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round((value >>> 16 & 0xFF) * value2);
      int computedValue2 = Math.round((value >>> 8 & 0xFF) * value2);
      int computedValue3 = Math.round((value & 0xFF) * value2);
      return value3 << 24 | computedValue << 16 | computedValue2 << 8 | computedValue3;
   }

   private static void drawItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      if (!itemStack.isEmpty()) {
         BeginFrameHelper.hudItemGlow(coordinate, coordinate2, coordinate3);
      }

      Minecraft instance = Minecraft.getInstance();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      float guiScale = instance.getWindow().getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = Math.round(coordinate * guiScale) / guiScale;
      float computedValue3 = Math.round(coordinate2 * guiScale) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue2, computedValue3);
      matrix3x2fStack.scale(computedValue);
      guiGraphicsExtractor.item(itemStack, 0, 0);
      matrix3x2fStack.popMatrix();
   }

   private static String durationText(int value) {
      int computedValue = Math.max(0, (int)Math.ceil(value / 20.0));
      int computedValue2 = computedValue / 3600;
      int computedValue3 = computedValue % 3600 / 60;
      int value2 = computedValue % 60;
      if (computedValue2 > 0) {
         Object[] objectValues = new Object[]{computedValue2, null, null};
         objectValues[1] = computedValue3;
         objectValues[2] = value2;
         return String.format("%d:%02d:%02d", objectValues);
      } else {
         Object[] objectValues2 = new Object[2];
         objectValues2[0] = computedValue3;
         objectValues2[1] = value2;
         return String.format("%d:%02d", objectValues2);
      }
   }

   public CooldownsHud() {
      this.rows = new HashMap<>();
      this.representativeStacks = new HashMap<>();
      this.lastFrameAt = System.nanoTime();
   }

   private float deltaSeconds() {
      long nowNanos = System.nanoTime();
      float result = clamp((float)(nowNanos - this.lastFrameAt) / 1.0E9F, 0.0F, 0.05F);
      this.lastFrameAt = nowNanos;
      return result;
   }

   private List<CooldownsHud.RowState> orderedRows() {
      ArrayList elements = new ArrayList<>(this.rows.values());
      ToDoubleFunction<net.skycore.client.hud.CooldownsHud.RowState> toDoubleFunction = rowState -> {
         return rowState.visualIndex;
      };
      elements.sort(Comparator.comparingDouble(toDoubleFunction));
      return elements;
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float value4 = value3;
      float computedValue = value4 * clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(computedValue);
      return value & 16777215 | roundedValue << 24;
   }

   public record CooldownSnapshot(Identifier key, ItemStack stack, int remainingTicks) {
      public Identifier key() {
         return this.key;
      }

      public int remainingTicks() {
         return this.remainingTicks;
      }

      public ItemStack stack() {
         return this.stack;
      }
   }

   static final class RowState {
      private int maxRemainingTicks;
      private CooldownsHud.CooldownSnapshot snapshot;
      private int targetIndex;
      private float visualIndex;
      private String currentDuration;
      private boolean target = true;
      private float progress;

      private RowState(int visualIndex2, CooldownsHud.CooldownSnapshot snapshot2) {
         this.currentDuration = "";
         this.visualIndex = visualIndex2;
         this.targetIndex = visualIndex2;
         this.snapshot = snapshot2;
         this.maxRemainingTicks = Math.max(1, snapshot2.remainingTicks());
      }

      private float cooldownProgress() {
         if (this.snapshot == null) {
            return 0.0F;
         }

         float maximum = (float)this.snapshot.remainingTicks() / Math.max(1, this.maxRemainingTicks);
         return CooldownsHud.clamp(maximum, 0.0F, 1.0F);
      }
   }
}
