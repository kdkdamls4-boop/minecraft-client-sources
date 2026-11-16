package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class BrushstrokeTrailRenderer {
   private final UploadHelper gpu = new UploadHelper("Skycore Anime Brushstroke Trails");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/anime_trails"))
      .withVertexShader(Identifier.parse("skycore:core/anime_trails"))
      .withFragmentShader(Identifier.parse("skycore:core/anime_trails"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int SHEETS = 2;
   private static final int SEGMENTS = 10;
   private static final int MAX_VISIBLE = 28;

   public void render(List<BrushstrokeTrailRenderer.Stroke> strokes, float coordinate, long timestamp) {
      if (!strokes.isEmpty() && RenderSystem.tryGetDevice() != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.gameRenderer != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            if (renderTarget != null && renderTarget.getColorTextureView() != null) {
               Camera camera = instance.gameRenderer.mainCamera();
               Vec3 position = camera.position();
               Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
               int computedValue = Math.max(0, strokes.size() - 28);
               int computedValue2 = strokes.size() - computedValue;
               int computedValue3 = computedValue2 * 2 * 10 * 6;
               int vertexSize = computedValue3 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();

               try (MeshBuffer meshBuffer = MeshBuffer.build(
                     vertexSize,
                     PrimitiveTopology.TRIANGLES,
                     DefaultVertexFormat.POSITION_TEX_COLOR,
                     bufferBuilder -> {
                        for (int index = computedValue; index < strokes.size(); index++) {
                           BrushstrokeTrailRenderer.Stroke stroke = (BrushstrokeTrailRenderer.Stroke)strokes.get(index);
                           float computedValue4 = (float)((timestamp - stroke.bornNanos()) / (stroke.lifetimeNanos() / Math.max(0.1, coordinate)));
                           if (!(computedValue4 < 0.0F) && !(computedValue4 >= 1.0F)) {
                              float minimum = Math.min(1.0F, computedValue4 / 0.26F);
                              float computedValue5 = 1.0F - (1.0F - minimum) * (1.0F - minimum) * (1.0F - minimum);
                              float minimum2 = Math.min(1.0F, computedValue4 / 0.065F);
                              float computedValue6 = Math.min(1.0F, (1.0F - computedValue4) / 0.55F);
                              float computedValue7 = smooth(minimum2) * smooth(computedValue6) * (0.64F + stroke.motion() * 0.36F);
                              if (!(computedValue7 <= 0.003F)) {
                                 int index2 = Math.min(255, Math.max(0, Math.round(computedValue7 * 243.0F)));
                                 int index3 = stroke.blackOnly() ? 0 : 16777215;
                                 int index4 = index2 << 24 | index3;
                                 float index5 = stroke.length() * computedValue5;

                                 for (int value = 0; value < 2; value++) {
                                    Vector4f[] vector4fValues = new Vector4f[11];
                                    Vector4f[] vector4fValues2 = new Vector4f[11];
                                    Vec3 position2 = value == 0 ? stroke.right().scale(0.94).add(0.0, 0.34, 0.0) : stroke.right().scale(-0.52).add(0.0, 0.84, 0.0);
                                    position2 = position2.normalize().scale(value == 0 ? 1.0 : 0.67);

                                    for (int value2 = 0; value2 <= 10; value2++) {
                                       float computedValue8 = value2 / 10.0F;
                                       double computedValue9 = Math.sin(computedValue8 * Math.PI) * (0.11 + 0.08 * Math.sin(stroke.seed())) * Math.sin(computedValue8 * 3.3 + stroke.seed());
                                       double computedValue10 = (0.065 + 0.1 * Math.sin(stroke.seed() * 2.7)) * computedValue8 * computedValue8;
                                       Vec3 position3 = stroke.origin()
                                          .add(stroke.backward().scale(0.1 + computedValue8 * index5))
                                          .add(stroke.right().scale(computedValue9))
                                          .add(0.0, computedValue10, 0.0);
                                       double width = Math.pow(Math.sin(Math.PI * computedValue8), 0.78);
                                       width *= 1.0 - 0.23 * computedValue8;
                                       width *= 0.94 + 0.06 * Math.sin(stroke.seed() * 1.7 + computedValue8 * 9.0);
                                       double computedValue11 = Math.max(5.0E-5, stroke.width() * width);
                                       vector4fValues[value2] = CameraViewPoseHelper.toViewSpace(position3.add(position2.scale(computedValue11)), position, matrix);
                                       vector4fValues2[value2] = CameraViewPoseHelper.toViewSpace(position3.add(position2.scale(-computedValue11)), position, matrix);
                                    }

                                    for (int value3 = 0; value3 < 10; value3++) {
                                       float computedValue12 = value3 / 10.0F;
                                       float computedValue13 = (value3 + 1) / 10.0F;
                                       vertex(bufferBuilder, vector4fValues[value3], computedValue12, 0.0F, index4);
                                       vertex(bufferBuilder, vector4fValues2[value3], computedValue12, 1.0F, index4);
                                       vertex(bufferBuilder, vector4fValues2[value3 + 1], computedValue13, 1.0F, index4);
                                       vertex(bufferBuilder, vector4fValues[value3], computedValue12, 0.0F, index4);
                                       vertex(bufferBuilder, vector4fValues2[value3 + 1], computedValue13, 1.0F, index4);
                                       vertex(bufferBuilder, vector4fValues[value3 + 1], computedValue13, 0.0F, index4);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  )) {
                  if (meshBuffer == null) {
                     return;
                  }

                  MeshData meshData = meshBuffer.mesh();
                  int value4 = meshData.vertexBuffer().remaining();
                  GpuBuffer gpuBuffer = this.gpu.upload(meshData);
                  CommandEncoder device = RenderSystem.getDevice().createCommandEncoder();
                  RenderPass depthTextureView = renderTarget.getDepthTextureView() != null ? device.createRenderPass(() -> {
                     return "Skycore Cartoon Trails";
                  }, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty()) : device.createRenderPass(() -> {
                     return "Skycore Cartoon Trails";
                  }, renderTarget.getColorTextureView(), Optional.empty());

                  try (depthTextureView) {
                     depthTextureView.setPipeline(PIPELINE);
                     depthTextureView.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     depthTextureView.setVertexBuffer(0, gpuBuffer.slice(0L, value4));
                     depthTextureView.draw(meshData.drawState().vertexCount(), 1, 0, 0);
                  }
               }
            }
         }
      }
   }

   public void release() {
      this.gpu.close();
   }

   private static float smooth(float value) {
      float minimum = Math.min(1.0F, value);
      value = Math.max(0.0F, minimum);
      return value * value * (3.0F - 2.0F * value);
   }

   private static void vertex(BufferBuilder bufferBuilder, Vector4f vector4f, float value, float value2, int color) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z);
      vertexConsumer.setUv(value, value2).setColor(color);
   }

   public static RenderPipeline pipeline() {
      return PIPELINE;
   }

   public record Stroke(
      Vec3 origin,
      Vec3 backward,
      Vec3 right,
      float length,
      float width,
      long lifetimeNanos,
      long bornNanos,
      float seed,
      float motion,
      boolean blackOnly,
      long sequence
   ) {
      public Vec3 right() {
         return this.right;
      }

      public Vec3 backward() {
         return this.backward;
      }

      public float length() {
         return this.length;
      }

      public Vec3 origin() {
         return this.origin;
      }

      public long sequence() {
         return this.sequence;
      }

      public float seed() {
         return this.seed;
      }

      public boolean blackOnly() {
         return this.blackOnly;
      }

      public long bornNanos() {
         return this.bornNanos;
      }

      public long lifetimeNanos() {
         return this.lifetimeNanos;
      }
   }
}
