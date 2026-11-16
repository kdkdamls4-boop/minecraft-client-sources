package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.ArcDividerDrawData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class ShimmerRenderer implements AutoCloseable {
   private static volatile ShimmerRenderer shimmerRenderer;
   private static final VertexFormat field10718 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field10719 = RenderPipeline.builder()
      .withLocation(method3148("pipeline/shimmer"))
      .withVertexShader(method3148("core/shimmer"))
      .withFragmentShader(method3148("core/shimmer"))
      .withVertexFormat(field10718, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ShimmerParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<ArcDividerDrawData> field10720 = new ArrayList<>(32);
   private final Map<ShimmerRenderer.Nested0472, ArcDividerRenderState> field10721 = new LinkedHashMap<>(16);
   private DrawContext field10722;
   private GpuBuffer field10723;
   private boolean field10724;

   int method3140(ArcDividerRenderState arcDividerRenderState) {
      int local5055_2 = this.field10720.size();
      if (local5055_2 >= 256) {
         return -1;
      }

      this.field10720.add(arcDividerRenderState.shimmer());
      this.field10724 = true;
      return local5055_2;
   }

   public void method3141() {
      if (!this.field10720.isEmpty() && this.field10724) {
         GpuBuffer gpuBuffer = this.method3154();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method3152(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field10724 = false;
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
               this.field10724 = true;
            }
         }
      }
   }

   public void method3142(ArcDividerDrawData arcDividerDrawData) {
      if (this.field10722 != null && arcDividerDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.field10722).getGuiRenderState();
            int layerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            Matrix3x2f matrix3x2f = GuiScale.method0795(this.field10722);
            ScreenRect screenRect = ScissorStack.method3969();
            ShimmerRenderer.Nested0472 local5057_6 = new ShimmerRenderer.Nested0472(guiRenderState, layerSerial, screenRect);
            ArcDividerRenderState arcDividerRenderState = this.field10721.get(local5057_6);
            if (arcDividerRenderState == null) {
               arcDividerRenderState = new ArcDividerRenderState(matrix3x2f, arcDividerDrawData, screenRect);
               this.field10721.put(local5057_6, arcDividerRenderState);
               guiRenderState.addSimpleElement(arcDividerRenderState);
            }
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[ShimmerRenderer] submit failed", exception);
         }
      }
   }

   public void method3143(DrawContext drawContext) {
      if (this.field10722 != drawContext) {
         this.field10721.clear();
      }

      this.field10722 = drawContext;
   }

   public boolean method3144(RenderPipeline renderPipeline) {
      return renderPipeline == field10719;
   }

   public static ShimmerRenderer method3145() {
      ShimmerRenderer currentShimmerRenderer = shimmerRenderer;
      if (currentShimmerRenderer == null) {
         synchronized (ShimmerRenderer.class) {
            currentShimmerRenderer = shimmerRenderer;
            if (currentShimmerRenderer == null) {
               currentShimmerRenderer = new ShimmerRenderer();
               shimmerRenderer = currentShimmerRenderer;
            }
         }
      }

      return currentShimmerRenderer;
   }

   @Override
   public void close() {
      this.field10720.clear();
      this.field10721.clear();
      this.method3150();
      this.field10722 = null;
   }

   public void method3146() {
      this.field10722 = null;
      this.field10721.clear();
   }

   public void method3147(RenderPass renderPass) {
      if (renderPass != null && !this.field10720.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3149();
         if (gpuBuffer != null) {
            renderPass.setUniform("ShimmerParamsArray", gpuBuffer);
         }
      }
   }

   private static Identifier method3148(String local5064_0) {
      return Identifier.of("deluxe", local5064_0);
   }

   private GpuBuffer method3149() {
      if (!this.field10724 && this.field10723 != null) {
         return this.field10723;
      }

      this.method3141();
      if (!this.field10724 && this.field10723 != null) {
         return this.field10723;
      }

      this.method3150();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.field10723 = RenderSystem.getDevice().createBuffer(() -> "deluxe_shimmer_params", 128, this.method3152(memoryStack));
         this.field10724 = false;
         gpuBuffer = this.field10723;
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

   private void method3150() {
      if (this.field10723 != null) {
         this.field10723.close();
         this.field10723 = null;
      }
   }

   public static void method3151() {
      ShimmerRenderer currentShimmerRenderer = shimmerRenderer;
      if (currentShimmerRenderer != null) {
         currentShimmerRenderer.close();
         shimmerRenderer = null;
      }
   }

   private ByteBuffer method3152(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(4096);

      for (int index = 0; index < this.field10720.size(); index++) {
         ArcDividerDrawData arcDividerDrawData = this.field10720.get(index);
         int local5068_5 = index * 4 * 4;
         byteBuffer.putFloat(local5068_5, arcDividerDrawData.progress());
         byteBuffer.putFloat(local5068_5 + 4, arcDividerDrawData.halfWidth());
         byteBuffer.putFloat(local5068_5 + 8, arcDividerDrawData.intensity());
         byteBuffer.putFloat(local5068_5 + 12, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private GpuBuffer method3154() {
      if (this.field10723 != null && !this.field10723.isClosed() && this.field10723.size() >= 4096L) {
         return this.field10723;
      }

      this.method3150();

      try {
         this.field10723 = RenderSystem.getDevice().createBuffer(() -> "deluxe_shimmer_params", 136, 4096L);
         return this.field10723;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private ShimmerRenderer() {
   }

   public void method3155() {
      this.field10720.clear();
      this.field10724 = false;
   }

   private record Nested0472(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
