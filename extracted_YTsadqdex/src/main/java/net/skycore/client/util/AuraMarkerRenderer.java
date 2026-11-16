package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class AuraMarkerRenderer {
   private static final BindGroupLayout MARKER_LAYOUT = BindGroupLayout.builder()
      .withSampler("texSampler")
      .withUniform("params", UniformType.UNIFORM_BUFFER)
      .build();
   private final UploadHelper gpu = new UploadHelper("Skycore Aura Marker Vertices");
   private long lastFrameTime;
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/aura_marker"))
      .withVertexShader(Identifier.parse("skycore:core/aura_marker"))
      .withFragmentShader(Identifier.parse("skycore:core/aura_marker"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(AuraMarkerRenderer.MARKER_LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
      .withCull(false)
      .build();
   private GpuBuffer paramsBuffer;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private float alpha;
   private LivingEntity lastTarget;

   public void release() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }

      this.gpu.close();
   }

   private void renderMarker(Minecraft minecraft, AuraMarkerRenderer.MarkerGeometry markerGeometry, int coordinate) {
      RenderTarget renderTarget = minecraft.gameRenderer.mainRenderTarget();
      GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
      if (colorTextureView != null) {
         AbstractTexture texture = minecraft.getTextureManager().getTexture(ClientTextures.TARGET);
         GpuTextureView textureView = texture != null ? texture.getTextureView() : null;
         if (textureView != null) {
            this.ensureParamsBuffer();
            this.writeParams(coordinate);

            try (MeshBuffer vertexSize = MeshBuffer.build(
                  4 * DefaultVertexFormat.POSITION_TEX.getVertexSize(), PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX, bufferBuilder -> {
                     float halfSize = markerGeometry.halfSize;
                     bufferBuilder.addVertex(markerGeometry.pose, -halfSize, -halfSize, 0.0F).setUv(0.0F, 1.0F);
                     bufferBuilder.addVertex(markerGeometry.pose, -halfSize, halfSize, 0.0F).setUv(0.0F, 0.0F);
                     float computedValue = -halfSize;
                     bufferBuilder.addVertex(markerGeometry.pose, halfSize, computedValue, 0.0F).setUv(1.0F, 1.0F);
                     bufferBuilder.addVertex(markerGeometry.pose, halfSize, halfSize, 0.0F).setUv(1.0F, 0.0F);
                  }
               )) {
               if (vertexSize == null) {
                  return;
               }

               MeshData meshData = vertexSize.mesh();
               int value = meshData.vertexBuffer().remaining();
               GpuBuffer gpuBuffer = this.gpu.upload(meshData);
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

               try (RenderPass device = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(() -> "Skycore Aura Marker Pass", colorTextureView, Optional.empty())) {
                  device.setPipeline(PIPELINE);
                  device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  device.setUniform("params", this.paramsBuffer);
                  device.bindTexture("texSampler", textureView, clampToEdge);
                  device.setVertexBuffer(0, gpuBuffer.slice(0L, value));
                  device.draw(4, 1, 0, 0);
               }
            }
         }
      }
   }

   private void ensureParamsBuffer() {
      if (this.paramsBuffer == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> {
            return "Skycore Aura Marker UBO";
         };
         long uNIFORM_SIZE = UNIFORM_SIZE;
         GpuBuffer gpuBuffer = device.createBuffer(supplier, 136, uNIFORM_SIZE);
         this.paramsBuffer = gpuBuffer;
      }
   }

   public void render(LivingEntity entity, float coordinate, int alpha2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && instance.player != null && instance.level != null && instance.gameRenderer != null) {
         this.updateAnimation(entity);
         LivingEntity lastTarget2 = this.lastTarget;
         if (lastTarget2 != null) {
            if (this.alpha <= 0.01F) {
               this.reset();
            } else {
               AuraMarkerRenderer.MarkerGeometry markerGeometry = this.markerGeometry(instance, lastTarget2, coordinate);
               if (markerGeometry != null) {
                  this.renderMarker(instance, markerGeometry, EasedFactorHelper.blend(alpha2, lastTarget2, this.alpha));
               }
            }
         }
      } else {
         this.reset();
      }
   }

   private AuraMarkerRenderer.MarkerGeometry markerGeometry(Minecraft minecraft, LivingEntity entity, float value) {
      Camera camera = minecraft.gameRenderer.mainCamera();
      if (camera != null && camera.isInitialized()) {
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(entity, value);
         float value2;
         if (entity.getBbWidth() < 1.0F) {
            value2 = 0.95F;
         } else if (entity.getBbWidth() > 2.0F) {
            value2 = 1.45F;
         } else {
            value2 = 1.0F;
         }

         float computedValue = 0.5F * value2 * this.alpha * EasedFactorHelper.scale(entity, 0.12F);
         if (computedValue <= 0.001F) {
            return null;
         }

         Matrix4f bbHeight = CameraViewPoseHelper.buildBillboardPose(
            camera, position, entity.getBbHeight() / 2.0, (float)(Math.sin(System.currentTimeMillis() / 1000.0) * 360.0)
         );
         return new AuraMarkerRenderer.MarkerGeometry(bbHeight, computedValue);
      } else {
         return null;
      }
   }

   private void updateAnimation(LivingEntity lastTarget2) {
      long now = System.currentTimeMillis();
      float value;
      if (this.lastFrameTime == 0L) {
         value = 0.016F;
      } else {
         long timestamp = now - this.lastFrameTime;
         value = (float)Math.min(100L, timestamp) / 1000.0F;
      }

      float value2 = value;
      this.lastFrameTime = now;
      if (valid(lastTarget2)) {
         this.lastTarget = lastTarget2;
      }

      float alpha2 = valid(lastTarget2) ? 1.0F : 0.0F;
      float computedValue = value2 * 2.5F;
      float clampedValue = Mth.clamp(computedValue, 0.0F, 1.0F);
      float alpha3 = this.alpha + (alpha2 - this.alpha) * clampedValue;
      this.alpha = alpha3;
   }

   public static RenderPipeline pipeline() {
      return PIPELINE;
   }

   private static boolean valid(LivingEntity entity) {
      if (entity != null) {
         if (entity.isAlive()) {
            if (!entity.isRemoved()) {
               return true;
            }
         }
      }

      return false;
   }

   public void reset() {
      this.lastTarget = null;
      this.alpha = 0.0F;
      this.lastFrameTime = 0L;
   }

   private void writeParams(int alpha2) {

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         Std140Builder std140Builder = Std140Builder.onStack(memoryStack, UNIFORM_SIZE);
         float computedValue = WriteHexByteHelper.red(alpha2) / 255.0F;
         float intermediate = WriteHexByteHelper.green(alpha2) / 255.0F;
         float computedValue2 = WriteHexByteHelper.blue(alpha2) / 255.0F;
         float computedValue3 = WriteHexByteHelper.alpha(alpha2) / 255.0F;
         ByteBuffer byteBuffer = std140Builder.putVec4(computedValue, intermediate, computedValue2, computedValue3).get();
         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder commandEncoder = device.createCommandEncoder();
         GpuBufferSlice gpuBufferSlice = this.paramsBuffer.slice();
         commandEncoder.writeToBuffer(gpuBufferSlice, byteBuffer);
      }
   }

   public record MarkerGeometry(Matrix4f pose, float halfSize) {
      public Matrix4f pose() {
         return this.pose;
      }

      public float halfSize() {
         return this.halfSize;
      }
   }
}
