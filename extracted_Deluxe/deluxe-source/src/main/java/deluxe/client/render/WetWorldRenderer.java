package deluxe.client.render;

import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.DeluxeClient;
import deluxe.client.MinecraftAccess;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class WetWorldRenderer implements MinecraftAccess {
   private static WetWorldRenderer wetWorldRenderer;
   private static final RenderPipeline field08020 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/wet_world"))
      .withVertexShader(DeluxeClient.id("core/wet_world"))
      .withFragmentShader(DeluxeClient.id("core/wet_world"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("WetWorldParams", UniformType.UNIFORM_BUFFER)
      .build();
   private GpuBuffer field08021;
   private GpuBuffer field08022;

   private WetWorldRenderer() {
   }

   public static WetWorldRenderer method2297() {
      if (wetWorldRenderer == null) {
         wetWorldRenderer = new WetWorldRenderer();
      }

      return wetWorldRenderer;
   }

   public void method2300() {
      SceneCopyBuffer.method3922();
      if (this.field08021 != null && !this.field08021.isClosed()) {
         this.field08021.close();
      }

      this.field08021 = null;
      if (this.field08022 != null && !this.field08022.isClosed()) {
         this.field08022.close();
      }

      this.field08022 = null;
   }

   public void method2301(Matrix4f matrix4f, Matrix4f matrix4f2) {
      CustomWorldModule customWorldModule = CustomWorldModule.method2204();
      if (customWorldModule.isEnabled() && customWorldModule.field07418.isChecked() && mc.world != null) {
         float local3587_4 = customWorldModule.field07419.isChecked()
            ? mc.world.getRainGradient(mc.getRenderTickCounter().getTickProgress(false))
            : 1.0F;
         float local3587_5 = customWorldModule.field07421.getValue() * local3587_4;
         float local3587_6 = customWorldModule.field07420.getValue() * local3587_4;
         float local3587_7 = customWorldModule.field07422.getValue() * local3587_4;
         if (local3587_5 <= 0.002F && local3587_6 <= 0.002F && local3587_7 <= 0.002F) {
            SceneCopyBuffer.method3922();
         } else {
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer != null
               && framebuffer.getColorAttachment() != null
               && framebuffer.getColorAttachmentView() != null
               && framebuffer.getDepthAttachmentView() != null
               && framebuffer.textureWidth > 0
               && framebuffer.textureHeight > 0) {
               GpuTextureView gpuTextureView = SceneCopyBuffer.method3920(framebuffer);
               if (gpuTextureView != null) {
                  if (this.field08021 == null || this.field08021.isClosed()) {
                     try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                        bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                        bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                        try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                           this.field08021 = RenderSystem.getDevice().createBuffer(() -> "deluxe_wet_world_quad", 32, builtBuffer.getBuffer());
                        }
                     }
                  }

                  if (this.field08022 == null || this.field08022.isClosed()) {
                     this.field08022 = RenderSystem.getDevice().createBuffer(() -> "deluxe_wet_world_params", 136, 208L);
                  }

                  float local3587_10 = (float)(mc.world.getTimeOfDay() % 24000L) / 24000.0F - 0.25F;
                  if (local3587_10 < 0.0F) {
                     local3587_10++;
                  }

                  float local3587_11 = local3587_10 + (1.0F - (float)((Math.cos(local3587_10 * Math.PI) + 1.0) / 2.0) - local3587_10) / 3.0F;
                  float local3587_12 = local3587_11 * (float) (Math.PI * 2);
                  int local3587_13 = customWorldModule.field07426.isChecked() ? customWorldModule.field07427.getValue() : CustomWorldModule.method2220();
                  MemoryStack memoryStack = MemoryStack.stackPush();

                  try {
                     ByteBuffer byteBuffer = memoryStack.calloc(208);
                     new Matrix4f(matrix4f2).mul(matrix4f).get(0, byteBuffer);
                     new Matrix4f(matrix4f2).mul(matrix4f).invert().get(64, byteBuffer);
                     byteBuffer.putFloat(128, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                     byteBuffer.putFloat(132, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                     byteBuffer.putFloat(136, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                     byteBuffer.putFloat(140, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
                     byteBuffer.putFloat(144, (local3587_13 >> 16 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(148, (local3587_13 >> 8 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(152, (local3587_13 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(156, local3587_5);
                     byteBuffer.putFloat(160, 0.0F);
                     byteBuffer.putFloat(164, (float)Math.cos(local3587_12));
                     byteBuffer.putFloat(168, (float)Math.sin(local3587_12));
                     byteBuffer.putFloat(172, local3587_7);
                     byteBuffer.putFloat(176, local3587_6);
                     byteBuffer.putFloat(180, customWorldModule.field07423.getValue());
                     byteBuffer.putFloat(184, customWorldModule.field07424.isChecked() ? 1.0F : 0.0F);
                     byteBuffer.putFloat(188, customWorldModule.field07425.getValue());
                     byteBuffer.putFloat(192, 8.0F);
                     byteBuffer.putFloat(196, customWorldModule.field07428.getValue());
                     byteBuffer.putFloat(200, customWorldModule.field07429.getValue());
                     RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field08022.slice(0L, 208L), byteBuffer);
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

                  try (RenderPass local3587_14x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "deluxe_wet_world", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                     local3587_14x.setPipeline(field08020);
                     local3587_14x.bindTexture("Sampler0", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     local3587_14x.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                     local3587_14x.setUniform("WetWorldParams", this.field08022);
                     local3587_14x.setVertexBuffer(0, this.field08021);
                     local3587_14x.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     local3587_14x.drawIndexed(0, 0, 6, 1);
                  }
               }
            }
         }
      } else {
         this.method2300();
      }
   }
}
