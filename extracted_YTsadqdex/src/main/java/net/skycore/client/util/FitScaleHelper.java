package net.skycore.client.util;

public final class FitScaleHelper {
   public static final float HEADER_BODY_GAP = 8.0F;
   public static final float HEADER_WIDTH = 976.0F;
   public static final float BASE_SCALE = 0.5F;
   public static final float PROFILE_GAP = 6.0F;
   public static final float GUI_RADIUS = 24.0F;
   public static final float PROFILE_HEIGHT = 32.0F;
   public static final float MODULE_GAP = 8.0F;
   public static final float MODULE_WIDTH = 316.66666F;
   public static final float HEADER_ITEM_RADIUS = 16.0F;
   public static final float AVATAR_BOX = 38.0F;
   public static final float SCROLL_WIDTH = 2.0F;
   public static final float PANEL_PADDING = 8.0F;
   public static final float CONTENT_WIDTH = 966.0F;
   public static final float GUI_WIDTH = 992.0F;
   public static final float CONFIG_INPUT_WIDTH = 160.0F;
   public static final float SEARCH_HEIGHT = 38.0F;
   public static final float BODY_WIDTH = 976.0F;
   public static final float MODULE_RADIUS = 16.0F;
   public static final float PANEL_RADIUS = 24.0F;
   public static final float THEMES_X = 0.0F;
   public static final float GUI_HEIGHT = 704.0F;
   public static final float MODULE_PADDING = 16.0F;
   public static final float SEARCH_GAP = 8.0F;
   public static final float AVATAR_SIZE = 30.0F;
   public static final float COLUMN_GAP = 8.0F;
   public static final float SEARCH_WIDTH = 192.0F;
   public static final float PANEL_GAP = 8.0F;
   public static final float HEADER_HEIGHT = 38.0F;
   public static final float MIN_SCALE = 0.35F;
   public static final float GUI_PADDING = 8.0F;
   public static final float PANEL_HEIGHT = 704.0F;
   public static final float MODULE_HEADER_HEIGHT = 82.0F;
   public static final float COLUMN_WIDTH = 316.66666F;
   public static final float BODY_HEIGHT = 642.0F;
   public static final float BLUR_RADIUS = 36.0F;
   public static final float PANEL_WIDTH = 992.0F;
   public static final float MAX_SCALE = 0.75F;

   public static float fitScale(int value, int value2, float value3) {
      float value4 = clampRenderFactor(value3);
      float maximum = Math.max(1.0F, value - 16.0F);
      float computedValue = value2 - 16.0F;
      float maximum2 = Math.max(1.0F, computedValue);
      float minimum = Math.min(maximum / 992.0F, maximum2 / 704.0F);
      return minimum < value4 ? Math.nextDown(minimum) : value4;
   }

   private FitScaleHelper() {
   }

   public static float clampRenderFactor(float coordinate) {
      if (!Float.isFinite(coordinate)) {
         return 0.5F;
      }

      return Math.max(0.35F, Math.min(0.75F, coordinate));
   }

   public static float footerHeight() {
      return 84.0F;
   }

   public static float px(float value, float value2) {
      return value * value2;
   }
}
