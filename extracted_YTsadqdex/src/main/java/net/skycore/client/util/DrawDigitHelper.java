package net.skycore.client.util;

public final class DrawDigitHelper {
   private static boolean isTransitioning(String text, String text2, long timestamp, long timestamp2, long timestamp3) {
      return !text.equals(text2) && timestamp > 0L && timestamp3 > 0L && timestamp2 >= timestamp && timestamp2 - timestamp < timestamp3 * 1000000L;
   }

   private static void drawDigit(float coordinate, float coordinate2, float coordinate3, String text, int coordinate4, float coordinate5, int coordinate6) {
      if (!(coordinate5 <= 0.001F)) {
         int value = coordinate4 >>> 24 & 0xFF;
         int color = coordinate4 & 16777215 | Math.round(value * clamp(coordinate5, 0.0F, 1.0F)) << 24;
         BeginFrameHelper.text(coordinate, coordinate2, coordinate3, text).weight(coordinate6).color(color).draw();
      }
   }

   private static float progress(long timestamp, long timestamp2, long timestamp3, boolean condition) {
      float computedValue = clamp((float)(timestamp2 - timestamp) / ((float)timestamp3 * 1000000.0F), 0.0F, 1.0F);
      float computedValue2 = 1.0F - computedValue;
      return condition ? 1.0F - computedValue2 * computedValue2 * computedValue2 : computedValue;
   }

   public static void renderLeft(
      float coordinate,
      float coordinate2,
      float coordinate3,
      String text,
      String text2,
      long timestamp,
      long timestamp2,
      long timestamp3,
      int coordinate4,
      float coordinate5,
      int coordinate6,
      float coordinate7,
      boolean condition
   ) {
      render(false, coordinate, coordinate2, coordinate3, text, text2, timestamp, timestamp2, timestamp3, coordinate4, coordinate5, coordinate6, coordinate7, condition);
   }

   private DrawDigitHelper() {
   }

   private static float digitX(TextHeightHelper textHeightHelper, String text, int value, float width, float width2, boolean condition) {
      float width3;
      if (condition) {
         width3 = width - textHeightHelper.getWidth(text, width2);
      } else {
         width3 = width;
      }

      float value2 = width3;
      String text2 = text.substring(0, value);
      float width4 = textHeightHelper.getWidth(text2, width2);
      float value3;
      if (value > 0) {
         char character = text.charAt(value - 1);
         char character2 = text.charAt(value);
         value3 = textHeightHelper.kerningAdvance(character, character2) * width2;
      } else {
         value3 = 0.0F;
      }

      float value4 = value3;
      return value2 + width4 + value4;
   }

   public static float width(String text, float width, int value) {
      return InterBoldHelper.forWeight(value).getWidth(text, width);
   }

   private static void render(
      boolean condition,
      float coordinate,
      float coordinate2,
      float width,
      String text,
      String text2,
      long timestamp,
      long timestamp2,
      long timestamp3,
      int coordinate3,
      float coordinate4,
      int coordinate5,
      float coordinate6,
      boolean condition2
   ) {
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(coordinate5);
      boolean transitioning = isTransitioning(text, text2, timestamp, timestamp2, timestamp3);
      float value = transitioning ? progress(timestamp, timestamp2, timestamp3, condition2) : 1.0F;
      int length = transitioning ? Math.max(text.length(), text2.length()) : text2.length();
      if (length != 0) {
         float width2 = textHeightHelper.getWidth(text, width);
         float width3 = textHeightHelper.getWidth(text2, width);
         float computedValue = condition ? coordinate - Math.max(width2, width3) - 2.0F : coordinate - 2.0F;
         BeginFrameHelper.pushScissor(computedValue, coordinate2 - coordinate6 - 2.0F, Math.max(width2, width3) + 4.0F, width + coordinate6 * 2.0F + 4.0F);

         try {
            for (int value2 = 0; value2 < length; value2++) {
               int computedValue2 = text.length() - 1 - value2;
               int computedValue3 = text2.length() - 1 - value2;
               String text3 = computedValue2 >= 0 ? text.substring(computedValue2, computedValue2 + 1) : "";
               String text4 = computedValue3 >= 0 ? text2.substring(computedValue3, computedValue3 + 1) : "";
               float value3 = computedValue2 >= 0 ? digitX(textHeightHelper, text, computedValue2, coordinate, width, condition) : digitX(textHeightHelper, text2, computedValue3, coordinate, width, condition);
               float value4 = computedValue3 >= 0 ? digitX(textHeightHelper, text2, computedValue3, coordinate, width, condition) : value3;
               float computedValue4 = value3 + (value4 - value3) * value;
               if (transitioning && !text3.equals(text4)) {
                  if (!text3.isEmpty()) {
                     drawDigit(computedValue4, coordinate2 + coordinate6 * value, width, text3, coordinate3, coordinate4 * (1.0F - value), coordinate5);
                  }

                  if (!text4.isEmpty()) {
                     drawDigit(computedValue4, coordinate2 - coordinate6 * (1.0F - value), width, text4, coordinate3, coordinate4 * value, coordinate5);
                  }
               } else if (!text4.isEmpty()) {
                  drawDigit(computedValue4, coordinate2, width, text4, coordinate3, coordinate4, coordinate5);
               }
            }
         } finally {
            BeginFrameHelper.popScissor();
         }
      }
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public static void renderRight(
      float coordinate,
      float coordinate2,
      float coordinate3,
      String text,
      String text2,
      long timestamp,
      long timestamp2,
      long timestamp3,
      int coordinate4,
      float coordinate5,
      int coordinate6,
      float coordinate7,
      boolean condition
   ) {
      render(true, coordinate, coordinate2, coordinate3, text, text2, timestamp, timestamp2, timestamp3, coordinate4, coordinate5, coordinate6, coordinate7, condition);
   }

   public static float slotWidth(String text, String text2, float width, int value, long timestamp, long timestamp2, long timestamp3, boolean condition) {
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(value);
      if (!isTransitioning(text, text2, timestamp, timestamp2, timestamp3)) {
         return textHeightHelper.getWidth(text2, width);
      }

      float width2 = progress(timestamp, timestamp2, timestamp3, condition);
      float width3 = textHeightHelper.getWidth(text, width);
      return width3 + (textHeightHelper.getWidth(text2, width) - width3) * width2;
   }
}
