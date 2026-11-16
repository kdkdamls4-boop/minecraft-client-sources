package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.DitherDrawData;
import deluxe.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class DitherRenderer implements AutoCloseable {
   private static volatile DitherRenderer ditherRenderer;
   private static final VertexFormat field11773 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field11774 = RenderPipeline.builder()
      .withLocation(method3351("pipeline/dither"))
      .withVertexShader(method3351("core/dither"))
      .withFragmentShader(method3351("core/dither"))
      .withVertexFormat(field11773, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("DitherParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<DitherDrawData> field11775 = new ArrayList<>(16);
   private final Map<DitherRenderer.Nested0485, DitherRenderState> field11776 = new LinkedHashMap<>(8);
   private DrawContext field11777;
   private GpuBuffer field11778;
   private boolean field11779;

   public static void method3342() {
      DitherRenderer currentDitherRenderer = ditherRenderer;
      if (currentDitherRenderer != null) {
         currentDitherRenderer.close();
         ditherRenderer = null;
      }
   }

   public void method3343(DrawContext drawContext) {
      if (this.field11777 != drawContext) {
         this.field11776.clear();
      }

      this.field11777 = drawContext;
   }

   private void method3344() {
      if (this.field11778 != null) {
         this.field11778.close();
         this.field11778 = null;
      }
   }

   private ByteBuffer method3345(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(8192);
      float local5452_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.field11775.size(); index++) {
         DitherDrawData ditherDrawData = this.field11775.get(index);
         int local5452_6 = index * 8 * 4;
         byteBuffer.putFloat(local5452_6, ditherDrawData.cellSize());
         byteBuffer.putFloat(local5452_6 + 4, ditherDrawData.width());
         byteBuffer.putFloat(local5452_6 + 8, ditherDrawData.height());
         byteBuffer.putFloat(local5452_6 + 12, ditherDrawData.radius());
         byteBuffer.putFloat(local5452_6 + 16, local5452_3);
         byteBuffer.putFloat(local5452_6 + 20, 7.31F);
         byteBuffer.putFloat(local5452_6 + 24, ditherDrawData.spread());
         byteBuffer.putFloat(local5452_6 + 28, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void method3346() {
      if (!this.field11775.isEmpty() && this.field11779) {
         GpuBuffer gpuBuffer = this.method3355();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method3345(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field11779 = false;
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
               this.field11779 = true;
            }
         }
      }
   }

   int method3347(DitherDrawData ditherDrawData) {
      int local5454_2 = this.field11775.size();
      if (local5454_2 >= 256) {
         return -1;
      }

      this.field11775.add(ditherDrawData);
      this.field11779 = true;
      return local5454_2;
   }

   public void method3348(DitherDrawData ditherDrawData) {
      if (this.field11777 != null && ditherDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.field11777).getGuiRenderState();
            int layerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            Matrix3x2f matrix3x2f = GuiScale.method0795(this.field11777);
            ScreenRect screenRect = ScissorStack.method3969();
            DitherRenderer.Nested0485 local5455_6 = new DitherRenderer.Nested0485(guiRenderState, layerSerial, screenRect);
            DitherRenderState ditherRenderState = this.field11776.get(local5455_6);
            if (ditherRenderState == null) {
               ditherRenderState = new DitherRenderState(matrix3x2f, screenRect);
               ditherRenderState.add(ditherDrawData);
               this.field11776.put(local5455_6, ditherRenderState);
               guiRenderState.addSimpleElement(ditherRenderState);
            } else {
               ditherRenderState.add(ditherDrawData);
            }
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[DitherRenderer] submit failed", exception);
         }
      }
   }

   public void method3349() {
      this.field11775.clear();
      this.field11779 = false;
   }

   private GpuBuffer method3350() {
      if (!this.field11779 && this.field11778 != null) {
         return this.field11778;
      }

      this.method3346();
      if (!this.field11779 && this.field11778 != null) {
         return this.field11778;
      }

      this.method3344();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.field11778 = RenderSystem.getDevice().createBuffer(() -> "deluxe_dither_params", 128, this.method3345(memoryStack));
         this.field11779 = false;
         gpuBuffer = this.field11778;
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

      return gpuBuffer;
   }

   private static Identifier method3351(String local5458_0) {
      return Identifier.of("deluxe", local5458_0);
   }

   private DitherRenderer() {
   }

   @Override
   public void close() {
      this.field11775.clear();
      this.field11776.clear();
      this.method3344();
      this.field11777 = null;
   }

   public void method3353() {
      this.field11777 = null;
      this.field11776.clear();
   }

   public boolean method3354(RenderPipeline renderPipeline) {
      return renderPipeline == field11774;
   }

   private GpuBuffer method3355() {
      if (this.field11778 != null && !this.field11778.isClosed() && this.field11778.size() >= 8192L) {
         return this.field11778;
      }

      this.method3344();

      try {
         this.field11778 = RenderSystem.getDevice().createBuffer(() -> "deluxe_dither_params", 136, 8192L);
         return this.field11778;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void method3356(RenderPass renderPass) {
      if (renderPass != null && !this.field11775.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3350();
         if (gpuBuffer != null) {
            renderPass.setUniform("DitherParamsArray", gpuBuffer);
         }
      }
   }

   public static DitherRenderer method3358() {
      DitherRenderer currentDitherRenderer = ditherRenderer;
      if (currentDitherRenderer == null) {
         synchronized (DitherRenderer.class) {
            currentDitherRenderer = ditherRenderer;
            if (currentDitherRenderer == null) {
               currentDitherRenderer = new DitherRenderer();
               ditherRenderer = currentDitherRenderer;
            }
         }
      }

      return currentDitherRenderer;
   }

   private record Nested0485(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
