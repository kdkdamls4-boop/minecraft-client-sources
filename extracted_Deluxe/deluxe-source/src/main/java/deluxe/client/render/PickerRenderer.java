package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.PickerDrawData;
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

public final class PickerRenderer implements AutoCloseable {
   private static volatile PickerRenderer pickerRenderer;
   public static final RenderPipeline field01774 = RenderPipeline.builder()
      .withLocation(method0924("pipeline/picker"))
      .withVertexShader(method0924("core/picker"))
      .withFragmentShader(method0924("core/picker"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("PickerParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<PickerDrawData> field01775 = new ArrayList<>(64);
   private DrawContext field01776;
   private GpuBuffer field01777;
   private boolean field01778 = true;

   public boolean method0916(RenderPipeline renderPipeline) {
      return renderPipeline == field01774;
   }

   private static void method0917(ByteBuffer byteBuffer, int local1222_1, int local1222_2) {
      byteBuffer.putFloat(local1222_1, (local1222_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 4, (local1222_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 8, (local1222_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 12, (local1222_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void method0918(RenderPass renderPass) {
      if (renderPass != null && !this.field01775.isEmpty()) {
         GpuBuffer gpuBuffer = this.method0926();
         if (gpuBuffer != null) {
            renderPass.setUniform("PickerParamsArray", gpuBuffer);
         }
      }
   }

   public void method0919() {
      this.field01775.clear();
      this.field01778 = false;
   }

   public void method0920(PickerDrawData pickerDrawData) {
      this.method0923(this.field01776, pickerDrawData);
   }

   @Override
   public void close() {
      this.field01775.clear();
      this.field01776 = null;
      this.method0922();
   }

   private void method0922() {
      if (this.field01777 != null) {
         this.field01777.close();
         this.field01777 = null;
      }
   }

   private void method0923(DrawContext drawContext, PickerDrawData pickerDrawData) {
      if (drawContext != null && pickerDrawData != null && pickerDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new PickerRenderState(matrix3x2f, pickerDrawData, ScissorStack.method3969()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static Identifier method0924(String local1231_0) {
      return Identifier.of("deluxe", local1231_0);
   }

   private GpuBuffer method0925() {
      if (this.field01777 != null && !this.field01777.isClosed() && this.field01777.size() >= 12288L) {
         return this.field01777;
      }

      this.method0922();

      try {
         this.field01777 = RenderSystem.getDevice().createBuffer(() -> "deluxe_picker_params", 136, 12288L);
         return this.field01777;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private GpuBuffer method0926() {
      if (!this.field01778 && this.field01777 != null) {
         return this.field01777;
      }

      this.method0931();
      if (!this.field01778 && this.field01777 != null) {
         return this.field01777;
      }

      this.method0922();
      this.method0931();
      return this.field01777;
   }

   public static void method0927() {
      PickerRenderer currentPickerRenderer = pickerRenderer;
      if (currentPickerRenderer != null) {
         currentPickerRenderer.close();
         pickerRenderer = null;
      }
   }

   private ByteBuffer method0928(MemoryStack memoryStack, List<PickerDrawData> local1235_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(12288, local1235_2.size() * 3 * 4 * 4));

      for (int index = 0; index < local1235_2.size(); index++) {
         PickerDrawData pickerDrawData = local1235_2.get(index);
         int local1235_6 = index * 3 * 4 * 4;
         byteBuffer.putFloat(local1235_6, pickerDrawData.mode());
         byteBuffer.putFloat(local1235_6 + 4, Math.max(pickerDrawData.radius(), 0.0F));
         byteBuffer.putFloat(local1235_6 + 8, Math.max(pickerDrawData.smoothness(), 0.5F));
         byteBuffer.putFloat(local1235_6 + 12, Math.max(0.0F, Math.min(1.0F, pickerDrawData.alpha())));
         byteBuffer.putFloat(local1235_6 + 16, pickerDrawData.w());
         byteBuffer.putFloat(local1235_6 + 20, pickerDrawData.h());
         byteBuffer.putFloat(local1235_6 + 24, Math.max(pickerDrawData.checkerPx(), 1.0F));
         byteBuffer.putFloat(local1235_6 + 28, 0.0F);
         method0917(byteBuffer, local1235_6 + 32, pickerDrawData.solidColor());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void method0929(DrawContext drawContext) {
      this.field01776 = drawContext;
   }

   private PickerRenderer() {
   }

   public static PickerRenderer method0930() {
      PickerRenderer currentPickerRenderer = pickerRenderer;
      if (currentPickerRenderer == null) {
         synchronized (PickerRenderer.class) {
            currentPickerRenderer = pickerRenderer;
            if (currentPickerRenderer == null) {
               currentPickerRenderer = new PickerRenderer();
               pickerRenderer = currentPickerRenderer;
            }
         }
      }

      return currentPickerRenderer;
   }

   public void method0931() {
      if (!this.field01775.isEmpty() && this.field01778) {
         GpuBuffer gpuBuffer = this.method0925();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method0928(memoryStack, this.field01775);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field01778 = false;
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
               this.field01778 = true;
            }
         }
      }
   }

   public void method0932() {
      this.field01776 = null;
   }

   public void method0933(DrawContext drawContext, PickerDrawData pickerDrawData) {
      this.method0929(drawContext);
      this.method0920(pickerDrawData);
      this.method0932();
   }

   int method0934(PickerDrawData pickerDrawData) {
      int local1242_2 = this.field01775.size();
      if (local1242_2 == 256) {
         return -1;
      }

      this.field01775.add(pickerDrawData);
      this.field01778 = true;
      return local1242_2;
   }
}
