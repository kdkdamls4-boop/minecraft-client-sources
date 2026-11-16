package deluxe.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class RasterTextRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field05739;
   private final TextureSetup field05740;
   private final ScreenRect field05741;
   private final List<RasterTextRenderState.Nested0409> field05742 = new ArrayList<>(64);
   private float field05743 = Float.MAX_VALUE;
   private float field05744 = Float.MAX_VALUE;
   private float field05745 = -Float.MAX_VALUE;
   private float field05746 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (RasterTextRenderState.Nested0409 local2674_3 : this.field05742) {
         this.vertex(vertexConsumer, local2674_3.x0(), local2674_3.y0(), local2674_3.u0(), local2674_3.v0(), local2674_3.colorTopLeft());
         this.vertex(vertexConsumer, local2674_3.x0(), local2674_3.y1(), local2674_3.u0(), local2674_3.v1(), local2674_3.colorBottomLeft());
         this.vertex(vertexConsumer, local2674_3.x1(), local2674_3.y1(), local2674_3.u1(), local2674_3.v1(), local2674_3.colorBottomRight());
         this.vertex(vertexConsumer, local2674_3.x1(), local2674_3.y0(), local2674_3.u1(), local2674_3.v0(), local2674_3.colorTopRight());
      }
   }

   @Override
   public ScreenRect bounds() {
      if (this.field05742.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local2675_1 = (int)Math.floor(this.field05743);
      int local2675_2 = (int)Math.floor(this.field05744);
      int local2675_3 = Math.max(1, (int)Math.ceil(this.field05745 - this.field05743));
      int local2675_4 = Math.max(1, (int)Math.ceil(this.field05746 - this.field05744));
      ScreenRect screenRect = new ScreenRect(local2675_1, local2675_2, local2675_3, local2675_4).transformEachVertex(this.field05739);
      return this.field05741 == null ? screenRect : this.field05741.intersection(screenRect);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field05741;
   }

   @Override
   public RenderPipeline pipeline() {
      return TextAtlasRenderer.field10231;
   }

   private void vertex(VertexConsumer vertexConsumer, float local2678_2, float local2678_3, float local2678_4, float local2678_5, int local2678_6) {
      vertexConsumer.vertex(this.field05739, local2678_2, local2678_3).texture(local2678_4, local2678_5).color(local2678_6);
   }

   RasterTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect) {
      this.field05739 = new Matrix3x2f(matrix3x2f);
      this.field05740 = textureSetup;
      this.field05741 = screenRect;
   }

   void add(
      float local2680_1,
      float local2680_2,
      float local2680_3,
      float local2680_4,
      float local2680_5,
      float local2680_6,
      float local2680_7,
      float local2680_8,
      int local2680_9,
      int local2680_10,
      int local2680_11,
      int local2680_12
   ) {
      this.field05742
         .add(
            new RasterTextRenderState.Nested0409(
               local2680_1,
               local2680_2,
               local2680_3,
               local2680_4,
               local2680_5,
               local2680_6,
               local2680_7,
               local2680_8,
               local2680_9,
               local2680_10,
               local2680_11,
               local2680_12
            )
         );
      this.field05743 = Math.min(this.field05743, local2680_1);
      this.field05744 = Math.min(this.field05744, local2680_2);
      this.field05745 = Math.max(this.field05745, local2680_3);
      this.field05746 = Math.max(this.field05746, local2680_4);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.field05740;
   }

   private record Nested0409(
      float x0,
      float y0,
      float x1,
      float y1,
      float u0,
      float v0,
      float u1,
      float v1,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
   }
}
