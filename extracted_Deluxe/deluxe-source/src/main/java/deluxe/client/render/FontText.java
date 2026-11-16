package deluxe.client.render;

import deluxe.client.render.FontFace;
import deluxe.client.util.FormattedText;
import deluxe.client.render.Render2D;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class FontText {
   public static void method1394(
      FontFace fontFace,
      String local2050_1,
      float local2050_2,
      float local2050_3,
      float local2050_4,
      int local2050_5,
      float local2050_6,
      float local2050_7,
      float local2050_8
   ) {
      Render2D.method4642(
         fontFace.getResourceName(), local2050_1, local2050_2, local2050_3, local2050_4, local2050_5, local2050_6, local2050_7, local2050_8
      );
   }

   private FontText() {
   }

   public static void method1395(
      FontFace fontFace,
      String local2052_1,
      float local2052_2,
      float local2052_3,
      float local2052_4,
      int local2052_5,
      int local2052_6,
      float local2052_7
   ) {
      Render2D.method4639(fontFace.getResourceName(), local2052_1, local2052_2 + local2052_7, local2052_3 + local2052_7, local2052_4, local2052_6);
      Render2D.method4639(fontFace.getResourceName(), local2052_1, local2052_2, local2052_3, local2052_4, local2052_5);
   }

   public static float method1396(FontFace fontFace, String local2053_1, float local2053_2) {
      return Render2D.method4738(fontFace.getResourceName(), local2053_1, local2053_2);
   }

   public static void method1397(FontFace fontFace, String local2054_1, float local2054_2, float local2054_3, float local2054_4, int local2054_5) {
      Render2D.method4639(fontFace.getResourceName(), local2054_1, local2054_2, local2054_3, local2054_4, local2054_5);
   }

   public static void method1398(FontFace fontFace, String local2055_1, float local2055_2, float local2055_3, float local2055_4, int local2055_5) {
      float local2055_6 = Math.max(0.5F, local2055_4 / 16.0F);
      float local2055_7 = Math.round(local2055_6 * 2.0F) / 2.0F;
      int local2055_8 = method1399(local2055_5);
      method1395(fontFace, local2055_1, local2055_2, local2055_3, local2055_4, local2055_5, local2055_8, local2055_7);
   }

   private static int method1399(int local2056_0) {
      int local2056_1 = local2056_0 >>> 24 & 0xFF;
      int local2056_2 = Math.round(local2056_1 * 0.55F);
      return local2056_2 << 24;
   }

   public static void method1400(FontFace fontFace, Text newText, float local2057_2, float local2057_3, float local2057_4, int local2057_5) {
      StringBuilder stringBuilder = new StringBuilder();

      for (FormattedText.Nested0521 local2057_8 : FormattedText.method4204(newText, local2057_5)) {
         if (local2057_8.obfuscated()) {
            Render2D.method4718(
               fontFace.getResourceName(),
               local2057_8.text(),
               local2057_2 + method1396(fontFace, stringBuilder.toString(), local2057_4),
               local2057_3,
               local2057_4,
               local2057_8.color() & 16777215 | local2057_5 & 0xFF000000
            );
         } else {
            method1397(
               fontFace,
               local2057_8.text(),
               local2057_2 + method1396(fontFace, stringBuilder.toString(), local2057_4),
               local2057_3,
               local2057_4,
               local2057_8.color() & 16777215 | local2057_5 & 0xFF000000
            );
         }

         stringBuilder.append(local2057_8.text());
      }
   }

   public static void method1401(
      DrawContext drawContext, FontFace fontFace, String local2058_2, float local2058_3, float local2058_4, float local2058_5, int local2058_6
   ) {
      Render2D.method4713(drawContext);
      method1397(fontFace, local2058_2, local2058_3, local2058_4, local2058_5, local2058_6);
      Render2D.method4754();
   }

   public static void method1402(
      FontFace fontFace,
      String local2059_1,
      float local2059_2,
      float local2059_3,
      float local2059_4,
      int local2059_5,
      float local2059_6,
      float local2059_7,
      float local2059_8,
      float local2059_9,
      float local2059_10
   ) {
      Render2D.method4737(
         fontFace.getResourceName(),
         local2059_1,
         local2059_2,
         local2059_3,
         local2059_4,
         local2059_5,
         local2059_6,
         local2059_7,
         local2059_8,
         local2059_9,
         local2059_10
      );
   }

   public static void method1403(
      FontFace fontFace,
      String local2060_1,
      float local2060_2,
      float local2060_3,
      float local2060_4,
      int local2060_5,
      float local2060_6,
      float local2060_7,
      float local2060_8,
      boolean local2060_9,
      boolean local2060_10
   ) {
      Render2D.method4751(
         fontFace.getResourceName(),
         local2060_1,
         local2060_2,
         local2060_3,
         local2060_4,
         local2060_5,
         local2060_6,
         local2060_7,
         local2060_8,
         local2060_9,
         local2060_10
      );
   }

   public static void method1404(
      FontFace fontFace,
      String local2061_1,
      float local2061_2,
      float local2061_3,
      float local2061_4,
      int local2061_5,
      int local2061_6,
      int local2061_7,
      int local2061_8
   ) {
      Render2D.method4669(
         fontFace.getResourceName(), local2061_1, local2061_2, local2061_3, local2061_4, local2061_5, local2061_6, local2061_7, local2061_8
      );
   }

   public static void method1405(FontFace fontFace, String local2062_1, float local2062_2, float local2062_3, float local2062_4, int local2062_5) {
      float local2062_6 = method1396(fontFace, local2062_1, local2062_4);
      method1397(fontFace, local2062_1, local2062_2 - local2062_6 * 0.5F, local2062_3, local2062_4, local2062_5);
   }
}
