package deluxe.client.render;

import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.DeluxeClient;
import deluxe.client.MinecraftAccess;
import deluxe.client.render.RoofHeightMap;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class WorldRainRenderer implements MinecraftAccess {
   private static WorldRainRenderer worldRainRenderer;
   private static final RenderPipeline field06899 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/world_rain"))
      .withVertexShader(DeluxeClient.id("core/ambient_particles"))
      .withFragmentShader(DeluxeClient.id("core/ambient_particles"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("AmbientParticlesParams", UniformType.UNIFORM_BUFFER)
      .build();
   private GpuBuffer field06901;
   private GpuBuffer field06902;
   private long field06903;

   private WorldRainRenderer() {
   }

   public void method1950() {
      if (this.field06901 != null && !this.field06901.isClosed()) {
         this.field06901.close();
      }

      this.field06901 = null;
      if (this.field06902 != null && !this.field06902.isClosed()) {
         this.field06902.close();
      }

      this.field06902 = null;
   }

   public static WorldRainRenderer method1953() {
      if (worldRainRenderer == null) {
         worldRainRenderer = new WorldRainRenderer();
      }

      return worldRainRenderer;
   }

   public void method1954(Matrix4f matrix4f, Matrix4f matrix4f2) {
      CustomWorldModule customWorldModule = CustomWorldModule.method2204();
      if (customWorldModule.isEnabled() && customWorldModule.field07430.isChecked() && mc.world != null) {
         float local3078_4 = customWorldModule.field07431.isChecked()
            ? mc.world.getRainGradient(mc.getRenderTickCounter().getTickProgress(false))
            : 1.0F;
         float local3078_5 = customWorldModule.field07432.getValue() * local3078_4;
         if (local3078_5 <= 0.002F) {
            if (this.field06901 != null && System.currentTimeMillis() - this.field06903 > 5000L) {
               this.method1950();
            }
         } else {
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer != null
               && framebuffer.getColorAttachmentView() != null
               && framebuffer.getDepthAttachmentView() != null
               && framebuffer.textureWidth > 0
               && framebuffer.textureHeight > 0) {
               RoofHeightMap.method3309();
               if (RoofHeightMap.method3310() != null) {
                  if (this.field06901 == null || this.field06901.isClosed()) {
                     Random random = new Random(1592639215L);

                     try (BufferAllocator bufferAllocator = new BufferAllocator(32000 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
                        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                        for (int index = 0; index < 8000; index++) {
                           float local3078_11 = random.nextFloat() * 40.0F;
                           float local3078_12 = random.nextFloat() * 30.0F;
                           float local3078_13 = random.nextFloat() * 40.0F;
                           int local3078_14 = 160 + random.nextInt(96) << 24
                              | random.nextInt(256) << 16
                              | random.nextInt(256) << 8
                              | random.nextInt(256);
                           bufferBuilder.vertex(local3078_11, local3078_12, local3078_13).texture(0.0F, 0.0F).color(local3078_14);
                           bufferBuilder.vertex(local3078_11, local3078_12, local3078_13).texture(0.0F, 1.0F).color(local3078_14);
                           bufferBuilder.vertex(local3078_11, local3078_12, local3078_13).texture(1.0F, 1.0F).color(local3078_14);
                           bufferBuilder.vertex(local3078_11, local3078_12, local3078_13).texture(1.0F, 0.0F).color(local3078_14);
                        }

                        try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                           this.field06901 = RenderSystem.getDevice().createBuffer(() -> "deluxe_world_rain_mesh", 32, builtBuffer.getBuffer());
                        }
                     }
                  }

                  if (this.field06902 == null || this.field06902.isClosed()) {
                     this.field06902 = RenderSystem.getDevice().createBuffer(() -> "deluxe_world_rain_params", 136, 192L);
                  }

                  int local3078_7 = MathHelper.clamp(Math.round(4000.0F * customWorldModule.field07433.getValue()), 1, 8000);
                  int local3078_8 = customWorldModule.field07440.isChecked() ? customWorldModule.field07441.getValue() : CustomWorldModule.method2220();
                  float local3078_9 = 22.0F * customWorldModule.field07434.getValue();
                  boolean isChecked = customWorldModule.field07439.isChecked();
                  MemoryStack memoryStack = MemoryStack.stackPush();

                  try {
                     ByteBuffer byteBuffer = memoryStack.calloc(192);
                     new Matrix4f(matrix4f2).mul(matrix4f).get(0, byteBuffer);
                     byteBuffer.putFloat(64, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                     byteBuffer.putFloat(68, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                     byteBuffer.putFloat(72, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                     byteBuffer.putFloat(76, (float)(System.currentTimeMillis() % 2000000L) / 1000.0F);
                     byteBuffer.putFloat(80, matrix4f.m00());
                     byteBuffer.putFloat(84, matrix4f.m10());
                     byteBuffer.putFloat(88, matrix4f.m20());
                     byteBuffer.putFloat(92, 0.09F);
                     byteBuffer.putFloat(96, matrix4f.m01());
                     byteBuffer.putFloat(100, matrix4f.m11());
                     byteBuffer.putFloat(104, matrix4f.m21());
                     byteBuffer.putFloat(108, 0.4F);
                     byteBuffer.putFloat(112, 1.6F * customWorldModule.field07434.getValue());
                     byteBuffer.putFloat(116, -local3078_9);
                     byteBuffer.putFloat(120, 1.0F * customWorldModule.field07434.getValue());
                     byteBuffer.putFloat(124, 0.0F);
                     byteBuffer.putFloat(128, 40.0F);
                     byteBuffer.putFloat(132, 30.0F);
                     byteBuffer.putFloat(136, 40.0F);
                     byteBuffer.putFloat(140, customWorldModule.field07435.getValue());
                     byteBuffer.putFloat(144, (local3078_8 >> 16 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(148, (local3078_8 >> 8 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(152, (local3078_8 & 0xFF) / 255.0F);
                     byteBuffer.putFloat(156, local3078_5);
                     byteBuffer.putFloat(160, RoofHeightMap.field11654);
                     byteBuffer.putFloat(164, RoofHeightMap.field11655);
                     byteBuffer.putFloat(168, RoofHeightMap.field11656);
                     byteBuffer.putFloat(172, isChecked ? 1.0F : 0.0F);
                     byteBuffer.putFloat(176, 64.0F);
                     RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field06902.slice(0L, 192L), byteBuffer);
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

                  try (RenderPass local3078_11x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_world_rain",
                           framebuffer.getColorAttachmentView(),
                           OptionalInt.empty(),
                           framebuffer.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )) {
                     local3078_11x.setPipeline(field06899);
                     local3078_11x.bindTexture("Sampler0", RoofHeightMap.method3310(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                     local3078_11x.setUniform("AmbientParticlesParams", this.field06902);
                     local3078_11x.setVertexBuffer(0, this.field06901);
                     local3078_11x.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local3078_7 * 6),
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     local3078_11x.drawIndexed(0, 0, local3078_7 * 6, 1);
                  }

                  this.field06903 = System.currentTimeMillis();
               }
            }
         }
      } else {
         this.method1950();
      }
   }
}
