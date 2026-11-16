package deluxe.client.render;

import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.DeluxeClient;
import deluxe.client.render.SceneCopyBuffer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.system.MemoryStack;

public final class SaturationRenderer {
   private static SaturationRenderer saturationRenderer;
   private static final RenderPipeline field07341 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/saturation"))
      .withVertexShader(DeluxeClient.id("core/saturation"))
      .withFragmentShader(DeluxeClient.id("core/saturation"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("SaturationParams", UniformType.UNIFORM_BUFFER)
      .build();
   private GpuBuffer field07342;
   private GpuBuffer field07343;

   public void method2157() {
      if (CustomWorldModule.method2204().isEnabled() && CustomWorldModule.method2204().field07408.getValue() != 1.0F) {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            GpuTextureView gpuTextureView = SceneCopyBuffer.method3920(framebuffer);
            if (gpuTextureView != null) {
               if (this.field07342 == null || this.field07342.isClosed()) {
                  try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                     BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                     bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                     bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                     try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                        this.field07342 = RenderSystem.getDevice().createBuffer(() -> "deluxe_saturation_quad", 32, builtBuffer.getBuffer());
                     }
                  }
               }

               if (this.field07343 == null || this.field07343.isClosed()) {
                  this.field07343 = RenderSystem.getDevice().createBuffer(() -> "deluxe_saturation_params", 136, 16L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(16);
                  byteBuffer.putFloat(0, CustomWorldModule.method2204().field07408.getValue());
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field07343.slice(0L, 16L), byteBuffer);
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

               try (RenderPass local3385_3x = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(() -> "deluxe_saturation", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                  local3385_3x.setPipeline(field07341);
                  local3385_3x.bindTexture("Sampler0", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                  local3385_3x.setUniform("SaturationParams", this.field07343);
                  local3385_3x.setVertexBuffer(0, this.field07342);
                  local3385_3x.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  local3385_3x.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      } else {
         this.method2160();
      }
   }

   public static SaturationRenderer method2158() {
      if (saturationRenderer == null) {
         saturationRenderer = new SaturationRenderer();
      }

      return saturationRenderer;
   }

   private SaturationRenderer() {
   }

   public void method2160() {
      SceneCopyBuffer.method3922();
      if (this.field07342 != null && !this.field07342.isClosed()) {
         this.field07342.close();
      }

      this.field07342 = null;
      if (this.field07343 != null && !this.field07343.isClosed()) {
         this.field07343.close();
      }

      this.field07343 = null;
   }
}
