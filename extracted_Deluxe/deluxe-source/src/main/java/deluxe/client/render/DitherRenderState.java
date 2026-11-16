package deluxe.client.render;

import deluxe.client.render.DitherDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class DitherRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field15109;
   private final ScreenRect field15110;
   private final List<DitherDrawData> field15111 = new ArrayList<>(4);
   private float field15112 = Float.MAX_VALUE;
   private float field15113 = Float.MAX_VALUE;
   private float field15114 = -Float.MAX_VALUE;
   private float field15115 = -Float.MAX_VALUE;

   void add(DitherDrawData ditherDrawData) {
      this.field15111.add(ditherDrawData);
      this.field15112 = Math.min(this.field15112, ditherDrawData.x());
      this.field15113 = Math.min(this.field15113, ditherDrawData.y());
      this.field15114 = Math.max(this.field15114, ditherDrawData.x() + ditherDrawData.width());
      this.field15115 = Math.max(this.field15115, ditherDrawData.y() + ditherDrawData.height());
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field15110;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (DitherDrawData ditherDrawData : this.field15111) {
         int local7462_4 = DitherRenderer.method3358().method3347(ditherDrawData);
         if (local7462_4 >= 0) {
            float local7462_5 = ditherDrawData.x();
            float local7462_6 = ditherDrawData.y();
            float local7462_7 = ditherDrawData.x() + ditherDrawData.width();
            float local7462_8 = ditherDrawData.y() + ditherDrawData.height();
            int local7462_9 = ditherDrawData.color();
            this.vertex(vertexConsumer, local7462_5, local7462_6, 0.0F, 0.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_5, local7462_8, 0.0F, 1.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_7, local7462_8, 1.0F, 1.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_7, local7462_6, 1.0F, 0.0F, local7462_9, local7462_4);
         }
      }
   }

   @Override
   public ScreenRect bounds() {
      if (this.field15110 != null) {
         return this.field15110;
      }

      if (this.field15111.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7463_1 = (int)Math.floor(this.field15112);
      int local7463_2 = (int)Math.floor(this.field15113);
      int local7463_3 = Math.max(1, (int)Math.ceil(this.field15114 - this.field15112));
      int local7463_4 = Math.max(1, (int)Math.ceil(this.field15115 - this.field15113));
      return new ScreenRect(local7463_1, local7463_2, local7463_3, local7463_4).transformEachVertex(this.field15109);
   }

   DitherRenderState(Matrix3x2f matrix3x2f, ScreenRect screenRect) {
      this.field15109 = new Matrix3x2f(matrix3x2f);
      this.field15110 = screenRect;
   }

   private void vertex(VertexConsumer vertexConsumer, float local7465_2, float local7465_3, float local7465_4, float local7465_5, int local7465_6, int local7465_7) {
      vertexConsumer.vertex(this.field15109, local7465_2, local7465_3).texture(local7465_4, local7465_5).color(local7465_6).lineWidth(local7465_7 + 1);
   }

   @Override
   public RenderPipeline pipeline() {
      return DitherRenderer.field11774;
   }
}
