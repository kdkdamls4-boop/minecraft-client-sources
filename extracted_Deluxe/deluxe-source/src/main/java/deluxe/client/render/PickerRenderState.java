package deluxe.client.render;

import deluxe.client.render.PickerDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class PickerRenderState implements SimpleGuiElementRenderState {
   private final PickerDrawData pickerDrawData;
   private final Matrix3x2f field04225;
   private final ScreenRect field04226;
   private final ScreenRect field04227;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local2063_2 = PickerRenderer.method0930().method0934(this.pickerDrawData);
      if (local2063_2 >= 0) {
         float local2063_3 = this.pickerDrawData.x();
         float local2063_4 = this.pickerDrawData.y();
         float local2063_5 = this.pickerDrawData.x() + this.pickerDrawData.w();
         float local2063_6 = this.pickerDrawData.y() + this.pickerDrawData.h();
         this.vertex(vertexConsumer, local2063_3, local2063_4, 0, 0, local2063_2);
         this.vertex(vertexConsumer, local2063_3, local2063_6, 0, 255, local2063_2);
         this.vertex(vertexConsumer, local2063_5, local2063_6, 255, 255, local2063_2);
         this.vertex(vertexConsumer, local2063_5, local2063_4, 255, 0, local2063_2);
      }
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect bounds() {
      return this.field04227;
   }

   private void vertex(VertexConsumer vertexConsumer, float local2066_2, float local2066_3, int local2066_4, int local2066_5, int local2066_6) {
      vertexConsumer.vertex(this.field04225, local2066_2, local2066_3).color(local2066_4, local2066_5, 255, 255).lineWidth(local2066_6 + 1);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.field04226;
   }

   @Override
   public RenderPipeline pipeline() {
      return PickerRenderer.field01774;
   }

   PickerRenderState(Matrix3x2f matrix3x2f, PickerDrawData initialPickerDrawData, ScreenRect screenRect) {
      this.pickerDrawData = initialPickerDrawData;
      this.field04225 = new Matrix3x2f(matrix3x2f);
      this.field04226 = screenRect;
      int local2069_4 = (int)Math.floor(initialPickerDrawData.x());
      int local2069_5 = (int)Math.floor(initialPickerDrawData.y());
      int local2069_6 = (int)Math.ceil(initialPickerDrawData.w());
      int local2069_7 = (int)Math.ceil(initialPickerDrawData.h());
      ScreenRect screenRect2 = new ScreenRect(local2069_4, local2069_5, local2069_6, local2069_7).transformEachVertex(this.field04225);
      this.field04227 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }
}
