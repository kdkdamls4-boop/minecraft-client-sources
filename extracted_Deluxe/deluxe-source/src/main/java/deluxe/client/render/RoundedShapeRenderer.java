package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.CornerRadii;
import deluxe.client.render.ShapePipelines;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;

public final class RoundedShapeRenderer {
   static final int field00237 = 2;
   static final int field00238 = 3;
   static final int field00239 = 6;

   private RoundedShapeRenderer() {
   }

   private static float method0505(int local628_0) {
      if (local628_0 == 6) {
         return 1.0F;
      } else {
         return local628_0 == 3 ? 0.0F : -1.0F;
      }
   }

   public static void method0506(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float local629_3,
      float local629_4,
      float local629_5,
      float local629_6,
      float local629_7,
      float local629_8,
      float local629_9,
      float local629_10,
      CornerRadii cornerRadii,
      float local629_12,
      float local629_13,
      float local629_14,
      float local629_15,
      int local629_16,
      int local629_17,
      int local629_18,
      boolean local629_19
   ) {
      if (drawContext != null && newTextureSetup != null && !(local629_5 <= 0.0F) && !(local629_6 <= 0.0F) && !(local629_15 <= 5.0E-4F)) {
         float local629_20 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
         float local629_21 = local629_18 != 6 && local629_18 != 3 ? local629_12 * local629_20 : local629_12;
         ScreenRect screenRect = ScissorStack.method3969();
         ScreenRect screenRect2 = ShapeDrawing.method3661(matrix3x2f, screenRect, local629_3, local629_4, local629_5, local629_6);
         if (screenRect2 != null) {
            int local629_24 = ShapeDrawing.method3649(local629_16, local629_15);
            int local629_25 = ShapeDrawing.method3649(local629_17, local629_15);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(
                  new RoundedShapeRenderer.Nested0352(
                     newTextureSetup,
                     new Matrix3x2f(matrix3x2f),
                     local629_3,
                     local629_4,
                     local629_3 + local629_5,
                     local629_4 + local629_6,
                     local629_7,
                     local629_8,
                     local629_9,
                     local629_10,
                     local629_24,
                     local629_25,
                     ShapeDrawing.method3658(cornerRadii.getTopLeft() * local629_20, cornerRadii.isRelative()),
                     ShapeDrawing.method3658(cornerRadii.getTopRight() * local629_20, cornerRadii.isRelative()),
                     ShapeDrawing.method3658(cornerRadii.getBottomRight() * local629_20, cornerRadii.isRelative()),
                     ShapeDrawing.method3658(cornerRadii.getBottomLeft() * local629_20, cornerRadii.isRelative()),
                     local629_21,
                     MathHelper.clamp(local629_13, -1.0F, 1.0F),
                     MathHelper.clamp(local629_14, -1.0F, 1.0F),
                     method0505(local629_18),
                     local629_19 && local629_18 == 3,
                     screenRect,
                     screenRect2
                  )
               );
         }
      }
   }

   record Nested0352(
      TextureSetup textureSetup,
      Matrix3x2f pose,
      float x1,
      float y1,
      float method0508,
      float method0510,
      float u1,
      float v1,
      float method0509,
      float method0514,
      int colorLeft,
      int colorRight,
      short rBR,
      short rTR,
      short rBL,
      short rTL,
      float value,
      float smoothStart,
      float smoothEnd,
      float typeSignal,
      boolean include2DBlur,
      ScreenRect scissorArea,
      ScreenRect bounds
   ) implements SimpleGuiElementRenderState {
      @Override
      public void setupVertices(VertexConsumer vertexConsumer) {
         this.emit(vertexConsumer, this.x1, this.y1, this.u1, this.v1, this.colorLeft);
         this.emit(vertexConsumer, this.x1, this.method0510, this.u1, this.method0514, this.colorLeft);
         this.emit(vertexConsumer, this.method0508, this.method0510, this.method0509, this.method0514, this.colorRight);
         this.emit(vertexConsumer, this.method0508, this.y1, this.method0509, this.v1, this.colorRight);
      }

      private void emit(VertexConsumer vertexConsumer, float local643_2, float local643_3, float local643_4, float local643_5, int local643_6) {
         vertexConsumer.vertex(this.pose, local643_2, local643_3)
            .color(local643_6)
            .texture(local643_4, local643_5)
            .overlay(this.rBR, this.rTR)
            .light(this.rBL, this.rTL)
            .normal(this.typeSignal, this.smoothStart, this.smoothEnd)
            .lineWidth(this.value);
      }

      @Override
      public RenderPipeline pipeline() {
         return this.include2DBlur ? ShapePipelines.TEXTURED_BLUR : ShapePipelines.TEXTURED;
      }
   }
}
