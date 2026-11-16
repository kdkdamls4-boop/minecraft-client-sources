package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class RenderSkyHelper implements AutoCloseable {
   private GpuBuffer sphere;
   private final GpuBuffer uniforms = RenderSystem.getDevice().createBuffer(() -> {
      return "Skycore source overlay uniforms";
   }, 136, 144L);
   private static final BlendFunction SOURCE_BLEND = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
   private static final BindGroupLayout SKY_TEXTURE = BindGroupLayout.builder()
      .withUniform("SourceOverlayUniforms", UniformType.UNIFORM_BUFFER)
      .withSampler("DepthSampler")
      .build();
   public static final RenderPipeline BLOCK = pipeline("source_block_overlay", "world/source_overlay", "world/block_overlay", SKY_TEXTURE);
   public static final RenderPipeline PLASMA = pipeline("source_sky_plasma", "world/source_overlay", "world/sky/source_plasma", SKY_TEXTURE);
   public static final RenderPipeline SUMMER = pipeline("source_sky_summer", "world/source_overlay", "world/sky/source_summer", SKY_TEXTURE);
   private GpuBuffer cube;

   public void renderSky(CameraRenderState cameraRenderState, RenderSkyHelper.Settings settings, boolean condition) {
      RenderTarget instance = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      Matrix4f matrix = viewProjection(instance, cameraRenderState);
      if (matrix != null) {
         if (this.sphere == null) {
            this.sphere = createSphere();
         }

         this.writeUniforms(matrix, settings, instance.width, instance.height);

         try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
               return "Skycore Ambience sky";
            }, instance.getColorTextureView(), Optional.empty())) {
            device.setPipeline(condition ? SUMMER : PLASMA);
            device.setUniform("SourceOverlayUniforms", this.uniforms);
            device.bindTexture("DepthSampler", instance.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            device.setVertexBuffer(0, this.sphere.slice());
            device.draw(6144, 1, 0, 0);
         }
      }
   }

   public void renderBlock(AABB bounds, CameraRenderState cameraRenderState, int coordinate, float coordinate2) {
      RenderTarget instance = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      Matrix4f matrix = viewProjection(instance, cameraRenderState);
      if (matrix != null) {
         bounds = bounds.inflate(0.001);
         matrix.translate((float)(bounds.minX - cameraRenderState.pos.x), (float)(bounds.minY - cameraRenderState.pos.y), (float)(bounds.minZ - cameraRenderState.pos.z))
            .scale((float)bounds.getXsize(), (float)bounds.getYsize(), (float)bounds.getZsize());
         this.writeUniforms(matrix, RenderSkyHelper.Settings.block(coordinate, Math.max(0.01F, coordinate2)), instance.width, instance.height);
         if (this.cube == null) {
            this.cube = createCube();
         }

         try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
               return "Skycore block overlay plasma";
            }, instance.getColorTextureView(), Optional.empty())) {
            device.setPipeline(BLOCK);
            device.setUniform("SourceOverlayUniforms", this.uniforms);
            device.bindTexture("DepthSampler", instance.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            device.setVertexBuffer(0, this.cube.slice());
            device.draw(36, 1, 0, 0);
         }
      }
   }

   private static GpuBuffer createSphere() {
      try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(6144 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
         BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

         for (int value = 0; value < 32; value++) {
            for (int value2 = 0; value2 < 32; value2++) {
               sphereVertex(bufferBuilder, value, value2);
               sphereVertex(bufferBuilder, value + 1, value2);
               sphereVertex(bufferBuilder, value + 1, value2 + 1);
               sphereVertex(bufferBuilder, value, value2);
               sphereVertex(bufferBuilder, value + 1, value2 + 1);
               sphereVertex(bufferBuilder, value, value2 + 1);
            }
         }

         try (MeshData meshData = bufferBuilder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> {
               return "Skycore Ambience sphere";
            }, 32, meshData.vertexBuffer());
         }
      }
   }

   private void writeUniforms(Matrix4f matrix, RenderSkyHelper.Settings settings, int value, int value2) {
      int value3 = settings.primary();
      int color = value3 >> 16 & 0xFF;
      int color2 = value3 >> 8 & 0xFF;
      int color3 = value3 & 0xFF;
      float[] floatValues = Color.RGBtoHSB(color, color2, color3, null);
      int computedValue = Color.HSBtoRGB(floatValues[0], floatValues[1], Math.clamp(floatValues[2] * 0.65F, 0.0F, 1.0F));

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, 144)
            .putMat4f(matrix)
            .putVec4(color / 255.0F, color2 / 255.0F, color3 / 255.0F, 1.0F)
            .putVec4((computedValue >> 16 & 0xFF) / 255.0F, (computedValue >> 8 & 0xFF) / 255.0F, (computedValue & 0xFF) / 255.0F, 1.0F)
            .putVec4((float)(System.currentTimeMillis() % 100000L) / 1000.0F, settings.speed(), settings.scale(), settings.outline())
            .putVec4(settings.glow(), settings.fill(), settings.alpha(), settings.night() ? 1.0F : 0.0F)
            .putVec4(1.0F / value, 1.0F / value2, 0.0F, 0.0F)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
      }
   }

   private static GpuBuffer createCube() {
      int[][] intArrayValues = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
      float[][] floatArrayValues = new float[][]{{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};

      try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(36 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
         BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

         for (int[] intValues : intArrayValues) {
            for (int index : new int[]{0, 2, 1, 0, 3, 2}) {
               int value = intValues[index];
               bufferBuilder.addVertex(value & 1, value >> 1 & 1, value >> 2 & 1).setUv(floatArrayValues[index][0], floatArrayValues[index][1]);
            }
         }

         try (MeshData meshData = bufferBuilder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> {
               return "Skycore block overlay cube";
            }, 32, meshData.vertexBuffer());
         }
      }
   }

   private static RenderPipeline pipeline(String text, String text2, String text3, BindGroupLayout bindGroupLayout) {
      Builder builder = RenderPipeline.builder().withLocation(Identifier.parse("skycore:pipeline/" + text));
      Identifier texture = Identifier.parse("skycore:" + text2);
      Builder builder2 = builder.withVertexShader(texture);
      String text4 = "skycore:" + text3;
      Identifier texture2 = Identifier.parse(text4);
      Builder builder3 = builder2.withFragmentShader(texture2).withBindGroupLayout(bindGroupLayout);
      ColorTargetState colorTargetState = new ColorTargetState(SOURCE_BLEND);
      Builder builder4 = builder3.withColorTargetState(colorTargetState);
      Optional<com.mojang.blaze3d.pipeline.DepthStencilState> optional = Optional.empty();
      Builder builder5 = builder4.withDepthStencilState(optional).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
      Builder builder6 = builder5.withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
      boolean condition = text.equals("source_block_overlay");
      Builder builder7 = builder6.withCull(condition);
      return builder7.build();
   }

   private static void sphereVertex(BufferBuilder bufferBuilder, int value, int value2) {
      float computedValue = (float) Math.PI * value / 32.0F;
      float computedValue2 = (float) (Math.PI * 2) * value2 / 32.0F;
      float sine = (float)Math.sin(computedValue);
      float computedValue3 = 100.0F * sine;
      double intermediate = computedValue2;
      float cosine = computedValue3 * (float)Math.cos(intermediate);
      double intermediate2 = computedValue;
      float cosine2 = 100.0F * (float)Math.cos(intermediate2);
      float computedValue4 = 100.0F * sine;
      double value3 = computedValue2;
      float sine2 = computedValue4 * (float)Math.sin(value3);
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(cosine, cosine2, sine2);
      float computedValue5 = value2 / 32.0F;
      float computedValue6 = value / 32.0F;
      vertexConsumer.setUv(computedValue5, computedValue6);
   }

   @Override
   public void close() {
      if (this.sphere != null) {
         this.sphere.close();
         this.sphere = null;
      }

      if (this.cube != null) {
         this.cube.close();
         this.cube = null;
      }

      this.uniforms.close();
   }

   private static Matrix4f viewProjection(RenderTarget renderTarget, CameraRenderState cameraRenderState) {
      if (renderTarget != null) {
         if (renderTarget.width > 0 && renderTarget.height > 0) {
            if (renderTarget.getColorTextureView() != null && renderTarget.getDepthTextureView() != null && cameraRenderState != null) {
               if (cameraRenderState.initialized) {
                  Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
                  Matrix4f matrix2;
                  if (matrix == null) {
                     matrix2 = null;
                  } else {
                     matrix2 = matrix.mul(cameraRenderState.viewRotationMatrix);
                  }

                  return matrix2;
               }
            }
         }
      }

      return null;
   }

   public record Settings(int primary, float speed, float scale, float outline, float glow, float fill, float alpha, boolean night) {
      public float alpha() {
         return this.alpha;
      }

      public float scale() {
         return this.scale;
      }

      public float glow() {
         return this.glow;
      }

      public boolean night() {
         return this.night;
      }

      public static RenderSkyHelper.Settings block(int value, float value2) {
         return new RenderSkyHelper.Settings(value, 1.2F, value2, 0.1F, 10.0F, 0.6F, 2.0F, false);
      }

      public float outline() {
         return this.outline;
      }

      public float fill() {
         return this.fill;
      }

      public float speed() {
         return this.speed;
      }

      public int primary() {
         return this.primary;
      }
   }
}
