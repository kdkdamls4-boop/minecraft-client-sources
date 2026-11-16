package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.ShapePipelines;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class PieShapeRenderer {
   public static void method1260(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float local1729_3,
      float local1729_4,
      float local1729_5,
      float local1729_6,
      float local1729_7,
      float local1729_8,
      float local1729_9,
      float local1729_10,
      int local1729_11,
      float local1729_12,
      float local1729_13
   ) {
      if (drawContext != null && !(local1729_12 <= 5.0E-4F)) {
         List<Vector2f> local1729_14 = method1262(local1729_5, local1729_6, local1729_8, local1729_9, local1729_10);
         if (local1729_14 != null) {
            float local1729_15 = (float)Math.toRadians(local1729_7 + local1729_8 / 2.0F);
            float local1729_16 = (float)Math.cos(local1729_15);
            float local1729_17 = (float)Math.sin(local1729_15);
            float local1729_18 = 0.0F;
            float local1729_19 = 0.0F;

            for (Vector2f vector2f : local1729_14) {
               vector2f.set(
                  local1729_3 + vector2f.x * local1729_16 - vector2f.y * local1729_17,
                  local1729_4 + vector2f.x * local1729_17 + vector2f.y * local1729_16
               );
               local1729_18 += vector2f.x / local1729_14.size();
               local1729_19 += vector2f.y / local1729_14.size();
            }

            ScreenRect screenRect = ScissorStack.method3969();
            ScreenRect screenRect2 = ShapeDrawing.method3661(
               matrix3x2f,
               screenRect,
               local1729_3 - local1729_6 - 2.0F,
               local1729_4 - local1729_6 - 2.0F,
               local1729_6 * 2.0F + 4.0F,
               local1729_6 * 2.0F + 4.0F
            );
            if (screenRect2 != null) {
               boolean local1729_22 = newTextureSetup != null;
               ((DrawContextMixin)drawContext)
                  .getGuiRenderState()
                  .addSimpleElement(
                     new PieShapeRenderer.Nested0378(
                        local1729_22 ? newTextureSetup : TextureSetup.empty(),
                        new Matrix3x2f(matrix3x2f),
                        local1729_14,
                        local1729_18,
                        local1729_19,
                        ShapeDrawing.method3649(local1729_11, local1729_12),
                        local1729_13,
                        local1729_22,
                        screenRect,
                        screenRect2
                     )
                  );
            }
         }
      }
   }

   private static List<Vector2f> method1261(
      float local1730_0, float local1730_1, float local1730_2, float local1730_3, float local1730_4, float local1730_5, float local1730_6
   ) {
      if (!(local1730_2 <= 0.0F) && !(Math.abs(local1730_4) < 0.01F)) {
         if (local1730_4 < 0.0F) {
            local1730_3 += local1730_4;
            local1730_4 = -local1730_4;
         }

         List<Vector2f> local1730_7 = method1265(local1730_2, local1730_4, local1730_5, local1730_6);
         if (local1730_7 == null) {
            return null;
         }

         float local1730_8 = (float)Math.toRadians(local1730_3 + local1730_4 / 2.0F);
         float local1730_9 = (float)Math.cos(local1730_8);
         float local1730_10 = (float)Math.sin(local1730_8);

         for (Vector2f vector2f : local1730_7) {
            vector2f.set(
               local1730_0 + vector2f.x * local1730_9 - vector2f.y * local1730_10,
               local1730_1 + vector2f.x * local1730_10 + vector2f.y * local1730_9
            );
         }

         return local1730_7;
      } else {
         return null;
      }
   }

   private static List<Vector2f> method1262(float local1731_0, float local1731_1, float local1731_2, float local1731_3, float local1731_4) {
      float local1731_5 = (float)Math.toRadians(Math.abs(local1731_2)) / 2.0F;
      float local1731_6 = Math.max(0.5F, Math.min(local1731_4, (local1731_1 - local1731_0) * 0.45F));
      float local1731_7 = Math.max(0.0F, local1731_3) / 2.0F + local1731_6;
      float local1731_8 = local1731_1 - local1731_6;
      float local1731_9 = local1731_0 + local1731_6;
      if (!(local1731_9 <= local1731_7) && !(local1731_8 <= local1731_9)) {
         float local1731_10 = local1731_5 - (float)Math.asin(local1731_7 / local1731_8);
         float local1731_11 = local1731_5 - (float)Math.asin(local1731_7 / local1731_9);
         if (!(local1731_10 <= 0.0F) && !(local1731_11 <= 0.0F)) {
            List<Vector2f> local1731_12 = new ArrayList<>();
            int local1731_13 = Math.max(2, Math.round((float)Math.toDegrees(local1731_10 * 2.0F) / 3.0F));

            for (int index = 0; index <= local1731_13; index++) {
               float local1731_15 = -local1731_10 + local1731_10 * 2.0F * index / local1731_13;
               local1731_12.add(new Vector2f(local1731_8 * (float)Math.cos(local1731_15), local1731_8 * (float)Math.sin(local1731_15)));
            }

            int local1731_14 = Math.max(2, Math.round((float)Math.toDegrees(local1731_11 * 2.0F) / 3.0F));

            for (int index2 = 0; index2 <= local1731_14; index2++) {
               float local1731_16 = local1731_11 - local1731_11 * 2.0F * index2 / local1731_14;
               local1731_12.add(new Vector2f(local1731_9 * (float)Math.cos(local1731_16), local1731_9 * (float)Math.sin(local1731_16)));
            }

            return method1266(local1731_12, local1731_6);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static float method1263(Vector2f vector2f, Vector2f vector2f2, float local1732_2, float local1732_3) {
      float local1732_4 = vector2f2.x - vector2f.x;
      float local1732_5 = vector2f2.y - vector2f.y;
      float local1732_6 = local1732_5;
      float local1732_7 = -local1732_4;
      float local1732_8 = (vector2f.x + vector2f2.x) / 2.0F - local1732_2;
      float local1732_9 = (vector2f.y + vector2f2.y) / 2.0F - local1732_3;
      if (local1732_6 * local1732_8 + local1732_7 * local1732_9 < 0.0F) {
         local1732_6 = -local1732_6;
         local1732_7 = -local1732_7;
      }

      return (float)Math.atan2(local1732_7, local1732_6);
   }

   public static void method1264(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float local1733_3,
      float local1733_4,
      float local1733_5,
      float local1733_6,
      float local1733_7,
      float local1733_8,
      float local1733_9,
      int local1733_10,
      float local1733_11,
      float local1733_12
   ) {
      if (drawContext != null && !(local1733_11 <= 5.0E-4F)) {
         List<Vector2f> local1733_13 = method1261(local1733_3, local1733_4, local1733_5, local1733_6, local1733_7, local1733_8, local1733_9);
         if (local1733_13 != null) {
            ScreenRect screenRect = ScissorStack.method3969();
            ScreenRect screenRect2 = ShapeDrawing.method3661(
               matrix3x2f,
               screenRect,
               local1733_3 - local1733_5 - 2.0F,
               local1733_4 - local1733_5 - 2.0F,
               local1733_5 * 2.0F + 4.0F,
               local1733_5 * 2.0F + 4.0F
            );
            if (screenRect2 != null) {
               float local1733_16 = 0.0F;
               float local1733_17 = 0.0F;

               for (Vector2f vector2f : local1733_13) {
                  local1733_16 += vector2f.x / local1733_13.size();
                  local1733_17 += vector2f.y / local1733_13.size();
               }

               boolean local1733_18 = newTextureSetup != null;
               ((DrawContextMixin)drawContext)
                  .getGuiRenderState()
                  .addSimpleElement(
                     new PieShapeRenderer.Nested0378(
                        local1733_18 ? newTextureSetup : TextureSetup.empty(),
                        new Matrix3x2f(matrix3x2f),
                        local1733_13,
                        local1733_16,
                        local1733_17,
                        ShapeDrawing.method3649(local1733_10, local1733_11),
                        local1733_12,
                        local1733_18,
                        screenRect,
                        screenRect2
                     )
                  );
            }
         }
      }
   }

   private static List<Vector2f> method1265(float local1734_0, float local1734_1, float local1734_2, float local1734_3) {
      float local1734_4 = (float)Math.toRadians(local1734_1) / 2.0F;
      float local1734_5 = (float)Math.sin(local1734_4);
      if (local1734_5 < 1.0E-4F) {
         return null;
      }

      float local1734_6 = Math.max(0.0F, local1734_2) / 2.0F;
      float local1734_7 = Math.max(0.5F, Math.min(local1734_3, (local1734_0 - local1734_6) * 0.45F));
      float local1734_8 = local1734_6 + local1734_7;
      float local1734_9 = local1734_0 - local1734_7;
      float local1734_10 = local1734_8 / local1734_5;
      if (!(local1734_9 <= 0.0F) && !(local1734_10 >= local1734_9)) {
         float local1734_11 = (float)Math.cos(local1734_4);
         float local1734_12 = (float)Math.sin(local1734_4);
         float local1734_13 = local1734_10 * local1734_11;
         float local1734_14 = -local1734_13
            + (float)Math.sqrt(Math.max(0.0F, local1734_13 * local1734_13 - local1734_10 * local1734_10 + local1734_9 * local1734_9));
         float local1734_15 = (float)Math.atan2(local1734_12 * local1734_14, local1734_10 + local1734_11 * local1734_14);
         if (local1734_15 <= 0.0F) {
            return null;
         }

         List<Vector2f> local1734_16 = new ArrayList<>();
         local1734_16.add(new Vector2f(local1734_10, 0.0F));
         int local1734_17 = Math.max(2, Math.round((float)Math.toDegrees(local1734_15 * 2.0F) / 4.0F));

         for (int index = 0; index <= local1734_17; index++) {
            float local1734_19 = -local1734_15 + local1734_15 * 2.0F * index / local1734_17;
            local1734_16.add(new Vector2f(local1734_9 * (float)Math.cos(local1734_19), local1734_9 * (float)Math.sin(local1734_19)));
         }

         return method1266(local1734_16, local1734_7);
      } else {
         return null;
      }
   }

   private static List<Vector2f> method1266(List<Vector2f> local1735_0, float local1735_1) {
      int local1735_2 = local1735_0.size();
      float local1735_3 = 0.0F;
      float local1735_4 = 0.0F;

      for (Vector2f vector2f : local1735_0) {
         local1735_3 += vector2f.x / local1735_2;
         local1735_4 += vector2f.y / local1735_2;
      }

      List<Vector2f> local1735_5 = new ArrayList<>();

      for (int index = 0; index < local1735_2; index++) {
         Vector2f vector2f2 = local1735_0.get((index - 1 + local1735_2) % local1735_2);
         Vector2f vector2f3 = local1735_0.get(index);
         Vector2f vector2f4 = local1735_0.get((index + 1) % local1735_2);
         float local1735_10 = method1263(vector2f2, vector2f3, local1735_3, local1735_4);
         float local1735_11 = method1263(vector2f3, vector2f4, local1735_3, local1735_4);
         float local1735_12 = local1735_11 - local1735_10;

         while (local1735_12 <= (float) -Math.PI) {
            local1735_12 += (float) (Math.PI * 2);
         }

         while (local1735_12 > (float) Math.PI) {
            local1735_12 -= (float) (Math.PI * 2);
         }

         int local1735_13 = Math.max(1, Math.round(Math.abs(local1735_12) / 0.2F));

         for (int index2 = 0; index2 <= local1735_13; index2++) {
            float local1735_15 = local1735_10 + local1735_12 * index2 / local1735_13;
            local1735_5.add(
               new Vector2f(vector2f3.x + (float)Math.cos(local1735_15) * local1735_1, vector2f3.y + (float)Math.sin(local1735_15) * local1735_1)
            );
         }
      }

      return local1735_5;
   }

   private PieShapeRenderer() {
   }

   record Nested0378(
      TextureSetup textureSetup,
      Matrix3x2f pose,
      List<Vector2f> shape,
      float anchorX,
      float anchorY,
      int color,
      float tintMix,
      boolean blur,
      ScreenRect scissorArea,
      ScreenRect bounds
   ) implements SimpleGuiElementRenderState {
      @Override
      public RenderPipeline pipeline() {
         return this.blur ? ShapePipelines.PIE_BLUR : ShapePipelines.PIE;
      }

      @Override
      public void setupVertices(VertexConsumer vertexConsumer) {
         int local1740_2 = this.shape.size();

         for (int index = 0; index < local1740_2; index++) {
            Vector2f vector2f = this.shape.get(index);
            Vector2f vector2f2 = this.shape.get((index + 1) % local1740_2);
            this.emit(vertexConsumer, this.anchorX, this.anchorY, 0.0F);
            this.emit(vertexConsumer, this.anchorX, this.anchorY, 0.0F);
            this.emit(vertexConsumer, vector2f2.x, vector2f2.y, 1.0F);
            this.emit(vertexConsumer, vector2f.x, vector2f.y, 1.0F);
         }
      }

      private void emit(VertexConsumer vertexConsumer, float local1747_2, float local1747_3, float local1747_4) {
         vertexConsumer.vertex(this.pose, local1747_2, local1747_3)
            .color(this.color)
            .texture(local1747_4, 0.0F)
            .overlay(0, 0)
            .light(0, 0)
            .normal(0.0F, 0.0F, 0.0F)
            .lineWidth(this.tintMix);
      }
   }
}
