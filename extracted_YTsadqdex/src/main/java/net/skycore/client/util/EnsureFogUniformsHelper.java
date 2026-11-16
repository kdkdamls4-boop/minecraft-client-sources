package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.render.AtmosphereModule;
import net.skycore.client.module.render.WorldTweaksModule;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class EnsureFogUniformsHelper {
   private static final int LAYERS = 2;
   private int streakCapacity;
   private GpuTextureView reflectedSky;
   private final Matrix4f invViewProj = new Matrix4f();
   private GpuBuffer streakVertexBuffer;
   private static final int GRID = 12;
   private static final int MAX_QUADS = 288;
   private TextureTarget sceneCopy;
   private final AtmosphereModule module;
   private final Matrix4f viewProj = new Matrix4f();
   private GpuBuffer rainGroundUniforms;
   private final Matrix4f view = new Matrix4f();
   private static final int FOG_UBO_SIZE = new Std140SizeCalculator()
         .putMat4f()
         .putMat4f()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .get()
      + 48;
   private static final int RAIN_GROUND_UBO_SIZE = new Std140SizeCalculator()
         .putMat4f()
         .putMat4f()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .putVec4()
         .get()
      + 48;
   private final Matrix4f proj = new Matrix4f();
   private boolean rainFailed;
   private final SkyPanoramaRenderer skyEnvironment = new SkyPanoramaRenderer();
   private GpuBuffer fogUniforms;

   private static float mix(int value, int value2, float value3) {
      return value + (value2 - value) * value3;
   }

   private void ensureFogUniforms() {
      if (this.fogUniforms == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> {
            return "Skycore Atmosphere Fog UBO";
         };
         long fOG_UBO_SIZE = FOG_UBO_SIZE;
         this.fogUniforms = device.createBuffer(supplier, 136, fOG_UBO_SIZE);
      }
   }

   private void ensureRainGroundUniforms() {
      if (this.rainGroundUniforms == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> "Skycore Atmosphere Rain Ground UBO";
         long rAIN_GROUND_UBO_SIZE = RAIN_GROUND_UBO_SIZE;
         GpuBuffer gpuBuffer = device.createBuffer(supplier, 136, rAIN_GROUND_UBO_SIZE);
         this.rainGroundUniforms = gpuBuffer;
      }
   }

   private static float smooth(float value, float value2, float value3) {
      float computedValue = Math.max(0.0F, Math.min(1.0F, (value3 - value) / (value2 - value)));
      return computedValue * computedValue * (3.0F - 2.0F * computedValue);
   }

   private void ensureStreakBuffer(int value) {
      int intermediate = -1;
      intermediate = (byte)-1;
      if (this.streakVertexBuffer != null) {
         if (this.streakCapacity >= value) {
            return;
         }
      }

      if (this.streakVertexBuffer != null) {
         this.streakVertexBuffer.close();
      }

      intermediate = Math.max(value, 4096);
      this.streakCapacity = intermediate;
      GpuDevice device = RenderSystem.getDevice();
      Supplier<java.lang.String> supplier = () -> {
         return "Skycore Atmosphere Rain Streaks";
      };
      GpuBuffer gpuBuffer = device.createBuffer(supplier, 40, this.streakCapacity);
      this.streakVertexBuffer = gpuBuffer;
   }

   public void release() {
      this.reflectedSky = null;
      this.skyEnvironment.close();
      if (this.sceneCopy != null) {
         this.sceneCopy.destroyBuffers();
         this.sceneCopy = null;
      }

      if (this.fogUniforms != null) {
         this.fogUniforms.close();
         this.fogUniforms = null;
      }

      if (this.rainGroundUniforms != null) {
         this.rainGroundUniforms.close();
         this.rainGroundUniforms = null;
      }

      if (this.streakVertexBuffer != null) {
         this.streakVertexBuffer.close();
         this.streakVertexBuffer = null;
         this.streakCapacity = 0;
      }

      this.rainFailed = false;
   }

   private float[] lightDirection(Minecraft minecraft) {
      float computedValue = skyAngle(minecraft) * (float) (Math.PI * 2);
      float sine = -Mth.sin(computedValue);
      float cosine = Mth.cos(computedValue);
      Camera camera = minecraft.gameRenderer.mainCamera();
      float y = camera.yRot() * (float) (Math.PI / 180.0);
      double value = y;
      float cosine2 = sine * Mth.cos(value);
      float sine2 = sine * Mth.sin(y);
      float computedValue2 = cosine2 * cosine2 + cosine * cosine + sine2 * sine2;
      float squareRoot = Mth.sqrt(computedValue2);
      return squareRoot < 1.0E-4F ? new float[]{0.0F, 1.0F, 0.0F} : new float[]{cosine2 / squareRoot, cosine / squareRoot, sine2 / squareRoot};
   }

   private static float mod(float value, float value2) {
      double intermediate = value / value2;
      return value - value2 * (float)Math.floor(intermediate);
   }

   private static boolean valid(RenderTarget renderTarget) {
      if (renderTarget != null && renderTarget.width > 0 && renderTarget.height > 0 && renderTarget.getColorTexture() != null) {
         if (renderTarget.getColorTextureView() != null) {
            if (renderTarget.getDepthTextureView() != null) {
               return true;
            }
         }
      }

      return false;
   }

   public void resetFailure() {
      this.rainFailed = false;
   }

   private void renderFog(Minecraft minecraft, RenderTarget renderTarget, CameraRenderState cameraRenderState, boolean condition) {
      this.ensureFogUniforms();
      GpuTextureView colorTextureView = this.sceneCopy.getColorTextureView();
      if (condition) {
         float radius = Math.max(2.0F, this.module.distanceBlur() * 12.0F);
         RunPassHelper.acquireView();
         RunPassHelper.requestBlurRadius(radius);
         RunPassHelper.captureNow();
         GpuTextureView gpuTextureView = RunPassHelper.acquireView();
         if (gpuTextureView != null) {
            colorTextureView = gpuTextureView;
         }
      }

      float width = 0.0F;
      if (this.module.duskVeil()) {
         float value = skyAngle(minecraft);
         float computedValue = Mth.clamp(1.0F - Math.abs(value - 0.5F) * 2.0F, 0.0F, 1.0F);
         width = (1.0F - computedValue) * this.module.duskStrength();
      }

      float width2 = this.module.volumetricFog() ? this.module.density() : (this.module.duskVeil() ? 0.35F * width : 0.0F);
      float effectiveRenderDistance = Math.max(48.0F, minecraft.options.getEffectiveRenderDistance() * 16.0F);
      float width3 = Math.min(this.module.fogEnd(), effectiveRenderDistance * 0.78F);
      float width4 = Math.min(this.module.fogStart(), width3 * 0.42F);
      width3 = Math.max(width4 + 10.0F, width3);
      float width5 = 0.0F;
      float width6 = 0.0F;
      float width7 = 0.35F;
      float width8 = 0.0F;
      float width9 = 0.0F;
      float width10 = 0.0F;
      if (this.module.wetReflections() && !this.module.rainOwnsWet()) {
         width5 = this.module.wetBlocks();
         width6 = this.module.wetWater();
         width7 = this.module.wetSlope();
         float maximum = Math.max(width5, width6);
         width8 = 0.55F + 0.35F * maximum;
         width9 = 0.28F + 0.25F * maximum;
         width10 = 0.4F + 0.35F * maximum;
      }

      int value2 = this.module.resolveFogColor();
      float[] floatValues = this.lightDirection(minecraft);
      Vec3 pos = cameraRenderState.pos;
      float width11 = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
      float device = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = this.appendSkyReflection(
               Std140Builder.onStack(memoryStack, FOG_UBO_SIZE)
                  .putMat4f(this.invViewProj)
                  .putMat4f(this.viewProj)
                  .putVec4(WriteHexByteHelper.red(value2) / 255.0F, WriteHexByteHelper.green(value2) / 255.0F, WriteHexByteHelper.blue(value2) / 255.0F, 1.0F)
                  .putVec4(floatValues[0], floatValues[1], floatValues[2], device)
                  .putVec4((float)pos.x(), (float)pos.y(), (float)pos.z(), width11)
                  .putVec4(1.0F / renderTarget.width, 1.0F / renderTarget.height, 0.0F, 0.0F)
                  .putVec4(width2, this.module.softness(), 0.0F, device)
                  .putVec4(width4, width3, width, this.module.distanceBlur())
                  .putVec4(width5, this.module.height(), width6, width7)
                  .putVec4(0.05F, effectiveRenderDistance, 0.0F, 0.0F)
                  .putVec4(width8, width9, width10, 1.0F)
                  .putVec4(8.0F, 0.6F, 24.0F, width11)
            )
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.fogUniforms.slice(), byteBuffer);
      }

      GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      GpuSampler clampToEdge2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

      try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Skycore Atmosphere Fog";
         }, renderTarget.getColorTextureView(), Optional.empty())) {
         device2.setPipeline(AtmospherePipelines.FOG);
         RenderSystem.bindDefaultUniforms(device2);
         device2.setUniform("AtmosphereUniforms", this.fogUniforms);
         device2.bindTexture("SkyEnvironmentSampler", this.reflectedSky != null ? this.reflectedSky : this.sceneCopy.getColorTextureView(), clampToEdge);
         device2.bindTexture("SceneSampler", this.sceneCopy.getColorTextureView(), clampToEdge);
         device2.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), clampToEdge2);
         device2.bindTexture("BlurSampler", colorTextureView, clampToEdge);
         device2.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device2.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   private static float hash(float value, float value2) {
      float computedValue = value * 12.9898F + value2 * 78.233F;
      int value3 = Float.floatToIntBits(computedValue);
      value3 ^= value3 >>> 16;
      value3 *= 2146121005;
      value3 ^= value3 >>> 15;
      return (value3 & 65535) * 1.5259022E-5F;
   }

   private void renderRain(Minecraft minecraft, RenderTarget renderTarget, CameraRenderState cameraRenderState) {
      float value = visualDensity(this.module.rainIntensity());
      if (this.module.rainAlways()) {
         value = Math.max(value, 0.32F);
      } else {
         float rainLevel = Mth.clamp(minecraft.level.getRainLevel(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)), 0.0F, 1.0F);
         value *= rainLevel;
      }

      int value2 = !this.module.wetReflections() || !(this.module.wetBlocks() > 0.001F) && !(this.module.wetWater() > 0.001F) ? 0 : 1;
      int value3 = this.module.rainSplashes() && value > 0.02F ? 1 : 0;
      int value4 = value > 0.02F ? 1 : 0;
      if (value2 != 0 || value3 != 0 || value4 != 0) {
         float computedValue = (float)(System.currentTimeMillis() % 100000L) / 1000.0F * 1.65F;
         int value5 = this.module.resolveFogColor();
         float[] floatValues = new float[]{
            mix(232, WriteHexByteHelper.red(value5), 0.72F) / 255.0F,
            mix(244, WriteHexByteHelper.green(value5), 0.72F) / 255.0F,
            mix(255, WriteHexByteHelper.blue(value5), 0.72F) / 255.0F
         };
         if (value2 != 0 || value3 != 0) {
            this.copyScene(renderTarget);
            this.renderRainGround(minecraft, renderTarget, cameraRenderState, value, computedValue, (value2 != 0), (value3 != 0), floatValues);
         }

         if (value4 != 0) {
            this.renderRainStreaks(minecraft, renderTarget, cameraRenderState, value, floatValues, computedValue);
         }
      }
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

      this.sceneCopy = new TextureTarget("skycore-atmosphere-scene", width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
   }

   private static void putVert(BufferBuilder bufferBuilder, Vec3 position, Matrix4f matrix, float value, float value2, float value3, float value4, float value5, int color) {
      Vector4f vector4f = new Vector4f(value, value2, value3, 1.0F).mul(matrix);
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z);
      vertexConsumer.setUv(value4, value5).setColor(color);
   }

   private void renderRainGround(Minecraft minecraft, RenderTarget renderTarget, CameraRenderState cameraRenderState, float coordinate, float coordinate2, boolean condition, boolean condition2, float[] floatValues) {
      this.ensureRainGroundUniforms();
      minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      float computedValue = skyAngle(minecraft) * (float) (Math.PI * 2);
      float computedValue2 = Math.max(0.08F, (float)Math.sin(computedValue));
      float computedValue3 = (float)Math.cos(computedValue) * 0.55F;
      float computedValue4 = (float)Math.sin(computedValue * 0.35) * 0.25F;
      int value = this.module.resolveFogColor();
      float computedValue5 = mixF(0.32F, WriteHexByteHelper.red(value) / 255.0F, 0.72F);
      float computedValue6 = mixF(0.42F, WriteHexByteHelper.green(value) / 255.0F, 0.72F);
      float computedValue7 = mixF(0.7F, WriteHexByteHelper.blue(value) / 255.0F, 0.72F);
      float computedValue8 = Math.max(this.module.wetBlocks(), this.module.wetWater());
      float computedValue9 = 0.55F + 0.35F * computedValue8;
      float computedValue10 = 0.28F + 0.25F * computedValue8;
      float computedValue11 = 0.4F + 0.35F * computedValue8;
      float value2 = mixF(0.18F, 0.28F, coordinate);
      float computedValue12 = this.module.rainSplashSize() * mixF(0.32F, 0.55F, coordinate);
      float device = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
      Vec3 pos = cameraRenderState.pos;

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = this.appendSkyReflection(
               Std140Builder.onStack(memoryStack, RAIN_GROUND_UBO_SIZE)
                  .putMat4f(this.viewProj)
                  .putMat4f(this.invViewProj)
                  .putVec4((float)pos.x(), (float)pos.y(), (float)pos.z(), coordinate2)
                  .putVec4(computedValue5, computedValue6, computedValue7, 0.0F)
                  .putVec4(computedValue3, computedValue2, computedValue4, 0.0F)
                  .putVec4(Mth.clamp(computedValue8, 0.0F, 1.0F), Mth.clamp(computedValue9, 0.0F, 1.0F), 1.0F, Mth.clamp(this.module.wetSlope(), 0.2F, 0.9F))
                  .putVec4(Mth.clamp(computedValue10, 0.0F, 1.0F), Mth.clamp(computedValue11, 0.0F, 1.0F), 1.0F / renderTarget.width, 1.0F / renderTarget.height)
                  .putVec4(8.0F, 0.55F, 28.0F, condition ? 1.0F : 0.0F)
                  .putVec4(coordinate2, condition2 ? coordinate : 0.0F, value2, computedValue12)
                  .putVec4(floatValues[0], floatValues[1], floatValues[2], 0.85F)
                  .putVec4(device, 0.0F, 0.0F, 0.0F)
            )
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.rainGroundUniforms.slice(), byteBuffer);
      }

      GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      GpuSampler clampToEdge2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

      try (RenderPass device2 = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Skycore Atmosphere Rain Ground", renderTarget.getColorTextureView(), Optional.empty())) {
         device2.setPipeline(AtmospherePipelines.RAIN_GROUND);
         RenderSystem.bindDefaultUniforms(device2);
         device2.setUniform("RainGroundUniforms", this.rainGroundUniforms);
         device2.bindTexture("SkyEnvironmentSampler", this.reflectedSky != null ? this.reflectedSky : this.sceneCopy.getColorTextureView(), clampToEdge);
         device2.bindTexture("SceneSampler", this.sceneCopy.getColorTextureView(), clampToEdge);
         device2.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), clampToEdge2);
         device2.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device2.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }
   }

   private void copyScene(RenderTarget renderTarget) {
      GpuDevice device = RenderSystem.getDevice();
      CommandEncoder commandEncoder = device.createCommandEncoder();
      GpuTexture colorTexture = renderTarget.getColorTexture();
      GpuTexture colorTexture2 = this.sceneCopy.getColorTexture();
      commandEncoder.copyTextureToTexture(colorTexture, colorTexture2, 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);
   }

   private static float visualDensity(float value) {
      float clampedValue = Mth.clamp(value, 0.0F, 1.0F);
      float computedValue = clampedValue * clampedValue * (3.0F - 2.0F * clampedValue);
      return Mth.lerp(computedValue, 0.16F, 0.46F);
   }

   private void renderRainStreaks(Minecraft minecraft, RenderTarget renderTarget, CameraRenderState cameraRenderState, float coordinate, float[] floatValues, float coordinate2) {
      Camera camera = minecraft.gameRenderer.mainCamera();
      float value = this.module.rainStreakSize();
      float value2 = 40.0F;
      float value3 = 28.0F;
      float computedValue = value2 * 2.0F / 12.0F;
      double x = camera.position().x;
      double z = camera.position().z;
      float computedValue2 = (float)(Math.floor(x / computedValue) * computedValue - value2);
      float computedValue3 = (float)(Math.floor(z / computedValue) * computedValue - value2);
      float computedValue4 = coordinate2 * (8.5F + coordinate * 6.0F);
      float value4 = 0.079F;
      float computedValue5 = -0.996F;
      float value5 = 0.03F;
      Quaternionf rotation = camera.rotation();
      float x2 = rotation.x;
      float y = rotation.y;
      float z2 = rotation.z;
      float w = rotation.w;
      float computedValue6 = 1.0F - 2.0F * (y * y + z2 * z2);
      float computedValue7 = 2.0F * (x2 * y + z2 * w);
      float computedValue8 = 2.0F * (x2 * z2 - y * w);
      Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
      Vec3 position = camera.position();
      float computedValue9 = mixF(0.42F, 0.72F, coordinate / 0.46F) * 0.92F;

      try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(1728 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
         BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
         int value6 = 0;

         for (int value7 = 0; value7 < 2; value7++) {
            for (int value8 = 0; value8 < 12; value8++) {
               for (int value9 = 0; value9 < 12; value9++) {
                  float computedValue10 = hash(value9 * 1.618F + value7 * 19.1F, value8 * 2.718F + value7 * 7.3F);
                  if (!(computedValue10 > coordinate)) {
                     float computedValue11 = hash(value9 + 13.7F + value7, value8 + 29.3F);
                     float computedValue12 = hash(value9 + 41.1F + value7, value8 + 3.9F);
                     float computedValue13 = mod(computedValue4 + computedValue10 * value3 + value7 * value3 * 0.5F, value3);
                     float computedValue14 = computedValue2 + (value9 + computedValue11) * computedValue - (float)x;
                     float computedValue15 = value3 * 0.5F - computedValue13;
                     float computedValue16 = computedValue3 + (value8 + computedValue12) * computedValue - (float)z;
                     float computedValue17 = computedValue14 * computedValue14 + computedValue16 * computedValue16;
                     if (!(computedValue17 > value2 * value2)) {
                        float squareRoot = (float)Math.sqrt(computedValue17);
                        float computedValue18 = (1.0F - smooth(value2 * 0.55F, value2, squareRoot)) * (1.0F - smooth(value3 * 0.42F, value3 * 0.5F, Math.abs(computedValue15)));
                        if (!(computedValue18 <= 0.001F)) {
                           float computedValue19 = value * (0.75F + computedValue10 * 0.5F);
                           float computedValue20 = 0.014F * computedValue19;
                           float computedValue21 = 0.42F * computedValue19;
                           float squareRoot2 = (float)Math.sqrt(computedValue17 + computedValue15 * computedValue15);
                           float computedValue22 = squareRoot2 > 1.0E-4F ? -computedValue14 / squareRoot2 : 0.0F;
                           float computedValue23 = squareRoot2 > 1.0E-4F ? -computedValue15 / squareRoot2 : 0.0F;
                           float computedValue24 = squareRoot2 > 1.0E-4F ? -computedValue16 / squareRoot2 : 0.0F;
                           float computedValue25 = computedValue5 * computedValue24 - value5 * computedValue23;
                           float computedValue26 = value5 * computedValue22 - value4 * computedValue24;
                           float computedValue27 = value4 * computedValue23 - computedValue5 * computedValue22;
                           float computedValue28 = computedValue25 * computedValue25 + computedValue26 * computedValue26 + computedValue27 * computedValue27;
                           if (computedValue28 < 1.0E-8F) {
                              computedValue25 = computedValue6;
                              computedValue26 = computedValue7;
                              computedValue27 = computedValue8;
                           } else {
                              float squareRoot3 = 1.0F / (float)Math.sqrt(computedValue28);
                              computedValue25 *= squareRoot3;
                              computedValue26 *= squareRoot3;
                              computedValue27 *= squareRoot3;
                           }

                           float computedValue29 = computedValue9 * computedValue18;
                           if (!(computedValue29 <= 0.002F)) {
                              int computedValue30 = WriteHexByteHelper.pack(
                                 (int)(floatValues[0] * 255.0F), (int)(floatValues[1] * 255.0F), (int)(floatValues[2] * 255.0F), (int)(computedValue29 * 255.0F)
                              );
                              emitQuad(bufferBuilder, position, matrix, computedValue14, computedValue15, computedValue16, computedValue25, computedValue26, computedValue27, value4, computedValue5, value5, computedValue20, computedValue21, computedValue30);
                              if (++value6 >= 288) {
                                 break;
                              }
                           }
                        }
                     }
                  }
               }

               if (value6 >= 288) {
                  break;
               }
            }

            if (value6 >= 288) {
               break;
            }
         }

         if (value6 != 0) {
            try (MeshData meshData = bufferBuilder.build()) {
               if (meshData != null) {
                  ByteBuffer byteBuffer = meshData.vertexBuffer();
                  int value10 = byteBuffer.remaining();
                  this.ensureStreakBuffer(value10);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.streakVertexBuffer.slice(0L, value10), byteBuffer);
                  GpuTextureView depthTextureView = renderTarget.getDepthTextureView();
                  RenderPass device = depthTextureView != null
                     ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                        return "Skycore Atmosphere Rain Streaks";
                     }, renderTarget.getColorTextureView(), Optional.empty(), depthTextureView, OptionalDouble.empty())
                     : RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                        return "Skycore Atmosphere Rain Streaks";
                     }, renderTarget.getColorTextureView(), Optional.empty());

                  try {
                     device.setPipeline(AtmospherePipelines.RAIN_STREAK);
                     device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     device.setVertexBuffer(0, this.streakVertexBuffer.slice(0L, value10));
                     device.draw(meshData.drawState().vertexCount(), 1, 0, 0);
                  } finally {
                     device.close();
                  }
               }
            }
         }
      }
   }

   private static float mixF(float value, float value2, float value3) {
      return value + (value2 - value) * value3;
   }

   private Std140Builder appendSkyReflection(Std140Builder std140Builder) {
      Std140Builder std140Builder2 = std140Builder.putVec4(0.0F, 0.0F, 0.0F, this.reflectedSky != null ? 1.0F : 0.0F);
      return std140Builder2.putVec4(0.0F, 0.0F, 0.0F, 0.0F).putVec4(0.0F, 0.0F, 0.0F, 0.0F);
   }

   private static void emitQuad(
      BufferBuilder bufferBuilder,
      Vec3 position,
      Matrix4f matrix,
      float value,
      float value2,
      float value3,
      float value4,
      float value5,
      float value6,
      float value7,
      float value8,
      float value9,
      float value10,
      float value11,
      int value12
   ) {
      float computedValue = value4 * value10;
      float computedValue2 = value5 * value10;
      float computedValue3 = value6 * value10;
      float computedValue4 = value7 * value11;
      float computedValue5 = value8 * value11;
      float computedValue6 = value9 * value11;
      putVert(bufferBuilder, position, matrix, value - computedValue - computedValue4, value2 - computedValue2 - computedValue5, value3 - computedValue3 - computedValue6, 0.0F, 0.0F, value12);
      putVert(bufferBuilder, position, matrix, value + computedValue - computedValue4, value2 + computedValue2 - computedValue5, value3 + computedValue3 - computedValue6, 1.0F, 0.0F, value12);
      putVert(bufferBuilder, position, matrix, value + computedValue + computedValue4, value2 + computedValue2 + computedValue5, value3 + computedValue3 + computedValue6, 1.0F, 1.0F, value12);
      putVert(bufferBuilder, position, matrix, value - computedValue - computedValue4, value2 - computedValue2 - computedValue5, value3 - computedValue3 - computedValue6, 0.0F, 0.0F, value12);
      putVert(bufferBuilder, position, matrix, value + computedValue + computedValue4, value2 + computedValue2 + computedValue5, value3 + computedValue3 + computedValue6, 1.0F, 1.0F, value12);
      putVert(bufferBuilder, position, matrix, value - computedValue + computedValue4, value2 - computedValue2 + computedValue5, value3 - computedValue3 + computedValue6, 0.0F, 1.0F, value12);
   }

   public void render(CameraRenderState cameraRenderState) {
      if (this.module.needsWorldCapture()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null && instance.level != null && instance.player != null && RenderSystem.tryGetDevice() != null) {
            if (cameraRenderState != null && cameraRenderState.initialized) {
               Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
               if (matrix != null) {
                  RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
                  if (valid(renderTarget)) {
                     this.proj.set(matrix);
                     this.view.set(cameraRenderState.viewRotationMatrix);
                     this.viewProj.set(this.proj).mul(this.view);
                     this.invViewProj.set(this.viewProj).invert();
                     WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
                     this.reflectedSky = null;
                     if (this.module.skyReflections() && worldTweaksModule != null && worldTweaksModule.reflectsSky()) {
                        this.reflectedSky = this.skyEnvironment.render(worldTweaksModule);
                     } else {
                        this.skyEnvironment.close();
                     }

                     boolean needsFogPass = this.module.needsFogPass();
                     boolean needsBlurCapture = this.module.needsBlurCapture();
                     int value = this.module.shouldRenderCustomRain() && !this.rainFailed ? 1 : 0;
                     if (needsFogPass || needsBlurCapture || value != 0 && (this.module.wetReflections() || this.module.rainSplashes())) {
                        this.ensureSceneCopy(renderTarget.width, renderTarget.height);
                        this.copyScene(renderTarget);
                     }

                     if (needsFogPass) {
                        this.renderFog(instance, renderTarget, cameraRenderState, needsBlurCapture);
                     }

                     if (value != 0) {
                        try {
                           this.renderRain(instance, renderTarget, cameraRenderState);
                        } catch (Throwable failure) {
                           this.rainFailed = true;
                           System.err.println("[Atmosphere] rain disabled: " + failure.getMessage());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static float skyAngle(Minecraft minecraft) {
      long gameTime = minecraft.level.getGameTime();
      WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
      if (worldTweaksModule != null) {
         if (worldTweaksModule.changesTime()) {
            gameTime = worldTweaksModule.getCustomTime();
         }
      }

      return (float)(gameTime % 24000L) / 24000.0F;
   }

   public EnsureFogUniformsHelper(AtmosphereModule module2) {
      this.module = module2;
   }
}
