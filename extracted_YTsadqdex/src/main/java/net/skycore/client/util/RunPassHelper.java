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

public final class RunPassHelper {
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec2().putVec2().putFloat().get();
   private static float requestedRadius;
   private static TextureTarget halfTarget;
   private static TextureTarget quarterTarget;
   private static final int PASS_COUNT = 3;
   private static GpuBuffer[] passUniforms;
   private static float activeRadius = 16.0F;

   private RunPassHelper() {
   }

   private static void runPass(int value, RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int value2, int value3, TextureTarget textureTarget, float value4, GpuSampler gpuSampler) {
      GpuBuffer gpuBuffer = passUniforms[value];

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE)
            .putFloat(0.5F / value2)
            .putFloat(0.5F / value3)
            .putFloat(0.0F)
            .putFloat(0.0F)
            .putFloat(value4)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(), byteBuffer);
      }

      try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Skycore GUI backdrop blur";
         }, textureTarget.getColorTextureView(), Optional.empty())) {
         device.setPipeline(renderPipeline);
         RenderSystem.bindDefaultUniforms(device);
         device.setUniform("GuiKawaseUniforms", gpuBuffer);
         device.bindTexture("CurrentInput", gpuTextureView, gpuSampler);
         device.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   public static GpuTextureView acquireView() {
      Minecraft mc = MinecraftContext.mc;
      if (mc != null && RenderSystem.tryGetDevice() != null) {
         RenderTarget renderTarget = mc.gameRenderer.mainRenderTarget();
         if (renderTarget != null && renderTarget.getColorTexture() != null && renderTarget.width > 1 && renderTarget.height > 1) {
            int maximum = Math.max(1, renderTarget.width / 2);
            int maximum2 = Math.max(1, renderTarget.height / 2);
            if (halfTarget == null || halfTarget.width != maximum || halfTarget.height != maximum2) {
               if (halfTarget != null) {
                  halfTarget.destroyBuffers();
               }

               if (quarterTarget != null) {
                  quarterTarget.destroyBuffers();
                  quarterTarget = null;
               }

               halfTarget = new TextureTarget("blade-gui-backdrop-half", maximum, maximum2, false, GpuFormat.RGBA8_UNORM);
               quarterTarget = new TextureTarget("blade-gui-backdrop-quarter", Math.max(1, maximum / 2), Math.max(1, maximum2 / 2), false, GpuFormat.RGBA8_UNORM);
            }

            return halfTarget.getColorTextureView();
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void ensureStaticResources() {
      if (passUniforms == null) {
         passUniforms = new GpuBuffer[3];

         for (int value = 0; value < 3; value++) {
            int value2 = value;
            GpuBuffer[] passUniforms2 = passUniforms;
            GpuDevice device = RenderSystem.getDevice();
            Supplier<java.lang.String> supplier = () -> {
               return "Skycore GUI backdrop UBO " + value2;
            };
            long uNIFORM_SIZE = UNIFORM_SIZE;
            passUniforms2[value] = device.createBuffer(supplier, 136, uNIFORM_SIZE);
         }
      }
   }

   public static void captureNow() {
      if (halfTarget != null && quarterTarget != null) {
         Minecraft mc = MinecraftContext.mc;
         if (mc != null && RenderSystem.tryGetDevice() != null) {
            RenderTarget renderTarget = mc.gameRenderer.mainRenderTarget();
            if (renderTarget != null
               && renderTarget.getColorTexture() != null
               && halfTarget.width == Math.max(1, renderTarget.width / 2)
               && halfTarget.height == Math.max(1, renderTarget.height / 2)) {
               if (requestedRadius > 0.0F) {
                  activeRadius = requestedRadius;
                  requestedRadius = 0.0F;
               }

               ensureStaticResources();
               float clampedValue = ClampByteHelper.clamp(activeRadius / 8.0F, 0.5F, 4.0F);
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
               runPass(0, GuiPipelines.GUI_BLUR_DOWN, renderTarget.getColorTextureView(), renderTarget.width, renderTarget.height, halfTarget, 1.0F, clampToEdge);
               if (activeRadius >= 6.0F) {
                  runPass(1, GuiPipelines.GUI_BLUR_DOWN, halfTarget.getColorTextureView(), halfTarget.width, halfTarget.height, quarterTarget, clampedValue, clampToEdge);
                  runPass(
                     2, GuiPipelines.GUI_BLUR_UP, quarterTarget.getColorTextureView(), quarterTarget.width, quarterTarget.height, halfTarget, clampedValue, clampToEdge
                  );
               }
            }
         }
      }
   }

   public static void requestBlurRadius(float radius) {
      requestedRadius = Math.max(requestedRadius, radius);
   }
}
