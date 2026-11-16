package deluxe.client.render;

import deluxe.client.render.BlurDrawBatch;
import deluxe.client.render.BlurDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class BlurRenderState implements SimpleGuiElementRenderState {
   private final BlurDrawData blurDrawData;
   private final Matrix3x2f field10622;
   private final ScreenRect field10623;
   private final ScreenRect field10624;
   private final BlurDrawBatch blurDrawBatch;

   @Override
   public TextureSetup textureSetup() {
      return this.blurDrawBatch.field11735;
   }

   private void vertex(VertexConsumer vertexConsumer, float local4957_2, float local4957_3, int local4957_4, int local4957_5) {
      vertexConsumer.vertex(this.field10622, local4957_2, local4957_3).color(local4957_4).lineWidth(local4957_5 + 1);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field10623;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local4959_2 = BatchedBlurRenderer.method4504().method4507(this.blurDrawData, this.blurDrawBatch);
      if (local4959_2 >= 0) {
         float local4959_3 = this.blurDrawData.x();
         float local4959_4 = this.blurDrawData.y();
         float local4959_5 = this.blurDrawData.x() + this.blurDrawData.width();
         float local4959_6 = this.blurDrawData.y() + this.blurDrawData.height();
         this.vertex(vertexConsumer, local4959_3, local4959_4, this.blurDrawData.colorTopLeft(), local4959_2);
         this.vertex(vertexConsumer, local4959_3, local4959_6, this.blurDrawData.colorBottomLeft(), local4959_2);
         this.vertex(vertexConsumer, local4959_5, local4959_6, this.blurDrawData.colorBottomRight(), local4959_2);
         this.vertex(vertexConsumer, local4959_5, local4959_4, this.blurDrawData.colorTopRight(), local4959_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return BatchedBlurRenderer.field15235;
   }

   BlurRenderState(Matrix3x2f matrix3x2f, BlurDrawData initialBlurDrawData, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch) {
      this.blurDrawData = initialBlurDrawData;
      this.field10622 = new Matrix3x2f(matrix3x2f);
      this.field10623 = screenRect;
      this.blurDrawBatch = initialBlurDrawBatch;
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialBlurDrawData.x()), Math.round(initialBlurDrawData.y()), Math.round(initialBlurDrawData.width()), Math.round(initialBlurDrawData.height())
         )
         .transformEachVertex(this.field10622);
      this.field10624 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect bounds() {
      return this.field10624;
   }
}
