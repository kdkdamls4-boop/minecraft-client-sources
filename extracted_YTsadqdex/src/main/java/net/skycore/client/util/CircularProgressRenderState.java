package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class CircularProgressRenderState extends ScissorAreaHelper {
   private final float thickness;
   private final float y;
   private final int waveStyle;
   private final int topColor;
   private final float progress;
   private static final int PROGRESS_SCALE = 32767;
   private final float size;
   private final int bottomColor;
   private final float x;

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float color = this.size * 0.5F;
      int color2 = PackSizeHelper.packSize(this.size);
      float computedValue = this.progress * 32767.0F;
      int roundedValue = Math.round(computedValue);
      float color3 = -color;
      float color4 = -color;
      this.addVertex(vertexConsumer, this.x, this.y, color3, color4, color2, roundedValue, this.topColor);
      float color5 = this.y + this.size;
      float color6 = -color;
      this.addVertex(vertexConsumer, this.x, color5, color6, color, color2, roundedValue, this.bottomColor);
      float color7 = this.x + this.size;
      color5 = this.y + this.size;
      this.addVertex(vertexConsumer, color7, color5, color, color, color2, roundedValue, this.bottomColor);
      float color8 = this.x + this.size;
      color4 = -color;
      this.addVertex(vertexConsumer, color8, this.y, color, color4, color2, roundedValue, this.topColor);
   }

   private void addVertex(VertexConsumer vertexConsumer, float value, float value2, float color, float color2, int value3, int value4, int color3) {
      float value5 = this.transformX(value, value2);
      float intermediate = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value5, intermediate, 0.0F).setColor(color3).setUv(color, color2);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setUv1(value3, value3);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setUv2(value4, this.waveStyle).setNormal(0.0F, 0.0F, 0.0F);
      vertexConsumer4.setLineWidth(this.thickness);
   }

   public CircularProgressRenderState(Matrix3x2fc matrix3x2fc, float x2, float y2, float value, float value2, int topColor2, int bottomColor2, float value3, int value4, ScreenRectangle screenRectangle) {
      super(matrix3x2fc, screenRectangle, x2, y2, value, value);
      this.x = x2;
      this.y = y2;
      this.size = Math.max(0.0F, value);
      this.thickness = Math.max(0.0F, value2);
      this.topColor = topColor2;
      this.bottomColor = bottomColor2;
      this.progress = Math.max(0.0F, Math.min(1.0F, value3));
      this.waveStyle = Math.clamp(value4, 0, 2);
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.RING;
   }

   public CircularProgressRenderState(Matrix3x2fc matrix3x2fc, float value, float value2, float value3, float value4, int value5, float value6, ScreenRectangle screenRectangle) {
      this(matrix3x2fc, value, value2, value3, value4, value5, value5, value6, 0, screenRectangle);
   }
}
