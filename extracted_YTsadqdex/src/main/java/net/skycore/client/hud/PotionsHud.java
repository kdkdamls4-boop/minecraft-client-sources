package net.skycore.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.skycore.client.SkycoreClient;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.InterBoldHelper;

public final class PotionsHud {
   private float visualBodyHeight;
   private static final float HEADER_TEXT_SIZE = 10.0F;
   private static final float EFFECT_TEXT_GAP = 5.0F;
   private static final float TIME_TEXT_SIZE = 7.5F;
   private static final float RADIUS = 9.0F;
   private float visibilityProgress;
   private long lastFrameAt;
   private static final float TIME_RING_GAP = 3.0F;
   private static final float ROW_PITCH = 17.0F;
   private static final float ROW_TEXT_SIZE = 10.0F;
   private static final float HEADER_HEIGHT = 26.0F;
   private static final float MIN_WIDTH = 116.0F;
   private static final float MOTION_BASE = 4.0E-6F;
   private static final float TIME_LEFT_PAD = 4.0F;
   private static final float TIME_CHIP_MIN_WIDTH = 28.0F;
   private static final float RING_SIZE = 7.0F;
   private static final float ROW_HEIGHT = 11.0F;
   private static final float HEADER_BODY_GAP = 4.0F;
   private static final float BODY_REVEAL_BASE = 8.0E-5F;
   private static final float TIME_CHIP_HEIGHT = 11.0F;
   private static final float EFFECT_ICON_SIZE = 7.0F;
   private static final float VISIBILITY_BASE = 1.0E-5F;
   private float visualWidth;
   private static final float BODY_PAD_Y = 7.0F;
   private static final float TIME_RIGHT_PAD = 3.0F;
   private static final float HEADER_ICON_SIZE = 12.0F;
   private static final float PAD_X = 9.0F;
   private final Draggable draggable = new Draggable("potions", 0.0065F, 0.14F);
   private static final float RING_THICKNESS = 1.0F;
   private static final float TIME_CHIP_RADIUS = 5.5F;
   private final Map<String, PotionsHud.RowState> rows = new HashMap<>();

   public void render(float width, float alpha, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      ThemeManager themeManager = SkycoreClient.themes();
      float width2 = this.deltaSeconds();
      var activeEffects = this.collectActiveEffects(instance);
      int value = !activeEffects.isEmpty() ? 1 : 0;
      this.syncRows(activeEffects, width2);
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

         for (PotionsHud.RowState rowState : rowStates) {
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
         BeginFrameHelper.text(width3 + 9.0F * width, computedValue, 10.0F * width, "Potions").weight(600).color(color).draw();
         String text = "e";
         float width8 = 12.0F * width;
         float width9 = InterBoldHelper.skycoreIcons().getWidth(text, width8);
         BeginFrameHelper.text(width3 + this.visualWidth - 9.0F * width - width9, width4 + 7.0F * width, width8, text)
            .font(InterBoldHelper.skycoreIcons())
            .color(color2)
            .draw();
         if (!(this.visualBodyHeight <= 0.01F)) {
            float width10 = width4 + width6 + height;
            this.drawBlock(width3, width10, this.visualWidth, this.visualBodyHeight, 9.0F * width, width, alpha, alpha2);
            float computedValue2 = 10.0F * width;
            float computedValue3 = 11.0F * width;
            float computedValue4 = width3 + this.visualWidth - 9.0F * width;

            for (PotionsHud.RowState rowState2 : rowStates) {
               if (!(rowState2.progress <= 0.001F) && rowState2.snapshot != null) {
                  float value5 = smoothstep(rowState2.progress);
                  float alpha7 = value5 * alpha2;
                  float computedValue5 = width10 + (7.0F + rowState2.visualIndex * 17.0F) * width - 1.5F;
                  float computedValue6 = (1.0F - value5) * (rowState2.target ? 2.5F : -2.0F) * width;
                  float computedValue7 = computedValue5 + 1.0F * width + computedValue6 + 1.0F;
                  Identifier texture = this.effectSprite(rowState2.snapshot);
                  float computedValue8 = width3 + 9.0F * width;
                  float computedValue9 = computedValue5 + 1.7F * width + computedValue6;
                  if (texture != null) {
                     BeginFrameHelper.texture(computedValue8, computedValue9 + 2.0F, 7.0F * width, 7.0F * width, texture)
                        .iconGlow(0.32F, 0.55F)
                        .color(alpha(shade(-1, alpha), alpha7))
                        .draw();
                  }

                  String text2 = this.effectDisplayName(rowState2.snapshot);
                  float computedValue10 = computedValue8 + 12.0F * width;
                  BeginFrameHelper.text(computedValue10, computedValue5 + 1.5F * width + computedValue6, computedValue2, text2).weight(500).color(alpha(alpha3, alpha7)).draw();
                  float width11 = 7.5F * width;
                  String currentDuration = rowState2.currentDuration;
                  float width12 = InterBoldHelper.forWeight(600).getWidth(currentDuration, width11);
                  float computedValue11 = 7.0F * width;
                  float maximum = Math.max(28.0F * width, 4.0F * width + width12 + 3.0F * width + computedValue11 + 3.0F * width);
                  float computedValue12 = computedValue4 - maximum;
                  BeginFrameHelper.rect(computedValue12, computedValue7, maximum, computedValue3).color(alpha(alpha5, alpha7)).radius(5.5F * width).hudGlow(false).draw();
                  BeginFrameHelper.text(computedValue12 + 4.0F * width, computedValue7 + 2.15F * width - 0.7F, width11, currentDuration).weight(600).color(alpha(alpha4, alpha7)).draw();
                  float computedValue13 = computedValue4 - 3.0F * width - computedValue11;
                  float computedValue14 = computedValue7 + (computedValue3 - computedValue11) * 0.5F;
                  BeginFrameHelper.ring(computedValue13, computedValue14, computedValue11).thickness(Math.max(1.0F, 1.0F * width)).color(alpha(alpha6, alpha7)).progress(1.0F).draw();
                  float color3 = rowState2.durationProgress();
                  if (color3 > 0.0F) {
                     BeginFrameHelper.ring(computedValue13, computedValue14, computedValue11).thickness(Math.max(1.0F, 1.0F * width)).color(alpha(color2, alpha7)).progress(color3).draw();
                  }
               }
            }
         }
      }
   }

