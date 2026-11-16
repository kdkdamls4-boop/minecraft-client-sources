package deluxe.client.ui.setting;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.setting.ColorSetting;
import deluxe.client.render.FontText;
import deluxe.client.render.GuiEffectsState;
import deluxe.client.util.GuiScale;
import deluxe.client.render.RectDrawData;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Colors;
import deluxe.client.render.FontFace;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.setting.NumberSetting;
import deluxe.client.render.Render2D;
import deluxe.client.setting.TextSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;

public final class SettingRenderContext {
   public static final float field12404 = 30.0F;
   public DrawContext drawContext;
   public float scale = 1.0F;
   public float mouseX;
   public float mouseY;
   private final Map<String, AnimatedValue> field12409 = new HashMap<>();
   private final Map<Object, AnimatedValue> field12410 = new IdentityHashMap<>();
   private final Map<SettingRenderContext.Nested0494, List<String>> field12411 = new LinkedHashMap<SettingRenderContext.Nested0494, List<String>>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<SettingRenderContext.Nested0494, List<String>> local5836_1) {
         return this.size() > 256;
      }
   };
   private final Map<Object, Boolean> field12412 = new HashMap<>();
   private final Map<Object, Object> field12413 = new IdentityHashMap<>();
   private final Map<ColorSetting, float[]> field12414 = new IdentityHashMap<>();
   public final Map<Object, float[]> field12415 = new IdentityHashMap<>();
   public final Map<Object, float[]> field12416 = new IdentityHashMap<>();
   public final List<SettingRenderContext.Nested0493> field12417 = new ArrayList<>();
   public int field12418;
   public NumberSetting numberSetting;
   public ColorSetting colorSetting;
   public int field12421;
   public TextSetting textSetting;
   public KeybindSetting keybindSetting;
   public Module module;
   public SettingRenderContext.Nested0492 field12425;
   public String field12426 = "";
   public Object field12427;
   public String field12428;
   public String field12429 = "";
   public final List<Runnable> field12430 = new ArrayList<>();
   private Map<String, long[]> field12431 = new HashMap<>();
   public Map<Object, Float> field12432 = new IdentityHashMap<>();
   public Object field12433;
   public float[] field12434;
   public float field12435;

   private List<String> method3536(String local5795_1, float local5795_2, float local5795_3) {
      SettingRenderContext.Nested0494 local5795_4 = new SettingRenderContext.Nested0494(local5795_1, local5795_2, local5795_3);
      List<String> local5795_5 = this.field12411.get(local5795_4);
      if (local5795_5 == null) {
         local5795_5 = List.copyOf(UiTheme.method2873(FontFace.SF, local5795_1, local5795_2, local5795_3));
         this.field12411.put(local5795_4, local5795_5);
      }

      return local5795_5;
   }

   public void method3539(TextSetting newTextSetting, float local5798_2, float local5798_3, float local5798_4, int local5798_5) {
      if (newTextSetting.hasSelection()) {
         Render2D.method4715(
            local5798_2 + FontText.method1396(FontFace.SF, newTextSetting.getValue().substring(0, newTextSetting.getSelectionStart()), local5798_4),
            local5798_3 - 1.0F * this.scale,
            FontText.method1396(
               FontFace.SF, newTextSetting.getValue().substring(newTextSetting.getSelectionStart(), newTextSetting.getSelectionEnd()), local5798_4
            ),
            local5798_4 + 2.0F * this.scale,
            1.0F * this.scale,
            UiTheme.method2877(UiTheme.field09686, 0.4F)
         );
      }

      FontText.method1397(FontFace.SF, newTextSetting.getValue(), local5798_2, local5798_3, local5798_4, local5798_5);
      if (System.currentTimeMillis() / 530L % 2L == 0L) {
         Render2D.method4715(
            local5798_2 + FontText.method1396(FontFace.SF, newTextSetting.getValue().substring(0, newTextSetting.getCaret()), local5798_4),
            local5798_3 - 0.5F * this.scale,
            0.5F * this.scale,
            local5798_4 + 1.0F * this.scale,
            0.25F * this.scale,
            local5798_5
         );
      }
   }

   public void method3540(float local5799_1, float local5799_2, float local5799_3, float local5799_4, float local5799_5, int local5799_6) {
      Render2D.method4702(local5799_1, local5799_2, local5799_3, local5799_4, local5799_5);
      Render2D.method4715(local5799_1, local5799_2, local5799_3, local5799_4, local5799_5, ClickGuiStyle.field10820);
      Render2D.method4715(local5799_1, local5799_2, local5799_3, local5799_4, local5799_5, ClickGuiStyle.field10822);
      Render2D.method4715(local5799_1, local5799_2, local5799_3, local5799_4, local5799_5, ClickGuiStyle.field10823);
      Render2D.method4769(local5799_1, local5799_2, local5799_3, local5799_4, local5799_5, 0.3F, local5799_6);
   }

   public static float method3541(float local5800_0, float local5800_1, float local5800_2) {
      return Math.max(local5800_1, Math.min(local5800_2, local5800_0));
   }

   public static String method3542() {
      return System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "";
   }

   public void method3543(float local5802_1, float local5802_2, float local5802_3, float local5802_4, float local5802_5, int local5802_6, int local5802_7) {
      Render2D.method4702(local5802_1, local5802_2, local5802_3, local5802_4, local5802_5);
      Render2D.method4715(local5802_1, local5802_2, local5802_3, local5802_4, local5802_5, ClickGuiStyle.field10820);
      Render2D.method4715(local5802_1, local5802_2, local5802_3, local5802_4, local5802_5, ClickGuiStyle.field10824);
      Render2D.method4715(local5802_1, local5802_2, local5802_3, local5802_4, local5802_5, local5802_6);
      Render2D.method4769(local5802_1, local5802_2, local5802_3, local5802_4, local5802_5, 0.3F, local5802_7);
   }

   public float method3544(String local5803_1, boolean local5803_2) {
      AnimatedValue animatedValue = this.field12409
         .computeIfAbsent(local5803_1, local5831_1 -> new AnimatedValue(150L).method3870(local5803_2 ? 1.0 : 0.0));
      animatedValue.method3869(local5803_2 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   public static float[] method3545(float[] floatArray, float local5804_1, float local5804_2, float local5804_3, float local5804_4) {
      if (floatArray == null) {
         return new float[]{local5804_1, local5804_2, Math.max(0.0F, local5804_3), Math.max(0.0F, local5804_4)};
      }

      float local5804_5 = Math.max(floatArray[0], local5804_1);
      float local5804_6 = Math.max(floatArray[1], local5804_2);
      float local5804_7 = Math.min(floatArray[0] + floatArray[2], local5804_1 + local5804_3);
      return new float[]{
         local5804_5,
         local5804_6,
         Math.max(0.0F, local5804_7 - local5804_5),
         Math.max(0.0F, Math.min(floatArray[1] + floatArray[3], local5804_2 + local5804_4) - local5804_6)
      };
   }

   public float method3546(String local5805_1) {
      AnimatedValue animatedValue = this.field12409.computeIfAbsent(local5805_1, local5821_0 -> new AnimatedValue(440L));
      animatedValue.method3869(1.0);
      return (float)animatedValue.method3871();
   }

   public void method3547(String local5807_1, float local5807_2, float local5807_3, float local5807_4, int local5807_5) {
      UiTheme.method2884(local5807_1, local5807_2, local5807_3 - 0.5F * this.scale, local5807_4, local5807_5);
   }

   public boolean method3548(Object local5808_1, boolean local5808_2) {
      return this.field12412.getOrDefault(local5808_1, local5808_2);
   }

   public void method3549(Object local5809_1, boolean local5809_2) {
      this.field12412.put(local5809_1, !this.method3548(local5809_1, local5809_2));
   }

   public void method3551(
      float local5811_1, float local5811_2, float local5811_3, float local5811_4, float[] floatArray, Runnable runnable, Runnable runnable2
   ) {
      this.method3559(local5811_1, local5811_2, local5811_3, local5811_4, floatArray, runnable, runnable2, null);
   }

   public void method3552() {
      if (!this.field12430.isEmpty()) {
         Render2D.method4692(this.drawContext);
         if (!GuiEffectsState.method3978()) {
            GuiEffectsState.method3976(true);
         }

         this.drawContext.createNewRootLayer();

         try {
            this.drawContext.applyBlur();
         } catch (IllegalStateException exception) {
         }

         Render2D.method4649(1);

         for (Runnable runnable : List.copyOf(this.field12430)) {
            runnable.run();
         }

         Render2D.method4692(this.drawContext);
         Render2D.method4649(0);
      }
   }

   public float[] method3553(ColorSetting newColorSetting) {
      return this.field12414
         .computeIfAbsent(
            newColorSetting,
            newColorSetting2 -> new float[]{newColorSetting2.getHue(), newColorSetting2.getSaturation(), newColorSetting2.getBrightness(), newColorSetting2.getAlpha() / 255.0F}
         );
   }

   public void method3554(float local5814_1, float local5814_2, float local5814_3, float local5814_4, float[] floatArray, Runnable runnable) {
      this.method3551(local5814_1, local5814_2, local5814_3, local5814_4, floatArray, runnable, null);
   }

   public void method3555(String local5815_1, String local5815_2, float local5815_3, float local5815_4, float local5815_5, float local5815_6) {
      this.method3569(
         local5815_1,
         local5815_3,
         local5815_4,
         5.5F * this.scale,
         UiTheme.method2881(UiTheme.field09681, UiTheme.field09680, local5815_6),
         local5815_5
      );
      if (local5815_2 != null && !local5815_2.isEmpty()) {
         float local5815_7 = local5815_4 + 8.0F * this.scale;

         for (String local5815_9 : this.method3536(local5815_2, 5.0F * this.scale, local5815_5)) {
            FontText.method1397(
               FontFace.SF,
               local5815_9,
               local5815_3,
               local5815_7,
               5.0F * this.scale,
               UiTheme.method2881(UiTheme.field09682, UiTheme.field09681, local5815_6)
            );
            local5815_7 += 6.0F * this.scale;
         }
      }
   }

   public float method3556(String local5816_1, float local5816_2) {
      int local5816_3 = this.method3570(local5816_1, local5816_2);
      return 7.0F * this.scale + (local5816_3 > 0 ? 1.0F * this.scale + local5816_3 * 6.0F * this.scale : 0.0F);
   }

   public void method3557(float local5817_1, float local5817_2, float local5817_3, float local5817_4, float local5817_5, int local5817_6, int local5817_7) {
      Render2D.method4665(local5817_1, local5817_2, local5817_3, local5817_4, local5817_5, -1090519041);
      Render2D.method4715(local5817_1, local5817_2, local5817_3, local5817_4, local5817_5, ClickGuiStyle.field10820);
      Render2D.method4715(local5817_1, local5817_2, local5817_3, local5817_4, local5817_5, Colors.method3727(local5817_6, 0.8F));
      Render2D.method4769(local5817_1, local5817_2, local5817_3, local5817_4, local5817_5, 0.5F, local5817_7);
   }

   public void method3558(ColorSetting newColorSetting, float[] floatArray) {
      newColorSetting.setValue(
         Math.round(Math.max(0.0F, Math.min(1.0F, floatArray[3])) * 255.0F) << 24 | Color.HSBtoRGB(floatArray[0], floatArray[1], floatArray[2]) & 16777215
      );
   }

   public void method3559(
      float local5819_1,
      float local5819_2,
      float local5819_3,
      float local5819_4,
      float[] floatArray,
      Runnable runnable,
      Runnable runnable2,
      Runnable runnable3
   ) {
      if (floatArray != null) {
         float local5819_9 = Math.min(local5819_1 + local5819_3, floatArray[0] + floatArray[2]);
         float local5819_10 = Math.min(local5819_2 + local5819_4, floatArray[1] + floatArray[3]);
         local5819_1 = Math.max(local5819_1, floatArray[0]);
         local5819_2 = Math.max(local5819_2, floatArray[1]);
         local5819_3 = local5819_9 - local5819_1;
         local5819_4 = local5819_10 - local5819_2;
         if (local5819_3 <= 0.0F || local5819_4 <= 0.0F) {
            return;
         }
      }

      this.field12417.add(new SettingRenderContext.Nested0493(local5819_1, local5819_2, local5819_3, local5819_4, runnable, runnable2, runnable3));
   }

   public float method3560(String local5820_1, float local5820_2) {
      AnimatedValue animatedValue = this.field12409.computeIfAbsent(local5820_1, local5796_1 -> new AnimatedValue(150L).method3870(local5820_2));
      animatedValue.method3869(local5820_2);
      return (float)animatedValue.method3871();
   }

   public static float method3562(float local5822_0) {
      float local5822_1 = GuiScale.method0794() / GuiScale.method0797();
      return local5822_1 > 1.0E-4F ? Math.round(local5822_0 * local5822_1) / local5822_1 : local5822_0;
   }

   public <T> T method3563(Object local5823_1, T local5823_2) {
      return (T)(Object)this.field12413.computeIfAbsent(local5823_1, local5797_1 -> local5823_2);
   }

   public void method3564(float local5824_1, float local5824_2, float local5824_3, float local5824_4, float local5824_5, int local5824_6, int local5824_7) {
      Render2D.method4715(local5824_1, local5824_2, local5824_3, local5824_4, local5824_5, ClickGuiStyle.field10824);
      Render2D.method4715(local5824_1, local5824_2, local5824_3, local5824_4, local5824_5, local5824_6);
      Render2D.method4769(local5824_1, local5824_2, local5824_3, local5824_4, local5824_5, 0.3F, local5824_7);
   }

   public static float method3565(float local5825_0) {
      return GuiScale.method0800(local5825_0);
   }

   public void method3566(float local5826_1, float local5826_2, float local5826_3, float local5826_4, float local5826_5, int local5826_6, int local5826_7) {
      Render2D.method4702(local5826_1, local5826_2, local5826_3, local5826_4, local5826_5);
      Render2D.method4715(local5826_1, local5826_2, local5826_3, local5826_4, local5826_5, ClickGuiStyle.field10820);
      Render2D.method4715(local5826_1, local5826_2, local5826_3, local5826_4, local5826_5, Colors.method3727(local5826_6, 0.8F));
      Render2D.method4769(local5826_1, local5826_2, local5826_3, local5826_4, local5826_5, 0.5F, local5826_7);
   }

   public float method3567(Object local5827_1, boolean local5827_2) {
      AnimatedValue animatedValue = this.field12410.get(local5827_1);
      if (animatedValue == null) {
         this.field12410.put(local5827_1, animatedValue = new AnimatedValue(220L).method3870(local5827_2 ? 1.0 : 0.0));
      }

      animatedValue.method3869(local5827_2 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   public void method3568(String local5828_1, float local5828_2, float local5828_3, float local5828_4, int local5828_5, float local5828_6) {
      if (Math.abs(local5828_6) < 0.01F) {
         UiTheme.method2884(local5828_1, local5828_2, local5828_3 - 0.5F * this.scale, local5828_4, local5828_5);
      } else {
         float local5828_7 = local5828_3 - 0.5F * this.scale;
         float[] floatArray = Render2D.method4655("icons", local5828_1, local5828_4);
         float local5828_9 = local5828_2 - (floatArray[0] + floatArray[2]) / 2.0F;
         Render2D.method4704(
            "icons",
            local5828_1,
            local5828_9,
            local5828_7 - (floatArray[1] + floatArray[3]) / 2.0F + 1.0F,
            local5828_4,
            local5828_5,
            local5828_6,
            local5828_2,
            local5828_7 + 1.0F
         );
      }
   }

   public void method3569(String local5829_1, float local5829_2, float local5829_3, float local5829_4, int local5829_5, float local5829_6) {
      float local5829_7 = FontText.method1396(FontFace.SF, local5829_1, local5829_4) - local5829_6;
      if (local5829_7 <= 0.0F) {
         FontText.method1397(FontFace.SF, local5829_1, local5829_2, local5829_3, local5829_4, local5829_5);
      } else {
         String local5829_8 = "marquee:" + local5829_1 + ":" + Math.round(local5829_2) + ":" + Math.round(local5829_6);
         boolean local5829_9 = UiTheme.method2879(
            this.mouseX, this.mouseY, local5829_2, local5829_3 - 1.5F * this.scale, local5829_6, local5829_4 + 3.0F * this.scale
         );
         long local5829_10 = System.currentTimeMillis();
         long[] longArray = this.field12431.get(local5829_8);
         float local5829_13 = 0.0F;
         if (local5829_9) {
            if (longArray == null) {
               this.field12431.put(local5829_8, longArray = new long[]{local5829_10, local5829_10});
            }

            if (local5829_10 - longArray[1] > 120L) {
               longArray[0] = local5829_10;
            }

            longArray[1] = local5829_10;
            long local5829_14 = Math.max(700L, (long)(local5829_7 / this.scale * 55.0F));
            long local5829_16 = 650L;
            long local5829_18 = local5829_10 - longArray[0] - 100L;
            long local5829_20 = Math.max(0L, local5829_18) % (2L * (local5829_14 + local5829_16));
            local5829_13 = local5829_18 < 0L
               ? 0.0F
               : (
                  local5829_20 < local5829_14
                     ? (float)local5829_20 / (float)local5829_14
                     : (
                        local5829_20 < local5829_14 + local5829_16
                           ? 1.0F
                           : (
                              local5829_20 < 2L * local5829_14 + local5829_16
                                 ? 1.0F - (float)(local5829_20 - local5829_14 - local5829_16) / (float)local5829_14
                                 : 0.0F
                           )
                     )
               );
            local5829_13 = local5829_13 * local5829_13 * (3.0F - 2.0F * local5829_13);
         }

         boolean local5829_14 = longArray != null && local5829_10 - longArray[1] < 400L;
         float local5829_15 = local5829_14 ? local5829_7 * this.method3560(local5829_8, local5829_13) : 0.0F;
         float local5829_16 = 4.0F * this.scale;
         FontText.method1402(
            FontFace.SF,
            local5829_1,
            local5829_2 - local5829_15,
            local5829_3,
            local5829_4,
            local5829_5,
            local5829_2,
            local5829_2 + local5829_6,
            local5829_16,
            method3541(local5829_15 / local5829_16, 0.0F, 1.0F),
            method3541((local5829_7 - local5829_15) / local5829_16, 0.0F, 1.0F)
         );
      }
   }

   public int method3570(String local5830_1, float local5830_2) {
      return local5830_1 != null && !local5830_1.isEmpty() ? this.method3536(local5830_1, 5.0F * this.scale, local5830_2).size() : 0;
   }

   public void method3572(float local5832_1, float local5832_2, float local5832_3, float local5832_4, float local5832_5, int local5832_6) {
      Render2D.method4725(local5832_1, local5832_2, local5832_3, local5832_4, local5832_5, 30.0F, 1.0F, ClickGuiStyle.field10825);
      Render2D.method4715(local5832_1, local5832_2, local5832_3, local5832_4, local5832_5, ClickGuiStyle.field10820);
      Render2D.method4715(local5832_1, local5832_2, local5832_3, local5832_4, local5832_5, ClickGuiStyle.field10821);
      Render2D.method4769(local5832_1, local5832_2, local5832_3, local5832_4, local5832_5, 0.5F, local5832_6);
   }

   public void method3573(String local5833_1, float local5833_2, float local5833_3, float local5833_4, float local5833_5) {
      if (local5833_1 != null && !local5833_1.isEmpty()) {
         if (UiTheme.method2879(this.mouseX, this.mouseY, local5833_2, local5833_3, local5833_4, local5833_5)) {
            this.field12428 = local5833_1;
         }
      }
   }

   public void method3574(DrawContext drawContext, float newScale, float newMouseX, float newMouseY) {
      this.drawContext = drawContext;
      this.scale = newScale;
      this.mouseX = newMouseX;
      this.mouseY = newMouseY;
      this.field12417.clear();
      this.field12418 = 0;
      this.field12415.clear();
      this.field12416.clear();
      this.field12428 = null;
      this.field12430.clear();
      this.field12433 = null;
      this.field12434 = null;
   }

   public void method3575(float local5835_1, float local5835_2, float local5835_3) {
      float local5835_4 = 10.74F * this.scale;
      float local5835_5 = 6.64F * this.scale;
      float local5835_6 = 4.88F * this.scale;
      Render2D.method4744(
         new RectDrawData(
               local5835_1,
               local5835_2,
               local5835_4,
               local5835_5,
               local5835_5 / 2.0F,
               local5835_5 / 2.0F,
               local5835_5 / 2.0F,
               local5835_5 / 2.0F,
               Render2D.method4724(UiTheme.method2881(ClickGuiStyle.field10794, ClickGuiStyle.field10796, local5835_3))
            )
            .withSmoothness(0.5F)
      );
      Render2D.method4682(
         local5835_1 + 0.93F * this.scale + local5835_3 * (local5835_4 - local5835_6 - 1.86F * this.scale) + local5835_6 / 2.0F,
         local5835_2 + local5835_5 / 2.0F,
         local5835_6 / 2.0F,
         0.5F,
         Render2D.method4724(UiTheme.method2881(ClickGuiStyle.field10795, UiTheme.field09686, local5835_3))
      );
   }

   public interface Nested0492 {
      void method3576(int var1);

      void method3577(char var1);
   }

   public record Nested0493(float x, float y, float w, float h, Runnable action, Runnable rightAction, Runnable middleAction) {
   }

   private record Nested0494(String text, float size, float width) {
   }
}
