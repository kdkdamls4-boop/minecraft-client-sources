package deluxe.client.ui.hud;

import deluxe.client.render.CornerRadii;
import deluxe.client.render.FontText;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;

public final class HudStyle {
   public static final float field16131 = 13.0F;
   public static final float field16132 = 2.0F;
   public static final float field16133 = 11.0F;
   public static final float field16134 = 13.0F;
   public static final float field16135 = 4.5F;
   private static final CornerRadii field16136 = CornerRadii.uniform(4.5F);
   private static final CornerRadii field16137 = CornerRadii.uniform(0.25F);

   public static float method4809(float local8003_0, float local8003_1) {
      float local8003_2 = local8003_0 > 0.0F && local8003_1 > 0.0F ? local8003_0 + 4.0F + local8003_1 : local8003_0 + local8003_1;
      return local8003_2 > 0.0F ? 2.0F + local8003_2 + 4.0F : 0.0F;
   }

   public static void method4810(float local8004_0, float local8004_1, float local8004_2, float local8004_3) {
      ShapeDrawing.method3663(local8004_0, local8004_1 - local8004_3 / 2.0F, 0.5F, local8004_3, method4815(1.0F), local8004_2 * 0.3F, field16137);
   }

   private HudStyle() {
   }

   public static int method4811(float local8007_0) {
      return UiTheme.method2877(ThemeColors.field13669, local8007_0);
   }

   public static void method4812(
      float local8008_0,
      float local8008_1,
      float local8008_2,
      float local8008_3,
      float local8008_4,
      float topLeft,
      ShapeDrawing.Nested0502 local8008_6
   ) {
      ShapeDrawing.method3659(
         local8008_6,
         true,
         local8008_0,
         local8008_1,
         local8008_2,
         local8008_3,
         local8008_4,
         CornerRadii.uniform(topLeft),
         method4818(),
         0.65F,
         UiTheme.method2881(-16777216, ThemeColors.field13669, 0.1F)
      );
   }

   private static void method4813(float local8009_0, float local8009_1, float local8009_2, float local8009_3, float local8009_4, CornerRadii cornerRadii) {
      ShapeDrawing.method3653(local8009_0, local8009_1, local8009_2, local8009_3, local8009_4, cornerRadii, ShapeDrawing.Nested0502.LOW, true);
      ShapeDrawing.method3663(local8009_0, local8009_1, local8009_2, local8009_3, method4818(), local8009_4 * 0.185F, cornerRadii);
   }

   public static float method4814(float local8010_0) {
      float local8010_1 = local8010_0 * local8010_0;
      float local8010_2 = local8010_1 >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * local8010_1);
      return 7.0F * (1.0F - local8010_2);
   }

   public static int method4815(float local8011_0) {
      return UiTheme.method2877(UiTheme.method2881(ThemeColors.field13669, -1, 0.68F), local8011_0);
   }

   public static float method4816(float local8012_0) {
      return local8012_0 + 5.5F - 3.5F;
   }

   public static void method4817(float local8013_0, float local8013_1, float local8013_2, float local8013_3, float local8013_4, float topLeft) {
      method4812(local8013_0, local8013_1, local8013_2, local8013_3, local8013_4, topLeft, ShapeDrawing.Nested0502.MIDDLE);
   }

   public static int method4818() {
      return UiTheme.method2881(ThemeColors.field13669, -16777216, 0.9F);
   }

   public static void method4819(float local8015_0, float local8015_1, float local8015_2, float local8015_3, float local8015_4, float topLeft) {
      method4813(local8015_0, local8015_1, local8015_2, local8015_3, local8015_4, CornerRadii.uniform(topLeft));
   }

   public static int method4820(float local8016_0) {
      return UiTheme.method2877(UiTheme.method2881(ThemeColors.field13669, -1, 0.92F), local8016_0);
   }

   public static void method4821(float local8017_0, float local8017_1, float local8017_2, float local8017_3, float local8017_4) {
      method4813(local8017_0, local8017_1, local8017_2, local8017_3, local8017_4, field16136);
   }

   public static float method4822(float local8018_0) {
      return local8018_0 > 0.0F ? 4.0F + local8018_0 + 4.0F : 0.0F;
   }

   public static float method4823(String local8019_0) {
      return 16.0F + FontText.method1396(FontFace.SEMI_BOLD, local8019_0, 6.0F) + 4.0F;
   }

   public static void method4824(float local8020_0, float local8020_1, float local8020_2, float local8020_3, HudIcons hudIcons, String local8020_5) {
      method4812(local8020_0, local8020_1, local8020_2, 13.0F, local8020_3, 4.5F, ShapeDrawing.Nested0502.LOW);
      hudIcons.method4010(local8020_0 + 4.0F, local8020_1 + (hudIcons.field13799 != null ? 2.0F : 2.5F), 8.0F, method4811(local8020_3));
      FontText.method1397(FontFace.SEMI_BOLD, local8020_5, local8020_0 + 16.0F, local8020_1 + 6.5F - 3.65F, 6.0F, method4820(local8020_3));
   }
}
