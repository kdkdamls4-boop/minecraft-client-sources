package net.skycore.client.hud;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.KeyBinds;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.InterBoldHelper;

public final class HotkeysHud {
   private static final float ROW_PITCH = 17.0F;
   private static final float HEADER_TEXT_SIZE = 10.0F;
   private static final float KEY_RIGHT_PAD = 4.0F;
   private static final float ROW_HEIGHT = 11.0F;
   private static final float RADIUS = 9.0F;
   private static final float VISIBILITY_BASE = 1.0E-5F;
   private static final float KEY_CHIP_MIN_WIDTH = 22.0F;
   private static final float KEY_CHIP_RADIUS = 5.5F;
   private static final float HEADER_BODY_GAP = 4.0F;
   private static final float KEY_ICON_SIZE = 7.0F;
   private static final float KEY_ICON_GAP = 3.0F;
   private float visualBodyHeight;
   private static final float MIN_WIDTH = 116.0F;
   private static final float KEY_TEXT_SIZE = 7.5F;
   private static final float BODY_PAD_Y = 7.0F;
   private static final float HEADER_ICON_SIZE = 12.0F;
   private final Map<ClientModule, HotkeysHud.RowState> rows;
   private static final float KEY_CHIP_HEIGHT = 11.0F;
   private final Draggable draggable = new Draggable("hotkeys", 0.0065F, 0.075F);
   private float visibilityProgress;
   private static final float MOTION_BASE = 4.0E-6F;
   private static final float PAD_X = 9.0F;
   private static final float KEY_LEFT_PAD = 4.0F;
   private static final float HEADER_HEIGHT = 26.0F;
   private static final float BODY_REVEAL_BASE = 8.0E-5F;
   private static final float ROW_TEXT_SIZE = 10.0F;
   private long lastFrameAt;
   private float visualWidth;

   private List<ClientModule> orderedRows(List<ClientModule> clientModules) {
      ArrayList elements = new ArrayList();
      Iterator iterator = clientModules.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return elements;
         }

         ClientModule element = (ClientModule)iterator.next();
         if (this.rows.containsKey(element)) {
            elements.add(element);
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

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float computedValue = value3 * clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(computedValue);
      return value & 16777215 | roundedValue << 24;
   }

