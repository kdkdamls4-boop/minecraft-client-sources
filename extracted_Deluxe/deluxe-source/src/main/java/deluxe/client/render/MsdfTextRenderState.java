package deluxe.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MsdfTextRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field04049;
   private final TextureSetup field04050;
   private final ScreenRect field04051;
   private final boolean field04052;
   private final float field04053;
   private final List<MsdfTextRenderState.Nested0379> field04054 = new ArrayList<>(128);
   private float field04055 = Float.MAX_VALUE;
   private float field04056 = Float.MAX_VALUE;
   private float field04057 = -Float.MAX_VALUE;
   private float field04058 = -Float.MAX_VALUE;

   @Override
   public TextureSetup textureSetup() {
      return this.field04050;
   }

   @Override
   public RenderPipeline pipeline() {
      return this.field04052 ? MsdfTextRenderer.field08325 : MsdfTextRenderer.field08324;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field04051;
   }

   private void include(float local1866_1, float local1866_2) {
      this.field04055 = Math.min(this.field04055, local1866_1);
      this.field04056 = Math.min(this.field04056, local1866_2);
      this.field04057 = Math.max(this.field04057, local1866_1);
      this.field04058 = Math.max(this.field04058, local1866_2);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      if (this.field04052) {
         for (MsdfTextRenderState.Nested0379 local1867_3 : this.field04054) {
            this.shimmerVertex(
               vertexConsumer,
               local1867_3.x0(),
               local1867_3.y0(),
               local1867_3.u0(),
               local1867_3.v0(),
               local1867_3.colorTopLeft(),
               local1867_3.method1345()
            );
            this.shimmerVertex(
               vertexConsumer,
               local1867_3.x1(),
               local1867_3.y1(),
               local1867_3.u0(),
               local1867_3.v1(),
               local1867_3.colorBottomLeft(),
               local1867_3.method1345()
            );
            this.shimmerVertex(
               vertexConsumer,
               local1867_3.method0508(),
               local1867_3.method0510(),
               local1867_3.u1(),
               local1867_3.v1(),
               local1867_3.colorBottomRight(),
               local1867_3.method1347()
            );
            this.shimmerVertex(
               vertexConsumer,
               local1867_3.method1348(),
               local1867_3.method1346(),
               local1867_3.u1(),
               local1867_3.v0(),
               local1867_3.colorTopRight(),
               local1867_3.method1347()
            );
         }
      } else {
         for (MsdfTextRenderState.Nested0379 local1867_3 : this.field04054) {
            this.vertex(vertexConsumer, local1867_3.x0(), local1867_3.y0(), local1867_3.u0(), local1867_3.v0(), local1867_3.colorTopLeft());
            this.vertex(vertexConsumer, local1867_3.x1(), local1867_3.y1(), local1867_3.u0(), local1867_3.v1(), local1867_3.colorBottomLeft());
            this.vertex(
               vertexConsumer,
               local1867_3.method0508(),
               local1867_3.method0510(),
               local1867_3.u1(),
               local1867_3.v1(),
               local1867_3.colorBottomRight()
            );
            this.vertex(
               vertexConsumer, local1867_3.method1348(), local1867_3.method1346(), local1867_3.u1(), local1867_3.v0(), local1867_3.colorTopRight()
            );
         }
      }
   }

   void addGlyph(
      float local1868_1,
      float local1868_2,
      float local1868_3,
      float local1868_4,
      float local1868_5,
      float local1868_6,
      float local1868_7,
      float local1868_8,
      int local1868_9,
      int local1868_10,
      int local1868_11,
      int local1868_12,
      float local1868_13,
      float local1868_14,
      float local1868_15,
      float local1868_16,
      float local1868_17
   ) {
      float local1868_18 = local1868_1;
      float local1868_19 = local1868_2;
      float local1868_20 = local1868_1;
      float local1868_21 = local1868_4;
      float local1868_22 = local1868_3;
      float local1868_23 = local1868_4;
      float local1868_24 = local1868_3;
      float local1868_25 = local1868_2;
      if (Math.abs(local1868_15) >= 0.001F) {
         float local1868_26 = (float)Math.toRadians(local1868_15);
         float local1868_27 = (float)Math.sin(local1868_26);
         float local1868_28 = (float)Math.cos(local1868_26);
         float[] floatArray = this.rotate(local1868_18, local1868_19, local1868_16, local1868_17, local1868_27, local1868_28);
         float[] floatArray2 = this.rotate(local1868_20, local1868_21, local1868_16, local1868_17, local1868_27, local1868_28);
         float[] floatArray3 = this.rotate(local1868_22, local1868_23, local1868_16, local1868_17, local1868_27, local1868_28);
         float[] floatArray4 = this.rotate(local1868_24, local1868_25, local1868_16, local1868_17, local1868_27, local1868_28);
         local1868_18 = floatArray[0];
         local1868_19 = floatArray[1];
         local1868_20 = floatArray2[0];
         local1868_21 = floatArray2[1];
         local1868_22 = floatArray3[0];
         local1868_23 = floatArray3[1];
         local1868_24 = floatArray4[0];
         local1868_25 = floatArray4[1];
      }

      this.field04054
         .add(
            new MsdfTextRenderState.Nested0379(
               local1868_18,
               local1868_19,
               local1868_20,
               local1868_21,
               local1868_22,
               local1868_23,
               local1868_24,
               local1868_25,
               local1868_5,
               local1868_6,
               local1868_7,
               local1868_8,
               local1868_9,
               local1868_10,
               local1868_11,
               local1868_12,
               local1868_13,
               local1868_14
            )
         );
      this.include(local1868_18, local1868_19);
      this.include(local1868_20, local1868_21);
      this.include(local1868_22, local1868_23);
      this.include(local1868_24, local1868_25);
   }

   private void vertex(VertexConsumer vertexConsumer, float local1869_2, float local1869_3, float local1869_4, float local1869_5, int local1869_6) {
      vertexConsumer.vertex(this.field04049, local1869_2, local1869_3).texture(local1869_4, local1869_5).color(local1869_6).lineWidth(this.field04053);
   }

   private float[] rotate(float local1870_1, float local1870_2, float local1870_3, float local1870_4, float local1870_5, float local1870_6) {
      float local1870_7 = local1870_1 - local1870_3;
      float local1870_8 = local1870_2 - local1870_4;
      return new float[]{
         local1870_3 + local1870_7 * local1870_6 - local1870_8 * local1870_5, local1870_4 + local1870_7 * local1870_5 + local1870_8 * local1870_6
      };
   }

   @Override
   public ScreenRect bounds() {
      if (this.field04054.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local1871_1 = (int)Math.floor(this.field04055);
      int local1871_2 = (int)Math.floor(this.field04056);
      int local1871_3 = Math.max(1, (int)Math.ceil(this.field04057 - this.field04055));
      int local1871_4 = Math.max(1, (int)Math.ceil(this.field04058 - this.field04056));
      ScreenRect screenRect = new ScreenRect(local1871_1, local1871_2, local1871_3, local1871_4).transformEachVertex(this.field04049);
      return this.field04051 == null ? screenRect : this.field04051.intersection(screenRect);
   }

   private void shimmerVertex(
      VertexConsumer vertexConsumer, float local1872_2, float local1872_3, float local1872_4, float local1872_5, int local1872_6, float local1872_7
   ) {
      vertexConsumer.vertex(this.field04049, local1872_2, local1872_3).texture(local1872_4, local1872_5).color(local1872_6).lineWidth(local1872_7);
   }

   MsdfTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect, boolean local1873_4, float local1873_5) {
      this.field04049 = new Matrix3x2f(matrix3x2f);
      this.field04050 = textureSetup;
      this.field04051 = screenRect;
      this.field04052 = local1873_4;
      this.field04053 = local1873_5;
   }

   private record Nested0379(
      float x0,
      float y0,
      float x1,
      float y1,
      float method0508,
      float method0510,
      float method1348,
      float method1346,
      float u0,
      float v0,
      float u1,
      float v1,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float method1345,
      float method1347
   ) {
   }
}
