package deluxe.client.render;

import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.DeluxeClient;
import deluxe.client.MinecraftAccess;
import deluxe.client.render.RoofHeightMap;
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
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class RainScreenRenderer implements MinecraftAccess {
   private static RainScreenRenderer rainScreenRenderer;
   private static final RenderPipeline field14834 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/rain_screen"))
      .withVertexShader(DeluxeClient.id("core/rain_screen"))
      .withFragmentShader(DeluxeClient.id("core/rain_screen"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withSampler("Sampler2")
      .withUniform("RainScreenParams", UniformType.UNIFORM_BUFFER)
      .build();
   private GpuBuffer field14835;
   private GpuBuffer field14836;
   private float field14837;

   public void method4318() {
      SceneCopyBuffer.method3922();
      if (this.field14835 != null && !this.field14835.isClosed()) {
         this.field14835.close();
      }

      this.field14835 = null;
      if (this.field14836 != null && !this.field14836.isClosed()) {
         this.field14836.close();
      }

      this.field14836 = null;
   }

   public static RainScreenRenderer method4319() {
      if (rainScreenRenderer == null) {
         rainScreenRenderer = new RainScreenRenderer();
      }

      return rainScreenRenderer;
   }

   public void method4322(Matrix4f matrix4f, Matrix4f matrix4f2) {
      CustomWorldModule customWorldModule = CustomWorldModule.method2204();
      if (customWorldModule.isEnabled() && customWorldModule.field07430.isChecked() && mc.world != null) {
         float local7284_4 = customWorldModule.field07431.isChecked()
            ? mc.world.getRainGradient(mc.getRenderTickCounter().getTickProgress(false))
            : 1.0F;
         boolean local7284_5 = mc.world.hasRain(BlockPos.ofFloored(mc.gameRenderer.getCamera().getCameraPos()));
         this.field14837 = this.field14837 + ((!local7284_5 && customWorldModule.field07431.isChecked() ? 0.0F : 1.0F) - this.field14837) * 0.06F;
         float local7284_6 = customWorldModule.field07437.getValue() * local7284_4 * this.field14837;
         float local7284_7 = customWorldModule.field07438.getValue() * local7284_4;
         if (local7284_6 <= 0.002F && local7284_7 <= 0.002F) {
            SceneCopyBuffer.method3922();
         } else {
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer != null
               && framebuffer.getColorAttachment() != null
               && framebuffer.getColorAttachmentView() != null
               && framebuffer.getDepthAttachmentView() != null
               && framebuffer.textureWidth > 0
               && framebuffer.textureHeight > 0) {
               if (this.field14835 == null || this.field14835.isClosed()) {
                  try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                     BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                     bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                     bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                     try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                        this.field14835 = RenderSystem.getDevice().createBuffer(() -> "deluxe_rain_screen_quad", 32, builtBuffer.getBuffer());
                     }
                  }
               }

               RoofHeightMap.method3309();
               if (RoofHeightMap.method3310() != null) {
                  GpuTextureView gpuTextureView = SceneCopyBuffer.method3920(framebuffer);
                  if (gpuTextureView != null) {
                     boolean local7284_10 = customWorldModule.field07439.isChecked() && local7284_7 > 0.002F;
                     if (this.field14836 == null || this.field14836.isClosed()) {
                        this.field14836 = RenderSystem.getDevice().createBuffer(() -> "deluxe_rain_screen_params", 136, 128L);
                     }

                     int local7284_11 = customWorldModule.field07440.isChecked() ? customWorldModule.field07441.getValue() : CustomWorldModule.method2220();
                     MemoryStack memoryStack = MemoryStack.stackPush();

                     try {
                        ByteBuffer byteBuffer = memoryStack.calloc(128);
                        new Matrix4f(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer);
                        byteBuffer.putFloat(64, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                        byteBuffer.putFloat(68, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                        byteBuffer.putFloat(72, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                        byteBuffer.putFloat(76, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
                        byteBuffer.putFloat(80, (local7284_11 >> 16 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(84, (local7284_11 >> 8 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(88, (local7284_11 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(92, (float)framebuffer.textureWidth / framebuffer.textureHeight);
                        byteBuffer.putFloat(96, local7284_6);
                        byteBuffer.putFloat(100, local7284_7);
                        byteBuffer.putFloat(104, 64.0F);
                        byteBuffer.putFloat(112, RoofHeightMap.field11654);
                        byteBuffer.putFloat(116, RoofHeightMap.field11655);
                        byteBuffer.putFloat(120, RoofHeightMap.field11656);
                        byteBuffer.putFloat(124, local7284_10 ? 1.0F : 0.0F);
                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14836.slice(0L, 128L), byteBuffer);
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

                     try (RenderPass local7284_12x = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "deluxe_rain_screen", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        local7284_12x.setPipeline(field14834);
                        local7284_12x.bindTexture("Sampler0", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local7284_12x.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local7284_12x.bindTexture("Sampler2", RoofHeightMap.method3310(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local7284_12x.setUniform("RainScreenParams", this.field14836);
                        local7284_12x.setVertexBuffer(0, this.field14835);
                        local7284_12x.setIndexBuffer(
                           RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                        );
                        local7284_12x.drawIndexed(0, 0, 6, 1);
                     }
                  }
               }
            }
         }
      } else {
         this.method4318();
      }
   }

   private RainScreenRenderer() {
   }
}
