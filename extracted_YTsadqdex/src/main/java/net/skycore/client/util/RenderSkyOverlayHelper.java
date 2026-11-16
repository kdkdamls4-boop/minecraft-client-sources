package net.skycore.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.WorldTweaksModule;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class RenderSkyOverlayHelper {
   private static final int SKY_UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().putVec4().get();
   private TextureTarget sceneCopy;
   private static final long SIYANIE_START_NANOS = System.nanoTime();
   private static final int SATURATION_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private final GpuBuffer skyUniforms = uniformBuffer("Skycore World Sky UBO", SKY_UNIFORM_SIZE);
   private TextureTarget skyClouds;
   private final GpuBuffer saturationUniforms = uniformBuffer("Skycore World Saturation UBO", SATURATION_UNIFORM_SIZE);

   private void renderSkyOverlay(SecondarySkyColorHelper secondarySkyColorHelper, CameraRenderState cameraRenderState, RenderPipeline renderPipeline, RenderPipeline renderPipeline2) {
      Minecraft instance = Minecraft.getInstance();
      RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
      if (valid(renderTarget) && cameraRenderState != null && cameraRenderState.initialized) {
         Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
         if (matrix != null) {
            Matrix4f matrix2 = matrix.mul(cameraRenderState.viewRotationMatrix).invert();
            float width = renderPipeline == null ? 1.0F : 2.0F;
            this.writeSkyUniforms(secondarySkyColorHelper, matrix2, renderTarget.width, renderTarget.height, width);
            GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            if (renderPipeline != null) {
               this.ensureSkyClouds(renderTarget.width, renderTarget.height);

               try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore WorldTweaks sky clouds";
                  }, this.skyClouds.getColorTextureView(), Optional.empty())) {
                  device.setPipeline(renderPipeline);
                  RenderSystem.bindDefaultUniforms(device);
                  device.setUniform("WorldSkyUniforms", this.skyUniforms);
                  device.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), clampToEdge);
                  drawFullscreen(device);
               }
            } else if (this.skyClouds != null) {
               this.skyClouds.destroyBuffers();
               this.skyClouds = null;
            }

            try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                  return "Skycore WorldTweaks sky";
               }, renderTarget.getColorTextureView(), Optional.empty())) {
               device2.setPipeline(renderPipeline2);
               RenderSystem.bindDefaultUniforms(device2);
               device2.setUniform("WorldSkyUniforms", this.skyUniforms);
               device2.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), clampToEdge);
               if (renderPipeline != null) {
                  device2.bindTexture("CloudSampler", this.skyClouds.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
               }

               drawFullscreen(device2);
            }
         }
      }
   }

   public static float siyanieTime() {
      long nowNanos = System.nanoTime();
      return (float)(nowNanos - SIYANIE_START_NANOS) / 1.0E9F;
   }

   public void renderSky(WorldTweaksModule worldTweaksModule, CameraRenderState cameraRenderState) {
      RenderPipeline renderPipeline;
      RenderPipeline renderPipeline2;
      switch (worldTweaksModule.skyEffect()) {
         case "Nebula":
            renderPipeline = WorldSkyPipelines.WORLD_SKY_CLOUDS_NEBULA;
            renderPipeline2 = WorldSkyPipelines.WORLD_SKY_NEBULA;
            break;
         case "Plasma":
            renderPipeline = WorldSkyPipelines.WORLD_SKY_CLOUDS_PLASMA;
            renderPipeline2 = WorldSkyPipelines.WORLD_SKY_PLASMA;
            break;
         case "Clouds":
            renderPipeline = WorldSkyPipelines.WORLD_SKY_CLOUDS_VOLUMETRIC;
            renderPipeline2 = WorldSkyPipelines.WORLD_SKY_VOLUMETRIC_CLOUDS;
            break;
         case "Aurora":
            renderPipeline = null;
            renderPipeline2 = WorldSkyPipelines.WORLD_SKY_SIYANIE;
            break;
         default:
            renderPipeline = WorldSkyPipelines.WORLD_SKY_CLOUDS_DEEP_SPACE;
            renderPipeline2 = WorldSkyPipelines.WORLD_SKY_DEEP_SPACE;
      }

      this.renderSkyOverlay(worldTweaksModule, cameraRenderState, renderPipeline, renderPipeline2);
   }

   private static void drawFullscreen(RenderPass renderPass) {
      int count = -1;
      GpuBufferSlice gpuBufferSlice = EnsureHelper.buffer().slice();
      renderPass.setVertexBuffer(0, gpuBufferSlice);
      count = EnsureHelper.vertexCount();
      renderPass.draw(count, 1, 0, 0);
   }

   public void release() {
      if (this.sceneCopy != null) {
         this.sceneCopy.destroyBuffers();
         this.sceneCopy = null;
      }

      if (this.skyClouds != null) {
         this.skyClouds.destroyBuffers();
         this.skyClouds = null;
      }

      this.skyUniforms.close();
      this.saturationUniforms.close();
   }

   public static float shaderTime() {
      return (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;
   }

   public void renderColorGrade(WorldTweaksModule worldTweaksModule) {
      Minecraft instance = Minecraft.getInstance();
      RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
      if (valid(renderTarget)) {
         this.ensureSceneCopy(renderTarget.width, renderTarget.height);
         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(renderTarget.getColorTexture(), this.sceneCopy.getColorTexture(), 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);
         float computedValue = worldTweaksModule.usesSaturation() ? Math.clamp(1.0F + worldTweaksModule.saturationAmount(), 0.0F, 3.0F) : 1.0F;
         float computedValue2 = worldTweaksModule.usesTemperature() ? Math.clamp(worldTweaksModule.temperatureAmount(), -1.0F, 1.0F) : 0.0F;

         try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, SATURATION_UNIFORM_SIZE).putVec4(computedValue, computedValue2, 0.0F, 0.0F).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.saturationUniforms.slice(), byteBuffer);
         }

         try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
               return "Skycore WorldTweaks color grade";
            }, renderTarget.getColorTextureView(), Optional.empty())) {
            device.setPipeline(WorldSkyPipelines.WORLD_SATURATION);
            RenderSystem.bindDefaultUniforms(device);
            GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            device.setUniform("SaturationUniforms", this.saturationUniforms);
            device.bindTexture("SceneSampler", this.sceneCopy.getColorTextureView(), clampToEdge);
            drawFullscreen(device);
         }
      }
   }

   private static boolean valid(RenderTarget renderTarget) {
      if (renderTarget != null && renderTarget.width > 0 && renderTarget.height > 0) {
         if (renderTarget.getColorTexture() != null) {
            if (renderTarget.getColorTextureView() != null) {
               if (renderTarget.getDepthTexture() != null) {
                  if (renderTarget.getDepthTextureView() != null) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private void ensureSceneCopy(int width, int height) {
      if (this.sceneCopy != null) {
         if (this.sceneCopy.width == width) {
            if (this.sceneCopy.height == height) {
               return;
            }
         }
      }

      if (this.sceneCopy != null) {
         this.sceneCopy.destroyBuffers();
      }

      TextureTarget textureTarget = new TextureTarget("gavs-world-tweaks-scene", width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
      this.sceneCopy = textureTarget;
   }

   private void ensureSkyClouds(int value, int value2) {
      int maximum = Math.max(1, value / 2);
      int maximum2 = Math.max(1, value2 / 2);
      if (this.skyClouds != null) {
         if (this.skyClouds.width == maximum) {
            if (this.skyClouds.height == maximum2) {
               return;
            }
         }
      }

      if (this.skyClouds != null) {
         this.skyClouds.destroyBuffers();
      }

      this.skyClouds = new TextureTarget("gavs-world-tweaks-sky-clouds", maximum, maximum2, false, WorldSkyPipelines.SKY_CLOUDS_FORMAT);
   }

   private void writeSkyUniforms(SecondarySkyColorHelper secondarySkyColorHelper, Matrix4f matrix, int value, int value2, float value3) {
      int value4 = secondarySkyColorHelper.primarySkyColor();
      int value5 = secondarySkyColorHelper.secondarySkyColor();
      WorldTweaksModule worldTweaksModule = secondarySkyColorHelper instanceof WorldTweaksModule worldTweaksModule2 && worldTweaksModule2.skyEffect().equals("Aurora") ? worldTweaksModule2 : null;
      float speed = worldTweaksModule == null ? shaderTime() : siyanieTime();

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer device = Std140Builder.onStack(memoryStack, SKY_UNIFORM_SIZE)
            .putMat4f(matrix)
            .putVec4(WriteHexByteHelper.red(value4) / 255.0F, WriteHexByteHelper.green(value4) / 255.0F, WriteHexByteHelper.blue(value4) / 255.0F, 1.0F)
            .putVec4(WriteHexByteHelper.red(value5) / 255.0F, WriteHexByteHelper.green(value5) / 255.0F, WriteHexByteHelper.blue(value5) / 255.0F, 1.0F)
            .putVec4(speed, secondarySkyColorHelper.skyIntensity(), secondarySkyColorHelper.skySpeed(), RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F)
            .putVec4(1.0F / value, 1.0F / value2, worldTweaksModule == null ? value3 : worldTweaksModule.siyanieSoftness(), worldTweaksModule == null ? 0.0F : worldTweaksModule.siyanieCoverage())
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.skyUniforms.slice(), device);
      }
   }

   private static GpuBuffer uniformBuffer(String text, int value) {
      return RenderSystem.getDevice().createBuffer(() -> {
         return text;
      }, 136, value);
   }
}
