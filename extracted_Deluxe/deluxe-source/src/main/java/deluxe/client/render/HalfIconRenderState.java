package deluxe.client.render;

import deluxe.client.render.HalfIconDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class HalfIconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field09401;
   private final TextureSetup field09402;
   private final List<HalfIconRenderState.Nested0454> field09403 = new ArrayList<>(8);
   private float field09404 = Float.MAX_VALUE;
   private float field09405 = Float.MAX_VALUE;
   private float field09406 = -Float.MAX_VALUE;
   private float field09407 = -Float.MAX_VALUE;

   @Override
   public TextureSetup textureSetup() {
      return this.field09402;
   }

   boolean empty() {
      return this.field09403.isEmpty();
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   @Override
   public RenderPipeline pipeline() {
      return RectHalfIconRenderer.field13623;
   }

   void add(HalfIconDrawData halfIconDrawData, List<HalfIconRenderState.Nested0453> local4312_2) {
      if (!local4312_2.isEmpty()) {
         this.field09403.add(new HalfIconRenderState.Nested0454(halfIconDrawData, local4312_2));
         this.field09404 = Math.min(this.field09404, halfIconDrawData.x());
         this.field09405 = Math.min(this.field09405, halfIconDrawData.y());
         this.field09406 = Math.max(this.field09406, halfIconDrawData.x() + halfIconDrawData.width());
         this.field09407 = Math.max(this.field09407, halfIconDrawData.y() + halfIconDrawData.height());
      }
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (HalfIconRenderState.Nested0454 local4313_3 : this.field09403) {
         int local4313_4 = RectHalfIconRenderer.method3933().method3936(local4313_3.rectangle());
         if (local4313_4 >= 0) {
            for (HalfIconRenderState.Nested0453 local4313_6 : local4313_3.quads()) {
               this.vertex(
                  vertexConsumer, local4313_6.x0(), local4313_6.y0(), local4313_6.u0(), local4313_6.v0(), local4313_6.color(), local4313_4
               );
               this.vertex(
                  vertexConsumer, local4313_6.x1(), local4313_6.y1(), local4313_6.u1(), local4313_6.v1(), local4313_6.color(), local4313_4
               );
               this.vertex(
                  vertexConsumer,
                  local4313_6.method0508(),
                  local4313_6.method0510(),
                  local4313_6.method0509(),
                  local4313_6.method0514(),
                  local4313_6.color(),
                  local4313_4
               );
               this.vertex(
                  vertexConsumer,
                  local4313_6.method1348(),
                  local4313_6.method1346(),
                  local4313_6.method1698(),
                  local4313_6.method1694(),
                  local4313_6.color(),
                  local4313_4
               );
            }
         }
      }
   }

   @Override
   public ScreenRect bounds() {
      return this.field09403.isEmpty()
         ? new ScreenRect(0, 0, 1, 1)
         : new ScreenRect(
               (int)Math.floor(this.field09404),
               (int)Math.floor(this.field09405),
               Math.max(1, (int)Math.ceil(this.field09406 - this.field09404)),
               Math.max(1, (int)Math.ceil(this.field09407 - this.field09405))
            )
            .transformEachVertex(this.field09401);
   }

   private void vertex(VertexConsumer vertexConsumer, float local4315_2, float local4315_3, float local4315_4, float local4315_5, int local4315_6, int local4315_7) {
      vertexConsumer.vertex(this.field09401, local4315_2, local4315_3).texture(local4315_4, local4315_5).color(local4315_6).lineWidth(local4315_7 + 1);
   }

   HalfIconRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup) {
      this.field09401 = new Matrix3x2f(matrix3x2f);
      this.field09402 = textureSetup;
   }

   record Nested0453(
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float method0508,
      float method0510,
      float method0509,
      float method0514,
      float method1348,
      float method1346,
      float method1698,
      float method1694,
      int color
   ) {
   }

   private record Nested0454(HalfIconDrawData rectangle, List<HalfIconRenderState.Nested0453> quads) {
   }
}
