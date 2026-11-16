package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.CircleDrawData;
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

public final class CircleRenderer implements AutoCloseable {
   private static volatile CircleRenderer circleRenderer;
   public static final RenderPipeline field14584 = RenderPipeline.builder()
      .withLocation(method4295("pipeline/circle"))
      .withVertexShader(method4295("core/circle"))
      .withFragmentShader(method4295("core/circle"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("CircleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<CircleDrawData> field14585 = new ArrayList<>(128);
   private DrawContext field14586;
   private GpuBuffer field14587;
   private boolean field14588 = true;

   private CircleDrawData method4287(CircleDrawData circleDrawData) {
      return new CircleDrawData(
         circleDrawData.x(),
         circleDrawData.y(),
         Math.max(circleDrawData.radius(), 0.0F),
         method4304(circleDrawData.thickness(), 0.0F, circleDrawData.radius()),
         Math.max(circleDrawData.smoothness(), 0.5F),
         circleDrawData.color()
      );
   }

   int method4288(CircleDrawData circleDrawData) {
      int local7202_2 = this.field14585.size();
      if (local7202_2 == 512) {
         return -1;
      }

      this.field14585.add(circleDrawData);
      this.field14588 = true;
      return local7202_2;
   }

   public boolean method4289(RenderPipeline renderPipeline) {
      return renderPipeline == field14584;
   }

   private GpuBuffer method4290() {
      if (!this.field14588 && this.field14587 != null) {
         return this.field14587;
      }

      this.method4302();
      if (!this.field14588 && this.field14587 != null) {
         return this.field14587;
      }

      this.method4301();
      this.method4302();
      return this.field14587;
   }

   public void method4291(DrawContext drawContext, CircleDrawData circleDrawData) {
      this.method4297(drawContext);
      this.method4303(circleDrawData);
      this.method4294();
   }

   @Override
   public void close() {
      this.field14585.clear();
      this.field14586 = null;
      this.method4301();
   }

   public static CircleRenderer method4292() {
      CircleRenderer currentCircleRenderer = circleRenderer;
      if (currentCircleRenderer == null) {
         synchronized (CircleRenderer.class) {
            currentCircleRenderer = circleRenderer;
            if (currentCircleRenderer == null) {
               currentCircleRenderer = new CircleRenderer();
               circleRenderer = currentCircleRenderer;
            }
         }
      }

      return currentCircleRenderer;
   }

   private GpuBuffer method4293() {
      if (this.field14587 != null && !this.field14587.isClosed() && this.field14587.size() >= 16384L) {
         return this.field14587;
      }

      this.method4301();

      try {
         this.field14587 = RenderSystem.getDevice().createBuffer(() -> "deluxe_circle_params", 136, 16384L);
         return this.field14587;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void method4294() {
      this.field14586 = null;
   }

   private static Identifier method4295(String local7211_0) {
      return Identifier.of("deluxe", local7211_0);
   }

   private CircleRenderer() {
   }

   public void method4296(RenderPass renderPass) {
      if (renderPass != null && !this.field14585.isEmpty()) {
         GpuBuffer gpuBuffer = this.method4290();
         if (gpuBuffer != null) {
            renderPass.setUniform("CircleParamsArray", gpuBuffer);
         }
      }
   }

   public void method4297(DrawContext drawContext) {
      this.field14586 = drawContext;
   }

   private void method4298(DrawContext drawContext, CircleDrawData circleDrawData) {
      if (drawContext != null && circleDrawData != null && circleDrawData.visible()) {
         try {
            CircleDrawData circleDrawData2 = this.method4287(circleDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new CircleRenderState(matrix3x2f, circleDrawData2, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public static void method4299() {
      CircleRenderer currentCircleRenderer = circleRenderer;
      if (currentCircleRenderer != null) {
         currentCircleRenderer.close();
         circleRenderer = null;
      }
   }

   private ByteBuffer method4300(MemoryStack memoryStack, List<CircleDrawData> local7217_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(16384, local7217_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local7217_2.size(); index++) {
         CircleDrawData circleDrawData = local7217_2.get(index);
         int local7217_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local7217_6, circleDrawData.radius());
         byteBuffer.putFloat(local7217_6 + 4, Math.max(circleDrawData.smoothness(), 0.0F));
         byteBuffer.putFloat(local7217_6 + 8, circleDrawData.thickness());
         byteBuffer.putFloat(local7217_6 + 12, 0.0F);
         method4307(byteBuffer, local7217_6 + 16, circleDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void method4301() {
      if (this.field14587 != null) {
         this.field14587.close();
         this.field14587 = null;
      }
   }

   public void method4302() {
      if (!this.field14585.isEmpty() && this.field14588) {
         GpuBuffer gpuBuffer = this.method4293();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method4300(memoryStack, this.field14585);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field14588 = false;
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
               this.field14588 = true;
            }
         }
      }
   }

   public void method4303(CircleDrawData circleDrawData) {
      this.method4298(this.field14586, circleDrawData);
   }

   private static float method4304(float local7221_0, float local7221_1, float local7221_2) {
      return Math.max(local7221_1, Math.min(local7221_2, local7221_0));
   }

   public void method4305() {
      this.field14585.clear();
      this.field14588 = false;
   }

   private static void method4307(ByteBuffer byteBuffer, int local7224_1, int local7224_2) {
      byteBuffer.putFloat(local7224_1, (local7224_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 4, (local7224_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 8, (local7224_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 12, (local7224_2 >>> 24 & 0xFF) / 255.0F);
   }
}
