package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class ShadowHelper extends ScissorAreaHelper {
   private final float x1;
   private final float y1;
   private final int bottomRightColor;
   private final float y0;
   private final float bottomLeftRadius;
   private final int topRightColor;
   private final float bottomRightRadius;
   private final int shadowColor;
   private final boolean shadow;
   private final int topLeftColor;
   private final float topRightRadius;
   private final int bottomLeftColor;
   private final float topLeftRadius;
   private boolean glassShadow;
   private final int borderColor;
   private final float borderThickness;
   private final float x0;
   private final float shadowBlur;

   public ShadowHelper(Matrix3x2fc matrix3x2fc, float value, float value2, float value3, float value4, int value5, float value6, float value7, int value8, ScreenRectangle screenRectangle) {
      this(matrix3x2fc, value, value2, value3, value4, value5, value5, value5, value5, value6, value6, value6, value6, value7, value8, 0.0F, 0, false, screenRectangle);
   }

   public ShadowHelper(
      Matrix3x2fc matrix3x2fc,
      float value,
      float value2,
      float value3,
      float value4,
      int value5,
      int value6,
      int value7,
      int value8,
      float value9,
      float value10,
      float value11,
      float value12,
      float value13,
      int value14,
      ScreenRectangle screenRectangle
   ) {
      this(matrix3x2fc, value, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, 0.0F, 0, false, screenRectangle);
   }

   public static ShadowHelper shadow(
      Matrix3x2fc matrix3x2fc,
      float value,
      float value2,
      float value3,
      float value4,
      float value5,
      float value6,
      float value7,
      float value8,
      float value9,
      int value10,
      ScreenRectangle screenRectangle
   ) {
      float maximum = Math.max(value9, 1.0F);
      return new ShadowHelper(matrix3x2fc, value, value2, value3, value4, 0, 0, 0, 0, value5, value6, value7, value8, 0.0F, 0, maximum, value10, true, screenRectangle);
   }

   private static float spread(float value) {
      return Math.max(value, 1.0F);
   }

   private void addVertex(
      VertexConsumer vertexConsumer,
      float value,
      float value2,
      float color,
      float color2,
      float value3,
      int value4,
      int value5,
      int value6,
      int value7,
      float width,
      float value8,
      float value9,
      float value10,
      int color3
   ) {
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(this.transformX(value, value2), this.transformY(value, value2), value3);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setColor(color3).setUv(color, color2);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setUv1(value4, value5).setUv2(value6, value7);
      VertexConsumer vertexConsumer5 = vertexConsumer4.setNormal(value8, value9, value10);
      vertexConsumer5.setLineWidth(width);
   }

   public ShadowHelper(
      Matrix3x2fc matrix3x2fc,
      float x02,
      float y02,
      float x12,
      float y12,
      int topLeftColor2,
      int bottomLeftColor2,
      int bottomRightColor2,
      int topRightColor2,
      float radius,
      float radius2,
      float radius3,
      float radius4,
      float borderThickness2,
      int borderColor2,
      float shadowBlur2,
      int shadowColor2,
      boolean shadow2,
      ScreenRectangle screenRectangle
   ) {
      super(
         matrix3x2fc,
         screenRectangle,
         shadow2 ? x02 - spread(shadowBlur2) : x02,
         shadow2 ? y02 - spread(shadowBlur2) : y02,
         x12 - x02 + (shadow2 ? spread(shadowBlur2) * 2.0F : 0.0F),
         y12 - y02 + (shadow2 ? spread(shadowBlur2) * 2.0F : 0.0F)
      );
      this.x0 = x02;
      this.y0 = y02;
      this.x1 = x12;
      this.y1 = y12;
      this.topLeftColor = topLeftColor2;
      this.bottomLeftColor = bottomLeftColor2;
      this.bottomRightColor = bottomRightColor2;
      this.topRightColor = topRightColor2;
      this.topLeftRadius = clampRadius(radius);
      this.topRightRadius = clampRadius(radius2);
      this.bottomRightRadius = clampRadius(radius3);
      this.bottomLeftRadius = clampRadius(radius4);
      this.borderThickness = borderThickness2;
      this.borderColor = borderColor2;
      this.shadowBlur = shadowBlur2;
      this.shadowColor = shadowColor2;
      this.shadow = shadow2;
   }

   @Override
   public RenderPipeline pipeline() {
      return this.glassShadow ? GuiPipelines.GLASS_SHADOW : GuiPipelines.RECT;
   }

   private static float clampRadius(float value) {
      return ClampByteHelper.clamp(value, 0.0F, 255.9375F);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public static ShadowHelper glassShadow(
      Matrix3x2fc matrix3x2fc,
      float value,
      float value2,
      float value3,
      float value4,
      float value5,
      float value6,
      float value7,
      float value8,
      float value9,
      int value10,
      ScreenRectangle screenRectangle
   ) {
      ShadowHelper shadowHelper = shadow(matrix3x2fc, value, value2, value3, value4, value5, value6, value7, value8, value9, value10, screenRectangle);
      shadowHelper.glassShadow = true;
      return shadowHelper;
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float x = (this.x1 - this.x0) * 0.5F;
      float y = (this.y1 - this.y0) * 0.5F;
      float value = this.shadow ? spread(this.shadowBlur) : 0.0F;
      int value2 = this.shadow ? this.shadowColor : this.borderColor;
      float color = PackSizeHelper.packZ(this.shadow ? this.shadowBlur : this.borderThickness, value2 >>> 24 & 0xFF, this.shadow);
      int color2 = PackSizeHelper.packSize(x * 2.0F);
      int color3 = PackSizeHelper.packSize(y * 2.0F);
      int color4 = PackSizeHelper.packRadius(this.topLeftRadius);
      int color5 = PackSizeHelper.packRadius(this.topRightRadius);
      float color6 = PackSizeHelper.packDual12(this.bottomRightRadius, this.bottomLeftRadius);
      float color7 = PackSizeHelper.snormChannel(value2 >> 16 & 0xFF);
      float color8 = PackSizeHelper.snormChannel(value2 >> 8 & 0xFF);
      float color9 = PackSizeHelper.snormChannel(value2 & 0xFF);
      float color10 = x + value;
      float color11 = y + value;
      this.addVertex(vertexConsumer, this.x0 - value, this.y0 - value, -color10, -color11, color, color2, color3, color4, color5, color6, color7, color8, color9, this.topLeftColor);
      this.addVertex(vertexConsumer, this.x0 - value, this.y1 + value, -color10, color11, color, color2, color3, color4, color5, color6, color7, color8, color9, this.bottomLeftColor);
      this.addVertex(vertexConsumer, this.x1 + value, this.y1 + value, color10, color11, color, color2, color3, color4, color5, color6, color7, color8, color9, this.bottomRightColor);
      this.addVertex(vertexConsumer, this.x1 + value, this.y0 - value, color10, -color11, color, color2, color3, color4, color5, color6, color7, color8, color9, this.topRightColor);
   }
}
