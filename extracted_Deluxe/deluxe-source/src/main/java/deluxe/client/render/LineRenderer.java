package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.LineDrawData;
import deluxe.client.render.ScissorStack;

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
import org.lwjgl.system.MemoryStack;

public final class LineRenderer implements AutoCloseable {
   private static volatile LineRenderer lineRenderer;
   public static final RenderPipeline field03517 = RenderPipeline.builder()
      .withLocation(method1268("pipeline/line"))
      .withVertexShader(method1268("core/line"))
      .withFragmentShader(method1268("core/line"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("LineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<LineDrawData> field03518 = new ArrayList<>(128);
   private DrawContext field03519;
   private GpuBuffer field03520;
   private boolean field03521 = true;

   private static Identifier method1268(String local1759_0) {
      return Identifier.of("deluxe", local1759_0);
   }

   @Override
   public void close() {
      this.field03518.clear();
      this.field03519 = null;
      this.method1272();
   }

   public void method1269(RenderPass renderPass) {
      if (renderPass != null && !this.field03518.isEmpty()) {
         GpuBuffer gpuBuffer = this.method1281();
         if (gpuBuffer != null) {
            renderPass.setUniform("LineParamsArray", gpuBuffer);
         }
      }
   }

   public void method1270() {
      this.field03518.clear();
      this.field03521 = false;
   }

   public void method1271() {
      this.field03519 = null;
   }

   private LineRenderer() {
   }

   private void method1272() {
      if (this.field03520 != null) {
         this.field03520.close();
         this.field03520 = null;
      }
   }

   int method1273(LineDrawData lineDrawData) {
      int local1766_2 = this.field03518.size();
      if (local1766_2 == 2048) {
         return -1;
      }

      this.field03518.add(lineDrawData);
      this.field03521 = true;
      return local1766_2;
   }

   public void method1274(DrawContext drawContext) {
      this.field03519 = drawContext;
   }

   public void method1275(LineDrawData lineDrawData) {
      this.method1276(this.field03519, lineDrawData);
   }

   private void method1276(DrawContext drawContext, LineDrawData lineDrawData) {
      if (drawContext != null && lineDrawData != null && lineDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new LineRenderState(matrix3x2f, lineDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private GpuBuffer method1277() {
      if (this.field03520 != null && !this.field03520.isClosed() && this.field03520.size() >= 65536L) {
         return this.field03520;
      }

      this.method1272();

      try {
         this.field03520 = RenderSystem.getDevice().createBuffer(() -> "deluxe_line_params", 136, 65536L);
         return this.field03520;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public static void method1278() {
      LineRenderer currentLineRenderer = lineRenderer;
      if (currentLineRenderer != null) {
         currentLineRenderer.close();
         lineRenderer = null;
      }
   }

   private ByteBuffer method1279(MemoryStack memoryStack, List<LineDrawData> local1772_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(65536, local1772_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local1772_2.size(); index++) {
         LineDrawData lineDrawData = local1772_2.get(index);
         int local1772_6 = lineDrawData.color();
         int local1772_7 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local1772_7, (local1772_6 >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 4, (local1772_6 >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 8, (local1772_6 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 12, (local1772_6 >>> 24 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 16, lineDrawData.length());
         byteBuffer.putFloat(local1772_7 + 20, lineDrawData.thickness());
         byteBuffer.putFloat(local1772_7 + 24, lineDrawData.fadeStart());
         byteBuffer.putFloat(local1772_7 + 28, lineDrawData.fadeEnd());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public static LineRenderer method1280() {
      LineRenderer currentLineRenderer = lineRenderer;
      if (currentLineRenderer == null) {
         synchronized (LineRenderer.class) {
            currentLineRenderer = lineRenderer;
            if (currentLineRenderer == null) {
               currentLineRenderer = new LineRenderer();
               lineRenderer = currentLineRenderer;
            }
         }
      }

      return currentLineRenderer;
   }

   private GpuBuffer method1281() {
      if (!this.field03521 && this.field03520 != null) {
         return this.field03520;
      }

      this.method1283();
      if (!this.field03521 && this.field03520 != null) {
         return this.field03520;
      }

      this.method1272();
      this.method1283();
      return this.field03520;
   }

   public boolean method1282(RenderPipeline renderPipeline) {
      return renderPipeline == field03517;
   }

   public void method1283() {
      if (!this.field03518.isEmpty() && this.field03521) {
         GpuBuffer gpuBuffer = this.method1277();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method1279(memoryStack, this.field03518);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field03521 = false;
               } catch (Throwable throwable) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                     }
                  }

                  throw throwable;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }
            } catch (RuntimeException exception) {
               this.field03521 = true;
            }
         }
      }
   }
}
