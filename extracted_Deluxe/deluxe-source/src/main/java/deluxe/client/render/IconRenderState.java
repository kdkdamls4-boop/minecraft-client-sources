package deluxe.client.render;

import deluxe.client.render.IconDrawData;
import deluxe.client.render.TextureReference;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class IconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field14906;
   private final TextureReference textureReference;
   private final List<IconDrawData> field14908 = new ArrayList<>(16);
   private float field14909 = Float.MAX_VALUE;
   private float field14910 = Float.MAX_VALUE;
   private float field14911 = -Float.MAX_VALUE;
   private float field14912 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (IconDrawData iconDrawData : this.field14908) {
         float local7343_4 = iconDrawData.x();
         float local7343_5 = iconDrawData.y();
         float local7343_6 = iconDrawData.x() + iconDrawData.size();
         float local7343_7 = iconDrawData.y() + iconDrawData.size();
         int local7343_8 = iconDrawData.color();
         this.vertex(vertexConsumer, local7343_4, local7343_5, iconDrawData.u0(), iconDrawData.v0(), local7343_8);
         this.vertex(vertexConsumer, local7343_4, local7343_7, iconDrawData.u0(), iconDrawData.v1(), local7343_8);
         this.vertex(vertexConsumer, local7343_6, local7343_7, iconDrawData.u1(), iconDrawData.v1(), local7343_8);
         this.vertex(vertexConsumer, local7343_6, local7343_5, iconDrawData.u1(), iconDrawData.v0(), local7343_8);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   private void vertex(VertexConsumer vertexConsumer, float local7345_2, float local7345_3, float local7345_4, float local7345_5, int local7345_6) {
      vertexConsumer.vertex(this.field14906, local7345_2, local7345_3).texture(local7345_4, local7345_5).color(local7345_6);
   }

   @Override
   public RenderPipeline pipeline() {
      return EffectIconRenderer.field04126;
   }

   void add(IconDrawData iconDrawData) {
      this.field14908.add(iconDrawData);
      this.field14909 = Math.min(this.field14909, iconDrawData.x());
      this.field14910 = Math.min(this.field14910, iconDrawData.y());
      this.field14911 = Math.max(this.field14911, iconDrawData.x() + iconDrawData.size());
      this.field14912 = Math.max(this.field14912, iconDrawData.y() + iconDrawData.size());
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureReference.setup();
   }

   @Override
   public ScreenRect bounds() {
      if (this.field14908.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7349_1 = (int)Math.floor(this.field14909);
      int local7349_2 = (int)Math.floor(this.field14910);
      int local7349_3 = Math.max(1, (int)Math.ceil(this.field14911 - this.field14909));
      int local7349_4 = Math.max(1, (int)Math.ceil(this.field14912 - this.field14910));
      return new ScreenRect(local7349_1, local7349_2, local7349_3, local7349_4).transformEachVertex(this.field14906);
   }

   IconRenderState(Matrix3x2f matrix3x2f, TextureReference initialTextureReference) {
      this.field14906 = new Matrix3x2f(matrix3x2f);
      this.textureReference = initialTextureReference;
   }
}
