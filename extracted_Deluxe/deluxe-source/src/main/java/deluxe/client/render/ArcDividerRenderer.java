package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.ShimmerDrawData;

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

public final class ArcDividerRenderer implements AutoCloseable {
   private static volatile ArcDividerRenderer arcDividerRenderer;
   public static final RenderPipeline field12989 = RenderPipeline.builder()
      .withLocation(method3751("pipeline/arc_divider"))
      .withVertexShader(method3751("core/arc_divider"))
      .withFragmentShader(method3751("core/arc_divider"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ArcParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<ShimmerDrawData> field12990 = new ArrayList<>(64);
   private DrawContext field12991;
   private GpuBuffer field12992;
   private boolean field12993 = true;

   public static ArcDividerRenderer method3734() {
      ArcDividerRenderer currentArcDividerRenderer = arcDividerRenderer;
      if (currentArcDividerRenderer == null) {
         synchronized (ArcDividerRenderer.class) {
            currentArcDividerRenderer = arcDividerRenderer;
            if (currentArcDividerRenderer == null) {
               currentArcDividerRenderer = new ArcDividerRenderer();
               arcDividerRenderer = currentArcDividerRenderer;
            }
         }
      }

      return currentArcDividerRenderer;
   }

   private ByteBuffer method3735(MemoryStack memoryStack, List<ShimmerDrawData> local6184_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local6184_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local6184_2.size(); index++) {
         ShimmerDrawData shimmerDrawData = local6184_2.get(index);
         int local6184_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local6184_6, shimmerDrawData.halfWidth());
         byteBuffer.putFloat(local6184_6 + 4, Math.max(shimmerDrawData.lift(), 0.0F));
         byteBuffer.putFloat(local6184_6 + 8, Math.max(shimmerDrawData.thickness(), 0.5F));
         byteBuffer.putFloat(local6184_6 + 12, Math.max(shimmerDrawData.feather(), 0.5F));
         method3741(byteBuffer, local6184_6 + 16, shimmerDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private GpuBuffer method3736() {
      if (this.field12992 != null && !this.field12992.isClosed() && this.field12992.size() >= 8192L) {
         return this.field12992;
      }

      this.method3745();

      try {
         this.field12992 = RenderSystem.getDevice().createBuffer(() -> "deluxe_arc_params", 136, 8192L);
         return this.field12992;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void method3737(ShimmerDrawData shimmerDrawData) {
      this.method3743(this.field12991, shimmerDrawData);
   }

   public void method3738(RenderPass renderPass) {
      if (renderPass != null && !this.field12990.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3747();
         if (gpuBuffer != null) {
            renderPass.setUniform("ArcParamsArray", gpuBuffer);
         }
      }
   }

   int method3739(ShimmerDrawData shimmerDrawData) {
      int local6188_2 = this.field12990.size();
      if (local6188_2 == 256) {
         return -1;
      }

      this.field12990.add(shimmerDrawData);
      this.field12993 = true;
      return local6188_2;
   }

   private ArcDividerRenderer() {
   }

   @Override
   public void close() {
      this.field12990.clear();
      this.field12991 = null;
      this.method3745();
   }

   private static void method3741(ByteBuffer byteBuffer, int local6193_1, int local6193_2) {
      byteBuffer.putFloat(local6193_1, (local6193_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 4, (local6193_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 8, (local6193_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 12, (local6193_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void method3742() {
      if (!this.field12990.isEmpty() && this.field12993) {
         GpuBuffer gpuBuffer = this.method3736();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method3735(memoryStack, this.field12990);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field12993 = false;
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
               this.field12993 = true;
            }
         }
      }
   }

   private void method3743(DrawContext drawContext, ShimmerDrawData shimmerDrawData) {
      if (drawContext != null && shimmerDrawData != null && shimmerDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new ShimmerRenderState(matrix3x2f, shimmerDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void method3744() {
      this.field12990.clear();
      this.field12993 = false;
   }

   private void method3745() {
      if (this.field12992 != null) {
         this.field12992.close();
         this.field12992 = null;
      }
   }

   public void method3746() {
      this.field12991 = null;
   }

   private GpuBuffer method3747() {
      if (!this.field12993 && this.field12992 != null) {
         return this.field12992;
      }

      this.method3742();
      if (!this.field12993 && this.field12992 != null) {
         return this.field12992;
      }

      this.method3745();
      this.method3742();
      return this.field12992;
   }

   public boolean method3748(RenderPipeline renderPipeline) {
      return renderPipeline == field12989;
   }

   public static void method3749() {
      ArcDividerRenderer currentArcDividerRenderer = arcDividerRenderer;
      if (currentArcDividerRenderer != null) {
         currentArcDividerRenderer.close();
         arcDividerRenderer = null;
      }
   }

   public void method3750(DrawContext drawContext) {
      this.field12991 = drawContext;
   }

   private static Identifier method3751(String local6203_0) {
      return Identifier.of("deluxe", local6203_0);
   }
}
