package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class SampleSurfaceHelper {
   private static final int TERRAIN_VERTICES = 29;
   private static final double MAX_DROP = 3.8;
   private final GpuBuffer[] circleUniforms;
   private TextureTarget sceneSnapshot;
   private Object terrainLevel;
   private final Map<SampleSurfaceHelper.Circle, SampleSurfaceHelper.Surface> terrain;
   private static final int UNIFORM_SIZE = 48;
   private static final int MAX_INSTANCES = 8;
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("SceneSampler")
      .withUniform("JumpCircleUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private final UploadHelper meshGpu = new UploadHelper("Skycore JumpCircles Disk Mesh");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/jump_circles"))
      .withVertexShader(Identifier.parse("skycore:core/jump_circles"))
      .withFragmentShader(Identifier.parse("skycore:core/jump_circles"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int TERRAIN_STEPS = 28;

   private SampleSurfaceHelper.Surface sampleSurface(Minecraft minecraft, SampleSurfaceHelper.Circle circle, float index) {
      Vec3 position = circle.position();
      double index2 = index / 0.83;
      Vec3[][] vec3ArrayValues = new Vec3[29][29];

      for (int value = 0; value < 29; value++) {
         double index3 = value * 2.0 / 28.0 - 1.0;

         for (int value2 = 0; value2 < 29; value2++) {
            double computedValue = value2 * 2.0 / 28.0 - 1.0;
            if (!(computedValue * computedValue + index3 * index3 > 1.1)) {
               double x = position.x + computedValue * index2;
               double z = position.z + index3 * index2;
               Vec3 position2 = new Vec3(x, position.y + 0.76, z);
               Vec3 position3 = new Vec3(x, position.y - 3.8, z);
               BlockHitResult blockHitResult = minecraft.level.clip(new ClipContext(position2, position3, Block.COLLIDER, Fluid.NONE, minecraft.player));
               if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getDirection() == Direction.UP) {
                  vec3ArrayValues[value][value2] = new Vec3(x, blockHitResult.getLocation().y + 0.024, z);
               }
            }
         }
      }

      return new SampleSurfaceHelper.Surface(index, vec3ArrayValues);
   }

   public static RenderPipeline pipeline() {
      return PIPELINE;
   }

   public void release() {
      this.terrain.clear();
      this.terrainLevel = null;
      this.meshGpu.close();
      if (this.sceneSnapshot != null) {
         this.sceneSnapshot.destroyBuffers();
         this.sceneSnapshot = null;
      }

      for (int value = 0; value < this.circleUniforms.length; value++) {
         if (this.circleUniforms[value] != null) {
            this.circleUniforms[value].close();
            this.circleUniforms[value] = null;
         }
      }
   }

   public SampleSurfaceHelper() {
      this.circleUniforms = new GpuBuffer[8];
      this.terrain = new IdentityHashMap<>();
   }

   public void render(List<SampleSurfaceHelper.Instance> instances, int coordinate, float coordinate2, long timestamp) {
      if (instances != null && !instances.isEmpty() && RenderSystem.tryGetDevice() != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.gameRenderer != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            if (renderTarget != null && renderTarget.width > 0 && renderTarget.height > 0 && renderTarget.getColorTextureView() != null && renderTarget.getDepthTextureView() != null) {
               int computedValue = Math.min(instances.size(), 8);
               if (this.terrainLevel != instance.level) {
                  this.terrain.clear();
                  this.terrainLevel = instance.level;
               }

               this.terrain.keySet().removeIf(circle -> {
                  return instances.stream().noneMatch(instance2 -> {
                     return instance2.circle() == circle;
                  });
               });
               SampleSurfaceHelper.Surface[] surfaceValues = new SampleSurfaceHelper.Surface[computedValue];

               for (int value = 0; value < computedValue; value++) {
                  SampleSurfaceHelper.Instance instance3 = (SampleSurfaceHelper.Instance)instances.get(value);
                  SampleSurfaceHelper.Surface surface = this.terrain.get(instance3.circle());
                  if (surface == null || Float.compare(surface.radius(), instance3.radius()) != 0) {
                     surface = this.sampleSurface(instance, instance3.circle(), instance3.radius());
                     this.terrain.put(instance3.circle(), surface);
                  }

                  surfaceValues[value] = surface;
               }

               this.ensureSnapshot(renderTarget.width, renderTarget.height);
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

               try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore JumpCircles Scene Capture";
                  }, this.sceneSnapshot.getColorTextureView(), Optional.empty())) {
                  device.setPipeline(MotionBlurPipelines.MOTION_BLUR_BLIT);
                  RenderSystem.bindDefaultUniforms(device);
                  device.bindTexture("CurrentSampler", renderTarget.getColorTextureView(), clampToEdge);
                  device.setVertexBuffer(0, EnsureHelper.buffer().slice());
                  device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
               }

               Camera camera = instance.gameRenderer.mainCamera();
               Vec3 position = camera.position();
               Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
               float alpha = (coordinate >>> 16 & 0xFF) / 255.0F;
               float alpha2 = (coordinate >>> 8 & 0xFF) / 255.0F;
               float alpha3 = (coordinate & 0xFF) / 255.0F;
               float computedValue2 = Math.max(0.0F, Math.min(1.0F, coordinate2)) * 9.0F;

               for (int value2 = 0; value2 < computedValue; value2++) {
                  if (this.circleUniforms[value2] == null) {
                     int value3 = value2;
                     this.circleUniforms[value2] = RenderSystem.getDevice().createBuffer(() -> {
                        return "Skycore JumpCircle UBO " + value3;
                     }, 136, 48L);
                  }

                  SampleSurfaceHelper.Instance instance4 = (SampleSurfaceHelper.Instance)instances.get(value2);

                  try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                     RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToBuffer(
                           this.circleUniforms[value2].slice(),
                           Std140Builder.onStack(memoryStack, 48)
                              .putVec4(instance4.age(), computedValue2, instance4.circle().seed(), (float)(timestamp / 1.0E9 % 4096.0))
                              .putVec4(alpha, alpha2, alpha3, instance4.opacity())
                              .putVec4(renderTarget.width, renderTarget.height, 1.0F / renderTarget.width, 1.0F / renderTarget.height)
                              .get()
                        );
                  }
               }

               int[] intValues = new int[computedValue];
               int[] intValues2 = new int[computedValue];
               int[] intValues3 = new int[]{0};
               int vertexSize = computedValue * 28 * 28 * 6 * DefaultVertexFormat.POSITION_TEX.getVertexSize();

               try (MeshBuffer meshBuffer = MeshBuffer.build(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX, bufferBuilder -> {
                     for (int value4 = 0; value4 < computedValue; value4++) {
                        intValues[value4] = intValues3[0];
                        Vec3[][] vec3ArrayValues = surfaceValues[value4].points();
                        Vector4f[][] vector4fArrayValues = new Vector4f[29][29];

                        for (int value5 = 0; value5 < 29; value5++) {
                           for (int value6 = 0; value6 < 29; value6++) {
                              if (vec3ArrayValues[value5][value6] != null) {
                                 vector4fArrayValues[value5][value6] = CameraViewPoseHelper.toViewSpace(vec3ArrayValues[value5][value6], position, matrix);
                              }
                           }
                        }

                        for (int value7 = 0; value7 < 28; value7++) {
                           float index = value7 * 2.0F / 28.0F - 1.0F;
                           float index2 = (value7 + 1) * 2.0F / 28.0F - 1.0F;

                           for (int value8 = 0; value8 < 28; value8++) {
                              float computedValue3 = value8 * 2.0F / 28.0F - 1.0F;
                              float computedValue4 = (value8 + 1) * 2.0F / 28.0F - 1.0F;
                              Vector4f vector4f = vector4fArrayValues[value7][value8];
                              Vector4f vector4f2 = vector4fArrayValues[value7][value8 + 1];
                              Vector4f vector4f3 = vector4fArrayValues[value7 + 1][value8 + 1];
                              Vector4f vector4f4 = vector4fArrayValues[value7 + 1][value8];
                              if (vector4f != null && vector4f2 != null && vector4f3 != null) {
                                 vertex(bufferBuilder, vector4f, computedValue3, index);
                                 vertex(bufferBuilder, vector4f2, computedValue4, index);
                                 vertex(bufferBuilder, vector4f3, computedValue4, index2);
                                 intValues3[0] += 3;
                              }

                              if (vector4f != null && vector4f3 != null && vector4f4 != null) {
                                 vertex(bufferBuilder, vector4f, computedValue3, index);
                                 vertex(bufferBuilder, vector4f3, computedValue4, index2);
                                 vertex(bufferBuilder, vector4f4, computedValue3, index2);
                                 intValues3[0] += 3;
                              }
                           }
                        }

                        intValues2[value4] = intValues3[0] - intValues[value4];
                     }
                  })) {
                  if (meshBuffer == null) {
                     return;
                  }

                  MeshData meshData = meshBuffer.mesh();
                  int value9 = meshData.vertexBuffer().remaining();
                  GpuBuffer gpuBuffer = this.meshGpu.upload(meshData);

                  try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                        return "Skycore Hand-Drawn Jump Circles";
                     }, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                     device2.setPipeline(PIPELINE);
                     device2.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     device2.bindTexture("SceneSampler", this.sceneSnapshot.getColorTextureView(), clampToEdge);
                     device2.setVertexBuffer(0, gpuBuffer.slice(0L, value9));

                     for (int value10 = 0; value10 < computedValue; value10++) {
                        if (intValues2[value10] != 0) {
                           device2.setUniform("JumpCircleUniforms", this.circleUniforms[value10]);
                           device2.draw(intValues2[value10], 1, intValues[value10], 0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void ensureSnapshot(int width, int height) {
      if (this.sceneSnapshot == null) {
         TextureTarget textureTarget = new TextureTarget("skycore-jump-circles-scene", width, height, false, WorldSkyPipelines.EFFECT_FORMAT);
         this.sceneSnapshot = textureTarget;
      } else {
         if (this.sceneSnapshot.width == width) {
            if (this.sceneSnapshot.height == height) {
               return;
            }
         }

         this.sceneSnapshot.resize(width, height);
      }
   }

   private static void vertex(BufferBuilder bufferBuilder, Vector4f vector4f, float value, float value2) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z);
      vertexConsumer.setUv(value, value2);
   }

   public record Circle(Vec3 position, long startedNanos, float seed) {
      public Vec3 position() {
         return this.position;
      }

      public float seed() {
         return this.seed;
      }

      public long startedNanos() {
         return this.startedNanos;
      }
   }

   public record Instance(SampleSurfaceHelper.Circle circle, float radius, float age, float opacity) {
      public float radius() {
         return this.radius;
      }

      public float opacity() {
         return this.opacity;
      }

      public SampleSurfaceHelper.Circle circle() {
         return this.circle;
      }
   }

   public record Surface(float radius, Vec3[][] points) {
      public float radius() {
         return this.radius;
      }

      public Vec3[][] points() {
         return this.points;
      }
   }
}
