package net.skycore.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.FocusModule;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class FocusBlurRenderer {
   private float visualDepth;
   private long lastFrameAt;
   private GpuBuffer uniforms;
   private TextureTarget sceneCopy;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().get();
   private float blend;
   private float lastRadius;
   private final FocusModule module;
   private final Matrix4f inverseViewProjection = new Matrix4f();

   public void render(CameraRenderState cameraRenderState) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && RenderSystem.tryGetDevice() != null && cameraRenderState != null && cameraRenderState.initialized) {
         float value = this.deltaSeconds();
         float value2 = this.module.focusReady() ? 1.0F : 0.0F;
         float value3 = value2 > this.blend ? 8.0F : 10.0F;
         this.blend = this.blend + (value2 - this.blend) * (1.0F - (float)Math.exp(-value3 * value));
         if (Math.abs(value2 - this.blend) < 0.001F) {
            this.blend = value2;
         }

         if (!(this.blend <= 0.002F)) {
            Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
            if (matrix != null) {
               RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
               if (renderTarget != null
                  && renderTarget.width > 0
                  && renderTarget.height > 0
                  && renderTarget.getColorTexture() != null
                  && renderTarget.getColorTextureView() != null
                  && renderTarget.getDepthTextureView() != null) {
                  this.ensureTargets(renderTarget.width, renderTarget.height);
                  this.ensureUniforms();
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .copyTextureToTexture(renderTarget.getColorTexture(), this.sceneCopy.getColorTexture(), 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);
                  RunPassHelper.acquireView();
                  RunPassHelper.requestBlurRadius(32.0F);
                  RunPassHelper.captureNow();
                  GpuTextureView gpuTextureView = RunPassHelper.acquireView();
                  if (gpuTextureView != null) {
                     float computedValue = Math.max(0.1F, this.module.focusDistance());
                     if (this.visualDepth <= 0.01F || this.blend <= 0.05F) {
                        this.visualDepth = computedValue;
                        this.lastRadius = this.module.focusRadius();
                     } else if (value2 > 0.5F) {
                        this.visualDepth = this.visualDepth + (computedValue - this.visualDepth) * (1.0F - (float)Math.exp(-8.0F * value));
                        this.lastRadius = this.lastRadius + (this.module.focusRadius() - this.lastRadius) * (1.0F - (float)Math.exp(-7.0F * value));
                     }

                     float maximum = Math.max(0.32F, this.visualDepth * 0.055F);
                     float radius = renderTarget.height <= 0 ? 1.0F : (float)renderTarget.width / renderTarget.height;
                     float device = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
                     this.inverseViewProjection.set(matrix).mul(cameraRenderState.viewRotationMatrix).invert();

                     try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                        ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE)
                           .putMat4f(this.inverseViewProjection)
                           .putVec4(this.visualDepth, maximum, this.blend, device)
                           .putVec4(this.lastRadius, radius, 0.0F, 0.0F)
                           .get();
                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
                     }

                     GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                     GpuSampler clampToEdge2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

                     try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                           return "Skycore Focus Blur";
                        }, renderTarget.getColorTextureView(), Optional.empty())) {
                        device2.setPipeline(FocusBlurPipeline.FOCUS_BLUR);
                        RenderSystem.bindDefaultUniforms(device2);
                        device2.setUniform("FocusBlurUniforms", this.uniforms);
                        device2.bindTexture("SceneSampler", this.sceneCopy.getColorTextureView(), clampToEdge);
                        device2.bindTexture("BlurSampler", gpuTextureView, clampToEdge);
                        device2.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), clampToEdge2);
                        device2.setVertexBuffer(0, EnsureHelper.buffer().slice());
                        device2.draw(EnsureHelper.vertexCount(), 1, 0, 0);
                     }
                  }
               }
            }
         }
      }
   }

   private void ensureUniforms() {
      if (this.uniforms == null) {
         GpuDevice device = RenderSystem.getDevice();
         GpuBuffer gpuBuffer = device.createBuffer(() -> {
            return "Skycore FocusBlur UBO";
         }, 136, UNIFORM_SIZE);
         this.uniforms = gpuBuffer;
      }
   }

   public void reset() {
      this.blend = 0.0F;
      this.visualDepth = 0.0F;
      this.lastRadius = 0.18F;
      this.lastFrameAt = System.nanoTime();
   }

   public FocusBlurRenderer(FocusModule module2) {
      this.lastRadius = 0.18F;
      this.lastFrameAt = System.nanoTime();
      this.module = module2;
   }

   private float deltaSeconds() {
      long nowNanos = System.nanoTime();
      float intermediate = (float)(nowNanos - this.lastFrameAt) / 1.0E9F;
      float result = Math.max(0.0F, Math.min(0.05F, intermediate));
      this.lastFrameAt = nowNanos;
      return result;
   }

   private void ensureTargets(int width, int height) {
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

      this.sceneCopy = new TextureTarget("skycore-focus-scene", width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
   }

   public void release() {
      this.reset();
      if (this.sceneCopy != null) {
         this.sceneCopy.destroyBuffers();
         this.sceneCopy = null;
      }

      if (this.uniforms != null) {
         this.uniforms.close();
         this.uniforms = null;
      }
   }
}
