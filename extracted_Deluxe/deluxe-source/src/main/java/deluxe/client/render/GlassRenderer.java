package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.GradientSplitBuffer;
import deluxe.client.render.BlurDrawBatch;
import deluxe.client.render.GlassDrawData;
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

public final class GlassRenderer implements AutoCloseable {
   private static volatile GlassRenderer glassRenderer;
   public static final RenderPipeline field09276 = RenderPipeline.builder()
      .withLocation(method2696("pipeline/glass"))
      .withVertexShader(method2696("ui/glass/glass"))
      .withFragmentShader(method2696("ui/glass/glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<GlassDrawData> field09277 = new ArrayList<>(64);
   private final List<BlurDrawBatch> field09278 = new ArrayList<>(64);
   private DrawContext field09279;
   private GpuBuffer field09280;
   private boolean field09281 = true;

   private GpuBuffer method2693() {
      if (!this.field09281 && this.field09280 != null) {
         return this.field09280;
      }

      this.method2708();
      if (!this.field09281 && this.field09280 != null) {
         return this.field09280;
      }

      this.method2701();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         ByteBuffer byteBuffer = this.method2697(memoryStack, this.field09277);
         this.field09280 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glass_params", 128, byteBuffer);
         this.field09281 = false;
         gpuBuffer = this.field09280;
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

   private GpuBuffer method2694() {
      if (this.field09280 != null && !this.field09280.isClosed() && this.field09280.size() >= 57344L) {
         return this.field09280;
      }

      this.method2701();

      try {
         this.field09280 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glass_params", 136, 57344L);
         return this.field09280;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   int method2695(GlassDrawData glassDrawData, BlurDrawBatch blurDrawBatch) {
      int local4207_3 = this.field09277.size();
      if (local4207_3 == 448) {
         return -1;
      }

      this.field09277.add(glassDrawData);
      this.field09278.add(blurDrawBatch);
      this.field09281 = true;
      return local4207_3;
   }

   private static Identifier method2696(String local4208_0) {
      return Identifier.of("deluxe", local4208_0);
   }

   @Override
   public void close() {
      this.field09277.clear();
      this.field09279 = null;
      this.method2701();
   }

   private ByteBuffer method2697(MemoryStack memoryStack, List<GlassDrawData> local4210_2) {
      int local4210_3 = Math.max(1, local4210_2.size()) * 8 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(local4210_3);

      for (int index = 0; index < local4210_2.size(); index++) {
         GlassDrawData glassDrawData = local4210_2.get(index);
         int local4210_7 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local4210_7, glassDrawData.radiusTopLeft());
         byteBuffer.putFloat(local4210_7 + 4, glassDrawData.radiusTopRight());
         byteBuffer.putFloat(local4210_7 + 8, glassDrawData.radiusBottomRight());
         byteBuffer.putFloat(local4210_7 + 12, glassDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local4210_7 + 16, glassDrawData.width());
         byteBuffer.putFloat(local4210_7 + 20, glassDrawData.height());
         byteBuffer.putFloat(local4210_7 + 24, glassDrawData.splitIndex());
         byteBuffer.putFloat(local4210_7 + 28, Math.max(glassDrawData.squirt(), 0.001F));
         byteBuffer.putFloat(local4210_7 + 32, glassDrawData.globalAlpha());
         byteBuffer.putFloat(local4210_7 + 36, glassDrawData.fresnelPower());
         byteBuffer.putFloat(local4210_7 + 40, glassDrawData.baseAlpha());
         byteBuffer.putFloat(local4210_7 + 44, glassDrawData.fresnelMix());
         method2706(byteBuffer, local4210_7 + 48, glassDrawData.fresnelColor());
         byteBuffer.putFloat(local4210_7 + 64, glassDrawData.fresnelInvert() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local4210_7 + 68, glassDrawData.distortStrength());
         byteBuffer.putFloat(local4210_7 + 72, glassDrawData.z());
         byteBuffer.putFloat(local4210_7 + 76, glassDrawData.colorOffset());
         method2706(byteBuffer, local4210_7 + 80, glassDrawData.color());
         method2706(byteBuffer, local4210_7 + 96, glassDrawData.secondColor());
         BlurDrawBatch blurDrawBatch = index < this.field09278.size() ? this.field09278.get(index) : null;
         if (blurDrawBatch != null) {
            byteBuffer.putFloat(local4210_7 + 112, blurDrawBatch.field11731);
            byteBuffer.putFloat(local4210_7 + 116, blurDrawBatch.field11732);
            byteBuffer.putFloat(local4210_7 + 120, blurDrawBatch.field11733);
            byteBuffer.putFloat(local4210_7 + 124, blurDrawBatch.field11734);
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void method2698(DrawContext drawContext) {
      this.field09279 = drawContext;
   }

   public void method2699(RenderPass renderPass) {
      if (renderPass != null && !this.field09277.isEmpty()) {
         GpuBuffer gpuBuffer = this.method2693();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlassParamsArray", gpuBuffer);
         }

         GpuBuffer gpuBuffer2 = ThemePaletteBuffer.method2152();
         if (gpuBuffer2 != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
         }

         GpuBuffer gpuBuffer3 = GradientSplitBuffer.method3125();
         if (gpuBuffer3 != null) {
            renderPass.setUniform("SplitParams", gpuBuffer3);
         }
      }
   }

   private void method2701() {
      if (this.field09280 != null) {
         this.field09280.close();
         this.field09280 = null;
      }
   }

   public void method2702() {
      this.field09277.clear();
      this.field09278.clear();
      this.field09281 = false;
   }

   public static GlassRenderer method2703() {
      GlassRenderer currentGlassRenderer = glassRenderer;
      if (currentGlassRenderer == null) {
         synchronized (GlassRenderer.class) {
            currentGlassRenderer = glassRenderer;
            if (currentGlassRenderer == null) {
               currentGlassRenderer = new GlassRenderer();
               glassRenderer = currentGlassRenderer;
            }
         }
      }

      return currentGlassRenderer;
   }

   public void method2704(GlassDrawData glassDrawData) {
      this.method2712(this.field09279, glassDrawData);
   }

   private static void method2706(ByteBuffer byteBuffer, int local4219_1, int local4219_2) {
      byteBuffer.putFloat(local4219_1, (local4219_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 4, (local4219_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 8, (local4219_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 12, (local4219_2 >>> 24 & 0xFF) / 255.0F);
   }

   private static float method2707(float local4220_0, float local4220_1, float local4220_2) {
      return Math.max(local4220_1, Math.min(local4220_2, local4220_0));
   }

   public void method2708() {
      if (!this.field09277.isEmpty() && this.field09281) {
         GpuBuffer gpuBuffer = this.method2694();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method2697(memoryStack, this.field09277);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field09281 = false;
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
               this.field09281 = true;
            }
         }
      }
   }

   public void method2709(DrawContext drawContext, GlassDrawData glassDrawData) {
      this.method2698(drawContext);
      this.method2704(glassDrawData);
      this.method2714();
   }

   private GlassDrawData method2710(GlassDrawData glassDrawData) {
      float local4223_2 = Math.max(0.0F, Math.min(glassDrawData.width(), glassDrawData.height()) * 0.5F);
      float local4223_3 = method2707(glassDrawData.radiusTopLeft(), 0.0F, local4223_2);
      float local4223_4 = method2707(glassDrawData.radiusTopRight(), 0.0F, local4223_2);
      float local4223_5 = method2707(glassDrawData.radiusBottomRight(), 0.0F, local4223_2);
      float local4223_6 = method2707(glassDrawData.radiusBottomLeft(), 0.0F, local4223_2);
      float local4223_7 = method2707(glassDrawData.globalAlpha(), 0.0F, 1.0F);
      float local4223_8 = Math.max(glassDrawData.fresnelPower(), 0.001F);
      float local4223_9 = method2707(glassDrawData.baseAlpha(), 0.0F, 1.0F);
      float local4223_10 = method2707(glassDrawData.fresnelMix(), 0.0F, 1.0F);
      float local4223_11 = Math.max(glassDrawData.squirt(), 0.001F);
      float local4223_12 = Float.isFinite(glassDrawData.blurRadius()) ? method2707(glassDrawData.blurRadius(), 0.1F, 64.0F) : 0.1F;
      float local4223_13 = Float.isFinite(glassDrawData.colorOffset()) ? glassDrawData.colorOffset() : 0.0F;
      local4223_13 -= (float)Math.floor(local4223_13);
      return local4223_3 == glassDrawData.radiusTopLeft()
            && local4223_4 == glassDrawData.radiusTopRight()
            && local4223_5 == glassDrawData.radiusBottomRight()
            && local4223_6 == glassDrawData.radiusBottomLeft()
            && local4223_7 == glassDrawData.globalAlpha()
            && local4223_8 == glassDrawData.fresnelPower()
            && local4223_9 == glassDrawData.baseAlpha()
            && local4223_10 == glassDrawData.fresnelMix()
            && local4223_11 == glassDrawData.squirt()
            && local4223_12 == glassDrawData.blurRadius()
            && local4223_13 == glassDrawData.colorOffset()
         ? glassDrawData
         : new GlassDrawData(
               glassDrawData.x(),
               glassDrawData.y(),
               glassDrawData.width(),
               glassDrawData.height(),
               local4223_3,
               local4223_4,
               local4223_5,
               local4223_6,
               glassDrawData.color(),
               local4223_7,
               local4223_8,
               glassDrawData.fresnelColor(),
               local4223_9,
               glassDrawData.fresnelInvert(),
               local4223_10,
               glassDrawData.distortStrength(),
               local4223_11,
               glassDrawData.z(),
               local4223_12,
               glassDrawData.secondColor(),
               local4223_13
            )
            .withSplitIndex(glassDrawData.splitIndex());
   }

   public static void method2711() {
      GlassRenderer currentGlassRenderer = glassRenderer;
      if (currentGlassRenderer != null) {
         currentGlassRenderer.close();
         glassRenderer = null;
      }
   }

   private GlassRenderer() {
   }

   private void method2712(DrawContext drawContext, GlassDrawData glassDrawData) {
      if (drawContext != null && glassDrawData != null && glassDrawData.visible()) {
         try {
            GlassDrawData glassDrawData2 = this.method2710(glassDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            BlurDrawBatch blurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.method4504()
               .method4521(
                  drawContext,
                  new BlurDrawData(
                     glassDrawData2.x(),
                     glassDrawData2.y(),
                     glassDrawData2.width(),
                     glassDrawData2.height(),
                     glassDrawData2.radiusTopLeft(),
                     glassDrawData2.radiusTopRight(),
                     glassDrawData2.radiusBottomRight(),
                     glassDrawData2.radiusBottomLeft(),
                     1.0F,
                     glassDrawData2.blurRadius(),
                     -1
                  ),
                  blurDrawBatch
               );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlassRenderState(matrix3x2f, glassDrawData2, ScissorStack.method3969(), blurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   public boolean method2713(RenderPipeline renderPipeline) {
      return renderPipeline == field09276;
   }

   public void method2714() {
      this.field09279 = null;
   }
}
