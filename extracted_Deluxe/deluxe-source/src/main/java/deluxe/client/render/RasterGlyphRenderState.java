package deluxe.client.render;

import deluxe.client.render.RasterGlyphQuad;
import deluxe.client.render.PixelSnap;
import deluxe.client.render.FontVertexQuad;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class RasterGlyphRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field14437;
   private final FontTexture fontTexture;
   private final ScreenRect field14439;
   private final RenderPipeline field14440;
   private final List<FontVertexQuad> field14441 = new ArrayList<>(128);
   private float field14442 = Float.MAX_VALUE;
   private float field14443 = Float.MAX_VALUE;
   private float field14444 = -Float.MAX_VALUE;
   private float field14445 = -Float.MAX_VALUE;

   @Override
   public ScreenRect bounds() {
      if (this.field14441.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7083_1 = (int)Math.floor(this.field14442);
      int local7083_2 = (int)Math.floor(this.field14443);
      int local7083_3 = Math.max(1, (int)Math.ceil(this.field14444 - this.field14442));
      int local7083_4 = Math.max(1, (int)Math.ceil(this.field14445 - this.field14443));
      ScreenRect screenRect = new ScreenRect(local7083_1, local7083_2, local7083_3, local7083_4).transformEachVertex(this.field14437);
      return this.field14439 == null ? screenRect : this.field14439.intersection(screenRect);
   }

   private void vertex(VertexConsumer vertexConsumer, float local7084_2, float local7084_3, float local7084_4, float local7084_5, int local7084_6) {
      vertexConsumer.vertex(this.field14437, local7084_2, local7084_3).texture(local7084_4, local7084_5).color(local7084_6);
   }

   private void include(float local7085_1, float local7085_2) {
      this.field14442 = Math.min(this.field14442, local7085_1);
      this.field14443 = Math.min(this.field14443, local7085_2);
      this.field14444 = Math.max(this.field14444, local7085_1);
      this.field14445 = Math.max(this.field14445, local7085_2);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field14439;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (FontVertexQuad fontVertexQuad : this.field14441) {
         this.vertex(vertexConsumer, fontVertexQuad.x0(), fontVertexQuad.y0(), fontVertexQuad.u0(), fontVertexQuad.v0(), fontVertexQuad.colorTopLeft());
         this.vertex(vertexConsumer, fontVertexQuad.x1(), fontVertexQuad.y1(), fontVertexQuad.u0(), fontVertexQuad.v1(), fontVertexQuad.colorBottomLeft());
         this.vertex(
            vertexConsumer, fontVertexQuad.method0508(), fontVertexQuad.method0510(), fontVertexQuad.u1(), fontVertexQuad.v1(), fontVertexQuad.colorBottomRight()
         );
         this.vertex(
            vertexConsumer, fontVertexQuad.method1348(), fontVertexQuad.method1346(), fontVertexQuad.u1(), fontVertexQuad.v0(), fontVertexQuad.colorTopRight()
         );
      }
   }

   void add(
      List<RasterGlyphQuad> local7088_1,
      float local7088_2,
      float local7088_3,
      int local7088_4,
      int local7088_5,
      int local7088_6,
      int local7088_7,
      float local7088_8,
      float local7088_9,
      float local7088_10,
      boolean local7088_11,
      boolean local7088_12,
      float local7088_13,
      float local7088_14,
      float local7088_15,
      float local7088_16,
      float local7088_17,
      boolean local7088_18
   ) {
      boolean local7088_19 = Math.abs(local7088_8) < 0.001F;
      if (local7088_19 && local7088_18) {
         local7088_2 = PixelSnap.method3300(local7088_2);
         local7088_3 = PixelSnap.method3300(local7088_3);
      }

      float local7088_20 = (float)Math.toRadians(local7088_8);
      float local7088_21 = (float)Math.sin(local7088_20);
      float local7088_22 = (float)Math.cos(local7088_20);

      for (RasterGlyphQuad rasterGlyphQuad : local7088_1) {
         float local7088_25 = rasterGlyphQuad.x0() + local7088_2;
         float local7088_26 = rasterGlyphQuad.y0() + local7088_3;
         float local7088_27 = rasterGlyphQuad.x1() + local7088_2;
         float local7088_28 = rasterGlyphQuad.y1() + local7088_3;
         float[] floatArray = this.rotate(local7088_25, local7088_26, local7088_9, local7088_10, local7088_21, local7088_22);
         float[] floatArray2 = this.rotate(local7088_25, local7088_28, local7088_9, local7088_10, local7088_21, local7088_22);
         float[] floatArray3 = this.rotate(local7088_27, local7088_28, local7088_9, local7088_10, local7088_21, local7088_22);
         float[] floatArray4 = this.rotate(local7088_27, local7088_26, local7088_9, local7088_10, local7088_21, local7088_22);
         this.field14441
            .add(
               new FontVertexQuad(
                  floatArray[0],
                  floatArray[1],
                  floatArray2[0],
                  floatArray2[1],
                  floatArray3[0],
                  floatArray3[1],
                  floatArray4[0],
                  floatArray4[1],
                  rasterGlyphQuad.u0(),
                  rasterGlyphQuad.v0(),
                  rasterGlyphQuad.u1(),
                  rasterGlyphQuad.v1(),
                  this.fadeColor(local7088_4, floatArray[0], local7088_11, local7088_12, local7088_13, local7088_14, local7088_15, local7088_16, local7088_17),
                  this.fadeColor(local7088_5, floatArray4[0], local7088_11, local7088_12, local7088_13, local7088_14, local7088_15, local7088_16, local7088_17),
                  this.fadeColor(local7088_6, floatArray3[0], local7088_11, local7088_12, local7088_13, local7088_14, local7088_15, local7088_16, local7088_17),
                  this.fadeColor(local7088_7, floatArray2[0], local7088_11, local7088_12, local7088_13, local7088_14, local7088_15, local7088_16, local7088_17)
               )
            );
         this.include(floatArray[0], floatArray[1]);
         this.include(floatArray2[0], floatArray2[1]);
         this.include(floatArray3[0], floatArray3[1]);
         this.include(floatArray4[0], floatArray4[1]);
      }
   }

   private float smoothstep(float local7089_1) {
      return local7089_1 * local7089_1 * (3.0F - 2.0F * local7089_1);
   }

   @Override
   public RenderPipeline pipeline() {
      return this.field14440;
   }

   RasterGlyphRenderState(Matrix3x2f matrix3x2f, FontTexture initialFontTexture, ScreenRect screenRect, RenderPipeline renderPipeline) {
      this.field14437 = new Matrix3x2f(matrix3x2f);
      this.fontTexture = initialFontTexture;
      this.field14439 = screenRect;
      this.field14440 = renderPipeline;
   }

   private float clamp(float local7092_1, float local7092_2, float local7092_3) {
      return Math.max(local7092_2, Math.min(local7092_3, local7092_1));
   }

   private int fadeColor(
      int local7093_1,
      float local7093_2,
      boolean local7093_3,
      boolean local7093_4,
      float local7093_5,
      float local7093_6,
      float local7093_7,
      float local7093_8,
      float local7093_9
   ) {
      if ((local7093_3 || local7093_4) && !(local7093_7 <= 0.0F) && !(local7093_6 <= local7093_5) && (!(local7093_8 <= 0.001F) || !(local7093_9 <= 0.001F))) {
         float local7093_10 = 1.0F;
         if (local7093_3) {
            float local7093_11 = this.smoothstep(this.clamp((local7093_2 - local7093_5) / local7093_7, 0.0F, 1.0F));
            local7093_10 = Math.min(local7093_10, 1.0F - this.clamp(local7093_8, 0.0F, 1.0F) * (1.0F - local7093_11));
         }

         if (local7093_4) {
            float local7093_11 = this.smoothstep(this.clamp((local7093_6 - local7093_2) / local7093_7, 0.0F, 1.0F));
            local7093_10 = Math.min(local7093_10, 1.0F - this.clamp(local7093_9, 0.0F, 1.0F) * (1.0F - local7093_11));
         }

         int local7093_11 = local7093_1 >>> 24 & 0xFF;
         return Math.round(local7093_11 * local7093_10) << 24 | local7093_1 & 16777215;
      } else {
         return local7093_1;
      }
   }

   @Override
   public TextureSetup textureSetup() {
      return this.fontTexture.method3529();
   }

   private float[] rotate(float local7095_1, float local7095_2, float local7095_3, float local7095_4, float local7095_5, float local7095_6) {
      float local7095_7 = local7095_1 - local7095_3;
      float local7095_8 = local7095_2 - local7095_4;
      return new float[]{
         local7095_3 + local7095_7 * local7095_6 - local7095_8 * local7095_5, local7095_4 + local7095_7 * local7095_5 + local7095_8 * local7095_6
      };
   }
}
