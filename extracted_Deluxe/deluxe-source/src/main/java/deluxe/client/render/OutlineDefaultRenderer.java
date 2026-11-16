package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.OutlineDrawData;
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

public final class OutlineDefaultRenderer implements AutoCloseable {
   private static volatile OutlineDefaultRenderer outlineDefaultRenderer;
   public static final RenderPipeline field04580 = RenderPipeline.builder()
      .withLocation(method1474("pipeline/outline_default"))
      .withVertexShader(method1474("core/outline_default"))
      .withFragmentShader(method1474("core/outline_default"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineParamsArray2", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<OutlineDrawData> field04581 = new ArrayList<>(128);
   private DrawContext field04582;
   private GpuBuffer field04583;
   private ByteBuffer field04584;
   private boolean field04585 = true;

   public void method1463(RenderPass renderPass) {
      if (renderPass != null && !this.field04581.isEmpty()) {
         GpuBuffer gpuBuffer = this.method1477();
         if (gpuBuffer != null) {
            renderPass.setUniform("OutlineParamsArray", gpuBuffer.slice(0L, 65536L));
            renderPass.setUniform("OutlineParamsArray2", gpuBuffer.slice(65536L, 65536L));
         }
      }
   }

   private static void method1464(ByteBuffer byteBuffer, int local2144_1, int local2144_2) {
      byteBuffer.putFloat(local2144_1, (local2144_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 4, (local2144_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 8, (local2144_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 12, (local2144_2 >>> 24 & 0xFF) / 255.0F);
   }

   private void method1465(DrawContext drawContext, OutlineDrawData outlineDrawData) {
      if (drawContext != null && outlineDrawData != null && outlineDrawData.visible()) {
         try {
            OutlineDrawData outlineDrawData2 = this.method1468(outlineDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new OutlineRenderState(matrix3x2f, outlineDrawData2, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private OutlineDefaultRenderer() {
   }

   public void method1466(OutlineDrawData outlineDrawData) {
      this.method1465(this.field04582, outlineDrawData);
   }

   private OutlineDrawData method1468(OutlineDrawData outlineDrawData) {
      float local2150_2 = Math.max(0.0F, Math.min(outlineDrawData.width(), outlineDrawData.height()) * 0.5F);
      float local2150_3 = Math.max(0.0F, Math.min(outlineDrawData.width(), outlineDrawData.height()) * 0.5F);
      float[] floatArray = null;
      if (RoundedClipStack.method4837()) {
         floatArray = new float[]{
            RoundedClipStack.method4834(),
            RoundedClipStack.method4828(),
            RoundedClipStack.method4831(),
            RoundedClipStack.method4826(),
            RoundedClipStack.method4827(),
            RoundedClipStack.method4825(),
            RoundedClipStack.method4830(),
            RoundedClipStack.method4832()
         };
      }

      return new OutlineDrawData(
         outlineDrawData.x(),
         outlineDrawData.y(),
         outlineDrawData.width(),
         outlineDrawData.height(),
         method1473(outlineDrawData.radiusTopLeft(), 0.0F, local2150_2),
         method1473(outlineDrawData.radiusTopRight(), 0.0F, local2150_2),
         method1473(outlineDrawData.radiusBottomRight(), 0.0F, local2150_2),
         method1473(outlineDrawData.radiusBottomLeft(), 0.0F, local2150_2),
         method1473(outlineDrawData.thickness(), 0.0F, local2150_3),
         outlineDrawData.colorTopLeft(),
         outlineDrawData.colorTopRight(),
         outlineDrawData.colorBottomRight(),
         outlineDrawData.colorBottomLeft(),
         Math.max(outlineDrawData.smoothness(), 0.0F),
         floatArray
      );
   }

   public boolean method1469(RenderPipeline renderPipeline) {
      return renderPipeline == field04580;
   }

   public void method1470() {
      this.field04581.clear();
      this.field04585 = false;
   }

   private void method1471() {
      if (this.field04583 != null) {
         this.field04583.close();
         this.field04583 = null;
      }
   }

   public static void method1472() {
      OutlineDefaultRenderer currentOutlineDefaultRenderer = outlineDefaultRenderer;
      if (currentOutlineDefaultRenderer != null) {
         currentOutlineDefaultRenderer.close();
         outlineDefaultRenderer = null;
      }
   }

   private static float method1473(float local2155_0, float local2155_1, float local2155_2) {
      return Math.max(local2155_1, Math.min(local2155_2, local2155_0));
   }

   private static Identifier method1474(String local2156_0) {
      return Identifier.of("deluxe", local2156_0);
   }

   public void method1475(DrawContext drawContext, OutlineDrawData outlineDrawData) {
      this.method1478(drawContext);
      this.method1466(outlineDrawData);
      this.method1483();
   }

   int method1476(OutlineDrawData outlineDrawData) {
      int local2158_2 = this.field04581.size();
      if (local2158_2 == 1024) {
         return -1;
      }

      this.field04581.add(outlineDrawData);
      this.field04585 = true;
      return local2158_2;
   }

   private GpuBuffer method1477() {
      if (!this.field04585 && this.field04583 != null) {
         return this.field04583;
      }

      this.method1482();
      if (!this.field04585 && this.field04583 != null) {
         return this.field04583;
      }

      this.method1471();
      this.method1482();
      return this.field04583;
   }

   public void method1478(DrawContext drawContext) {
      this.field04582 = drawContext;
   }

   @Override
   public void close() {
      this.field04581.clear();
      this.field04582 = null;
      this.method1471();
      if (this.field04584 != null) {
         MemoryUtil.memFree(this.field04584);
         this.field04584 = null;
      }
   }

   public static OutlineDefaultRenderer method1479() {
      OutlineDefaultRenderer currentOutlineDefaultRenderer = outlineDefaultRenderer;
      if (currentOutlineDefaultRenderer == null) {
         synchronized (OutlineDefaultRenderer.class) {
            currentOutlineDefaultRenderer = outlineDefaultRenderer;
            if (currentOutlineDefaultRenderer == null) {
               currentOutlineDefaultRenderer = new OutlineDefaultRenderer();
               outlineDefaultRenderer = currentOutlineDefaultRenderer;
            }
         }
      }

      return currentOutlineDefaultRenderer;
   }

   private GpuBuffer method1480() {
      if (this.field04583 != null && !this.field04583.isClosed() && this.field04583.size() >= 131072L) {
         return this.field04583;
      }

      this.method1471();

      try {
         this.field04583 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_default_params", 136, 131072L);
         return this.field04583;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private ByteBuffer method1481(List<OutlineDrawData> local2164_1) {
      int local2164_2 = Math.min(131072, local2164_1.size() * 8 * 4 * 4);
      if (local2164_2 <= 0) {
         return null;
      }

      if (this.field04584 == null) {
         this.field04584 = MemoryUtil.memCalloc(131072);
      }

      ByteBuffer byteBuffer = this.field04584;
      byteBuffer.clear();
      MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer), 0, local2164_2);

      for (int index = 0; index < local2164_1.size(); index++) {
         OutlineDrawData outlineDrawData = local2164_1.get(index);
         int local2164_6 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local2164_6, outlineDrawData.radiusTopLeft());
         byteBuffer.putFloat(local2164_6 + 4, outlineDrawData.radiusTopRight());
         byteBuffer.putFloat(local2164_6 + 8, outlineDrawData.radiusBottomRight());
         byteBuffer.putFloat(local2164_6 + 12, outlineDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local2164_6 + 16, outlineDrawData.width());
         byteBuffer.putFloat(local2164_6 + 20, outlineDrawData.height());
         byteBuffer.putFloat(local2164_6 + 24, outlineDrawData.thickness());
         byteBuffer.putFloat(local2164_6 + 28, outlineDrawData.smoothness());
         method1464(byteBuffer, local2164_6 + 32, outlineDrawData.colorTopLeft());
         method1464(byteBuffer, local2164_6 + 48, outlineDrawData.colorTopRight());
         method1464(byteBuffer, local2164_6 + 64, outlineDrawData.colorBottomRight());
         method1464(byteBuffer, local2164_6 + 80, outlineDrawData.colorBottomLeft());
         float[] floatArray = outlineDrawData.scissor();
         if (floatArray != null) {
            float local2164_8 = GuiScale.method0794();
            float local2164_9 = GuiScale.method0796();
            byteBuffer.putFloat(local2164_6 + 96, floatArray[0] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 100, local2164_9 - (floatArray[1] + floatArray[3]) * local2164_8);
            byteBuffer.putFloat(local2164_6 + 104, floatArray[2] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 108, floatArray[3] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 112, floatArray[7] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 116, floatArray[6] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 120, floatArray[5] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 124, floatArray[4] * local2164_8);
         }
      }

      byteBuffer.limit(local2164_2).position(0);
      return byteBuffer;
   }

   public void method1482() {
      if (!this.field04581.isEmpty() && this.field04585) {
         GpuBuffer gpuBuffer = this.method1480();
         if (gpuBuffer != null) {
            ByteBuffer byteBuffer = this.method1481(this.field04581);
            if (byteBuffer != null && byteBuffer.remaining() > 0) {
               try {
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field04585 = false;
               } catch (RuntimeException exception) {
                  this.field04585 = true;
               }
            }
         }
      }
   }

   public void method1483() {
      this.field04582 = null;
   }
}
