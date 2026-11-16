package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.RippleDrawData;
import deluxe.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class RippleRenderer implements AutoCloseable {
   private static volatile RippleRenderer rippleRenderer;
   private static final VertexFormat field14885 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field14886 = RenderPipeline.builder()
      .withLocation(method4353("pipeline/ripple"))
      .withVertexShader(method4353("core/ripple"))
      .withFragmentShader(method4353("core/ripple"))
      .withVertexFormat(field14885, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RippleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<RippleDrawData> field14887 = new ArrayList<>(128);
   private DrawContext field14888;
   private GpuBuffer field14889;
   private boolean field14890 = true;

   private ByteBuffer method4344(MemoryStack memoryStack, List<RippleDrawData> local7312_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(40960, local7312_2.size() * 5 * 4 * 4));

      for (int index = 0; index < local7312_2.size(); index++) {
         RippleDrawData rippleDrawData = local7312_2.get(index);
         int local7312_6 = index * 5 * 4 * 4;
         byteBuffer.putFloat(local7312_6, rippleDrawData.radiusTopLeft());
         byteBuffer.putFloat(local7312_6 + 4, rippleDrawData.radiusTopRight());
         byteBuffer.putFloat(local7312_6 + 8, rippleDrawData.radiusBottomRight());
         byteBuffer.putFloat(local7312_6 + 12, rippleDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local7312_6 + 16, rippleDrawData.width());
         byteBuffer.putFloat(local7312_6 + 20, rippleDrawData.height());
         byteBuffer.putFloat(local7312_6 + 24, rippleDrawData.smoothness());
         byteBuffer.putFloat(local7312_6 + 28, rippleDrawData.isTextured() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local7312_6 + 32, rippleDrawData.centerX());
         byteBuffer.putFloat(local7312_6 + 36, rippleDrawData.centerY());
         byteBuffer.putFloat(local7312_6 + 40, rippleDrawData.rippleRadius());
         byteBuffer.putFloat(local7312_6 + 44, rippleDrawData.rippleSmoothness());
         method4346(byteBuffer, local7312_6 + 48, rippleDrawData.sourceColor());
         method4346(byteBuffer, local7312_6 + 64, rippleDrawData.targetColor());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void method4345() {
      if (this.field14889 != null) {
         this.field14889.close();
         this.field14889 = null;
      }
   }

   private static void method4346(ByteBuffer byteBuffer, int local7314_1, int local7314_2) {
      byteBuffer.putFloat(local7314_1, (local7314_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 4, (local7314_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 8, (local7314_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 12, (local7314_2 >>> 24 & 0xFF) / 255.0F);
   }

   private void method4347(DrawContext drawContext, RippleDrawData rippleDrawData) {
      if (drawContext != null && rippleDrawData != null && rippleDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new RippleRenderState(matrix3x2f, rippleDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private GpuBuffer method4349() {
      if (this.field14889 != null && !this.field14889.isClosed() && this.field14889.size() >= 40960L) {
         return this.field14889;
      }

      this.method4345();

      try {
         this.field14889 = RenderSystem.getDevice().createBuffer(() -> "deluxe_ripple_params", 136, 40960L);
         return this.field14889;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void method4350() {
      if (!this.field14887.isEmpty() && this.field14890) {
         GpuBuffer gpuBuffer = this.method4349();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method4344(memoryStack, this.field14887);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field14890 = false;
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
               this.field14890 = true;
            }
         }
      }
   }

   @Override
   public void close() {
      this.field14887.clear();
      this.field14888 = null;
      this.method4345();
   }

   public void method4351(RippleDrawData rippleDrawData) {
      this.method4347(this.field14888, rippleDrawData);
   }

   public void method4352() {
      this.field14887.clear();
      this.field14890 = false;
   }

   private static Identifier method4353(String local7323_0) {
      return Identifier.of("deluxe", local7323_0);
   }

   public static void method4354() {
      RippleRenderer currentRippleRenderer = rippleRenderer;
      if (currentRippleRenderer != null) {
         currentRippleRenderer.close();
         rippleRenderer = null;
      }
   }

   private RippleRenderer() {
   }

   public static RippleRenderer method4355() {
      RippleRenderer currentRippleRenderer = rippleRenderer;
      if (currentRippleRenderer == null) {
         synchronized (RippleRenderer.class) {
            currentRippleRenderer = rippleRenderer;
            if (currentRippleRenderer == null) {
               currentRippleRenderer = new RippleRenderer();
               rippleRenderer = currentRippleRenderer;
            }
         }
      }

      return currentRippleRenderer;
   }

   public boolean method4356(RenderPipeline renderPipeline) {
      return renderPipeline == field14886;
   }

   private GpuBuffer method4357() {
      if (!this.field14890 && this.field14889 != null) {
         return this.field14889;
      }

      this.method4350();
      if (!this.field14890 && this.field14889 != null) {
         return this.field14889;
      }

      this.method4345();
      this.method4350();
      return this.field14889;
   }

   int method4358(RippleDrawData rippleDrawData) {
      int local7329_2 = this.field14887.size();
      if (local7329_2 == 512) {
         return -1;
      }

      this.field14887.add(rippleDrawData);
      this.field14890 = true;
      return local7329_2;
   }

   public void method4359(RenderPass renderPass) {
      if (renderPass != null && !this.field14887.isEmpty()) {
         GpuBuffer gpuBuffer = this.method4357();
         if (gpuBuffer != null) {
            renderPass.setUniform("RippleParamsArray", gpuBuffer);
         }
      }
   }

   public void method4360(DrawContext drawContext) {
      this.field14888 = drawContext;
   }

   public void method4361() {
      this.field14888 = null;
   }
}
