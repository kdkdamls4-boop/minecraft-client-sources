package deluxe.client.render;

import deluxe.client.util.GuiScale;
import deluxe.client.render.ItemGeometryQuad;
import deluxe.client.util.TextureBinding;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GuiItemRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field08948;
   private final TextureBinding textureBinding;
   private float[] field08950 = new float[256];
   private int[] field08951 = new int[16];
   private float[] field08952 = new float[16];
   private ScreenRect field08953;
   private int field08954;
   private float field08955 = Float.MAX_VALUE;
   private float field08956 = Float.MAX_VALUE;
   private float field08957 = -Float.MAX_VALUE;
   private float field08958 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (int index = 0; index < this.field08954; index++) {
         int local4066_3 = ItemRenderer.method1673().method1685(this.field08952[index]);
         if (local4066_3 >= 0) {
            int local4066_4 = index * 16;
            int local4066_5 = this.field08951[index];
            this.vertex(
               vertexConsumer,
               this.field08950[local4066_4],
               this.field08950[local4066_4 + 1],
               this.field08950[local4066_4 + 2],
               this.field08950[local4066_4 + 3],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.field08950[local4066_4 + 4],
               this.field08950[local4066_4 + 5],
               this.field08950[local4066_4 + 6],
               this.field08950[local4066_4 + 7],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.field08950[local4066_4 + 8],
               this.field08950[local4066_4 + 9],
               this.field08950[local4066_4 + 10],
               this.field08950[local4066_4 + 11],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.field08950[local4066_4 + 12],
               this.field08950[local4066_4 + 13],
               this.field08950[local4066_4 + 14],
               this.field08950[local4066_4 + 15],
               local4066_5,
               local4066_3
            );
         }
      }
   }

   private void ensureCapacity(int local4067_1) {
      if (local4067_1 > this.field08951.length) {
         int local4067_2 = Math.max(local4067_1, this.field08951.length * 2);
         float[] floatArray = new float[local4067_2 * 16];
         int[] intArray = new int[local4067_2];
         float[] floatArray2 = new float[local4067_2];
         System.arraycopy(this.field08950, 0, floatArray, 0, this.field08954 * 16);
         System.arraycopy(this.field08951, 0, intArray, 0, this.field08954);
         System.arraycopy(this.field08952, 0, floatArray2, 0, this.field08954);
         this.field08950 = floatArray;
         this.field08951 = intArray;
         this.field08952 = floatArray2;
      }
   }

   private void vertex(VertexConsumer vertexConsumer, float local4068_2, float local4068_3, float local4068_4, float local4068_5, int local4068_6, int local4068_7) {
      vertexConsumer.vertex(this.field08948, local4068_2, local4068_3).texture(local4068_4, local4068_5).color(local4068_6).lineWidth(local4068_7 + 1);
   }

   GuiItemRenderState(Matrix3x2f matrix3x2f, TextureBinding initialTextureBinding, ScreenRect screenRect) {
      this.field08948 = new Matrix3x2f(matrix3x2f);
      this.textureBinding = initialTextureBinding;
      this.field08953 = screenRect;
   }

   void add(ItemGeometryQuad itemGeometryQuad, float local4070_2, float local4070_3, float local4070_4, int local4070_5, float local4070_6) {
      this.ensureCapacity(this.field08954 + 1);
      int local4070_7 = this.field08954 * 16;
      float local4070_8 = local4070_2 + itemGeometryQuad.x0() * local4070_4;
      float local4070_9 = local4070_3 + itemGeometryQuad.y0() * local4070_4;
      float local4070_10 = local4070_2 + itemGeometryQuad.x1() * local4070_4;
      float local4070_11 = local4070_3 + itemGeometryQuad.y1() * local4070_4;
      float local4070_12 = local4070_2 + itemGeometryQuad.method0508() * local4070_4;
      float local4070_13 = local4070_3 + itemGeometryQuad.method0510() * local4070_4;
      float local4070_14 = local4070_2 + itemGeometryQuad.method1348() * local4070_4;
      float local4070_15 = local4070_3 + itemGeometryQuad.method1346() * local4070_4;
      this.field08950[local4070_7] = local4070_8;
      this.field08950[local4070_7 + 1] = local4070_9;
      this.field08950[local4070_7 + 2] = itemGeometryQuad.u0();
      this.field08950[local4070_7 + 3] = itemGeometryQuad.v0();
      this.field08950[local4070_7 + 4] = local4070_10;
      this.field08950[local4070_7 + 5] = local4070_11;
      this.field08950[local4070_7 + 6] = itemGeometryQuad.u1();
      this.field08950[local4070_7 + 7] = itemGeometryQuad.v1();
      this.field08950[local4070_7 + 8] = local4070_12;
      this.field08950[local4070_7 + 9] = local4070_13;
      this.field08950[local4070_7 + 10] = itemGeometryQuad.method0509();
      this.field08950[local4070_7 + 11] = itemGeometryQuad.method0514();
      this.field08950[local4070_7 + 12] = local4070_14;
      this.field08950[local4070_7 + 13] = local4070_15;
      this.field08950[local4070_7 + 14] = itemGeometryQuad.method1698();
      this.field08950[local4070_7 + 15] = itemGeometryQuad.method1694();
      this.field08951[this.field08954] = local4070_5;
      this.field08952[this.field08954] = local4070_6;
      this.field08954++;
      this.include(local4070_8, local4070_9);
      this.include(local4070_10, local4070_11);
      this.include(local4070_12, local4070_13);
      this.include(local4070_14, local4070_15);
   }

   @Override
   public ScreenRect bounds() {
      if (this.field08954 == 0) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local4071_1 = (int)Math.floor(this.field08955);
      int local4071_2 = (int)Math.floor(this.field08956);
      int local4071_3 = Math.max(1, (int)Math.ceil(this.field08957 - this.field08955));
      int local4071_4 = Math.max(1, (int)Math.ceil(this.field08958 - this.field08956));
      return new ScreenRect(local4071_1, local4071_2, local4071_3, local4071_4).transformEachVertex(this.field08948);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field08953;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.field08948.m00() * GuiScale.method0797() < GuiScale.method0794()
         ? this.textureBinding.linearSetup()
         : this.textureBinding.setup();
   }

   @Override
   public RenderPipeline pipeline() {
      return ItemRenderer.field05255;
   }

   private void include(float local4075_1, float local4075_2) {
      this.field08955 = Math.min(this.field08955, local4075_1);
      this.field08956 = Math.min(this.field08956, local4075_2);
      this.field08957 = Math.max(this.field08957, local4075_1);
      this.field08958 = Math.max(this.field08958, local4075_2);
   }
}
