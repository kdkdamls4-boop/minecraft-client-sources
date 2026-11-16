package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.MenuGridDrawData;
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

public final class MenuGridRenderer implements AutoCloseable {
   private static volatile MenuGridRenderer menuGridRenderer;
   private static final VertexFormat field08377 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field08378 = RenderPipeline.builder()
      .withLocation(method2399("pipeline/menu_grid"))
      .withVertexShader(method2399("core/menu_grid"))
      .withFragmentShader(method2399("core/menu_grid"))
      .withVertexFormat(field08377, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("MenuGridParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<MenuGridDrawData> field08379 = new ArrayList<>(4);
   private final Map<MenuGridRenderer.Nested0433, MenuGridRenderState> field08380 = new LinkedHashMap<>(4);
   private DrawContext field08381;
   private GpuBuffer field08382;
   private boolean field08383;

   private GpuBuffer method2393() {
      if (this.field08382 != null && !this.field08382.isClosed() && this.field08382.size() >= 2048L) {
         return this.field08382;
      }

      this.method2403();

      try {
         this.field08382 = RenderSystem.getDevice().createBuffer(() -> "deluxe_menu_grid_params", 136, 2048L);
         return this.field08382;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   @Override
   public void close() {
      this.field08379.clear();
      this.field08380.clear();
      this.method2403();
      this.field08381 = null;
   }

   private GpuBuffer method2394() {
      if (!this.field08383 && this.field08382 != null) {
         return this.field08382;
      }

      this.method2409();
      if (!this.field08383 && this.field08382 != null) {
         return this.field08382;
      }

      this.method2403();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.field08382 = RenderSystem.getDevice().createBuffer(() -> "deluxe_menu_grid_params", 128, this.method2407(memoryStack));
         this.field08383 = false;
         gpuBuffer = this.field08382;
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

   public void method2395() {
      this.field08381 = null;
      this.field08380.clear();
   }

   public void method2396(RenderPass renderPass) {
      if (renderPass != null && !this.field08379.isEmpty()) {
         GpuBuffer gpuBuffer = this.method2394();
         if (gpuBuffer != null) {
            renderPass.setUniform("MenuGridParamsArray", gpuBuffer);
         }
      }
   }

   public void method2398(DrawContext drawContext) {
      if (this.field08381 != drawContext) {
         this.field08380.clear();
      }

      this.field08381 = drawContext;
   }

   private static Identifier method2399(String local3740_0) {
      return Identifier.of("deluxe", local3740_0);
   }

   public void method2400(MenuGridDrawData menuGridDrawData) {
      if (this.field08381 != null && menuGridDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.field08381).getGuiRenderState();
            Matrix3x2f matrix3x2f = GuiScale.method0795(this.field08381);
            ScreenRect screenRect = ScissorStack.method3969();
            MenuGridRenderer.Nested0433 local3741_5 = new MenuGridRenderer.Nested0433(
               guiRenderState, ((GuiLayerSerialAccess)guiRenderState).getLayerSerial(), screenRect
            );
            MenuGridRenderState menuGridRenderState = this.field08380.get(local3741_5);
            if (menuGridRenderState == null) {
               menuGridRenderState = new MenuGridRenderState(matrix3x2f, screenRect);
               menuGridRenderState.add(menuGridDrawData);
               this.field08380.put(local3741_5, menuGridRenderState);
               guiRenderState.addSimpleElement(menuGridRenderState);
            } else {
               menuGridRenderState.add(menuGridDrawData);
            }
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[MenuGridRenderer] submit failed", exception);
         }
      }
   }

   public static void method2401() {
      MenuGridRenderer currentMenuGridRenderer = menuGridRenderer;
      if (currentMenuGridRenderer != null) {
         currentMenuGridRenderer.close();
         menuGridRenderer = null;
      }
   }

   public boolean method2402(RenderPipeline renderPipeline) {
      return renderPipeline == field08378;
   }

   private void method2403() {
      if (this.field08382 != null) {
         this.field08382.close();
         this.field08382 = null;
      }
   }

   public void method2404() {
      this.field08379.clear();
      this.field08383 = false;
   }

   int method2406(MenuGridDrawData menuGridDrawData) {
      if (this.field08379.size() >= 64) {
         return -1;
      }

      this.field08379.add(menuGridDrawData);
      this.field08383 = true;
      return this.field08379.size() - 1;
   }

   private MenuGridRenderer() {
   }

   private ByteBuffer method2407(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(2048);
      float local3750_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.field08379.size(); index++) {
         int local3750_5 = index * 8 * 4;
         byteBuffer.putFloat(local3750_5, this.field08379.get(index).width());
         byteBuffer.putFloat(local3750_5 + 4, this.field08379.get(index).height());
         byteBuffer.putFloat(local3750_5 + 8, this.field08379.get(index).cellPx());
         byteBuffer.putFloat(local3750_5 + 12, this.field08379.get(index).lineWidthPx());
         byteBuffer.putFloat(local3750_5 + 16, this.field08379.get(index).mouseX() - this.field08379.get(index).x());
         byteBuffer.putFloat(local3750_5 + 20, this.field08379.get(index).mouseY() - this.field08379.get(index).y());
         byteBuffer.putFloat(local3750_5 + 24, this.field08379.get(index).influenceRadiusPx());
         byteBuffer.putFloat(local3750_5 + 28, local3750_3);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public static MenuGridRenderer method2408() {
      MenuGridRenderer currentMenuGridRenderer = menuGridRenderer;
      if (currentMenuGridRenderer == null) {
         synchronized (MenuGridRenderer.class) {
            currentMenuGridRenderer = menuGridRenderer;
            if (currentMenuGridRenderer == null) {
               currentMenuGridRenderer = new MenuGridRenderer();
               menuGridRenderer = currentMenuGridRenderer;
            }
         }
      }

      return currentMenuGridRenderer;
   }

   public void method2409() {
      if (!this.field08379.isEmpty() && this.field08383) {
         GpuBuffer gpuBuffer = this.method2393();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method2407(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field08383 = false;
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
               this.field08383 = true;
            }
         }
      }
   }

   private record Nested0433(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
