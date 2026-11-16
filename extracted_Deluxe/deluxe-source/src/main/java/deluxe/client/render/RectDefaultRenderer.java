package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.RectDrawData;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.RoundedClipStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

public final class RectDefaultRenderer implements AutoCloseable {
   private static volatile RectDefaultRenderer rectDefaultRenderer;
   public static final RenderPipeline field14513 = RenderPipeline.builder()
      .withLocation(method4240("pipeline/rect_default"))
      .withVertexShader(method4240("core/rect_default"))
      .withFragmentShader(method4240("core/rect_default"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectangleParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("RectangleParamsArray2", UniformType.UNIFORM_BUFFER)
      .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<RectDrawData> field14514 = new ArrayList<>(128);
   private DrawContext field14515;
   private GpuBuffer field14516;
   private ByteBuffer field14517;
   private boolean field14518 = true;

   private static Identifier method4240(String local7111_0) {
      return Identifier.of("deluxe", local7111_0);
   }

   public void method4241() {
      this.field14515 = null;
   }

   public void method4242(DrawContext drawContext) {
      this.field14515 = drawContext;
   }

   public static RectDefaultRenderer method4243() {
      RectDefaultRenderer currentRectDefaultRenderer = rectDefaultRenderer;
      if (currentRectDefaultRenderer == null) {
         synchronized (RectDefaultRenderer.class) {
            currentRectDefaultRenderer = rectDefaultRenderer;
            if (currentRectDefaultRenderer == null) {
               currentRectDefaultRenderer = new RectDefaultRenderer();
               rectDefaultRenderer = currentRectDefaultRenderer;
            }
         }
      }

      return currentRectDefaultRenderer;
   }

   private static void method4244(ByteBuffer byteBuffer, int local7115_1, int local7115_2) {
      byteBuffer.putFloat(local7115_1, (local7115_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 4, (local7115_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 8, (local7115_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 12, (local7115_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void method4245(DrawContext drawContext, RectDrawData rectDrawData) {
      this.method4242(drawContext);
      this.method4249(rectDrawData);
      this.method4241();
   }

   public static void method4246() {
      RectDefaultRenderer currentRectDefaultRenderer = rectDefaultRenderer;
      if (currentRectDefaultRenderer != null) {
         currentRectDefaultRenderer.close();
         rectDefaultRenderer = null;
      }
   }

   int method4247(RectDrawData rectDrawData) {
      int local7118_2 = this.field14514.size();
      if (local7118_2 == 1024) {
         return -1;
      }

      this.field14514.add(rectDrawData);
      this.field14518 = true;
      return local7118_2;
   }

   public void method4248() {
      if (!this.field14514.isEmpty() && this.field14518) {
         GpuBuffer gpuBuffer = this.method4253();
         if (gpuBuffer != null) {
            ByteBuffer byteBuffer = this.method4259(this.field14514);
            if (byteBuffer != null && byteBuffer.remaining() > 0) {
               try {
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field14518 = false;
               } catch (RuntimeException exception) {
                  this.field14518 = true;
               }
            }
         }
      }
   }

   public void method4249(RectDrawData rectDrawData) {
      this.method4254(this.field14515, rectDrawData);
   }

   public void method4250() {
      this.field14514.clear();
      this.field14518 = false;
   }

   private static float method4251(float local7123_0, float local7123_1, float local7123_2) {
      return Math.max(local7123_1, Math.min(local7123_2, local7123_0));
   }

   private RectDefaultRenderer() {
   }

   private GpuBuffer method4253() {
      if (this.field14516 != null && !this.field14516.isClosed() && this.field14516.size() >= 131072L) {
         return this.field14516;
      }

      this.method4256();

      try {
         this.field14516 = RenderSystem.getDevice().createBuffer(() -> "deluxe_rect_default_params", 136, 131072L);
         return this.field14516;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private void method4254(DrawContext drawContext, RectDrawData rectDrawData) {
      if (drawContext != null && rectDrawData != null && rectDrawData.visible()) {
         try {
            RectDrawData rectDrawData2 = this.method4258(rectDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new RectRenderState(matrix3x2f, rectDrawData2, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public boolean method4255(RenderPipeline renderPipeline) {
      return renderPipeline == field14513;
   }

   private void method4256() {
      if (this.field14516 != null) {
         this.field14516.close();
         this.field14516 = null;
      }
   }

   private GpuBuffer method4257() {
      if (!this.field14518 && this.field14516 != null) {
         return this.field14516;
      }

      this.method4248();
      if (!this.field14518 && this.field14516 != null) {
         return this.field14516;
      }

      this.method4256();
      this.method4248();
      return this.field14516;
   }

   private RectDrawData method4258(RectDrawData rectDrawData) {
      float local7131_2 = Math.max(0.0F, Math.min(rectDrawData.width(), rectDrawData.height()) * 0.5F);
      float local7131_3 = 0.0F;
      float local7131_4 = 0.0F;
      float local7131_5 = 0.0F;
      float local7131_6 = 0.0F;
      float local7131_7 = 0.0F;
      float local7131_8 = 0.0F;
      float local7131_9 = 0.0F;
      float local7131_10 = 0.0F;
      if (RoundedClipStack.method4837()) {
         local7131_3 = RoundedClipStack.method4834();
         local7131_4 = RoundedClipStack.method4828();
         local7131_5 = RoundedClipStack.method4831();
         local7131_6 = RoundedClipStack.method4826();
         local7131_7 = RoundedClipStack.method4827();
         local7131_8 = RoundedClipStack.method4825();
         local7131_9 = RoundedClipStack.method4830();
         local7131_10 = RoundedClipStack.method4832();
      } else {
         local7131_3 = rectDrawData.scissorX();
         local7131_4 = rectDrawData.scissorY();
         local7131_5 = rectDrawData.scissorWidth();
         local7131_6 = rectDrawData.scissorHeight();
         local7131_7 = rectDrawData.scissorRadiusTopLeft();
         local7131_8 = rectDrawData.scissorRadiusTopRight();
         local7131_9 = rectDrawData.scissorRadiusBottomRight();
         local7131_10 = rectDrawData.scissorRadiusBottomLeft();
      }

      return new RectDrawData(
         Math.round(rectDrawData.x() * GuiScale.method0794()) / GuiScale.method0794(),
         Math.round(rectDrawData.y() * GuiScale.method0794()) / GuiScale.method0794(),
         Math.max(
            (
                  Math.round((rectDrawData.x() + rectDrawData.width()) * GuiScale.method0794())
                     - Math.round(rectDrawData.x() * GuiScale.method0794())
               )
               / GuiScale.method0794(),
            1.0F / GuiScale.method0794()
         ),
         Math.max(
            (
                  Math.round((rectDrawData.y() + rectDrawData.height()) * GuiScale.method0794())
                     - Math.round(rectDrawData.y() * GuiScale.method0794())
               )
               / GuiScale.method0794(),
            1.0F / GuiScale.method0794()
         ),
         method4251(rectDrawData.radiusTopLeft(), 0.0F, local7131_2),
         method4251(rectDrawData.radiusTopRight(), 0.0F, local7131_2),
         method4251(rectDrawData.radiusBottomRight(), 0.0F, local7131_2),
         method4251(rectDrawData.radiusBottomLeft(), 0.0F, local7131_2),
         rectDrawData.colorTopLeft(),
         rectDrawData.colorTopRight(),
         rectDrawData.colorBottomRight(),
         rectDrawData.colorBottomLeft(),
         Math.max(rectDrawData.smoothness(), 0.0F),
         local7131_3,
         local7131_4,
         local7131_5,
         local7131_6,
         local7131_7,
         local7131_8,
         local7131_9,
         local7131_10,
         rectDrawData.paletteMode(),
         rectDrawData.paletteTint(),
         rectDrawData.paletteAlpha()
      );
   }

   private ByteBuffer method4259(List<RectDrawData> local7132_1) {
      int local7132_2 = Math.min(131072, local7132_1.size() * 8 * 4 * 4);
      if (local7132_2 <= 0) {
         return null;
      }

      if (this.field14517 == null) {
         this.field14517 = MemoryUtil.memCalloc(131072);
      }

      ByteBuffer byteBuffer = this.field14517;
      byteBuffer.clear();
      MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer), 0, local7132_2);

      for (int index = 0; index < local7132_1.size(); index++) {
         RectDrawData rectDrawData = local7132_1.get(index);
         int local7132_6 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local7132_6, rectDrawData.radiusTopLeft());
         byteBuffer.putFloat(local7132_6 + 4, rectDrawData.radiusTopRight());
         byteBuffer.putFloat(local7132_6 + 8, rectDrawData.radiusBottomRight());
         byteBuffer.putFloat(local7132_6 + 12, rectDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local7132_6 + 16, rectDrawData.width());
         byteBuffer.putFloat(local7132_6 + 20, rectDrawData.height());
         byteBuffer.putFloat(local7132_6 + 24, rectDrawData.smoothness());
         byteBuffer.putFloat(local7132_6 + 28, rectDrawData.paletteMode());
         if (rectDrawData.paletteMode() != 0) {
            byteBuffer.putFloat(local7132_6 + 32, rectDrawData.paletteTint());
            byteBuffer.putFloat(local7132_6 + 36, rectDrawData.paletteAlpha());
         } else {
            method4244(byteBuffer, local7132_6 + 32, rectDrawData.colorTopLeft());
            method4244(byteBuffer, local7132_6 + 48, rectDrawData.colorTopRight());
            method4244(byteBuffer, local7132_6 + 64, rectDrawData.colorBottomRight());
            method4244(byteBuffer, local7132_6 + 80, rectDrawData.colorBottomLeft());
         }

         float local7132_7 = GuiScale.method0794();
         float local7132_8 = GuiScale.method0796();
         byteBuffer.putFloat(local7132_6 + 96, rectDrawData.scissorX() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 100, local7132_8 - (rectDrawData.scissorY() + rectDrawData.scissorHeight()) * local7132_7);
         byteBuffer.putFloat(local7132_6 + 104, rectDrawData.scissorWidth() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 108, rectDrawData.scissorHeight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 112, rectDrawData.scissorRadiusBottomLeft() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 116, rectDrawData.scissorRadiusBottomRight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 120, rectDrawData.scissorRadiusTopRight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 124, rectDrawData.scissorRadiusTopLeft() * local7132_7);
      }

      byteBuffer.limit(local7132_2).position(0);
      return byteBuffer;
   }

   @Override
   public void close() {
      this.field14514.clear();
      this.field14515 = null;
      this.method4256();
      if (this.field14517 != null) {
         MemoryUtil.memFree(this.field14517);
         this.field14517 = null;
      }
   }

   public void method4260(RenderPass renderPass) {
      if (renderPass != null && !this.field14514.isEmpty()) {
         GpuBuffer gpuBuffer = this.method4257();
         if (gpuBuffer != null) {
            renderPass.setUniform("RectangleParamsArray", gpuBuffer.slice(0L, 65536L));
            renderPass.setUniform("RectangleParamsArray2", gpuBuffer.slice(65536L, 65536L));
         }

         GpuBuffer gpuBuffer2 = ThemePaletteBuffer.method2152();
         if (gpuBuffer2 != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
         }
      }
   }
}
