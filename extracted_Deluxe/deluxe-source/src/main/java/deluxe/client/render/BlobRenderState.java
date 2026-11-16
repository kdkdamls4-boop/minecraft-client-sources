package deluxe.client.render;

import deluxe.client.render.BlobDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class BlobRenderState implements SimpleGuiElementRenderState {
   private final BlobDrawData blobDrawData;
   private final Matrix3x2f field11031;
   private final ScreenRect field11032;
   private final ScreenRect field11033;

   @Override
   public RenderPipeline pipeline() {
      return BlobRenderer.field10394;
   }

   @Override
   public ScreenRect bounds() {
      return this.field11033;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local5213_2 = BlobRenderer.method3064().method3055(this.blobDrawData);
      if (local5213_2 >= 0) {
         float local5213_3 = this.blobDrawData.x();
         float local5213_4 = this.blobDrawData.y();
         float local5213_5 = this.blobDrawData.x() + this.blobDrawData.w();
         float local5213_6 = this.blobDrawData.y() + this.blobDrawData.h();
         this.vertex(vertexConsumer, local5213_3, local5213_4, 0, 0, local5213_2);
         this.vertex(vertexConsumer, local5213_3, local5213_6, 0, 255, local5213_2);
         this.vertex(vertexConsumer, local5213_5, local5213_6, 255, 255, local5213_2);
         this.vertex(vertexConsumer, local5213_5, local5213_4, 255, 0, local5213_2);
      }
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   BlobRenderState(Matrix3x2f matrix3x2f, BlobDrawData initialBlobDrawData, ScreenRect screenRect) {
      this.blobDrawData = initialBlobDrawData;
      this.field11031 = new Matrix3x2f(matrix3x2f);
      this.field11032 = screenRect;
      int local5215_4 = (int)Math.floor(initialBlobDrawData.x());
      int local5215_5 = (int)Math.floor(initialBlobDrawData.y());
      int local5215_6 = (int)Math.ceil(initialBlobDrawData.x() + initialBlobDrawData.w()) - local5215_4;
      int local5215_7 = (int)Math.ceil(initialBlobDrawData.y() + initialBlobDrawData.h()) - local5215_5;
      ScreenRect screenRect2 = new ScreenRect(local5215_4, local5215_5, local5215_6, local5215_7).transformEachVertex(this.field11031);
      this.field11033 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field11032;
   }

   private void vertex(VertexConsumer vertexConsumer, float local5217_2, float local5217_3, int local5217_4, int local5217_5, int local5217_6) {
      vertexConsumer.vertex(this.field11031, local5217_2, local5217_3).color(local5217_4, local5217_5, 255, 255).lineWidth(local5217_6 + 1);
   }
}