   public void render(float width, float alpha, List<ClientModule> clientModules, boolean condition) {
      ThemeManager themeManager = SkycoreClient.themes();
      float width2 = this.deltaSeconds();
      boolean keyBind = clientModules.stream().anyMatch(clientModule -> clientModule.isEnabled() && clientModule.getKeyBind() != -1);
      this.syncRows(clientModules, width2);
      var clientModules2 = this.orderedRows(clientModules);
      int value = !condition && !keyBind ? 0 : 1;
      this.visibilityProgress = approach(this.visibilityProgress, value != 0 ? 1.0F : 0.0F, width2, 1.0E-5F);
      float alpha2 = smoothstep(this.visibilityProgress);
      if (value == 0 && alpha2 <= 0.001F) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         float width3 = this.draggable.x();
         float width4 = this.draggable.y();
         float width5 = this.targetWidth(clientModules2, width);
         this.visualWidth = approach(this.visualWidth, width5, width2, 4.0E-6F);
         if (this.visualWidth <= 0.01F) {
            this.visualWidth = width5;
         }

         int value2 = 0;

         for (ClientModule clientModule2 : clientModules2) {
            HotkeysHud.RowState rowState = this.rows.get(clientModule2);
            if (rowState != null && rowState.target) {
               value2++;
            }
         }

         int value3 = keyBind && value2 > 0 ? 1 : 0;
         float width6 = 26.0F * width;
         float height = 4.0F * width;
         float height2 = value3 != 0 ? (25.0F + Math.max(0, value2 - 1) * 17.0F) * width : 0.0F;
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
         int alpha5 = shade(alpha(-1, 0.25F * alpha2), alpha);
         int alpha6 = shade(alpha(-1, 0.02F * alpha2), alpha);
         this.drawBlock(width3, width4, this.visualWidth, width6, 9.0F * width, width, alpha, alpha2);
         float computedValue = width4 + 8.5F * width - 0.9F;
         BeginFrameHelper.text(width3 + 9.0F * width, computedValue, 10.0F * width, "Keybinds").weight(600).color(color).draw();
         String text = "\ue004";
         float width8 = 12.0F * width;
         float width9 = InterBoldHelper.csGuiIcons().getWidth(text, width8);
         BeginFrameHelper.text(width3 + this.visualWidth - 9.0F * width - width9, width4 + 7.0F * width, width8, text)
            .font(InterBoldHelper.csGuiIcons())
            .color(color2)
            .draw();
         if (!(this.visualBodyHeight <= 0.01F)) {
            float width10 = width4 + width6 + height;
            this.drawBlock(width3, width10, this.visualWidth, this.visualBodyHeight, 9.0F * width, width, alpha, alpha2);
            float computedValue2 = 10.0F * width;
            float computedValue3 = 11.0F * width;
            float computedValue4 = width3 + this.visualWidth - 9.0F * width;
            int value4 = 0;

            for (ClientModule clientModule3 : clientModules2) {
               HotkeysHud.RowState rowState2 = this.rows.get(clientModule3);
               if (rowState2 != null && !(rowState2.progress <= 0.001F)) {
                  float value5 = smoothstep(rowState2.progress);
                  float alpha7 = value5 * alpha2;
                  float computedValue5 = width10 + (7.0F + rowState2.visualIndex * 17.0F) * width - 1.5F;
                  float computedValue6 = (1.0F - value5) * (rowState2.target ? 2.5F : -2.0F) * width;
                  float computedValue7 = computedValue5 + 1.0F * width + computedValue6 + 1.0F;
                  BeginFrameHelper.text(width3 + 9.0F * width, computedValue5 + 1.5F * width + computedValue6, computedValue2, clientModule3.getName()).weight(500).color(alpha(alpha3, alpha7)).draw();
                  String keyBind2 = KeyBinds.name(clientModule3.getKeyBind());
                  float width11 = 7.5F * width;
                  float width12 = InterBoldHelper.forWeight(600).getWidth(keyBind2, width11);
                  float width13 = 7.0F * width;
                  float width14 = InterBoldHelper.csGuiIcons().getWidth(text, width13);
                  float maximum = Math.max(22.0F * width, 4.0F * width + width12 + 3.0F * width + width14 + 4.0F * width);
                  float computedValue8 = computedValue4 - maximum;
                  BeginFrameHelper.rect(computedValue8, computedValue7, maximum, computedValue3).color(alpha(alpha6, alpha7)).radius(5.5F * width).hudGlow(false).draw();
                  BeginFrameHelper.text(computedValue8 + 4.0F * width, computedValue7 + 2.15F * width - 0.7F, width11, keyBind2).weight(600).color(alpha(alpha4, alpha7)).draw();
                  BeginFrameHelper.text(computedValue4 - 4.0F * width - width14, computedValue7 + 1.95F * width, width13, text)
                     .font(InterBoldHelper.csGuiIcons())
                     .color(alpha(alpha5, alpha7))
                     .draw();
                  value4++;
               }
            }
         }
      }
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   public Draggable draggable() {
      return this.draggable;
   }

   public HotkeysHud() {
      this.rows = new IdentityHashMap<>();
      this.lastFrameAt = System.nanoTime();
   }

   private static float approach(float value, float value2, float value3, float value4) {
      if (value == 0.0F && value2 > 0.0F) {
         return Math.min(value2, 0.08F);
      }

      float computedValue = 1.0F - (float)Math.pow(value4, value3);
      float computedValue2 = value + (value2 - value) * computedValue;
      return Math.abs(value2 - computedValue2) < 0.001F ? value2 : computedValue2;
   }

