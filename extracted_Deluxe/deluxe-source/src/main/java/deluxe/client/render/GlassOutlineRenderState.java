package deluxe.client.render;

import deluxe.client.render.BlurDrawBatch;
import deluxe.client.render.GlassOutlineDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GlassOutlineRenderState implements SimpleGuiElementRenderState {
   private final GlassOutlineDrawData glassOutlineDrawData;
   private final Matrix3x2f field08503;
   private final ScreenRect field08504;
   private final ScreenRect field08505;
   private final BlurDrawBatch blurDrawBatch;

   private void vertex(VertexConsumer vertexConsumer, float local3899_2, float local3899_3, int local3899_4, int local3899_5, int local3899_6) {
      vertexConsumer.vertex(this.field08503, local3899_2, local3899_3).color(local3899_4, local3899_5, 255, 255).lineWidth(local3899_6 + 1);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.blurDrawBatch.field11735;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local3901_2 = OutlineGlassRenderer.method2432().method2425(this.glassOutlineDrawData, this.blurDrawBatch);
      if (local3901_2 >= 0) {
         float local3901_3 = this.glassOutlineDrawData.x();
         float local3901_4 = this.glassOutlineDrawData.y();
         float local3901_5 = this.glassOutlineDrawData.x() + this.glassOutlineDrawData.width();
         float local3901_6 = this.glassOutlineDrawData.y() + this.glassOutlineDrawData.height();
         this.vertex(vertexConsumer, local3901_3, local3901_4, 0, 0, local3901_2);
         this.vertex(vertexConsumer, local3901_3, local3901_6, 0, 255, local3901_2);
         this.vertex(vertexConsumer, local3901_5, local3901_6, 255, 255, local3901_2);
         this.vertex(vertexConsumer, local3901_5, local3901_4, 255, 0, local3901_2);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field08504;
   }

   GlassOutlineRenderState(Matrix3x2f matrix3x2f, GlassOutlineDrawData initialGlassOutlineDrawData, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch) {
      this.glassOutlineDrawData = initialGlassOutlineDrawData;
      this.field08503 = new Matrix3x2f(matrix3x2f);
      this.field08504 = screenRect;
      this.blurDrawBatch = initialBlurDrawBatch;
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialGlassOutlineDrawData.x()), Math.round(initialGlassOutlineDrawData.y()), Math.round(initialGlassOutlineDrawData.width()), Math.round(initialGlassOutlineDrawData.height())
         )
         .transformEachVertex(this.field08503);
      this.field08505 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public RenderPipeline pipeline() {
      return OutlineGlassRenderer.field08405;
   }

   @Override
   public ScreenRect bounds() {
      return this.field08505;
   }
}
