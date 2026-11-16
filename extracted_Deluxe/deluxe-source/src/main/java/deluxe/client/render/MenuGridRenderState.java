package deluxe.client.render;

import deluxe.client.render.MenuGridDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MenuGridRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f field11442;
   private final ScreenRect field11443;
   private final List<MenuGridDrawData> field11444 = new ArrayList<>(2);
   private float field11445 = Float.MAX_VALUE;
   private float field11446 = Float.MAX_VALUE;
   private float field11447 = -Float.MAX_VALUE;
   private float field11448 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (MenuGridDrawData menuGridDrawData : this.field11444) {
         int local5304_4 = MenuGridRenderer.method2408().method2406(menuGridDrawData);
         if (local5304_4 >= 0) {
            this.vertex(vertexConsumer, menuGridDrawData.x(), menuGridDrawData.y(), 0.0F, 0.0F, menuGridDrawData.color(), local5304_4);
            this.vertex(vertexConsumer, menuGridDrawData.x(), menuGridDrawData.y() + menuGridDrawData.height(), 0.0F, 1.0F, menuGridDrawData.color(), local5304_4);
            this.vertex(
               vertexConsumer, menuGridDrawData.x() + menuGridDrawData.width(), menuGridDrawData.y() + menuGridDrawData.height(), 1.0F, 1.0F, menuGridDrawData.color(), local5304_4
            );
            this.vertex(vertexConsumer, menuGridDrawData.x() + menuGridDrawData.width(), menuGridDrawData.y(), 1.0F, 0.0F, menuGridDrawData.color(), local5304_4);
         }
      }
   }

   MenuGridRenderState(Matrix3x2f matrix3x2f, ScreenRect screenRect) {
      this.field11442 = new Matrix3x2f(matrix3x2f);
      this.field11443 = screenRect;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   private void vertex(VertexConsumer vertexConsumer, float local5307_2, float local5307_3, float local5307_4, float local5307_5, int local5307_6, int local5307_7) {
      vertexConsumer.vertex(this.field11442, local5307_2, local5307_3).texture(local5307_4, local5307_5).color(local5307_6).lineWidth(local5307_7 + 1);
   }

   @Override
   public ScreenRect bounds() {
      if (this.field11443 != null) {
         return this.field11443;
      } else {
         return this.field11444.isEmpty()
            ? new ScreenRect(0, 0, 1, 1)
            : new ScreenRect(
                  (int)Math.floor(this.field11445),
                  (int)Math.floor(this.field11446),
                  Math.max(1, (int)Math.ceil(this.field11447 - this.field11445)),
                  Math.max(1, (int)Math.ceil(this.field11448 - this.field11446))
               )
               .transformEachVertex(this.field11442);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field11443;
   }

   @Override
   public RenderPipeline pipeline() {
      return MenuGridRenderer.field08378;
   }

   void add(MenuGridDrawData menuGridDrawData) {
      this.field11444.add(menuGridDrawData);
      this.field11445 = Math.min(this.field11445, menuGridDrawData.x());
      this.field11446 = Math.min(this.field11446, menuGridDrawData.y());
      this.field11447 = Math.max(this.field11447, menuGridDrawData.x() + menuGridDrawData.width());
      this.field11448 = Math.max(this.field11448, menuGridDrawData.y() + menuGridDrawData.height());
   }
}