   public Draggable draggable() {
      return this.draggable;
   }

   private float targetWidth(List<PotionsHud.RowState> rowStates, float value) {
      float width = 18.0F * value
         + InterBoldHelper.forWeight(600).getWidth("Potions", 10.0F * value)
         + 28.0F * value
         + InterBoldHelper.skycoreIcons().getWidth("e", 12.0F * value);
      float maximum = Math.max(116.0F * value, width);
      float width2 = 7.5F * value;
      float computedValue = 7.0F * value;

      for (PotionsHud.RowState rowState : rowStates) {
         if (rowState.snapshot != null) {
            String text = rowState.currentDuration != null && !rowState.currentDuration.isEmpty() ? rowState.currentDuration : this.durationText(rowState.snapshot);
            float width3 = InterBoldHelper.forWeight(600).getWidth(text, width2);
            float maximum2 = Math.max(28.0F * value, 4.0F * value + width3 + 3.0F * value + computedValue + 3.0F * value);
            float width4 = 9.0F * value
               + 7.0F * value
               + 5.0F * value
               + InterBoldHelper.forWeight(500).getWidth(this.effectDisplayName(rowState.snapshot), 10.0F * value)
               + 10.0F * value
               + maximum2
               + 9.0F * value;
            maximum = Math.max(maximum, width4);
         }
      }

      return maximum;
   }

   public PotionsHud() {
      this.lastFrameAt = System.nanoTime();
   }

   private String effectDisplayName(MobEffectInstance mobEffectInstance) {
      String text = this.effectName(mobEffectInstance);
      int amplifier = mobEffectInstance.getAmplifier() + 1;
      return amplifier > 1 ? text + " " + amplifier : text;
   }

   private String effectName(MobEffectInstance mobEffectInstance) {
      MutableComponent descriptionId = Component.translatable(mobEffectInstance.getDescriptionId());
      return descriptionId.getString();
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round(value3 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   private float deltaSeconds() {
      long nowNanos = System.nanoTime();
      float result = clamp((float)(nowNanos - this.lastFrameAt) / 1.0E9F, 0.0F, 0.05F);
      this.lastFrameAt = nowNanos;
      return result;
   }

   private void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float alpha, float color2) {
      int color3 = -1;
      color3 = (byte)1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color3 = shade(HudStyle.background(color2), alpha);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(color3).radius(color).hudGlow(false).blur(HudStyle.blurRadius(radius), color2);
         rectBuilder2.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder3 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color3 = shade(alpha(-15856114, color2), alpha);
         BeginFrameHelper.RectBuilder rectBuilder4 = rectBuilder3.color(color3).radius(color).hudGlow(false);
         rectBuilder4.draw();
      }
   }

   private static float approach(float value, float value2, float value3, float value4) {
      if (value == 0.0F && value2 > 0.0F) {
         return Math.min(value2, 0.08F);
      } else {
         float computedValue = 1.0F - (float)Math.pow(value4, value3);
         float computedValue2 = value + (value2 - value) * computedValue;
         float computedValue3 = value2 - computedValue2;
         return Math.abs(computedValue3) < 0.001F ? value2 : computedValue2;
      }
   }

   private List<PotionsHud.RowState> orderedRows() {
      ArrayList<PotionsHud.RowState> rowStates = new ArrayList<>(this.rows.values());
      rowStates.sort(Comparator.comparingDouble(rowState -> rowState.visualIndex));
      return rowStates;
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      float computedValue = (value >>> 16 & 0xFF) * value2;
      int roundedValue = Math.round(computedValue);
      int computedValue2 = Math.round((value >>> 8 & 0xFF) * value2);
      float computedValue3 = (value & 0xFF) * value2;
      int roundedValue2 = Math.round(computedValue3);
      return value3 << 24 | roundedValue << 16 | computedValue2 << 8 | roundedValue2;
   }

