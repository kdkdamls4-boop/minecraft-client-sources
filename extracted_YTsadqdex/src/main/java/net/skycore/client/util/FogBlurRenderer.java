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
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.FogModule;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class FogBlurRenderer {
   private GpuBuffer uniforms;
   private final Matrix4f inverseViewProjection = new Matrix4f();
   private final FogModule module;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
   private TextureTarget sceneCopy;

   private void ensureUniforms() {
      if (this.uniforms == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> {
            return "Skycore FogBlur UBO";
         };
         long uNIFORM_SIZE = UNIFORM_SIZE;
         GpuBuffer gpuBuffer = device.createBuffer(supplier, 136, uNIFORM_SIZE);
         this.uniforms = gpuBuffer;
      }
   }

   private static float renderDistanceBlocks(Minecraft minecraft) {
      return minecraft.options.getEffectiveRenderDistance() * 16.0F;
   }

   public FogBlurRenderer(FogModule module2) {
      this.module = module2;
   }

   public void render(CameraRenderState cameraRenderState) {
      if (this.module.usesFogBlur()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null && RenderSystem.tryGetDevice() != null && cameraRenderState != null && cameraRenderState.initialized) {
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
                  float value = this.module.blurStrength().floatValue();
                  float clampedValue = ClampByteHelper.clamp(value * 2.2F, 2.0F, 28.0F);
                  RunPassHelper.acquireView();
                  RunPassHelper.requestBlurRadius(clampedValue);
                  RunPassHelper.captureNow();
                  GpuTextureView gpuTextureView = RunPassHelper.acquireView();
                  if (gpuTextureView != null) {
                     GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                     GpuSampler clampToEdge2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                     this.inverseViewProjection.set(matrix).mul(cameraRenderState.viewRotationMatrix).invert();
                     float computedValue = Math.max(64.0F, renderDistanceBlocks(instance));
                     float computedValue2 = Math.min(this.module.blurDistance().floatValue(), computedValue * 0.85F);
                     float computedValue3 = Math.min(computedValue * 0.92F, Math.max(computedValue2 + 24.0F, computedValue2 * 2.4F));
                     int value2 = this.module.resolveBlurTint();
                     boolean get = this.module.blurTint().get();
                     float gameTime = instance.level != null ? (float)(instance.level.getGameTime() % 7200L) / 20.0F * 2.5F : 0.0F;
                     float device = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
                     float value3 = get ? 0.45F : 0.0F;
                     float value4 = get ? 1.0F : 0.0F;
                     float computedValue4 = WriteHexByteHelper.red(value2) / 255.0F;
                     float computedValue5 = WriteHexByteHelper.green(value2) / 255.0F;
                     float computedValue6 = WriteHexByteHelper.blue(value2) / 255.0F;

                     try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                        ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE)
                           .putMat4f(this.inverseViewProjection)
                           .putVec4(value, computedValue2, computedValue3, device)
                           .putVec4(computedValue4, computedValue5, computedValue6, 1.0F)
                           .putVec4(1.0F, 1.0F, 1.0F, 1.0F)
                           .putVec4(1.0F, 1.0F, 1.0F, 1.0F)
                           .putVec4(computedValue4, computedValue5, computedValue6, 1.0F)
                           .putVec4(value3, gameTime, value4, 0.0F)
                           .get();
                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
                     }

                     try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                           return "Skycore FogBlur";
                        }, renderTarget.getColorTextureView(), Optional.empty())) {
                        device2.setPipeline(FogBlurPipeline.FOG_BLUR);
                        RenderSystem.bindDefaultUniforms(device2);
                        device2.setUniform("FogBlurUniforms", this.uniforms);
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

   private void ensureTargets(int width, int height) {
      if (this.sceneCopy != null) {
         if (this.sceneCopy.width == width && this.sceneCopy.height == height) {
            return;
         }
      }

      if (this.sceneCopy != null) {
         this.sceneCopy.destroyBuffers();
      }

      TextureTarget textureTarget = new TextureTarget("skycore-fog-scene", width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
      this.sceneCopy = textureTarget;
   }

   public void release() {
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
