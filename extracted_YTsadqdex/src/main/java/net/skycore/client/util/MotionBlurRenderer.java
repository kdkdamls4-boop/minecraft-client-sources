package net.skycore.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.skycore.client.module.render.MotionBlurModule;
import org.lwjgl.system.MemoryStack;

public final class MotionBlurRenderer {
   private boolean hasHistory;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private TextureTarget history;
   private final MotionBlurModule module;
   private TextureTarget scratch;
   private GpuBuffer uniforms;
   private int fullWidth;
   private int fullHeight;

   public void release() {
      this.hasHistory = false;
      this.fullWidth = 0;
      this.fullHeight = 0;
      this.history = destroy(this.history);
      this.scratch = destroy(this.scratch);
      if (this.uniforms != null) {
         this.uniforms.close();
         this.uniforms = null;
      }
   }

   private static TextureTarget destroy(TextureTarget textureTarget) {
      if (textureTarget != null) {
         textureTarget.destroyBuffers();
      }

      return null;
   }

   private void ensureUniforms() {
      if (this.uniforms == null) {
         this.uniforms = RenderSystem.getDevice().createBuffer(() -> {
            return "Skycore MotionBlur UBO";
         }, 136, UNIFORM_SIZE);
      }
   }

   private TextureTarget ensureTarget(TextureTarget textureTarget, String text, int width, int height) {
      if (textureTarget == null) {
         return new TextureTarget(text, width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
      }

      if (textureTarget.width == width) {
         if (textureTarget.height == height) {
            return textureTarget;
         }
      }

      textureTarget.resize(width, height);
      return textureTarget;
   }

   public MotionBlurRenderer(MotionBlurModule module2) {
      this.module = module2;
   }

   private void drawBlend(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, float coordinate, GpuSampler gpuSampler) {
      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE).putVec4(coordinate, 0.0F, 0.0F, 0.0F).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
      }

      try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Skycore MotionBlur";
         }, gpuTextureView, Optional.empty())) {
         device.setPipeline(MotionBlurPipelines.MOTION_BLUR);
         RenderSystem.bindDefaultUniforms(device);
         device.setUniform("MotionBlurParams", this.uniforms);
         device.bindTexture("CurrentSampler", gpuTextureView2, gpuSampler);
         device.bindTexture("HistorySampler", gpuTextureView3, gpuSampler);
         device.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   public void resetHistory() {
      this.hasHistory = false;
   }

   public void render() {
      if (this.module.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null && instance.level != null && instance.player != null && RenderSystem.tryGetDevice() != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            if (renderTarget != null && renderTarget.width > 0 && renderTarget.height > 0 && renderTarget.getColorTexture() != null && renderTarget.getColorTextureView() != null) {
               int width = Math.max(1, (renderTarget.width + 1) / 2);
               int width2 = Math.max(1, (renderTarget.height + 1) / 2);
               this.ensureTargets(renderTarget.width, renderTarget.height, width, width2);
               this.ensureUniforms();
               float color = Math.max(0.0F, Math.min(0.98F, this.module.strength().floatValue() / 100.0F * 0.95F));
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
               if (!this.hasHistory) {
                  this.blit(this.history.getColorTextureView(), renderTarget.getColorTextureView(), clampToEdge);
                  this.hasHistory = true;
               } else {
                  this.drawBlend(this.scratch.getColorTextureView(), renderTarget.getColorTextureView(), this.history.getColorTextureView(), color, clampToEdge);
                  this.blit(renderTarget.getColorTextureView(), this.scratch.getColorTextureView(), clampToEdge);
                  this.blit(this.history.getColorTextureView(), this.scratch.getColorTextureView(), clampToEdge);
               }
            }
         } else {
            this.release();
         }
      }
   }

   private void blit(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler) {
      try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Skycore MotionBlur Blit", gpuTextureView, Optional.empty())) {
         device.setPipeline(MotionBlurPipelines.MOTION_BLUR_BLIT);
         RenderSystem.bindDefaultUniforms(device);
         device.bindTexture("CurrentSampler", gpuTextureView2, gpuSampler);
         device.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   private void ensureTargets(int fullWidth2, int fullHeight2, int value, int value2) {
      historyValidation: {
         if (this.fullWidth == fullWidth2) {
            if (this.fullHeight == fullHeight2) {
               break historyValidation;
            }
         }

         this.hasHistory = false;
         this.fullWidth = fullWidth2;
         this.fullHeight = fullHeight2;
      }

      this.scratch = this.ensureTarget(this.scratch, "skycore-motion-blur-scratch", fullWidth2, fullHeight2);
      TextureTarget textureTarget = this.ensureTarget(this.history, "skycore-motion-blur-history", value, value2);
      this.history = textureTarget;
   }
}