   private String effectKey(MobEffectInstance mobEffectInstance) {
      Holder<net.minecraft.world.effect.MobEffect> effect = mobEffectInstance.getEffect();
      var optional = effect.unwrapKey();
      Function<net.minecraft.resources.ResourceKey, java.lang.String> function = resourceKey -> {
         return resourceKey.identifier().toString();
      };
      var optional2 = optional.map(function);
      String descriptionId = mobEffectInstance.getDescriptionId();
      return optional2.orElse(descriptionId);
   }

   private Identifier effectSprite(MobEffectInstance mobEffectInstance) {
      var effect = mobEffectInstance.getEffect().unwrapKey();
      Function<net.minecraft.resources.ResourceKey, net.minecraft.resources.Identifier> function = resourceKey -> {
         return resourceKey.identifier();
      };
      var optional = effect.map(function);
      Function<net.minecraft.resources.Identifier, net.minecraft.resources.Identifier> namespace = texture -> {
         String namespace2 = texture.getNamespace();
         String path = texture.getPath();
         String text = "textures/mob_effect/" + path + ".png";
         return Identifier.fromNamespaceAndPath(namespace2, text);
      };
      var optional2 = optional.map(namespace);
      return (Identifier)optional2.orElse(null);
   }

   private void syncRows(List<MobEffectInstance> mobEffectInstances, float progress) {
      for (PotionsHud.RowState rowState : this.rows.values()) {
         rowState.target = false;
      }

      int value = 0;

      for (MobEffectInstance mobEffectInstance : mobEffectInstances) {
         String text = this.effectKey(mobEffectInstance);
         PotionsHud.RowState rowState2 = this.rows.get(text);
         if (rowState2 == null) {
            rowState2 = new PotionsHud.RowState(value, mobEffectInstance);
            this.rows.put(text, rowState2);
         }

         rowState2.target = true;
         rowState2.targetIndex = value++;
         rowState2.snapshot = mobEffectInstance;
         rowState2.currentDuration = this.durationText(mobEffectInstance);
         if (!mobEffectInstance.isInfiniteDuration()) {
            rowState2.maxDurationTicks = Math.max(rowState2.maxDurationTicks, Math.max(1, mobEffectInstance.getDuration()));
         }
      }

      Iterator iterator = this.rows.values().iterator();

      while (iterator.hasNext()) {
         PotionsHud.RowState element = (PotionsHud.RowState)iterator.next();
         element.progress = approach(element.progress, element.target ? 1.0F : 0.0F, progress, 4.0E-6F);
         element.visualIndex = approach(element.visualIndex, element.targetIndex, progress, 3.0E-5F);
         if (!element.target && element.progress < 0.005F) {
            iterator.remove();
         }
      }
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   private String durationText(MobEffectInstance mobEffectInstance) {
      if (mobEffectInstance.isInfiniteDuration()) {
         return "**:**";
      } else {
         double duration = mobEffectInstance.getDuration() / 20.0;
         int computedValue = Math.max(0, (int)Math.ceil(duration));
         int computedValue2 = computedValue / 3600;
         int computedValue3 = computedValue % 3600 / 60;
         int value = computedValue % 60;
         if (computedValue2 > 0) {
            Object[] objectValues = new Object[]{computedValue2, computedValue3, value};
            return String.format("%d:%02d:%02d", objectValues);
         } else {
            Object[] objectValues2 = new Object[]{computedValue3, null};
            objectValues2[1] = value;
            return String.format("%d:%02d", objectValues2);
         }
      }
   }

   private List<MobEffectInstance> collectActiveEffects(Minecraft minecraft) {
      if (minecraft.player == null) {
         return List.of();
      }

      ArrayList elements = new ArrayList<>(minecraft.player.getActiveEffects());
      Function<net.minecraft.world.effect.MobEffectInstance, java.lang.String> function = this::effectName;
      Comparator<net.minecraft.world.effect.MobEffectInstance> comparator = Comparator.comparing(function);
      elements.sort(comparator);
      return elements;
   }

   static final class RowState {
      private boolean target = true;
      private String currentDuration = "";
      private float progress;
      private int maxDurationTicks;
      private MobEffectInstance snapshot;
      private int targetIndex;
      private float visualIndex;

      private RowState(int visualIndex2, MobEffectInstance snapshot2) {
         this.visualIndex = visualIndex2;
         this.targetIndex = visualIndex2;
         this.snapshot = snapshot2;
         this.maxDurationTicks = snapshot2.isInfiniteDuration() ? 1 : Math.max(1, snapshot2.getDuration());
      }

      private float durationProgress() {
         if (this.snapshot == null) {
            return 0.0F;
         }

         if (this.snapshot.isInfiniteDuration()) {
            return 1.0F;
         }

         float duration = this.snapshot.getDuration();
         float maximum = duration / Math.max(1, this.maxDurationTicks);
         return PotionsHud.clamp(maximum, 0.0F, 1.0F);
      }
   }
}
