package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.render.Outline360DrawData;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ArcColorSpan;

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

public final class Outline360Renderer implements AutoCloseable {
   private static volatile Outline360Renderer outline360Renderer;
   public static final RenderPipeline field09310 = RenderPipeline.builder()
      .withLocation(method2738("pipeline/outline_360"))
      .withVertexShader(method2738("core/outline_360"))
      .withFragmentShader(method2738("core/outline_360"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360ParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360RangesArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<Outline360DrawData> field09311 = new ArrayList<>(64);
   private DrawContext field09312;
   private GpuBuffer field09313;
   private GpuBuffer field09314;
   private boolean field09315 = true;

   public static void method2716() {
      Outline360Renderer currentOutline360Renderer = outline360Renderer;
      if (currentOutline360Renderer != null) {
         currentOutline360Renderer.close();
         outline360Renderer = null;
      }
   }

   private static void method2717(ByteBuffer byteBuffer, int local4231_1, int local4231_2) {
      byteBuffer.putFloat(local4231_1, (local4231_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 4, (local4231_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 8, (local4231_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 12, (local4231_2 >>> 24 & 0xFF) / 255.0F);
   }

   private static float method2718(float local4232_0, float local4232_1, float local4232_2) {
      return Math.max(local4232_1, Math.min(local4232_2, local4232_0));
   }

   private void method2719() {
      if (this.field09315 || this.field09313 == null || this.field09314 == null) {
         this.method2736();
         if (this.field09315 || this.field09313 == null || this.field09314 == null) {
            this.method2732();
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(16384);
               ByteBuffer byteBuffer2 = memoryStack.calloc(32768);
               this.method2734(byteBuffer, byteBuffer2, this.field09311);
               this.field09313 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_360_params", 128, byteBuffer);
               this.field09314 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_360_ranges", 128, byteBuffer2);
               this.field09315 = false;
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
         }
      }
   }

   private boolean method2720() {
      boolean local4234_1 = this.field09313 != null && !this.field09313.isClosed() && this.field09313.size() >= 16384L;
      boolean local4234_2 = this.field09314 != null && !this.field09314.isClosed() && this.field09314.size() >= 32768L;
      if (local4234_1 && local4234_2) {
         return true;
      }

      this.method2732();

      try {
         this.field09313 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_360_params", 136, 16384L);
         this.field09314 = RenderSystem.getDevice().createBuffer(() -> "deluxe_outline_360_ranges", 136, 32768L);
         return true;
      } catch (RuntimeException exception) {
         this.method2732();
         return false;
      }
   }

   public void method2721(DrawContext drawContext) {
      this.field09312 = drawContext;
   }

   public void method2723() {
      this.field09311.clear();
      this.field09315 = false;
   }

   public void method2724(RenderPass renderPass) {
      if (renderPass != null && !this.field09311.isEmpty()) {
         this.method2719();
         if (this.field09313 != null) {
            renderPass.setUniform("Outline360ParamsArray", this.field09313);
         }

         if (this.field09314 != null) {
            renderPass.setUniform("Outline360RangesArray", this.field09314);
         }
      }
   }

   public boolean method2725(RenderPipeline renderPipeline) {
      return renderPipeline == field09310;
   }

   private Outline360DrawData method2727(Outline360DrawData outline360DrawData) {
      float local4241_2 = Math.max(0.0F, Math.min(outline360DrawData.width(), outline360DrawData.height()) * 0.5F);
      float local4241_3 = Math.max(0.0F, Math.min(outline360DrawData.width(), outline360DrawData.height()) * 0.5F);
      return new Outline360DrawData(
         outline360DrawData.x(),
         outline360DrawData.y(),
         outline360DrawData.width(),
         outline360DrawData.height(),
         method2718(outline360DrawData.radiusTopLeft(), 0.0F, local4241_2),
         method2718(outline360DrawData.radiusTopRight(), 0.0F, local4241_2),
         method2718(outline360DrawData.radiusBottomRight(), 0.0F, local4241_2),
         method2718(outline360DrawData.radiusBottomLeft(), 0.0F, local4241_2),
         method2718(outline360DrawData.thickness(), 0.0F, local4241_3),
         outline360DrawData.defaultColor(),
         Math.max(outline360DrawData.smoothness(), 0.0F),
         Math.max(outline360DrawData.blendDegrees(), 0.0F),
         outline360DrawData.angleOffsetDegrees(),
         outline360DrawData.ranges()
      );
   }

   @Override
   public void close() {
      this.field09311.clear();
      this.field09312 = null;
      this.method2732();
   }

   private void method2728(DrawContext drawContext, Outline360DrawData outline360DrawData) {
      if (drawContext != null && outline360DrawData != null && outline360DrawData.visible()) {
         try {
            Outline360DrawData outline360DrawData2 = this.method2727(outline360DrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new Outline360RenderState(matrix3x2f, outline360DrawData2));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void method2729(DrawContext drawContext, Outline360DrawData outline360DrawData) {
      this.method2721(drawContext);
      this.method2735(outline360DrawData);
      this.method2737();
   }

   int method2730(Outline360DrawData outline360DrawData) {
      int local4245_2 = this.field09311.size();
      if (local4245_2 == 256) {
         return -1;
      }

      this.field09311.add(outline360DrawData);
      this.field09315 = true;
      return local4245_2;
   }

   public static Outline360Renderer method2731() {
      Outline360Renderer currentOutline360Renderer = outline360Renderer;
      if (currentOutline360Renderer == null) {
         synchronized (Outline360Renderer.class) {
            currentOutline360Renderer = outline360Renderer;
            if (currentOutline360Renderer == null) {
               currentOutline360Renderer = new Outline360Renderer();
               outline360Renderer = currentOutline360Renderer;
            }
         }
      }

      return currentOutline360Renderer;
   }

   private Outline360Renderer() {
   }

   private void method2732() {
      if (this.field09313 != null) {
         this.field09313.close();
         this.field09313 = null;
      }

      if (this.field09314 != null) {
         this.field09314.close();
         this.field09314 = null;
      }
   }

   private void method2734(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, List<Outline360DrawData> local4251_3) {
      int local4251_4 = 0;

      for (int index = 0; index < local4251_3.size(); index++) {
         Outline360DrawData outline360DrawData = local4251_3.get(index);
         int local4251_7 = index * 4 * 4 * 4;
         int local4251_8 = Math.min(outline360DrawData.ranges().size(), 1024 - local4251_4);
         byteBuffer.putFloat(local4251_7, outline360DrawData.radiusTopLeft());
         byteBuffer.putFloat(local4251_7 + 4, outline360DrawData.radiusTopRight());
         byteBuffer.putFloat(local4251_7 + 8, outline360DrawData.radiusBottomRight());
         byteBuffer.putFloat(local4251_7 + 12, outline360DrawData.radiusBottomLeft());
         byteBuffer.putFloat(local4251_7 + 16, outline360DrawData.width());
         byteBuffer.putFloat(local4251_7 + 20, outline360DrawData.height());
         byteBuffer.putFloat(local4251_7 + 24, outline360DrawData.thickness());
         byteBuffer.putFloat(local4251_7 + 28, outline360DrawData.smoothness());
         method2717(byteBuffer, local4251_7 + 32, outline360DrawData.defaultColor());
         byteBuffer.putFloat(local4251_7 + 48, local4251_4);
         byteBuffer.putFloat(local4251_7 + 52, local4251_8);
         byteBuffer.putFloat(local4251_7 + 56, outline360DrawData.blendDegrees());
         byteBuffer.putFloat(local4251_7 + 60, outline360DrawData.angleOffsetDegrees());

         for (int index2 = 0; index2 < local4251_8; index2++) {
            ArcColorSpan arcColorSpan = outline360DrawData.ranges().get(index2);
            int local4251_11 = (local4251_4 + index2) * 2 * 4 * 4;
            byteBuffer2.putFloat(local4251_11, arcColorSpan.startDegrees());
            byteBuffer2.putFloat(local4251_11 + 4, arcColorSpan.endDegrees());
            byteBuffer2.putFloat(local4251_11 + 8, 0.0F);
            byteBuffer2.putFloat(local4251_11 + 12, 0.0F);
            method2717(byteBuffer2, local4251_11 + 16, arcColorSpan.color());
         }

         local4251_4 += local4251_8;
      }

      byteBuffer.position(0);
      byteBuffer2.position(0);
   }

   public void method2735(Outline360DrawData outline360DrawData) {
      this.method2728(this.field09312, outline360DrawData);
   }

   public void method2736() {
      if (!this.field09311.isEmpty() && this.field09315) {
         if (this.method2720()) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(16384);
                  ByteBuffer byteBuffer2 = memoryStack.calloc(32768);
                  this.method2734(byteBuffer, byteBuffer2, this.field09311);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field09313.slice(0L, byteBuffer.remaining()), byteBuffer);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field09314.slice(0L, byteBuffer2.remaining()), byteBuffer2);
                  this.field09315 = false;
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
               this.field09315 = true;
            }
         }
      }
   }

   public void method2737() {
      this.field09312 = null;
   }

   private static Identifier method2738(String local4255_0) {
      return Identifier.of("deluxe", local4255_0);
   }
}
