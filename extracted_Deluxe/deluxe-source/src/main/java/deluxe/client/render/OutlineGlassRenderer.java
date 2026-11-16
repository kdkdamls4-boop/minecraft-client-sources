package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.BlurDrawBatch;
import deluxe.client.render.GlassOutlineDrawData;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.BlurDrawData;

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

public final class OutlineGlassRenderer implements AutoCloseable {
   private static volatile OutlineGlassRenderer outlineGlassRenderer;
   public static final RenderPipeline field08405 = RenderPipeline.builder()
      .withLocation(method2427("pipeline/outline_glass"))
      .withVertexShader(method2427("core/outline_glass"))
      .withFragmentShader(method2427("core/outline_glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassOutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<GlassOutlineDrawData> field08406 = new ArrayList<>(128);
   private final List<BlurDrawBatch> field08407 = new ArrayList<>(128);
   private DrawContext field08408;
   private GpuBuffer field08409;
   private boolean field08410 = true;

   public void method2415(RenderPass renderPass) {
      if (renderPass != null && !this.field08406.isEmpty()) {
         GpuBuffer gpuBuffer = this.method2417();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlassOutlineParamsArray", gpuBuffer);
         }
      }
   }

   private void method2416() {
      if (this.field08409 != null) {
         this.field08409.close();
         this.field08409 = null;
      }
   }

   private GpuBuffer method2417() {
      if (!this.field08410 && this.field08409 != null) {
         return this.field08409;
      }

      this.method2420();
      if (!this.field08410 && this.field08409 != null) {
         return this.field08409;
      }

      this.method2416();
      this.method2420();
      return this.field08409;
   }

   @Override
   public void close() {
      this.field08406.clear();
      this.field08407.clear();
      this.field08408 = null;
      this.method2416();
   }

   private static float method2418(float local3815_0, float local3815_1, float local3815_2) {
      return Math.max(local3815_1, Math.min(local3815_2, local3815_0));
   }

   public void method2419() {
      this.field08408 = null;
   }

   public void method2420() {
      if (!this.field08406.isEmpty() && this.field08410) {
         GpuBuffer gpuBuffer = this.method2424();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method2429(memoryStack, this.field08406);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field08410 = false;
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
               this.field08410 = true;
            }
         }
      }
   }

   private void method2421(DrawContext drawContext, GlassOutlineDrawData glassOutlineDrawData) {
      if (drawContext != null && glassOutlineDrawData != null && glassOutlineDrawData.visible()) {
         try {
            GlassOutlineDrawData glassOutlineDrawData2 = this.method2435(glassOutlineDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            BlurDrawBatch blurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.method4504()
               .method4521(
                  drawContext,
                  new BlurDrawData(
                     glassOutlineDrawData2.x(),
                     glassOutlineDrawData2.y(),
                     glassOutlineDrawData2.width(),
                     glassOutlineDrawData2.height(),
                     glassOutlineDrawData2.radiusTopLeft(),
                     glassOutlineDrawData2.radiusTopRight(),
                     glassOutlineDrawData2.radiusBottomRight(),
                     glassOutlineDrawData2.radiusBottomLeft(),
                     1.0F,
                     30.0F,
                     -1
                  ),
                  blurDrawBatch
               );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlassOutlineRenderState(matrix3x2f, glassOutlineDrawData2, ScissorStack.method3969(), blurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private OutlineGlassRenderer() {
   }

   private static void method2423(ByteBuffer byteBuffer, int local3821_1, int local3821_2) {
      byteBuffer.putFloat(local3821_1, (local3821_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 4, (local3821_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 8, (local3821_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 12, (local3821_2 >>> 24 & 0xFF) / 255.0F);
   }

   private GpuBuffer method2424() {
      if (this.field08409 != null && !this.field08409.isClosed() && this.field08409.size() >= 49056L) {
         return this.field08409;
      }

      this.method2416();

      try {
         this.field08409 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_glass_params", 136, 49056L);
         return this.field08409;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   int method2425(GlassOutlineDrawData glassOutlineDrawData, BlurDrawBatch blurDrawBatch) {
      int local3823_3 = this.field08406.size();
      if (local3823_3 == 438) {
         return -1;
      }

      this.field08406.add(glassOutlineDrawData);
      this.field08407.add(blurDrawBatch);
      this.field08410 = true;
      return local3823_3;
   }

   public void method2426() {
      this.field08406.clear();
      this.field08407.clear();
      this.field08410 = false;
   }

   private static Identifier method2427(String local3825_0) {
      return Identifier.of("deluxe", local3825_0);
   }

   public void method2428(GlassOutlineDrawData glassOutlineDrawData) {
      this.method2421(this.field08408, glassOutlineDrawData);
   }

   private ByteBuffer method2429(MemoryStack memoryStack, List<GlassOutlineDrawData> local3827_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(49056, local3827_2.size() * 7 * 4 * 4));

      for (int index = 0; index < local3827_2.size(); index++) {
         GlassOutlineDrawData glassOutlineDrawData = local3827_2.get(index);
         int local3827_6 = index * 7 * 4 * 4;
         byteBuffer.putFloat(local3827_6, glassOutlineDrawData.radiusTopLeft());
         byteBuffer.putFloat(local3827_6 + 4, glassOutlineDrawData.radiusTopRight());
         byteBuffer.putFloat(local3827_6 + 8, glassOutlineDrawData.radiusBottomRight());
         byteBuffer.putFloat(local3827_6 + 12, glassOutlineDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local3827_6 + 16, glassOutlineDrawData.width());
         byteBuffer.putFloat(local3827_6 + 20, glassOutlineDrawData.height());
         byteBuffer.putFloat(local3827_6 + 24, glassOutlineDrawData.thickness());
         byteBuffer.putFloat(local3827_6 + 28, glassOutlineDrawData.smoothness());
         byteBuffer.putFloat(local3827_6 + 32, glassOutlineDrawData.globalAlpha());
         byteBuffer.putFloat(local3827_6 + 36, glassOutlineDrawData.fresnelPower());
         byteBuffer.putFloat(local3827_6 + 40, glassOutlineDrawData.baseAlpha());
         byteBuffer.putFloat(local3827_6 + 44, glassOutlineDrawData.fresnelMix());
         method2423(byteBuffer, local3827_6 + 48, glassOutlineDrawData.fresnelColor());
         byteBuffer.putFloat(local3827_6 + 64, glassOutlineDrawData.fresnelInvert() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local3827_6 + 68, glassOutlineDrawData.distortStrength());
         byteBuffer.putFloat(local3827_6 + 72, glassOutlineDrawData.z());
         byteBuffer.putFloat(local3827_6 + 76, Math.max(glassOutlineDrawData.squirt(), 0.001F));
         method2423(byteBuffer, local3827_6 + 80, glassOutlineDrawData.color());
         BlurDrawBatch blurDrawBatch = index < this.field08407.size() ? this.field08407.get(index) : null;
         if (blurDrawBatch != null) {
            byteBuffer.putFloat(local3827_6 + 96, blurDrawBatch.field11731);
            byteBuffer.putFloat(local3827_6 + 100, blurDrawBatch.field11732);
            byteBuffer.putFloat(local3827_6 + 104, blurDrawBatch.field11733);
            byteBuffer.putFloat(local3827_6 + 108, blurDrawBatch.field11734);
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public boolean method2430(RenderPipeline renderPipeline) {
      return renderPipeline == field08405;
   }

   public void method2431(DrawContext drawContext, GlassOutlineDrawData glassOutlineDrawData) {
      this.method2433(drawContext);
      this.method2428(glassOutlineDrawData);
      this.method2419();
   }

   public static OutlineGlassRenderer method2432() {
      OutlineGlassRenderer currentOutlineGlassRenderer = outlineGlassRenderer;
      if (currentOutlineGlassRenderer == null) {
         synchronized (OutlineGlassRenderer.class) {
            currentOutlineGlassRenderer = outlineGlassRenderer;
            if (currentOutlineGlassRenderer == null) {
               currentOutlineGlassRenderer = new OutlineGlassRenderer();
               outlineGlassRenderer = currentOutlineGlassRenderer;
            }
         }
      }

      return currentOutlineGlassRenderer;
   }

   public void method2433(DrawContext drawContext) {
      this.field08408 = drawContext;
   }

   public static void method2434() {
      OutlineGlassRenderer currentOutlineGlassRenderer = outlineGlassRenderer;
      if (currentOutlineGlassRenderer != null) {
         currentOutlineGlassRenderer.close();
         outlineGlassRenderer = null;
      }
   }

   private GlassOutlineDrawData method2435(GlassOutlineDrawData glassOutlineDrawData) {
      float local3833_2 = Math.max(0.0F, Math.min(glassOutlineDrawData.width(), glassOutlineDrawData.height()) * 0.5F);
      float local3833_3 = Math.max(0.0F, Math.min(glassOutlineDrawData.width(), glassOutlineDrawData.height()) * 0.5F);
      return new GlassOutlineDrawData(
         glassOutlineDrawData.x(),
         glassOutlineDrawData.y(),
         glassOutlineDrawData.width(),
         glassOutlineDrawData.height(),
         method2418(glassOutlineDrawData.radiusTopLeft(), 0.0F, local3833_2),
         method2418(glassOutlineDrawData.radiusTopRight(), 0.0F, local3833_2),
         method2418(glassOutlineDrawData.radiusBottomRight(), 0.0F, local3833_2),
         method2418(glassOutlineDrawData.radiusBottomLeft(), 0.0F, local3833_2),
         method2418(glassOutlineDrawData.thickness(), 0.0F, local3833_3),
         glassOutlineDrawData.color(),
         method2418(glassOutlineDrawData.globalAlpha(), 0.0F, 1.0F),
         Math.max(glassOutlineDrawData.fresnelPower(), 0.001F),
         glassOutlineDrawData.fresnelColor(),
         method2418(glassOutlineDrawData.baseAlpha(), 0.0F, 1.0F),
         glassOutlineDrawData.fresnelInvert(),
         method2418(glassOutlineDrawData.fresnelMix(), 0.0F, 1.0F),
         glassOutlineDrawData.distortStrength(),
         Math.max(glassOutlineDrawData.squirt(), 0.001F),
         Math.max(glassOutlineDrawData.smoothness(), 0.0F),
         glassOutlineDrawData.z()
      );
   }
}
