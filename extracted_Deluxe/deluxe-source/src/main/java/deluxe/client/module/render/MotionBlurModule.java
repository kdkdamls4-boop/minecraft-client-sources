package deluxe.client.module.render;

import deluxe.client.DeluxeClient;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;

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
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "MotionBlur", description = "Смазывает картинку шлейфом из предыдущих кадров.", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {
   private static MotionBlurModule motionBlurModule;
   private static final RenderPipeline field13282 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/motion_blur"))
      .withVertexShader(DeluxeClient.id("core/motion_blur"))
      .withFragmentShader(DeluxeClient.id("core/motion_blur"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("MotionBlurParams", UniformType.UNIFORM_BUFFER)
      .build();
   public NumberSetting numberSetting = this.addSetting(
      new NumberSetting("Сила", "Насколько долго тянется шлейф за кадром.").withRange(1.0F, 10.0F).withStep(1.0F).setValue(5.0F)
   );
   private SimpleFramebuffer field13284;
   private GpuBuffer field13285;
   private GpuBuffer field13286;
   private boolean field13287;
   private long field13288;
   private ClientWorld field13289;

   @Override
   protected void onEnable() {
      this.field13287 = false;
   }

   private MotionBlurModule() {
   }

   public static MotionBlurModule method3837() {
      if (motionBlurModule == null) {
         motionBlurModule = new MotionBlurModule();
      }

      return motionBlurModule;
   }

   @Override
   protected void onDisable() {
      this.field13287 = false;
      if (this.field13284 != null) {
         this.field13284.delete();
         this.field13284 = null;
      }

      if (this.field13285 != null && !this.field13285.isClosed()) {
         this.field13285.close();
      }

      this.field13285 = null;
      if (this.field13286 != null && !this.field13286.isClosed()) {
         this.field13286.close();
      }

      this.field13286 = null;
   }

   public static void method3838() {
      if (motionBlurModule != null && motionBlurModule.isEnabled()) {
         motionBlurModule.method3839();
      }
   }

   private void method3839() {
      if (mc.world != null
         && mc.getFramebuffer() != null
         && mc.getFramebuffer().getColorAttachmentView() != null
         && mc.getFramebuffer().textureWidth > 0
         && mc.getFramebuffer().textureHeight > 0) {
         if (this.field13289 != mc.world) {
            this.field13289 = mc.world;
            this.field13287 = false;
         }

         if (this.field13284 == null) {
            this.field13284 = new SimpleFramebuffer(
               "deluxe_motion_blur_history", mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight, false
            );
            this.field13287 = false;
         }

         if (this.field13284.textureWidth != mc.getFramebuffer().textureWidth
            || this.field13284.textureHeight != mc.getFramebuffer().textureHeight) {
            this.field13284.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            this.field13287 = false;
         }

         if (this.field13287) {
            if (this.field13285 == null || this.field13285.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.field13285 = RenderSystem.getDevice().createBuffer(() -> "deluxe_motion_blur_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.field13286 == null || this.field13286.isClosed()) {
               this.field13286 = RenderSystem.getDevice().createBuffer(() -> "deluxe_motion_blur_params", 136, 16L);
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(16);
               byteBuffer.putFloat(
                  0,
                  (float)Math.pow(
                     this.numberSetting.floatValue() / 100.0F * 0.9F, Math.max(1L, Math.min(100L, System.currentTimeMillis() - this.field13288)) / 50.0
                  )
               );
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field13286.slice(0L, 16L), byteBuffer);
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

            try (RenderPass local6319_1x = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> "deluxe_motion_blur", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
               local6319_1x.setPipeline(field13282);
               local6319_1x.bindTexture("Sampler0", this.field13284.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
               local6319_1x.setUniform("MotionBlurParams", this.field13286);
               local6319_1x.setVertexBuffer(0, this.field13285);
               local6319_1x.setIndexBuffer(
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
               );
               local6319_1x.drawIndexed(0, 0, 6, 1);
            }
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(
               mc.getFramebuffer().getColorAttachment(),
               this.field13284.getColorAttachment(),
               0,
               0,
               0,
               0,
               0,
               mc.getFramebuffer().textureWidth,
               mc.getFramebuffer().textureHeight
            );
         this.field13287 = true;
         this.field13288 = System.currentTimeMillis();
      }
   }
}
