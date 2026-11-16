package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.SoftGlowDrawData;
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

public final class SoftGlowRenderer implements AutoCloseable {
   private static volatile SoftGlowRenderer softGlowRenderer;
   public static final RenderPipeline field08512 = RenderPipeline.builder()
      .withLocation(method2487("pipeline/soft_glow"))
      .withVertexShader(method2487("core/soft_glow"))
      .withFragmentShader(method2487("core/soft_glow"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("SoftGlowParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<SoftGlowDrawData> field08513 = new ArrayList<>(64);
   private DrawContext field08514;
   private GpuBuffer field08515;
   private boolean field08516 = true;

   public static void method2481() {
      SoftGlowRenderer currentSoftGlowRenderer = softGlowRenderer;
      if (currentSoftGlowRenderer != null) {
         currentSoftGlowRenderer.close();
         softGlowRenderer = null;
      }
   }

   public static SoftGlowRenderer method2483() {
      SoftGlowRenderer currentSoftGlowRenderer = softGlowRenderer;
      if (currentSoftGlowRenderer == null) {
         synchronized (SoftGlowRenderer.class) {
            currentSoftGlowRenderer = softGlowRenderer;
            if (currentSoftGlowRenderer == null) {
               currentSoftGlowRenderer = new SoftGlowRenderer();
               softGlowRenderer = currentSoftGlowRenderer;
            }
         }
      }

      return currentSoftGlowRenderer;
   }

   public boolean method2484(RenderPipeline renderPipeline) {
      return renderPipeline == field08512;
   }

   public void method2485() {
      this.field08513.clear();
      this.field08516 = false;
   }

   private void method2486(DrawContext drawContext, SoftGlowDrawData softGlowDrawData) {
      if (drawContext != null && softGlowDrawData != null && softGlowDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new SoftGlowRenderState(matrix3x2f, softGlowDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static Identifier method2487(String local3912_0) {
      return Identifier.of("deluxe", local3912_0);
   }

   public void method2488(DrawContext drawContext) {
      this.field08514 = drawContext;
   }

   private ByteBuffer method2489(MemoryStack memoryStack, List<SoftGlowDrawData> local3914_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(10240, local3914_2.size() * 5 * 4 * 4));

      for (int index = 0; index < local3914_2.size(); index++) {
         SoftGlowDrawData softGlowDrawData = local3914_2.get(index);
         int local3914_6 = index * 5 * 4 * 4;
         float local3914_7 = Math.max(softGlowDrawData.width() / 2.0F, 0.0F);
         float local3914_8 = Math.max(softGlowDrawData.height() / 2.0F, 0.0F);
         float local3914_9 = Math.min(local3914_7, local3914_8);
         byteBuffer.putFloat(local3914_6, local3914_7);
         byteBuffer.putFloat(local3914_6 + 4, local3914_8);
         byteBuffer.putFloat(local3914_6 + 8, softGlowDrawData.pad());
         byteBuffer.putFloat(local3914_6 + 12, Math.max(softGlowDrawData.softness(), 1.0E-4F));
         byteBuffer.putFloat(local3914_6 + 16, method2496(softGlowDrawData.radiusTopLeft(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 20, method2496(softGlowDrawData.radiusTopRight(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 24, method2496(softGlowDrawData.radiusBottomRight(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 28, method2496(softGlowDrawData.radiusBottomLeft(), 0.0F, local3914_9));
         int local3914_10 = softGlowDrawData.color();
         float local3914_11 = (local3914_10 >>> 24 & 0xFF) / 255.0F * Math.max(softGlowDrawData.intensity(), 0.0F);
         byteBuffer.putFloat(local3914_6 + 32, (local3914_10 >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 36, (local3914_10 >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 40, (local3914_10 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 44, local3914_11);
         byteBuffer.putFloat(local3914_6 + 48, softGlowDrawData.dirX());
         byteBuffer.putFloat(local3914_6 + 52, softGlowDrawData.dirY());
         byteBuffer.putFloat(local3914_6 + 56, method2496(softGlowDrawData.dirStrength(), 0.0F, 1.0F));
         byteBuffer.putFloat(local3914_6 + 60, softGlowDrawData.mode());
         byteBuffer.putFloat(local3914_6 + 64, Math.max(softGlowDrawData.falloff(), 0.25F));
         byteBuffer.putFloat(local3914_6 + 68, 0.0F);
         byteBuffer.putFloat(local3914_6 + 72, 0.0F);
         byteBuffer.putFloat(local3914_6 + 76, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private SoftGlowRenderer() {
   }

   public void method2490(RenderPass renderPass) {
      if (renderPass != null && !this.field08513.isEmpty()) {
         GpuBuffer gpuBuffer = this.method2492();
         if (gpuBuffer != null) {
            renderPass.setUniform("SoftGlowParams", gpuBuffer);
         }
      }
   }

   public void method2491() {
      if (!this.field08513.isEmpty() && this.field08516) {
         GpuBuffer gpuBuffer = this.method2494();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method2489(memoryStack, this.field08513);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field08516 = false;
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
               this.field08516 = true;
            }
         }
      }
   }

   private GpuBuffer method2492() {
      if (!this.field08516 && this.field08515 != null) {
         return this.field08515;
      }

      this.method2491();
      if (!this.field08516 && this.field08515 != null) {
         return this.field08515;
      }

      this.method2498();
      this.method2491();
      return this.field08515;
   }

   public void method2493() {
      this.field08514 = null;
   }

   @Override
   public void close() {
      this.field08513.clear();
      this.field08514 = null;
      this.method2498();
   }

   private GpuBuffer method2494() {
      if (this.field08515 != null && !this.field08515.isClosed() && this.field08515.size() >= 10240L) {
         return this.field08515;
      }

      this.method2498();

      try {
         this.field08515 = RenderSystem.getDevice().createBuffer(() -> "deluxe_soft_glow_params", 136, 10240L);
         return this.field08515;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void method2495(SoftGlowDrawData softGlowDrawData) {
      this.method2486(this.field08514, softGlowDrawData);
   }

   private static float method2496(float local3923_0, float local3923_1, float local3923_2) {
      return Math.max(local3923_1, Math.min(local3923_2, local3923_0));
   }

   int method2497(SoftGlowDrawData softGlowDrawData) {
      int local3924_2 = this.field08513.size();
      if (local3924_2 >= 128) {
         return -1;
      }

      this.field08513.add(softGlowDrawData);
      this.field08516 = true;
      return local3924_2;
   }

   private void method2498() {
      if (this.field08515 != null) {
         this.field08515.close();
         this.field08515 = null;
      }
   }

   public void method2499(DrawContext drawContext, SoftGlowDrawData softGlowDrawData) {
      this.method2488(drawContext);
      this.method2495(softGlowDrawData);
      this.method2493();
   }
}
