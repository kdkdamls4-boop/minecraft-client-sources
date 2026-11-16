package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.module.render.SwordAnimationsModule;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.system.MemoryStack;

public final class HandOutlineRenderer {
   private static HandOutlineRenderer handOutlineRenderer;
   private static final RenderPipeline field00047 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/hand_outline"))
      .withVertexShader(DeluxeClient.id("core/hand_outline"))
      .withFragmentShader(DeluxeClient.id("core/hand_outline"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("HandOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field00048 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/hand_outline_blur"))
      .withVertexShader(DeluxeClient.id("core/hand_outline_blur"))
      .withFragmentShader(DeluxeClient.id("core/hand_outline_blur"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("HandOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private SimpleFramebuffer field00049;
   private SimpleFramebuffer field00050;
   private GpuTexture field00051;
   private GpuTextureView field00052;
   private SimpleFramebuffer field00053;
   private GpuBuffer field00054;
   private GpuBuffer field00055;
   private boolean field00056;
   private boolean field00057;

   public void method0462() {
      if (this.field00057) {
         this.field00057 = false;
         this.field00056 = false;
         if (SwordAnimationsModule.method1770().method1766()) {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (framebuffer != null
               && framebuffer.getColorAttachmentView() != null
               && this.field00051 != null
               && this.field00052 != null
               && !this.field00052.isClosed()) {
               if (this.field00051.getWidth(0) == framebuffer.textureWidth && this.field00051.getHeight(0) == framebuffer.textureHeight) {
                  if (this.field00054 == null || this.field00054.isClosed()) {
                     try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                        bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                        bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                        try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                           this.field00054 = RenderSystem.getDevice().createBuffer(() -> "deluxe_hand_outline_quad", 32, builtBuffer.getBuffer());
                        }
                     }
                  }

                  if (this.field00055 == null || this.field00055.isClosed()) {
                     this.field00055 = RenderSystem.getDevice().createBuffer(() -> "deluxe_hand_outline_params", 136, 48L);
                  }

                  MemoryStack memoryStack = MemoryStack.stackPush();

                  try {
                     ByteBuffer byteBuffer = memoryStack.calloc(48);
                     byteBuffer.putFloat(0, (SwordAnimationsModule.method1770().method1767() >> 16 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(4, (SwordAnimationsModule.method1770().method1767() >> 8 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(8, (SwordAnimationsModule.method1770().method1767() & 0xFF) / 255.0F);
                     byteBuffer.putFloat(12, SwordAnimationsModule.method1770().field05594.getValue());
                     byteBuffer.putFloat(24, SwordAnimationsModule.method1770().field05593.getValue());
                     byteBuffer.putFloat(28, SwordAnimationsModule.method1770().field05595.getValue());
                     byteBuffer.putFloat(32, 1.0F / framebuffer.textureWidth);
                     byteBuffer.putFloat(36, 1.0F / framebuffer.textureHeight);
                     RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field00055.slice(0L, 48L), byteBuffer);
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

                  this.field00053 = this.method0469(this.field00053, "deluxe_hand_outline_blur", framebuffer.textureWidth, framebuffer.textureHeight);

                  try (RenderPass local511_2x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_hand_outline_blur", this.field00053.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                        )) {
                     local511_2x.setPipeline(field00048);
                     local511_2x.bindTexture("Sampler0", this.field00052, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                     local511_2x.setUniform("HandOutlineParams", this.field00055);
                     this.method0470(local511_2x);
                  }

                  try (RenderPass local511_2x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "deluxe_hand_outline", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                     local511_2x.setPipeline(field00047);
                     local511_2x.bindTexture("Sampler0", this.field00053.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     local511_2x.bindTexture("Sampler1", this.field00052, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                     local511_2x.setUniform("HandOutlineParams", this.field00055);
                     this.method0470(local511_2x);
                  }
               }
            }
         }
      }
   }

   public void method0463() {
      if (this.field00056 && SwordAnimationsModule.method1770().method1766()) {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null && framebuffer.getDepthAttachment() != null) {
            GpuTexture depthAttachment = framebuffer.getDepthAttachment();
            if (this.field00051 != null
               && (
                  this.field00051.isClosed()
                     || this.field00051.getWidth(0) != framebuffer.textureWidth
                     || this.field00051.getHeight(0) != framebuffer.textureHeight
                     || this.field00051.getFormat() != depthAttachment.getFormat()
               )) {
               this.method0465();
            }

            if (this.field00051 == null) {
               this.field00051 = RenderSystem.getDevice()
                  .createTexture(() -> "deluxe_hand_depth", 15, depthAttachment.getFormat(), framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
               this.field00052 = RenderSystem.getDevice().createTextureView(this.field00051);
            }

            RenderSystem.getDevice()
               .createCommandEncoder()
               .copyTextureToTexture(depthAttachment, this.field00051, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            this.field00057 = true;
         }
      }
   }

   private void method0465() {
      if (this.field00052 != null) {
         if (!this.field00052.isClosed()) {
            this.field00052.close();
         }

         this.field00052 = null;
      }

      if (this.field00051 != null) {
         if (!this.field00051.isClosed()) {
            this.field00051.close();
         }

         this.field00051 = null;
      }
   }

   public static HandOutlineRenderer method0466() {
      if (handOutlineRenderer == null) {
         handOutlineRenderer = new HandOutlineRenderer();
      }

      return handOutlineRenderer;
   }

   public void method0468() {
      this.field00056 = false;
      this.field00057 = false;
      if (this.field00049 != null) {
         this.field00049.delete();
         this.field00049 = null;
      }

      if (this.field00050 != null) {
         this.field00050.delete();
         this.field00050 = null;
      }

      this.method0465();
      if (this.field00053 != null) {
         this.field00053.delete();
         this.field00053 = null;
      }

      if (this.field00054 != null && !this.field00054.isClosed()) {
         this.field00054.close();
      }

      this.field00054 = null;
      if (this.field00055 != null && !this.field00055.isClosed()) {
         this.field00055.close();
      }

      this.field00055 = null;
   }

   private SimpleFramebuffer method0469(SimpleFramebuffer simpleFramebuffer, String local519_2, int local519_3, int local519_4) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(local519_2, local519_3, local519_4, false);
      }

      if (simpleFramebuffer.textureWidth != local519_3 || simpleFramebuffer.textureHeight != local519_4) {
         simpleFramebuffer.resize(local519_3, local519_4);
      }

      return simpleFramebuffer;
   }

   private void method0470(RenderPass renderPass) {
      renderPass.setVertexBuffer(0, this.field00054);
      renderPass.setIndexBuffer(
         RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
      );
      renderPass.drawIndexed(0, 0, 6, 1);
   }

   private HandOutlineRenderer() {
   }

   public void method0472() {
      if (!SwordAnimationsModule.method1770().method1766()) {
         this.method0468();
      } else {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null && framebuffer.getDepthAttachment() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
            this.field00056 = true;
         }
      }
   }
}
