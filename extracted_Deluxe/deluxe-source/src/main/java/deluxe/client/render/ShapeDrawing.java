package deluxe.client.render;

import deluxe.client.util.GuiScale;
import deluxe.client.ui.hud.HudRenderContext;

import deluxe.client.render.EasingTexture;

import deluxe.client.util.Easings;
import deluxe.client.render.RoundedShapeRenderer;
import deluxe.client.render.PieShapeRenderer;
import deluxe.client.render.TexturedShapeRenderer;
import deluxe.client.render.GlassShapeRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class ShapeDrawing {
   public static void method3648(
      ShapeDrawing.Nested0502 local6077_0,
      float local6077_1,
      float local6077_2,
      float local6077_3,
      float local6077_4,
      float local6077_5,
      float local6077_6,
      float local6077_7,
      float local6077_8,
      int local6077_9,
      float local6077_10,
      float local6077_11
   ) {
      TextureSetup textureSetup = ShapeBlurManager.INSTANCE.method4063(local6077_0.ordinal(), false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      if (textureSetup == null) {
         PieShapeRenderer.method1260(
            method3651(),
            null,
            method3657(),
            local6077_1,
            local6077_2,
            local6077_3,
            local6077_4,
            local6077_5,
            local6077_6,
            local6077_7,
            local6077_8,
            local6077_9,
            local6077_11 * local6077_10,
            1.0F
         );
      } else {
         PieShapeRenderer.method1260(
            method3651(),
            textureSetup,
            method3657(),
            local6077_1,
            local6077_2,
            local6077_3,
            local6077_4,
            local6077_5,
            local6077_6,
            local6077_7,
            local6077_8,
            local6077_9,
            local6077_11,
            local6077_10
         );
      }
   }

   public static int method3649(int local6078_0, float local6078_1) {
      int local6078_2 = Math.round((local6078_0 >>> 24 & 0xFF) * MathHelper.clamp(local6078_1, 0.0F, 1.0F));
      return local6078_2 << 24 | local6078_0 & 16777215;
   }

   private static void method3650(
      Matrix3x2f matrix3x2f,
      float local6079_1,
      float local6079_2,
      float local6079_3,
      float local6079_4,
      int local6079_5,
      float local6079_6,
      float local6079_7,
      CornerRadii cornerRadii,
      ShapeDrawing.Nested0502 local6079_9,
      boolean local6079_10
   ) {
      TextureSetup textureSetup = method3660(matrix3x2f, local6079_1, local6079_2, local6079_3, local6079_4, 0.0F, cornerRadii, local6079_9, local6079_10);
      if (textureSetup != null) {
         RoundedShapeRenderer.method0506(
            method3651(),
            textureSetup,
            matrix3x2f,
            local6079_1,
            local6079_2,
            local6079_3,
            local6079_4,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            cornerRadii,
            local6079_6,
            -0.5F,
            0.5F,
            local6079_7,
            local6079_5,
            local6079_5,
            3,
            local6079_10
         );
      }
   }

   private static DrawContext method3651() {
      return HudRenderContext.method4340();
   }

   public static void method3652(
      ShapeDrawing.Nested0502 local6081_0,
      float local6081_1,
      float local6081_2,
      float local6081_3,
      float local6081_4,
      float local6081_5,
      float local6081_6,
      float local6081_7,
      int local6081_8,
      float local6081_9,
      float local6081_10
   ) {
      TextureSetup textureSetup = ShapeBlurManager.INSTANCE.method4063(local6081_0.ordinal(), false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      if (textureSetup == null) {
         PieShapeRenderer.method1264(
            method3651(),
            null,
            method3657(),
            local6081_1,
            local6081_2,
            local6081_3,
            local6081_4,
            local6081_5,
            local6081_6,
            local6081_7,
            local6081_8,
            local6081_10 * local6081_9,
            1.0F
         );
      } else {
         PieShapeRenderer.method1264(
            method3651(),
            textureSetup,
            method3657(),
            local6081_1,
            local6081_2,
            local6081_3,
            local6081_4,
            local6081_5,
            local6081_6,
            local6081_7,
            local6081_8,
            local6081_10,
            local6081_9
         );
      }
   }

   private ShapeDrawing() {
   }

   public static void method3653(
      float local6083_0,
      float local6083_1,
      float local6083_2,
      float local6083_3,
      float local6083_4,
      CornerRadii cornerRadii,
      ShapeDrawing.Nested0502 local6083_6,
      boolean local6083_7
   ) {
      method3650(method3657(), local6083_0, local6083_1, local6083_2, local6083_3, -1, 0.0F, local6083_4, cornerRadii, local6083_6, local6083_7);
   }

   public static void method3654(
      float local6084_0,
      float local6084_1,
      float local6084_2,
      float local6084_3,
      float local6084_4,
      int local6084_5,
      float local6084_6,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.method1717(
         method3651(),
         method3657(),
         local6084_0 - local6084_4,
         local6084_1 - local6084_4,
         local6084_2 + local6084_4 * 2.0F,
         local6084_3 + local6084_4 * 2.0F,
         cornerRadii,
         local6084_4,
         0.0F,
         local6084_6,
         local6084_5,
         local6084_5,
         local6084_5,
         local6084_5,
         1
      );
   }

   public static void method3655(
      float local6085_0,
      float local6085_1,
      float local6085_2,
      float local6085_3,
      int local6085_4,
      int local6085_5,
      int local6085_6,
      int local6085_7,
      float local6085_8,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.method1717(
         method3651(),
         method3657(),
         local6085_0,
         local6085_1,
         local6085_2,
         local6085_3,
         cornerRadii,
         0.0F,
         0.0F,
         local6085_8,
         local6085_7,
         local6085_6,
         local6085_4,
         local6085_5,
         0
      );
   }

   private static float method3656(float local6086_0) {
      return Math.round(local6086_0 * 2.0F) / 2.0F;
   }

   private static Matrix3x2f method3657() {
      DrawContext drawContext = method3651();
      return drawContext == null ? new Matrix3x2f() : new Matrix3x2f(GuiScale.method0795(drawContext));
   }

   public static short method3658(float local6088_0, boolean local6088_1) {
      int local6088_2 = MathHelper.clamp(Math.round(Math.max(0.0F, local6088_0) * 16.0F), 0, 32767);
      return (short)(local6088_1 ? -local6088_2 : local6088_2);
   }

   public static void method3659(
      ShapeDrawing.Nested0502 local6089_0,
      boolean local6089_1,
      float local6089_2,
      float local6089_3,
      float local6089_4,
      float local6089_5,
      float local6089_6,
      CornerRadii cornerRadii,
      int local6089_8,
      float local6089_9,
      int local6089_10
   ) {
      local6089_2 = method3656(local6089_2);
      local6089_3 = method3656(local6089_3);
      local6089_4 = method3656(local6089_4);
      local6089_5 = method3656(local6089_5);
      float local6089_11 = cornerRadii.maxRadius() * 0.65F;
      float local6089_12 = local6089_6 * local6089_6 * 0.8F;
      Matrix3x2f matrix3x2f = method3657();
      if (!(local6089_11 <= 0.0F) && !(local6089_12 <= 5.0E-4F)) {
         TextureSetup textureSetup = method3660(matrix3x2f, local6089_2, local6089_3, local6089_4, local6089_5, 0.0F, cornerRadii, local6089_0, local6089_1);
         TextureSetup textureSetup2 = EasingTexture.getTexture(Easings.EASE_IN_QUAD);
         if (textureSetup != null && textureSetup.texure0() != null && textureSetup2 != null) {
            TextureSetup textureSetup3 = TextureSetup.of(textureSetup.texure0(), textureSetup.sampler0(), textureSetup2.texure0(), textureSetup2.sampler0());
            float local6089_17 = (local6089_10 >>> 24 & 0xFF) / 255.0F * local6089_12;
            GlassShapeRenderer.method3644(
               method3651(),
               textureSetup3,
               matrix3x2f,
               local6089_2 - local6089_11,
               local6089_3 - local6089_11,
               local6089_4 + local6089_11 * 2.0F,
               local6089_5 + local6089_11 * 2.0F,
               cornerRadii,
               local6089_11,
               local6089_8,
               local6089_6,
               local6089_9,
               local6089_10,
               local6089_17,
               local6089_1
            );
         }
      } else {
         method3650(
            matrix3x2f, local6089_2, local6089_3, local6089_4, local6089_5, local6089_8, local6089_9, local6089_6, cornerRadii, local6089_0, local6089_1
         );
      }
   }

   private static TextureSetup method3660(
      Matrix3x2f matrix3x2f,
      float local6090_1,
      float local6090_2,
      float local6090_3,
      float local6090_4,
      float local6090_5,
      CornerRadii cornerRadii,
      ShapeDrawing.Nested0502 local6090_7,
      boolean local6090_8
   ) {
      float local6090_9 = 0.0F;
      float local6090_10 = 0.0F;
      float local6090_11 = 0.0F;
      float local6090_12 = 0.0F;
      float local6090_13 = 0.0F;
      if (local6090_8) {
         float local6090_14 = local6090_1 - local6090_5;
         float local6090_15 = local6090_2 - local6090_5;
         float local6090_16 = local6090_3 + local6090_5 * 2.0F;
         float local6090_17 = local6090_4 + local6090_5 * 2.0F;
         Vector2f vector2f = matrix3x2f.transformPosition(local6090_14, local6090_15, new Vector2f());
         Vector2f vector2f2 = matrix3x2f.transformPosition(local6090_14 + local6090_16, local6090_15 + local6090_17, new Vector2f());
         float local6090_20 = Math.min(vector2f.x, vector2f2.x);
         float local6090_21 = Math.min(vector2f.y, vector2f2.y);
         float local6090_22 = Math.max(vector2f.x, vector2f2.x);
         float local6090_23 = Math.max(vector2f.y, vector2f2.y);
         Window window = MinecraftClient.getInstance().getWindow();
         float scaleFactor = window.getScaleFactor();
         float local6090_26 = scaleFactor <= 0.0F ? window.getScaledWidth() : window.getFramebufferWidth() / scaleFactor;
         float local6090_27 = scaleFactor <= 0.0F ? window.getScaledHeight() : window.getFramebufferHeight() / scaleFactor;
         local6090_9 = local6090_20 / local6090_26;
         local6090_11 = local6090_22 / local6090_26;
         local6090_10 = 1.0F - local6090_23 / local6090_27;
         local6090_12 = 1.0F - local6090_21 / local6090_27;
         local6090_13 = cornerRadii.maxRadius() * scaleFactor * matrix3x2f.m00();
      }

      return ShapeBlurManager.INSTANCE.method4063(local6090_7.ordinal(), local6090_8, local6090_9, local6090_10, local6090_11, local6090_12, local6090_13);
   }

   public static ScreenRect method3661(Matrix3x2f matrix3x2f, ScreenRect screenRect, float local6091_2, float local6091_3, float local6091_4, float local6091_5) {
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(local6091_2), Math.round(local6091_3), Math.max(1, Math.round(local6091_4)), Math.max(1, Math.round(local6091_5))
         )
         .transformEachVertex(matrix3x2f);
      return screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   public static void method3662(
      float local6092_0,
      float local6092_1,
      float local6092_2,
      float local6092_3,
      float local6092_4,
      float local6092_5,
      float local6092_6,
      int local6092_7,
      float local6092_8
   ) {
      PieShapeRenderer.method1264(
         method3651(),
         null,
         method3657(),
         local6092_0,
         local6092_1,
         local6092_2,
         local6092_3,
         local6092_4,
         local6092_5,
         local6092_6,
         local6092_7,
         local6092_8,
         1.0F
      );
   }

   public static void method3663(
      float local6093_0, float local6093_1, float local6093_2, float local6093_3, int local6093_4, float local6093_5, CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.method1717(
         method3651(),
         method3657(),
         local6093_0,
         local6093_1,
         local6093_2,
         local6093_3,
         cornerRadii,
         0.0F,
         0.0F,
         local6093_5,
         local6093_4,
         local6093_4,
         local6093_4,
         local6093_4,
         0
      );
   }

   public static void method3664(
      float local6094_0,
      float local6094_1,
      float local6094_2,
      float local6094_3,
      float local6094_4,
      int local6094_5,
      float local6094_6,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.method1717(
         method3651(),
         method3657(),
         local6094_0,
         local6094_1,
         local6094_2,
         local6094_3,
         cornerRadii,
         0.0F,
         local6094_4,
         local6094_6,
         local6094_5,
         local6094_5,
         local6094_5,
         local6094_5,
         4
      );
   }

   public enum Nested0502 {
      LOW,
      MIDDLE,
      MAX;
   }
}
