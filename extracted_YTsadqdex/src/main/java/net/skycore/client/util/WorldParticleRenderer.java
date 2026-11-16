package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class WorldParticleRenderer {
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder().withSampler("ParticleSampler").build();
   private static final RenderPipeline HEX_TOTEM = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/hex_totem_particle"))
      .withVertexShader(Identifier.parse("skycore:core/world_tweaks_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/hex_totem_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(WorldParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private final UploadHelper gpu = new UploadHelper("Skycore WorldTweaks Particles");
   private static final RenderPipeline ADDITIVE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_tweaks_particle_additive"))
      .withVertexShader(Identifier.parse("skycore:core/world_tweaks_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_tweaks_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(WorldParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline TRANSLUCENT = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/world_tweaks_particle"))
      .withVertexShader(Identifier.parse("skycore:core/world_tweaks_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_tweaks_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(WorldParticleRenderer.LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();

   private void render(List<WorldParticleRenderer.Sprite> sprites, RenderPipeline renderPipeline) {
      if (!sprites.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
            if (colorTextureView != null) {
               HashMap<Identifier, List<WorldParticleRenderer.Sprite>> lookup = new HashMap<>();

               for (WorldParticleRenderer.Sprite sprite : sprites) {
                  lookup.computeIfAbsent(sprite.texture(), texture -> {
                     return new ArrayList<>();
                  }).add(sprite);
               }

               Camera camera = instance.gameRenderer.mainCamera();
               Vec3 position = camera.position();
               Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
               GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
               GpuTextureView depthTextureView = renderTarget.getDepthTextureView();

               for (Entry<Identifier, List<WorldParticleRenderer.Sprite>> entry : lookup.entrySet()) {
                  AbstractTexture textureManager = instance.getTextureManager().getTexture((Identifier)entry.getKey());
                  GpuTextureView textureView = textureManager != null ? textureManager.getTextureView() : null;
                  if (textureView != null) {
                     try (MeshBuffer value = MeshBuffer.build(
                           (entry.getValue()).size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize(),
                           PrimitiveTopology.TRIANGLES,
                           DefaultVertexFormat.POSITION_TEX_COLOR,
                           bufferBuilder -> {
                              for (WorldParticleRenderer.Sprite sprite2 : entry.getValue()) {
                                 Vector4f vector4f = CameraViewPoseHelper.toViewSpace(sprite2.position(), position, matrix);
                                 float color = sprite2.halfSize();
                                 float computedValue = sprite2.rotationDegrees() * (float) (Math.PI / 180.0);
                                 float cosine = Mth.cos(computedValue);
                                 float sine = Mth.sin(computedValue);
                                 this.addVertex(bufferBuilder, vector4f, -color, -color, cosine, sine, 0.0F, 1.0F, sprite2.color());
                                 this.addVertex(bufferBuilder, vector4f, color, -color, cosine, sine, 1.0F, 1.0F, sprite2.color());
                                 this.addVertex(bufferBuilder, vector4f, color, color, cosine, sine, 1.0F, 0.0F, sprite2.color());
                                 this.addVertex(bufferBuilder, vector4f, -color, -color, cosine, sine, 0.0F, 1.0F, sprite2.color());
                                 this.addVertex(bufferBuilder, vector4f, color, color, cosine, sine, 1.0F, 0.0F, sprite2.color());
                                 this.addVertex(bufferBuilder, vector4f, -color, color, cosine, sine, 0.0F, 0.0F, sprite2.color());
                              }
                           }
                        )) {
                        if (value != null) {
                           MeshData meshData = value.mesh();
                           int value2 = meshData.vertexBuffer().remaining();
                           GpuBuffer gpuBuffer = this.gpu.upload(meshData);
                           RenderPass device = depthTextureView != null ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                              return "Skycore WorldTweaks Particles Pass";
                           }, colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty()) : RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                              return "Skycore WorldTweaks Particles Pass";
                           }, colorTextureView, Optional.empty());

                           try {
                              device.setPipeline(renderPipeline);
                              device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                              device.bindTexture("ParticleSampler", textureView, clampToEdge);
                              device.setVertexBuffer(0, gpuBuffer.slice(0L, value2));
                              device.draw((entry.getValue()).size() * 6, 1, 0, 0);
                           } finally {
                              device.close();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void render(List<WorldParticleRenderer.Sprite> sprites, boolean condition) {
      RenderPipeline renderPipeline;
      if (condition) {
         renderPipeline = ADDITIVE;
      } else {
         renderPipeline = TRANSLUCENT;
      }

      this.render(sprites, renderPipeline);
   }

   private void addVertex(BufferBuilder bufferBuilder, Vector4f vector4f, float value, float value2, float value3, float value4, float value5, float value6, int color) {
      float computedValue = value * value3 - value2 * value4;
      float computedValue2 = value * value4 + value2 * value3;
      float x = vector4f.x + computedValue;
      float y = vector4f.y + computedValue2;
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(x, y, vector4f.z);
      VertexConsumer vertexConsumer2 = vertexConsumer.setUv(value5, value6);
      vertexConsumer2.setColor(color);
   }

   public static RenderPipeline[] pipelines() {
      RenderPipeline[] renderPipelineValues = new RenderPipeline[3];
      renderPipelineValues[0] = ADDITIVE;
      renderPipelineValues[1] = TRANSLUCENT;
      renderPipelineValues[2] = HEX_TOTEM;
      return renderPipelineValues;
   }

   public void renderHexTotem(List<WorldParticleRenderer.Sprite> sprites) {
      this.render(sprites, HEX_TOTEM);
   }

   public record Sprite(Identifier texture, Vec3 position, float halfSize, float rotationDegrees, int color) {
      public int color() {
         return this.color;
      }

      public float rotationDegrees() {
         return this.rotationDegrees;
      }

      public float halfSize() {
         return this.halfSize;
      }

      public Vec3 position() {
         return this.position;
      }

      public Identifier texture() {
         return this.texture;
      }
   }
}
