package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class VertexHelper extends ScissorAreaHelper implements FontRenderState {
   private final float y;
   private final float x;
   private final NeedsFallbackHelper layout;

   private void vertex(VertexConsumer vertexConsumer, float value, float value2, float value3, float value4, int color, int value5, float value6) {
      float value7 = this.transformX(value, value2);
      float intermediate = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value7, intermediate, value6).setColor(color);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setUv(value3, value4).setUv1(0, 0).setUv2(value5, 0);
      vertexConsumer3.setNormal(0.0F, 0.0F, 0.0F).setLineWidth(0.0F);
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.TEXT;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.layout.font().textureSetup();
   }

   @Override
   public TextHeightHelper font() {
      return this.layout.font();
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float size = this.layout.size();
      float y2 = this.y + this.layout.font().ascender(size);
      float color = this.layout.font().localPxPerSdfUnit(size);
      int color2 = PackSizeHelper.packRadius(this.layout.font().distanceRange());

      for (NeedsFallbackHelper.Glyph glyph : this.layout.glyphs()) {
         HeightHelper heightHelper = glyph.rendered();
         if (heightHelper != null && heightHelper.hasGeometry()) {
            float x2 = this.x + glyph.x();
            float color3 = x2 + heightHelper.left() * size;
            float color4 = x2 + heightHelper.right() * size;
            float color5 = y2 - heightHelper.top() * size;
            float color6 = y2 - heightHelper.bottom() * size;
            this.vertex(vertexConsumer, color3, color5, heightHelper.minU(), heightHelper.minV(), glyph.color(), color2, color);
            this.vertex(vertexConsumer, color3, color6, heightHelper.minU(), heightHelper.maxV(), glyph.color(), color2, color);
            this.vertex(vertexConsumer, color4, color6, heightHelper.maxU(), heightHelper.maxV(), glyph.color(), color2, color);
            this.vertex(vertexConsumer, color4, color5, heightHelper.maxU(), heightHelper.minV(), glyph.color(), color2, color);
         }
      }
   }

   public VertexHelper(Matrix3x2fc matrix3x2fc, float x2, float y2, NeedsFallbackHelper layout2, ScreenRectangle screenRectangle) {
      super(matrix3x2fc, screenRectangle, x2 - 2.0F, y2 - 2.0F, layout2.width() + 4.0F, layout2.font().lineHeight(layout2.size()) + 4.0F);
      this.layout = layout2;
      this.x = x2;
      this.y = y2;
   }
}
