package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WarpTextDrawData;
import deluxe.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

public final class WarpTextRenderer implements AutoCloseable {
   private static volatile WarpTextRenderer warpTextRenderer;
   private static final VertexFormat field01438 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field01439 = RenderPipeline.builder()
      .withLocation(method0817("pipeline/warp_text"))
      .withVertexShader(method0817("core/warp_text"))
      .withFragmentShader(method0817("core/warp_text"))
      .withVertexFormat(field01438, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("WarpTextParams", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   private final List<WarpTextDrawData> field01440 = new ArrayList<>(2);
   private DrawContext field01441;
   private GpuBuffer field01442;
   private boolean field01443;

   public void method0802(DrawContext drawContext) {
      this.field01441 = drawContext;
   }

   public boolean method0803(RenderPipeline renderPipeline) {
      return renderPipeline == field01439;
   }

   public void method0804(RenderPass renderPass) {
      if (renderPass != null && !this.field01440.isEmpty()) {
         GpuBuffer gpuBuffer = this.method0809();
         if (gpuBuffer != null) {
            renderPass.setUniform("WarpTextParams", gpuBuffer);
         }
      }
   }

   private void method0805() {
      if (this.field01442 != null) {
         this.field01442.close();
         this.field01442 = null;
      }
   }

   private WarpTextRenderer() {
   }

   public void method0806() {
      this.field01440.clear();
      this.field01443 = false;
   }

   @Override
   public void close() {
      this.field01440.clear();
      this.method0805();
      this.field01441 = null;
   }

   public void method0808(WarpTextDrawData warpTextDrawData) {
      if (this.field01441 != null && warpTextDrawData != null && warpTextDrawData.glyphQuads().length != 0) {
         try {
            TextureSetup textureSetup = this.method0819(warpTextDrawData.atlasTexture());
            if (textureSetup == null) {
               return;
            }

            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.field01441).getGuiRenderState();
            guiRenderState.addSimpleElement(
               new WarpTextRenderState(GuiScale.method0795(this.field01441), textureSetup, ScissorStack.method3969(), warpTextDrawData)
            );
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[WarpTextRenderer] submit failed", exception);
         }
      }
   }

   private GpuBuffer method0809() {
      if (!this.field01443 && this.field01442 != null) {
         return this.field01442;
      }

      this.method0814();
      if (!this.field01443 && this.field01442 != null) {
         return this.field01442;
      }

      this.method0805();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.field01442 = RenderSystem.getDevice().createBuffer(() -> "deluxe_warp_text_params", 128, this.method0811(memoryStack));
         this.field01443 = false;
         gpuBuffer = this.field01442;
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

   public static void method0810() {
      WarpTextRenderer currentWarpTextRenderer = warpTextRenderer;
      if (currentWarpTextRenderer != null) {
         currentWarpTextRenderer.close();
         warpTextRenderer = null;
      }
   }

   private ByteBuffer method0811(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(4096);
      float local1086_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.field01440.size(); index++) {
         int local1086_5 = index * 64 * 4 * 4;
         int local1086_6 = Math.min(30, this.field01440.get(index).glyphQuads().length / 8);
         byteBuffer.putFloat(local1086_5, local1086_6);
         byteBuffer.putFloat(local1086_5 + 4, this.field01440.get(index).pxRange());
         byteBuffer.putFloat(local1086_5 + 8, this.field01440.get(index).influenceRadiusPx());
         byteBuffer.putFloat(local1086_5 + 12, this.field01440.get(index).cellPx());
         byteBuffer.putFloat(local1086_5 + 16, this.field01440.get(index).mouseLocalX());
         byteBuffer.putFloat(local1086_5 + 20, this.field01440.get(index).mouseLocalY());
         byteBuffer.putFloat(local1086_5 + 24, this.field01440.get(index).width());
         byteBuffer.putFloat(local1086_5 + 28, this.field01440.get(index).height());
         byteBuffer.putFloat(local1086_5 + 32, local1086_3);

         for (int index2 = 0; index2 < local1086_6; index2++) {
            int local1086_8 = local1086_5 + (3 + index2 * 2) * 4 * 4;

            for (int index3 = 0; index3 < 8; index3++) {
               byteBuffer.putFloat(local1086_8 + index3 * 4, this.field01440.get(index).glyphQuads()[index2 * 8 + index3]);
            }
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   int method0812(WarpTextDrawData warpTextDrawData) {
      if (this.field01440.size() >= 4) {
         return -1;
      }

      this.field01440.add(warpTextDrawData);
      this.field01443 = true;
      return this.field01440.size() - 1;
   }

   public static WarpTextRenderer method0813() {
      WarpTextRenderer currentWarpTextRenderer = warpTextRenderer;
      if (currentWarpTextRenderer == null) {
         synchronized (WarpTextRenderer.class) {
            currentWarpTextRenderer = warpTextRenderer;
            if (currentWarpTextRenderer == null) {
               currentWarpTextRenderer = new WarpTextRenderer();
               warpTextRenderer = currentWarpTextRenderer;
            }
         }
      }

      return currentWarpTextRenderer;
   }

   public void method0814() {
      if (!this.field01440.isEmpty() && this.field01443) {
         GpuBuffer gpuBuffer = this.method0818();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method0811(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field01443 = false;
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
               this.field01443 = true;
            }
         }
      }
   }

   public void method0815() {
      this.field01441 = null;
   }

   private static Identifier method0817(String local1092_0) {
      return Identifier.of("deluxe", local1092_0);
   }

   private GpuBuffer method0818() {
      if (this.field01442 != null && !this.field01442.isClosed() && this.field01442.size() >= 4096L) {
         return this.field01442;
      }

      this.method0805();

      try {
         this.field01442 = RenderSystem.getDevice().createBuffer(() -> "deluxe_warp_text_params", 136, 4096L);
         return this.field01442;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private TextureSetup method0819(Identifier identifier) {
      if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
         AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
         return texture != null && texture.getGlTextureView() != null
            ? TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            : null;
      } else {
         return null;
      }
   }
}
