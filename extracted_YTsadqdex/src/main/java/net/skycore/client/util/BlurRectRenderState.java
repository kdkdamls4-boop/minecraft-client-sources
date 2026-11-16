package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class BlurRectRenderState extends ScissorAreaHelper {
   private final float x0;
   private final float y0;
   private final float opacity;
   private final float radius;
   private final int tintColor;
   private final float y1;
   private final float x1;
   private final TextureSetup textureSetup;

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      int value = 0;
      value = (byte)0;
      float x = (this.x1 - this.x0) * 0.5F;
      float y = (this.y1 - this.y0) * 0.5F;
      int computedValue = PackSizeHelper.packSize(x * 2.0F);
      int computedValue2 = PackSizeHelper.packSize(y * 2.0F);
      int value2 = PackSizeHelper.packRadius(this.radius);
      value = Math.round(this.opacity * 255.0F);
      int value3 = ClampByteHelper.clampByte(value);
      float computedValue3 = -x;
      float intermediate = -y;
      this.addVertex(vertexConsumer, this.x0, this.y0, computedValue3, intermediate, computedValue, computedValue2, value2, value3);
      float computedValue4 = -x;
      this.addVertex(vertexConsumer, this.x0, this.y1, computedValue4, y, computedValue, computedValue2, value2, value3);
      this.addVertex(vertexConsumer, this.x1, this.y1, x, y, computedValue, computedValue2, value2, value3);
      this.addVertex(vertexConsumer, this.x1, this.y0, x, -y, computedValue, computedValue2, value2, value3);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup;
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.BLUR_RECT;
   }

   private void addVertex(VertexConsumer vertexConsumer, float value, float value2, float value3, float value4, int value5, int value6, int value7, int value8) {
      float value9 = this.transformX(value, value2);
      float intermediate = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value9, intermediate, 0.0F);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setColor(this.tintColor);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setUv(value3, value4);
      VertexConsumer vertexConsumer5 = vertexConsumer4.setUv1(value5, value6);
      vertexConsumer5.setUv2(value7, value8).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(0.0F);
   }

   public BlurRectRenderState(
      Matrix3x2fc matrix3x2fc,
      float x02,
      float y02,
      float x12,
      float y12,
      int tintColor2,
      float radius2,
      float radius3,
      float opacity2,
      GpuTextureView gpuTextureView,
      ScreenRectangle screenRectangle
   ) {
      super(matrix3x2fc, screenRectangle, x02, y02, x12 - x02, y12 - y02);
      this.x0 = x02;
      this.y0 = y02;
      this.x1 = x12;
      this.y1 = y12;
      this.tintColor = tintColor2;
      this.radius = radius2;
      this.opacity = opacity2;
      this.textureSetup = TextureSetup.singleTexture(gpuTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
      RunPassHelper.requestBlurRadius(radius3);
   }
}
