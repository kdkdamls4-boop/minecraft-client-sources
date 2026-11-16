package deluxe.client.render;

import deluxe.client.render.Outline360DrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class Outline360RenderState implements SimpleGuiElementRenderState {
   private final Outline360DrawData outline360DrawData;
   private final Matrix3x2f field09604;
   private final ScreenRect field09605;

   Outline360RenderState(Matrix3x2f matrix3x2f, Outline360DrawData initialOutline360DrawData) {
      this.outline360DrawData = initialOutline360DrawData;
      this.field09604 = new Matrix3x2f(matrix3x2f);
      this.field09605 = new ScreenRect(
            Math.round(initialOutline360DrawData.x()), Math.round(initialOutline360DrawData.y()), Math.round(initialOutline360DrawData.width()), Math.round(initialOutline360DrawData.height())
         )
         .transformEachVertex(this.field09604);
   }

   private void vertex(VertexConsumer vertexConsumer, float local4449_2, float local4449_3, int local4449_4, int local4449_5, int local4449_6) {
      vertexConsumer.vertex(this.field09604, local4449_2, local4449_3).color(local4449_4, local4449_5, 255, 255).lineWidth(local4449_6 + 1);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local4450_2 = Outline360Renderer.method2731().method2730(this.outline360DrawData);
      if (local4450_2 >= 0) {
         float local4450_3 = this.outline360DrawData.x();
         float local4450_4 = this.outline360DrawData.y();
         float local4450_5 = this.outline360DrawData.x() + this.outline360DrawData.width();
         float local4450_6 = this.outline360DrawData.y() + this.outline360DrawData.height();
         this.vertex(vertexConsumer, local4450_3, local4450_4, 0, 0, local4450_2);
         this.vertex(vertexConsumer, local4450_3, local4450_6, 0, 255, local4450_2);
         this.vertex(vertexConsumer, local4450_5, local4450_6, 255, 255, local4450_2);
         this.vertex(vertexConsumer, local4450_5, local4450_4, 255, 0, local4450_2);
      }
   }

   @Override
   public ScreenRect bounds() {
      return this.field09605;
   }

   @Override
   public RenderPipeline pipeline() {
      return Outline360Renderer.field09310;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }
}
