package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class MenuBackgroundRenderState extends ScissorAreaHelper {
   private final float radius;
   private final float y0;
   private final int mode;
   private final int primaryColor;
   private final int secondaryColor;
   private final float y1;
   private final float x0;
   private final float x1;
   private final float seconds;

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.MENU_BACKGROUND;
   }

   private void addVertex(VertexConsumer vertexConsumer, float value, float value2, float value3, float value4, int value5, int value6, int value7, float width) {
      float value8 = this.transformX(value, value2);
      float intermediate = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value8, intermediate, this.seconds);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setColor(this.primaryColor);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setUv(value3, value4);
      VertexConsumer vertexConsumer5 = vertexConsumer4.setUv1(value5, value6).setUv2(this.mode, value7);
      VertexConsumer vertexConsumer6 = vertexConsumer5.setNormal(0.0F, 0.0F, 1.0F);
      vertexConsumer6.setLineWidth(width);
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      int color = -1;
      int value = 0;
      color = (byte)1;
      value = (byte)0;
      value = this.secondaryColor >> 16 & 0xFF;
      color = this.secondaryColor >> 8 & 0xFF;
      int value2 = PackSizeHelper.packU8Pair(value, color);
      value = this.secondaryColor & 0xFF;
      color = this.secondaryColor >>> 24 & 0xFF;
      int value3 = PackSizeHelper.packU8Pair(value, color);
      float x = this.x1 - this.x0;
      float maximum = Math.max(1.0F, x);
      float y = this.y1 - this.y0;
      float maximum2 = Math.max(1.0F, y);
      int computedValue = PackSizeHelper.packRadius(maximum / maximum2);
      float computedValue2 = this.radius / maximum2;
      float minimum = Math.min(0.5F, computedValue2);
      this.addVertex(vertexConsumer, this.x0, this.y0, 0.0F, 0.0F, value2, value3, computedValue, minimum);
      this.addVertex(vertexConsumer, this.x0, this.y1, 0.0F, 1.0F, value2, value3, computedValue, minimum);
      this.addVertex(vertexConsumer, this.x1, this.y1, 1.0F, 1.0F, value2, value3, computedValue, minimum);
      this.addVertex(vertexConsumer, this.x1, this.y0, 1.0F, 0.0F, value2, value3, computedValue, minimum);
   }

   public MenuBackgroundRenderState(
      Matrix3x2fc matrix3x2fc, float x02, float y02, float x12, float y12, float seconds2, int mode2, float radius2, int primaryColor2, int secondaryColor2, ScreenRectangle screenRectangle
   ) {
      super(matrix3x2fc, screenRectangle, x02, y02, x12 - x02, y12 - y02);
      this.x0 = x02;
      this.y0 = y02;
      this.x1 = x12;
      this.y1 = y12;
      this.seconds = seconds2;
      this.mode = mode2;
      this.radius = radius2;
      this.primaryColor = primaryColor2;
      this.secondaryColor = secondaryColor2;
   }
}
