package net.skycore.client.hud;

import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.setting.ModeSetting;

public final class HudStyle {
   private static final float BLUR_RADIUS = 36.0F;

   public static int background(float alpha) {
      ThemeManager themeManager = SkycoreClient.themes();
      return alpha(themeManager.backgroundHud(), alpha);
   }

   private HudStyle() {
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float value4 = value3;
      float minimum = Math.min(1.0F, value2);
      float maximum = value4 * Math.max(0.0F, minimum);
      int roundedValue = Math.round(maximum);
      return value & 16777215 | roundedValue << 24;
   }

   public static boolean transparent() {
      ClickGUIModule clickGUIModule = SkycoreClient.modules().get(ClickGUIModule.class).orElse(null);
      if (clickGUIModule != null) {
         ModeSetting modeSetting = clickGUIModule.style();
         if (modeSetting.is("Transparent")) {
            return true;
         }
      }

      return false;
   }

   public static float blurRadius(float value) {
      return 36.0F * value;
   }

   public static int border(float value) {
      return 0;
   }

   public static boolean blur() {
      return transparent();
   }
}
