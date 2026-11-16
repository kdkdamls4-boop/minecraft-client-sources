package net.skycore.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.skycore.client.module.render.HandsModule;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class UpdateTrailHelper {
   private TextureTarget trailA;
   private final HandsModule module;
   private TextureTarget mask;
   private TextureTarget sceneBefore;
   private long lastFrameNs;
   private GpuBuffer maskParams;
   private GpuBuffer fireParams;
   private boolean hasHistory;
   private static final int MASK_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final long STALE_FRAME_NS = 220000000L;
   private int width;
   private TextureTarget sceneAfter;
   private GpuBuffer trailParams;
   private final TextureTarget[] blurDown = new TextureTarget[3];
   private boolean capturing;
   private static final int TRAIL_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().get();
   private TextureTarget blurA;
   private long startedNs;
   private int effectWidth;
   private boolean trailAIsRead;
   private TextureTarget trailB;
   private static final int BLUR_SIZE = new Std140SizeCalculator().putVec4().get();
   private int height;
   private int effectHeight;
   private final TextureTarget[] blurUp = new TextureTarget[3];
   private GpuBuffer blurParams;
   private static final int FIRE_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().get();

   private void updateTrail(long timestamp, float width2) {
      TextureTarget textureTarget = this.trailAIsRead ? this.trailA : this.trailB;
      TextureTarget textureTarget2 = this.trailAIsRead ? this.trailB : this.trailA;
      int value = this.module.color();
      float width3 = (float)(timestamp - this.startedNs) / 1.0E9F;
      float width4 = this.module.activity();
      float width5 = this.module.slash();

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, TRAIL_SIZE)
            .putVec4(this.effectWidth, this.effectHeight, width3, this.module.intensity())
            .putVec4((value >>> 16 & 0xFF) / 255.0F, (value >>> 8 & 0xFF) / 255.0F, (value & 0xFF) / 255.0F, this.module.rainbowScale())
            .putVec4(width2, this.module.speed(), this.module.length(), this.module.trailSoftness())
            .putVec4(this.module.trailBlur(), this.module.smoke(), width4, this.module.trailFade())
            .putVec4(width5, this.module.slashDirection(), this.module.rainbow() ? 1.0F : (this.module.custom() ? 2.0F : 0.0F), this.module.rainbowSpeed())
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.trailParams.slice(), byteBuffer);
      }

      GpuSampler gpuSampler = sampler(FilterMode.LINEAR);

      try (RenderPass renderPass = pass("trail", textureTarget2)) {
         renderPass.setPipeline(TargetEffectPipelines.TRAIL);
         renderPass.setUniform("HandTrailData", this.trailParams);
         renderPass.bindTexture("PrevTrailSampler", textureTarget.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("SceneSampler", this.sceneAfter.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("MaskSampler", this.mask.getColorTextureView(), gpuSampler);
         draw(renderPass);
      }

      this.trailAIsRead = ((!this.trailAIsRead ? 1 : 0) != 0);
      this.hasHistory = true;
   }

   public void resetHistory() {
      this.capturing = false;
      this.hasHistory = false;
      this.trailAIsRead = true;
      this.lastFrameNs = 0L;
      this.startedNs = System.nanoTime();
   }

   private void ensureUniforms() {
      if (this.maskParams == null) {
         GpuBuffer gpuBuffer = uniform("mask", MASK_SIZE);
         this.maskParams = gpuBuffer;
      }

      if (this.trailParams == null) {
         GpuBuffer gpuBuffer2 = uniform("trail", TRAIL_SIZE);
         this.trailParams = gpuBuffer2;
      }

      if (this.blurParams == null) {
         this.blurParams = uniform("blur", BLUR_SIZE);
      }

      if (this.fireParams == null) {
         GpuBuffer gpuBuffer3 = uniform("fire", FIRE_SIZE);
         this.fireParams = gpuBuffer3;
      }
   }

   private static void destroy(TextureTarget textureTarget) {
      if (textureTarget != null) {
         textureTarget.destroyBuffers();
      }
   }

   private void createMask() {
      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, MASK_SIZE).putVec4(this.width, this.height, 0.0F, 0.0F).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.maskParams.slice(), byteBuffer);
      }

      GpuSampler gpuSampler = sampler(FilterMode.LINEAR);
      GpuSampler gpuSampler2 = sampler(FilterMode.NEAREST);

      try (RenderPass renderPass = pass("mask", this.mask)) {
         renderPass.setPipeline(TargetEffectPipelines.MASK);
         renderPass.setUniform("MaskData", this.maskParams);
         renderPass.bindTexture("BeforeSampler", this.sceneBefore.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("AfterSampler", this.sceneAfter.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("DepthBeforeSampler", this.sceneBefore.getDepthTextureView(), gpuSampler2);
         renderPass.bindTexture("DepthAfterSampler", this.sceneAfter.getDepthTextureView(), gpuSampler2);
         draw(renderPass);
      }
   }

   private static void clearTrail(TextureTarget textureTarget) {
      CommandEncoder device = RenderSystem.getDevice().createCommandEncoder();
      GpuTexture colorTexture = textureTarget.getColorTexture();
      Vector4f vector4f = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
      device.clearColorTexture(colorTexture, vector4f);
   }

   private void ensureTargets(RenderTarget width2) {
      if (width2.width != this.width || width2.height != this.height || this.sceneBefore == null) {
         this.release();
         this.width = width2.width;
         this.height = width2.height;
         this.effectWidth = Math.max(1, (this.width + 1) / 2);
         this.effectHeight = Math.max(1, (this.height + 1) / 2);
         this.sceneBefore = this.target("scene_before", true, this.width, this.height);
         this.sceneAfter = this.target("scene_after", true, this.width, this.height);
         this.mask = this.target("mask", false, this.width, this.height);
         this.trailA = this.target("trail_a", false, this.effectWidth, this.effectHeight);
         this.trailB = this.target("trail_b", false, this.effectWidth, this.effectHeight);
         this.blurA = this.target("blur_a", false, this.effectWidth, this.effectHeight);
         int effectWidth2 = this.effectWidth;
         int effectHeight2 = this.effectHeight;

         for (int value = 0; value < this.blurDown.length; value++) {
            effectWidth2 = Math.max(1, effectWidth2 / 2);
            effectHeight2 = Math.max(1, effectHeight2 / 2);
            this.blurDown[value] = this.target("blur_down_" + value, false, effectWidth2, effectHeight2);
            this.blurUp[value] = this.target("blur_up_" + value, false, effectWidth2, effectHeight2);
         }

         clearTrail(this.trailA);
         clearTrail(this.trailB);
      }
   }

   public void release() {
      this.resetHistory();
      destroy(this.sceneBefore);
      this.sceneBefore = null;
      destroy(this.sceneAfter);
      this.sceneAfter = null;
      destroy(this.mask);
      this.mask = null;
      destroy(this.trailA);
      this.trailA = null;
      destroy(this.trailB);
      this.trailB = null;
      destroy(this.blurA);
      this.blurA = null;

      for (int value = 0; value < this.blurDown.length; value++) {
         destroy(this.blurDown[value]);
         this.blurDown[value] = null;
         destroy(this.blurUp[value]);
         this.blurUp[value] = null;
      }

      if (this.maskParams != null) {
         this.maskParams.close();
         this.maskParams = null;
      }

      if (this.trailParams != null) {
         this.trailParams.close();
         this.trailParams = null;
      }

      if (this.blurParams != null) {
         this.blurParams.close();
         this.blurParams = null;
      }

      if (this.fireParams != null) {
         this.fireParams.close();
         this.fireParams = null;
      }

      this.width = this.height = this.effectWidth = this.effectHeight = 0;
   }

   private boolean ready() {
      if (this.module.isEnabled() && RenderSystem.tryGetDevice() != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null && instance.level != null && instance.gameRenderer != null && instance.options.getCameraType().isFirstPerson()) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            return renderTarget != null
               && renderTarget.width > 0
               && renderTarget.height > 0
               && renderTarget.getColorTexture() != null
               && renderTarget.getColorTextureView() != null
               && renderTarget.getDepthTexture() != null
               && renderTarget.getDepthTextureView() != null;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void composite(RenderTarget renderTarget, long timestamp) {
      int value = this.module.color();
      float width2 = (float)(timestamp - this.startedNs) / 1.0E9F;

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, FIRE_SIZE)
            .putVec4(this.width, this.height, width2, this.module.intensity())
            .putVec4((value >>> 16 & 0xFF) / 255.0F, (value >>> 8 & 0xFF) / 255.0F, (value & 0xFF) / 255.0F, this.module.rainbowSpeed())
            .putVec4(this.module.rainbowScale(), this.module.speed(), this.module.length(), this.module.handSoftness())
            .putVec4(this.module.handBlur(), this.module.smoke(), this.module.activity(), this.module.rainbow() ? 1.0F : 0.0F)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.fireParams.slice(), byteBuffer);
      }

      GpuSampler gpuSampler = sampler(FilterMode.LINEAR);

      try (RenderPass renderPass = pass("composite", renderTarget)) {
         renderPass.setPipeline(TargetEffectPipelines.FIRE);
         renderPass.setUniform("HandFireData", this.fireParams);
         renderPass.bindTexture("SceneSampler", this.sceneAfter.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("BlurSampler", this.blurA.getColorTextureView(), gpuSampler);
         renderPass.bindTexture("MaskSampler", this.mask.getColorTextureView(), gpuSampler);
         draw(renderPass);
      }
   }

   private static GpuSampler sampler(FilterMode filterMode) {
      SamplerCache samplerCache = RenderSystem.getSamplerCache();
      return samplerCache.getClampToEdge(filterMode);
   }

   public UpdateTrailHelper(HandsModule module2) {
      this.trailAIsRead = true;
      this.startedNs = System.nanoTime();
      this.module = module2;
   }

   private TextureTarget target(String text, boolean condition, int value, int value2) {
      String text2 = "skycore-soul-hands-" + text;
      return new TextureTarget(text2, value, value2, condition, WorldSkyPipelines.EFFECT_FORMAT);
   }

   private void kawasePass(GpuTextureView gpuTextureView, TextureTarget textureTarget, int value, int value2, float value3) {
      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, BLUR_SIZE).putVec4(value, value2, value3, 0.0F).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.blurParams.slice(), byteBuffer);
      }

      try (RenderPass renderPass = pass("kawase", textureTarget)) {
         renderPass.setPipeline(TargetEffectPipelines.BLUR);
         renderPass.setUniform("HandBlurData", this.blurParams);
         renderPass.bindTexture("Sampler0", gpuTextureView, sampler(FilterMode.LINEAR));
         draw(renderPass);
      }
   }

   private static RenderPass pass(String text, RenderTarget renderTarget) {
      CommandEncoder device = RenderSystem.getDevice().createCommandEncoder();
      return device.createRenderPass(() -> "Skycore SOULBASE hands " + text, renderTarget.getColorTextureView(), Optional.empty());
   }

   private void blurTrail() {
      TextureTarget textureTarget = this.trailAIsRead ? this.trailA : this.trailB;
      int value = this.module.handBlur() >= 2.15F ? 3 : (this.module.handBlur() >= 1.05F ? 2 : 1);
      float color = Math.max(0.55F, this.module.handBlur() * 1.22F * 0.5F);
      int effectWidth2 = this.effectWidth;
      int effectHeight2 = this.effectHeight;

      for (int value2 = 0; value2 < value; value2++) {
         this.kawasePass(textureTarget.getColorTextureView(), this.blurDown[value2], effectWidth2, effectHeight2, color);
         textureTarget = this.blurDown[value2];
         effectWidth2 = textureTarget.width;
         effectHeight2 = textureTarget.height;
      }

      for (int index = value - 1; index >= 0; index--) {
         this.kawasePass(textureTarget.getColorTextureView(), this.blurUp[index], effectWidth2, effectHeight2, color);
         textureTarget = this.blurUp[index];
         effectWidth2 = textureTarget.width;
         effectHeight2 = textureTarget.height;
      }

      this.kawasePass(textureTarget.getColorTextureView(), this.blurA, effectWidth2, effectHeight2, color);
   }

   public void beforeHands() {
      this.capturing = false;
      if (this.ready()) {
         RenderTarget instance = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         this.ensureTargets(instance);
         this.ensureUniforms();
         GpuTexture colorTexture = instance.getColorTexture();
         this.copy(colorTexture, this.sceneBefore.getColorTexture());
         this.copy(instance.getDepthTexture(), this.sceneBefore.getDepthTexture());
         this.capturing = true;
      }
   }

   public void afterHands() {
      if (this.capturing) {
         this.capturing = false;
         if (!this.ready()) {
            this.resetHistory();
         } else {
            RenderTarget instance = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (instance.width == this.width && instance.height == this.height) {
               this.copy(instance.getColorTexture(), this.sceneAfter.getColorTexture());
               this.copy(instance.getDepthTexture(), this.sceneAfter.getDepthTexture());
               long nowNanos = System.nanoTime();
               long timestamp = nowNanos - this.lastFrameNs;
               if (this.lastFrameNs == 0L || timestamp > 220000000L) {
                  this.hasHistory = false;
               }

               float computedValue = this.lastFrameNs == 0L ? 0.016666668F : Math.max(0.004166667F, Math.min(0.05F, (float)timestamp / 1.0E9F));
               this.lastFrameNs = nowNanos;
               if (!this.hasHistory) {
                  clearTrail(this.trailA);
                  clearTrail(this.trailB);
               }

               this.createMask();
               this.updateTrail(nowNanos, computedValue);
               this.blurTrail();
               this.composite(instance, nowNanos);
               this.module.advancePulse(computedValue);
            } else {
               this.resetHistory();
            }
         }
      }
   }

   private static GpuBuffer uniform(String text, int value) {
      GpuDevice device = RenderSystem.getDevice();
      Supplier<java.lang.String> supplier = () -> {
         return "Skycore SOULBASE hands " + text;
      };
      long timestamp = value;
      return device.createBuffer(supplier, 136, timestamp);
   }

   private static void draw(RenderPass renderPass) {
      int count = -1;
      count = (byte)-1;
      renderPass.setVertexBuffer(0, EnsureHelper.buffer().slice());
      count = EnsureHelper.vertexCount();
      renderPass.draw(count, 1, 0, 0);
   }

   private void copy(GpuTexture gpuTexture, GpuTexture gpuTexture2) {
      GpuDevice device = RenderSystem.getDevice();
      CommandEncoder commandEncoder = device.createCommandEncoder();
      commandEncoder.copyTextureToTexture(gpuTexture, gpuTexture2, 0, 0, 0, 0, 0, this.width, this.height);
   }
}
