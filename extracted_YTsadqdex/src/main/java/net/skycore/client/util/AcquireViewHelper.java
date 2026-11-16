package net.skycore.client.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;

public final class AcquireViewHelper {
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec2().putVec2().putFloat().get();
   private static final int PASS_COUNT = 6;
   private static TextureTarget pongTarget;
   private static float requestedRadius;
   private static GpuBuffer[] passUniforms;
   private static TextureTarget pingTarget;
   private static float activeRadius = 14.0F;

   private AcquireViewHelper() {
   }

   public static GpuTextureView acquireView() {
      Minecraft mc = MinecraftContext.mc;
      if (mc != null && RenderSystem.tryGetDevice() != null) {
         RenderTarget renderTarget = mc.gameRenderer.mainRenderTarget();
         if (renderTarget != null && renderTarget.getColorTexture() != null && renderTarget.width > 1 && renderTarget.height > 1) {
            int maximum = Math.max(1, renderTarget.width / 2);
            int maximum2 = Math.max(1, renderTarget.height / 2);
            if (pingTarget == null || pongTarget == null || pingTarget.width != maximum || pingTarget.height != maximum2) {
               destroyTargets();
               pingTarget = new TextureTarget("gavs-matte-blur-ping", maximum, maximum2, false, GpuFormat.RGBA8_UNORM);
               pongTarget = new TextureTarget("gavs-matte-blur-pong", maximum, maximum2, false, GpuFormat.RGBA8_UNORM);
            }

            return pongTarget.getColorTextureView();
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void requestBlurRadius(float radius) {
      float maximum = Math.max(requestedRadius, radius);
      requestedRadius = maximum;
   }

   private static void ensureStaticResources() {
      if (passUniforms == null) {
         passUniforms = new GpuBuffer[6];

         for (int value = 0; value < 6; value++) {
            int value2 = value;
            GpuBuffer[] passUniforms2 = passUniforms;
            GpuDevice device = RenderSystem.getDevice();
            Supplier<java.lang.String> supplier = () -> {
               return "Skycore matte backdrop UBO " + value2;
            };
            long uNIFORM_SIZE = UNIFORM_SIZE;
            passUniforms2[value] = device.createBuffer(supplier, 136, uNIFORM_SIZE);
         }
      }
   }

   private static void runPass(int value, RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int value2, int value3, TextureTarget textureTarget, float value4, GpuSampler gpuSampler) {
      GpuBuffer gpuBuffer = passUniforms[value];

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE)
            .putFloat(1.0F / value2)
            .putFloat(1.0F / value3)
            .putFloat(0.0F)
            .putFloat(0.0F)
            .putFloat(value4)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(), byteBuffer);
      }

      try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Skycore matte backdrop blur";
         }, textureTarget.getColorTextureView(), Optional.empty())) {
         device.setPipeline(renderPipeline);
         RenderSystem.bindDefaultUniforms(device);
         device.setUniform("GuiKawaseUniforms", gpuBuffer);
         device.bindTexture("CurrentInput", gpuTextureView, gpuSampler);
         device.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   private static void destroyTargets() {
      if (pingTarget != null) {
         pingTarget.destroyBuffers();
         pingTarget = null;
      }

      if (pongTarget != null) {
         pongTarget.destroyBuffers();
         pongTarget = null;
      }
   }

   public static void captureNow() {
      if (pingTarget != null && pongTarget != null) {
         Minecraft mc = MinecraftContext.mc;
         if (mc != null && RenderSystem.tryGetDevice() != null) {
            RenderTarget renderTarget = mc.gameRenderer.mainRenderTarget();
            if (renderTarget != null
               && renderTarget.getColorTexture() != null
               && pingTarget.width == Math.max(1, renderTarget.width / 2)
               && pingTarget.height == Math.max(1, renderTarget.height / 2)) {
               if (requestedRadius > 0.0F) {
                  activeRadius = requestedRadius;
                  requestedRadius = 0.0F;
               }

               ensureStaticResources();
               float clampedValue = ClampByteHelper.clamp(activeRadius / 14.0F, 0.65F, 2.5F);
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
               runPass(0, GuiPipelines.GUI_MATTE_BLUR_DOWN, renderTarget.getColorTextureView(), renderTarget.width, renderTarget.height, pingTarget, clampedValue, clampToEdge);
               runPass(1, GuiPipelines.GUI_MATTE_BLUR_DOWN, pingTarget.getColorTextureView(), pingTarget.width, pingTarget.height, pongTarget, clampedValue, clampToEdge);
               runPass(2, GuiPipelines.GUI_MATTE_BLUR_DOWN, pongTarget.getColorTextureView(), pongTarget.width, pongTarget.height, pingTarget, clampedValue, clampToEdge);
               runPass(3, GuiPipelines.GUI_MATTE_BLUR_UP, pingTarget.getColorTextureView(), pingTarget.width, pingTarget.height, pongTarget, clampedValue, clampToEdge);
               runPass(4, GuiPipelines.GUI_MATTE_BLUR_UP, pongTarget.getColorTextureView(), pongTarget.width, pongTarget.height, pingTarget, clampedValue, clampToEdge);
               runPass(5, GuiPipelines.GUI_MATTE_BLUR_UP, pingTarget.getColorTextureView(), pingTarget.width, pingTarget.height, pongTarget, clampedValue, clampToEdge);
            }
         }
      }
   }
}
