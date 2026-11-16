package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class TargetParticleRenderer {
   private GpuBuffer uniformBuffer;
   private final UploadHelper gpu = new UploadHelper("Skycore Target Particles Vertices");
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("BloomSampler")
      .withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline THROUGH_WALLS_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_particle_through_walls"))
      .withVertexShader(Identifier.parse("skycore:core/world_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(TargetParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_particle"))
      .withVertexShader(Identifier.parse("skycore:core/world_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(TargetParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final RenderPipeline ALPHA_THROUGH_WALLS_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_particle_alpha_through_walls"))
      .withVertexShader(Identifier.parse("skycore:core/world_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(TargetParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline ALPHA_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_particle_alpha"))
      .withVertexShader(Identifier.parse("skycore:core/world_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();

   public void render(List<TargetParticleRenderer.Sprite> sprites, float coordinate) {
      this.render(sprites, coordinate, false, true);
   }

   private void addVertex(BufferBuilder bufferBuilder, Vector4f vector4f, float value, float value2, float value3, float value4, int color) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(vector4f.x + value, vector4f.y + value2, vector4f.z);
      vertexConsumer.setUv(value3, value4).setColor(color);
   }

   private void ensureUniform(float value) {
      if (this.uniformBuffer == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> {
            return "Skycore Target Particles UBO";
         };
         this.uniformBuffer = device.createBuffer(supplier, 136, UNIFORM_SIZE);
      }

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         Std140Builder std140Builder = Std140Builder.onStack(memoryStack, UNIFORM_SIZE).putVec4(value, 0.0F, 0.0F, 0.0F);
         ByteBuffer byteBuffer = std140Builder.get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniformBuffer.slice(), byteBuffer);
      }
   }

   public static RenderPipeline[] pipelines() {
      RenderPipeline[] renderPipelineValues = new RenderPipeline[]{PIPELINE, THROUGH_WALLS_PIPELINE, ALPHA_PIPELINE, null};
      renderPipelineValues[3] = ALPHA_THROUGH_WALLS_PIPELINE;
      return renderPipelineValues;
   }

   public void render(List<TargetParticleRenderer.Sprite> sprites, float coordinate, boolean condition, boolean condition2) {
      Minecraft instance = Minecraft.getInstance();
      if (!sprites.isEmpty() && instance.level != null) {
         RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
         GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
         if (colorTextureView != null) {
            AbstractTexture texture = instance.getTextureManager().getTexture(ClientTextures.BLOOM);
            GpuTextureView textureView = texture != null ? texture.getTextureView() : null;
            if (textureView != null) {
               try (MeshBuffer vertexSize = MeshBuffer.build(
                     sprites.size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize(),
                     PrimitiveTopology.TRIANGLES,
                     DefaultVertexFormat.POSITION_TEX_COLOR,
                     bufferBuilder -> {
                        Camera camera = instance.gameRenderer.mainCamera();
                        Vec3 position = camera.position();
                        Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);

                        for (TargetParticleRenderer.Sprite sprite : sprites) {
                           Vector4f vector4f = CameraViewPoseHelper.toViewSpace(sprite.position(), position, matrix);
                           float value = sprite.halfSize();
                           int value2 = sprite.color();
                           this.addVertex(bufferBuilder, vector4f, -value, -value, 0.0F, 0.0F, value2);
                           this.addVertex(bufferBuilder, vector4f, -value, value, 0.0F, 1.0F, value2);
                           this.addVertex(bufferBuilder, vector4f, value, value, 1.0F, 1.0F, value2);
                           this.addVertex(bufferBuilder, vector4f, -value, -value, 0.0F, 0.0F, value2);
                           this.addVertex(bufferBuilder, vector4f, value, value, 1.0F, 1.0F, value2);
                           this.addVertex(bufferBuilder, vector4f, value, -value, 1.0F, 0.0F, value2);
                        }
                     }
                  )) {
                  if (vertexSize == null) {
                     return;
                  }

                  MeshData meshData = vertexSize.mesh();
                  int value3 = meshData.vertexBuffer().remaining();
                  GpuBuffer gpuBuffer = this.gpu.upload(meshData);
                  this.ensureUniform(coordinate);
                  GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                  GpuTextureView depthTextureView = renderTarget.getDepthTextureView();
                  RenderPass device = !condition && depthTextureView != null ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore Target Particles Pass";
                  }, colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty()) : RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore Target Particles Pass";
                  }, colorTextureView, Optional.empty());

                  try {
                     RenderPipeline renderPipeline = condition2 ? (condition ? THROUGH_WALLS_PIPELINE : PIPELINE) : (condition ? ALPHA_THROUGH_WALLS_PIPELINE : ALPHA_PIPELINE);
                     device.setPipeline(renderPipeline);
                     device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     device.setUniform("WorldParticleUniforms", this.uniformBuffer);
                     device.bindTexture("BloomSampler", textureView, clampToEdge);
                     device.setVertexBuffer(0, gpuBuffer.slice(0L, value3));
                     device.draw(sprites.size() * 6, 1, 0, 0);
                  } finally {
                     device.close();
                  }
               }
            }
         }
      }
   }

   public void render(List<TargetParticleRenderer.Sprite> sprites, float coordinate, boolean condition) {
      this.render(sprites, coordinate, condition, true);
   }

   public record Sprite(Vec3 position, float halfSize, int color) {
      public Vec3 position() {
         return this.position;
      }

      public float halfSize() {
         return this.halfSize;
      }

      public int color() {
         return this.color;
      }
   }
}
