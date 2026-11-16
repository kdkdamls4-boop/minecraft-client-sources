package deluxe.client.render;

import deluxe.client.ui.UiTheme;

import deluxe.client.render.Render2D;

import java.awt.Color;

public final class ThemeColors {
   public static int field13661 = new Color(15, 14, 17, 184).getRGB();
   public static int field13662 = new Color(255, 255, 255, 3).getRGB();
   public static int field13663 = new Color(255, 255, 255, 5).getRGB();
   public static int __ = new Color(255, 255, 255, 10).getRGB();
   public static int field13664 = new Color(255, 255, 255, 20).getRGB();
   public static int field13665 = -1;
   public static int field13666 = new Color(255, 255, 255, 184).getRGB();
   public static int field13667 = new Color(255, 255, 255, 122).getRGB();
   public static int field13668 = new Color(255, 255, 255, 61).getRGB();
   public static int field13669 = -12092417;
   public static int field13670 = 285212671;

   public static void method3961(String local6576_0, float local6576_1, float local6576_2, float local6576_3, int local6576_4) {
      UiTheme.method2884(local6576_0, local6576_1, local6576_2 - 0.5F, local6576_3, local6576_4);
   }

   private ThemeColors() {
   }

   public static void method3962(float local6578_0, float local6578_1, float local6578_2, float local6578_3) {
      Render2D.method4742(local6578_0 + local6578_2 - 9.0F, local6578_1 + local6578_3 - 9.0F, 5.0F, 2.5F, 0.6F, 2, field13670);
   }

   public static void method3963(float local6579_0, float local6579_1, float local6579_2, float local6579_3, float local6579_4) {
      Render2D.method4702(local6579_0, local6579_1, local6579_2, local6579_3, local6579_4);
      Render2D.method4769(local6579_0, local6579_1, local6579_2, local6579_3, local6579_4, 0.5F, field13663);
   }

   public static void method3964(float local6580_0, float local6580_1, float local6580_2) {
      Render2D.method4715(local6580_0, local6580_1 - local6580_2 / 2.0F, 0.5F, local6580_2, 0.25F, __);
   }

   public static float method3965(float local6582_0) {
      return Math.max(0.0F, Math.min(1.0F, local6582_0));
   }

   public static void method3966(float local6583_0, float local6583_1, float local6583_2, float local6583_3, int local6583_4) {
      float local6583_5 = local6583_0 - local6583_2 / 2.0F;
      float local6583_6 = local6583_1 - local6583_2 / 2.0F;
      float local6583_7 = method3965(local6583_3);
      if (local6583_7 < 0.004F) {
         Render2D.method4749(local6583_5, local6583_6, local6583_2, local6583_2, local6583_2 / 2.0F, 1.0F, field13663);
      } else {
         Render2D.method4749(
            local6583_5,
            local6583_6,
            local6583_2,
            local6583_2,
            local6583_2 / 2.0F,
            1.0F,
            field13663,
            ArcColorSpan.of(90.0F - local6583_7 * 360.0F, 90.0F, local6583_4)
         );
         if (local6583_7 < 0.996F) {
            float local6583_8 = local6583_2 / 2.0F - 0.5F;
            double local6583_9 = Math.toRadians(local6583_7 * 360.0);
            Render2D.method4682(local6583_0, local6583_1 - local6583_8, 0.5F, 0.25F, local6583_4);
            Render2D.method4682(
               local6583_0 + local6583_8 * (float)Math.sin(local6583_9), local6583_1 - local6583_8 * (float)Math.cos(local6583_9), 0.5F, 0.25F, local6583_4
            );
         }
      }
   }
}