   private float targetWidth(List<ClientModule> clientModules, float value) {
      float width = 18.0F * value
         + InterBoldHelper.forWeight(600).getWidth("Keybinds", 10.0F * value)
         + 28.0F * value
         + InterBoldHelper.csGuiIcons().getWidth("\ue004", 12.0F * value);
      float maximum = Math.max(116.0F * value, width);

      for (ClientModule clientModule : clientModules) {
         HotkeysHud.RowState rowState = this.rows.get(clientModule);
         if (rowState != null && rowState.target) {
            String keyBind = KeyBinds.name(clientModule.getKeyBind());
            float width2 = 7.5F * value;
            float width3 = InterBoldHelper.forWeight(600).getWidth(keyBind, width2);
            float width4 = InterBoldHelper.csGuiIcons().getWidth("\ue004", 7.0F * value);
            float maximum2 = Math.max(22.0F * value, 4.0F * value + width3 + 3.0F * value + width4 + 4.0F * value);
            float width5 = 9.0F * value + InterBoldHelper.forWeight(500).getWidth(clientModule.getName(), 10.0F * value) + 16.0F * value + maximum2 + 9.0F * value;
            maximum = Math.max(maximum, width5);
         }
      }

      return maximum;
   }

   private float deltaSeconds() {
      long nowNanos = System.nanoTime();
      float result = clamp((float)(nowNanos - this.lastFrameAt) / 1.0E9F, 0.0F, 0.05F);
      this.lastFrameAt = nowNanos;
      return result;
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float coordinate5, float alpha) {
      int color2 = -1;
      color2 = (byte)-1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color2 = HudStyle.background(alpha);
         color2 = shade(color2, coordinate5);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(color2).radius(color).hudGlow(false);
         float value = HudStyle.blurRadius(radius);
         BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.blur(value, alpha);
         rectBuilder3.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder4 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         color2 = alpha(-15856114, alpha);
         BeginFrameHelper.RectBuilder rectBuilder5 = rectBuilder4.color(shade(color2, coordinate5));
         BeginFrameHelper.RectBuilder rectBuilder6 = rectBuilder5.radius(color);
         BeginFrameHelper.RectBuilder rectBuilder7 = rectBuilder6.hudGlow(false);
         rectBuilder7.draw();
      }
   }

   private void syncRows(List<ClientModule> clientModules, float progress) {
      for (HotkeysHud.RowState rowState : this.rows.values()) {
         rowState.target = false;
      }

      int value = 0;

      for (ClientModule clientModule : clientModules) {
         int keyBind = clientModule.isEnabled() && clientModule.getKeyBind() != -1 ? 1 : 0;
         HotkeysHud.RowState rowState2 = this.rows.get(clientModule);
         if (keyBind != 0) {
            if (rowState2 == null) {
               rowState2 = new HotkeysHud.RowState(value);
               this.rows.put(clientModule, rowState2);
            }

            rowState2.target = true;
            rowState2.targetIndex = value++;
         }
      }

      Iterator iterator = this.rows.entrySet().iterator();

      while (iterator.hasNext()) {
         HotkeysHud.RowState value2 = (HotkeysHud.RowState)((Entry)iterator.next()).getValue();
         value2.progress = approach(value2.progress, value2.target ? 1.0F : 0.0F, progress, 4.0E-6F);
         value2.visualIndex = approach(value2.visualIndex, value2.targetIndex, progress, 3.0E-5F);
         if (!value2.target && value2.progress < 0.005F) {
            iterator.remove();
         }
      }
   }

   static final class RowState {
      private float progress;
      private int targetIndex;
      private float visualIndex;
      private boolean target = true;

      private RowState(int visualIndex2) {
         this.visualIndex = visualIndex2;
         this.targetIndex = visualIndex2;
      }
   }
}
