package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

public final class BaselineYHelper extends ScissorAreaHelper implements FontRenderState {
   private final float offsetY;
   private final TextAlignment align;
   private final float letterSpacing;
   private final float outlineThickness;
   private final int outlineColor;
   private final float offsetX;
   private final float oblique;
   private final int color;
   private final float y;
   private final TextHeightHelper font;
   private final float x;
   private final float size;
   private final TextHeightHelper.Paragraph paragraph;

   public BaselineYHelper(
      Matrix3x2fc matrix3x2fc,
      TextHeightHelper font2,
      float x2,
      float y2,
      float size2,
      TextHeightHelper.Paragraph paragraph2,
      int color2,
      int outlineColor2,
      float outlineThickness2,
      float letterSpacing2,
      TextAlignment align2,
      float offsetX2,
      float offsetY2,
      float oblique2,
      ScreenRectangle screenRectangle
   ) {
      super(
         matrix3x2fc,
         screenRectangle,
         boundsX(x2, paragraph2.maxWidth(size2, letterSpacing2), align2) + offsetX2 - (outlineThickness2 + 2.0F),
         y2 + offsetY2 - (outlineThickness2 + 2.0F),
         paragraph2.maxWidth(size2, letterSpacing2) + (outlineThickness2 + 2.0F) * 2.0F,
         (paragraph2.lines().isEmpty() ? 0.0F : font2.lineHeight(size2) * paragraph2.lines().size()) + (outlineThickness2 + 2.0F) * 2.0F
      );
      this.font = font2;
      this.x = x2;
      this.y = y2;
      this.size = size2;
      this.paragraph = paragraph2;
      this.color = color2;
      this.outlineColor = outlineColor2;
      this.outlineThickness = outlineThickness2;
      this.letterSpacing = letterSpacing2;
      this.align = align2;
      this.offsetX = offsetX2;
      this.offsetY = offsetY2;
      this.oblique = oblique2;
   }

   private float baselineY(float value) {
      float y2 = this.y + this.offsetY;
      return y2 + this.font.ascender(this.size);
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float y2 = this.y + this.offsetY + this.font.ascender(this.size);
      float value = this.font.lineHeight(this.size);
      int value2 = PackSizeHelper.packU8Pair(this.outlineColor >> 16 & 0xFF, this.outlineColor >> 8 & 0xFF);
      int value3 = PackSizeHelper.packU8Pair(this.outlineColor & 0xFF, this.outlineColor >>> 24 & 0xFF);
      int value4 = PackSizeHelper.packRadius(this.font.distanceRange());
      float value5 = this.font.localPxPerSdfUnit(this.size);
      float value6 = this.outlineThickness > 0.0F ? this.outlineThickness : 0.0F;
      float computedValue = value6 / this.size;
      float computedValue2 = value6 * (this.font.atlasSize() / this.size) / this.font.atlasWidth();
      float computedValue3 = value6 * (this.font.atlasSize() / this.size) / this.font.atlasHeight();
      var lines = this.paragraph.lines();

      for (int value7 = 0; value7 < lines.size(); value7++) {
         TextHeightHelper.Line line = (TextHeightHelper.Line)lines.get(value7);
         float index = this.alignedX(line.width(this.size, this.letterSpacing)) + this.offsetX;
         float index2 = y2 + value * value7;

         for (int value8 = 0; value8 < line.glyphCount(); value8++) {
            HeightHelper heightHelper = line.glyphAt(value8);
            if (heightHelper.hasGeometry()) {
               float computedValue4 = index + line.penX(value8, this.size, this.letterSpacing);
               float computedValue5 = computedValue4 + (heightHelper.left() - computedValue) * this.size;
               float computedValue6 = computedValue4 + (heightHelper.right() + computedValue) * this.size;
               float computedValue7 = index2 - (heightHelper.top() + computedValue) * this.size;
               float computedValue8 = index2 - (heightHelper.bottom() - computedValue) * this.size;
               float computedValue9 = heightHelper.minU() - computedValue2;
               float computedValue10 = heightHelper.maxU() + computedValue2;
               float computedValue11 = heightHelper.minV() - computedValue3;
               float computedValue12 = heightHelper.maxV() + computedValue3;
               this.vertex(vertexConsumer, computedValue5, computedValue7, computedValue9, computedValue11, value2, value3, value4, value5);
               this.vertex(vertexConsumer, computedValue5, computedValue8, computedValue9, computedValue12, value2, value3, value4, value5);
               this.vertex(vertexConsumer, computedValue6, computedValue8, computedValue10, computedValue12, value2, value3, value4, value5);
               this.vertex(vertexConsumer, computedValue6, computedValue7, computedValue10, computedValue11, value2, value3, value4, value5);
            }
         }
      }
   }

   private static float boundsX(float value, float value2, TextAlignment textAlignment) {
      return switch (textAlignment) {
         case LEFT -> value;
         case CENTER -> value - value2 * 0.5F;
         case RIGHT -> value - value2;
         default -> {
            throw new MatchException(null, null);
         }
      };
   }

   private void vertex(VertexConsumer vertexConsumer, float value, float value2, float value3, float value4, int value5, int value6, int value7, float value8) {
      float computedValue = this.baselineY(value2) - value2;
      float computedValue2 = value + computedValue * this.oblique;
      float value9 = this.transformX(computedValue2, value2);
      float intermediate = this.transformY(computedValue2, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value9, intermediate, value8).setColor(this.color);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setUv(value3, value4).setUv1(value5, value6).setUv2(value7, 0);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setNormal(0.0F, 0.0F, 0.0F);
      vertexConsumer4.setLineWidth(this.outlineThickness);
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.TEXT;
   }

   private float alignedX(float value) {
      float result;
      switch (this.align) {
         case LEFT:
            result = this.x;
            break;
         case CENTER:
            result = this.x - value * 0.5F;
            break;
         case RIGHT:
            result = this.x - value;
            break;
         default:
            throw new MatchException(null, null);
      }

      return result;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.font.textureSetup();
   }

   @Override
   public TextHeightHelper font() {
      return this.font;
   }
}
