package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.CornerBracketDrawData;
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

public final class CornerBracketRenderer implements AutoCloseable {
   private static volatile CornerBracketRenderer cornerBracketRenderer;
   public static final RenderPipeline field14562 = RenderPipeline.builder()
      .withLocation(method4281("pipeline/corner_bracket"))
      .withVertexShader(method4281("core/corner_bracket"))
      .withFragmentShader(method4281("core/corner_bracket"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BracketParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<CornerBracketDrawData> field14563 = new ArrayList<>(32);
   private DrawContext field14564;
   private GpuBuffer field14565;
   private boolean field14566 = true;

   private void method4266() {
      if (this.field14565 != null) {
         this.field14565.close();
         this.field14565 = null;
      }
   }

   public static void method4267() {
      CornerBracketRenderer currentCornerBracketRenderer = cornerBracketRenderer;
      if (currentCornerBracketRenderer != null) {
         currentCornerBracketRenderer.close();
         cornerBracketRenderer = null;
      }
   }

   public void method4268(CornerBracketDrawData cornerBracketDrawData) {
      this.method4286(this.field14564, cornerBracketDrawData);
   }

   public void method4269() {
      this.field14564 = null;
   }

   private CornerBracketDrawData method4270(CornerBracketDrawData cornerBracketDrawData) {
      float local7181_2 = Math.max(cornerBracketDrawData.size(), 0.0F);
      float local7181_3 = method4278(cornerBracketDrawData.radius(), 0.0F, local7181_2);
      float local7181_4 = method4278(cornerBracketDrawData.stroke(), 0.0F, local7181_3 * 2.0F);
      return new CornerBracketDrawData(
         cornerBracketDrawData.x(),
         cornerBracketDrawData.y(),
         local7181_2,
         local7181_3,
         local7181_4,
         Math.max(cornerBracketDrawData.smoothness(), 0.05F),
         cornerBracketDrawData.quadrant() & 3,
         cornerBracketDrawData.color()
      );
   }

   @Override
   public void close() {
      this.field14563.clear();
      this.field14564 = null;
      this.method4266();
   }

   public void method4272() {
      this.field14563.clear();
      this.field14566 = false;
   }

   public void method4273(RenderPass renderPass) {
      if (renderPass != null && !this.field14563.isEmpty()) {
         GpuBuffer gpuBuffer = this.method4284();
         if (gpuBuffer != null) {
            renderPass.setUniform("BracketParamsArray", gpuBuffer);
         }
      }
   }

   private GpuBuffer method4274() {
      if (this.field14565 != null && !this.field14565.isClosed() && this.field14565.size() >= 8192L) {
         return this.field14565;
      }

      this.method4266();

      try {
         this.field14565 = RenderSystem.getDevice().createBuffer(() -> "deluxe_bracket_params", 136, 8192L);
         return this.field14565;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private CornerBracketRenderer() {
   }

   public void method4275(DrawContext drawContext, CornerBracketDrawData cornerBracketDrawData) {
      this.method4277(drawContext);
      this.method4268(cornerBracketDrawData);
      this.method4269();
   }

   private static void method4276(ByteBuffer byteBuffer, int local7189_1, int local7189_2) {
      byteBuffer.putFloat(local7189_1, (local7189_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 4, (local7189_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 8, (local7189_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 12, (local7189_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void method4277(DrawContext drawContext) {
      this.field14564 = drawContext;
   }

   private static float method4278(float local7192_0, float local7192_1, float local7192_2) {
      return Math.max(local7192_1, Math.min(local7192_2, local7192_0));
   }

   private ByteBuffer method4279(MemoryStack memoryStack, List<CornerBracketDrawData> local7193_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local7193_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local7193_2.size(); index++) {
         CornerBracketDrawData cornerBracketDrawData = local7193_2.get(index);
         int local7193_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local7193_6, cornerBracketDrawData.size());
         byteBuffer.putFloat(local7193_6 + 4, cornerBracketDrawData.radius());
         byteBuffer.putFloat(local7193_6 + 8, cornerBracketDrawData.stroke());
         byteBuffer.putFloat(local7193_6 + 12, Math.max(cornerBracketDrawData.smoothness(), 0.05F));
         method4276(byteBuffer, local7193_6 + 16, cornerBracketDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void method4280() {
      if (!this.field14563.isEmpty() && this.field14566) {
         GpuBuffer gpuBuffer = this.method4274();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method4279(memoryStack, this.field14563);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field14566 = false;
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
               this.field14566 = true;
            }
         }
      }
   }

   private static Identifier method4281(String local7195_0) {
      return Identifier.of("deluxe", local7195_0);
   }

   public boolean method4282(RenderPipeline renderPipeline) {
      return renderPipeline == field14562;
   }

   public static CornerBracketRenderer method4283() {
      CornerBracketRenderer currentCornerBracketRenderer = cornerBracketRenderer;
      if (currentCornerBracketRenderer == null) {
         synchronized (CornerBracketRenderer.class) {
            currentCornerBracketRenderer = cornerBracketRenderer;
            if (currentCornerBracketRenderer == null) {
               currentCornerBracketRenderer = new CornerBracketRenderer();
               cornerBracketRenderer = currentCornerBracketRenderer;
            }
         }
      }

      return currentCornerBracketRenderer;
   }

   private GpuBuffer method4284() {
      if (!this.field14566 && this.field14565 != null) {
         return this.field14565;
      }

      this.method4280();
      if (!this.field14566 && this.field14565 != null) {
         return this.field14565;
      }

      this.method4266();
      this.method4280();
      return this.field14565;
   }

   int method4285(CornerBracketDrawData cornerBracketDrawData) {
      int local7199_2 = this.field14563.size();
      if (local7199_2 == 256) {
         return -1;
      }

      this.field14563.add(cornerBracketDrawData);
      this.field14566 = true;
      return local7199_2;
   }

   private void method4286(DrawContext drawContext, CornerBracketDrawData cornerBracketDrawData) {
      if (drawContext != null && cornerBracketDrawData != null) {
         try {
            CornerBracketDrawData cornerBracketDrawData2 = this.method4270(cornerBracketDrawData);
            if (!cornerBracketDrawData2.visible()) {
               return;
            }

            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new CornerBracketRenderState(matrix3x2f, cornerBracketDrawData2, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }
}
