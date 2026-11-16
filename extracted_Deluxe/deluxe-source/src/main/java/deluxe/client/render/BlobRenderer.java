package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.BlobDrawData;
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

public final class BlobRenderer implements AutoCloseable {
   private static volatile BlobRenderer blobRenderer;
   public static final RenderPipeline field10394 = RenderPipeline.builder()
      .withLocation(method3060("pipeline/blob"))
      .withVertexShader(method3060("core/blob"))
      .withFragmentShader(method3060("core/blob"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BlobParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BlobDrawData> field10395 = new ArrayList<>(16);
   private DrawContext field10396;
   private GpuBuffer field10397;
   private boolean field10398;

   public void method3048() {
      if (!this.field10395.isEmpty() && this.field10398) {
         GpuBuffer gpuBuffer = this.method3059();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method3062(memoryStack, this.field10395);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field10398 = false;
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
               this.field10398 = true;
            }
         }
      }
   }

   private static void method3049(ByteBuffer byteBuffer, int local4877_1, int local4877_2) {
      byteBuffer.putFloat(local4877_1, (local4877_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 4, (local4877_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 8, (local4877_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 12, (local4877_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void method3050() {
      this.field10396 = null;
   }

   private void method3051() {
      if (this.field10397 != null) {
         this.field10397.close();
         this.field10397 = null;
      }
   }

   public void method3052() {
      this.field10395.clear();
      this.field10398 = false;
   }

   public void method3053(BlobDrawData blobDrawData) {
      this.method3063(this.field10396, blobDrawData);
   }

   private BlobRenderer() {
   }

   public void method3054(DrawContext drawContext) {
      this.field10396 = drawContext;
   }

   int method3055(BlobDrawData blobDrawData) {
      int local4885_2 = this.field10395.size();
      if (local4885_2 == 128) {
         return -1;
      }

      this.field10395.add(blobDrawData);
      this.field10398 = true;
      return local4885_2;
   }

   private GpuBuffer method3056() {
      if (!this.field10398 && this.field10397 != null) {
         return this.field10397;
      }

      this.method3048();
      if (!this.field10398 && this.field10397 != null) {
         return this.field10397;
      }

      this.method3051();
      this.method3048();
      return this.field10397;
   }

   public static void method3057() {
      BlobRenderer currentBlobRenderer = blobRenderer;
      if (currentBlobRenderer != null) {
         currentBlobRenderer.close();
         blobRenderer = null;
      }
   }

   public void method3058(RenderPass renderPass) {
      if (renderPass != null && !this.field10395.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3056();
         if (gpuBuffer != null) {
            renderPass.setUniform("BlobParamsArray", gpuBuffer);
         }
      }
   }

   @Override
   public void close() {
      this.field10395.clear();
      this.field10396 = null;
      this.method3051();
   }

   private GpuBuffer method3059() {
      if (this.field10397 != null && !this.field10397.isClosed() && this.field10397.size() >= 8192L) {
         return this.field10397;
      }

      this.method3051();

      try {
         this.field10397 = RenderSystem.getDevice().createBuffer(() -> "deluxe_blob_params", 136, 8192L);
         return this.field10397;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static Identifier method3060(String local4891_0) {
      return Identifier.of("deluxe", local4891_0);
   }

   private ByteBuffer method3062(MemoryStack memoryStack, List<BlobDrawData> local4893_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local4893_2.size() * 4 * 4 * 4));

      for (int index = 0; index < local4893_2.size(); index++) {
         BlobDrawData blobDrawData = local4893_2.get(index);
         int local4893_6 = index * 4 * 4 * 4;
         byteBuffer.putFloat(local4893_6, blobDrawData.w());
         byteBuffer.putFloat(local4893_6 + 4, blobDrawData.h());
         byteBuffer.putFloat(local4893_6 + 8, blobDrawData.centerX());
         byteBuffer.putFloat(local4893_6 + 12, blobDrawData.centerY());
         byteBuffer.putFloat(local4893_6 + 16, blobDrawData.radiusX());
         byteBuffer.putFloat(local4893_6 + 20, blobDrawData.radiusY());
         byteBuffer.putFloat(local4893_6 + 24, blobDrawData.falloff());
         byteBuffer.putFloat(local4893_6 + 28, Math.max(blobDrawData.feather(), 0.001F));
         byteBuffer.putFloat(local4893_6 + 32, blobDrawData.clipTL());
         byteBuffer.putFloat(local4893_6 + 36, blobDrawData.clipTR());
         byteBuffer.putFloat(local4893_6 + 40, blobDrawData.clipBR());
         byteBuffer.putFloat(local4893_6 + 44, blobDrawData.clipBL());
         method3049(byteBuffer, local4893_6 + 48, blobDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void method3063(DrawContext drawContext, BlobDrawData blobDrawData) {
      if (drawContext != null && blobDrawData != null && blobDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new BlobRenderState(matrix3x2f, blobDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public static BlobRenderer method3064() {
      BlobRenderer currentBlobRenderer = blobRenderer;
      if (currentBlobRenderer == null) {
         synchronized (BlobRenderer.class) {
            currentBlobRenderer = blobRenderer;
            if (currentBlobRenderer == null) {
               currentBlobRenderer = new BlobRenderer();
               blobRenderer = currentBlobRenderer;
            }
         }
      }

      return currentBlobRenderer;
   }

   public boolean method3065(RenderPipeline renderPipeline) {
      return renderPipeline == field10394;
   }
}
