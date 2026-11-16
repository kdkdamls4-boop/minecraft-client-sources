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
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
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
import org.lwjgl.system.MemoryStack;

public final class ThunderPipelines {
   private static final BindGroupLayout THUNDER_UNIFORMS = BindGroupLayout.builder().withUniform("ThunderUniforms", UniformType.UNIFORM_BUFFER).build();
   private static final RenderPipeline LIGHTNING = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/thunder_shatter_lightning"))
      .withVertexShader(Identifier.parse("skycore:core/thunder_shatter"))
      .withFragmentShader(Identifier.parse("skycore:core/thunder_shatter_lightning"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(ThunderPipelines.THUNDER_UNIFORMS)
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private final UploadHelper prismGpu;
   private static final int UNIFORM_SIZE = 16;
   private static final RenderPipeline PRISM = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/thunder_shatter_prism"))
      .withVertexShader(Identifier.parse("skycore:core/thunder_shatter"))
      .withFragmentShader(Identifier.parse("skycore:core/thunder_shatter_prism"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(THUNDER_UNIFORMS)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private GpuBuffer uniformBuffer;
   private final List<ThunderPipelines.Bolt> bolts;
   private final UploadHelper lightningGpu = new UploadHelper("Skycore Thunder Filaments");
   private final List<ThunderPipelines.Glass> glass;

   public void bolt(Vec3 position, Vec3 position2, double value, double value2, float value3, int value4) {
      if (position != null && position2 != null && position.distanceToSqr(position2) > 1.0E-7 && value3 > 0.005F) {
         if (this.bolts.size() < 350) {
            this.bolts.add(new ThunderPipelines.Bolt(position, position2, (float)value, (float)value2, value3, value4));
         }
      }
   }

   public static List<RenderPipeline> pipelines() {
      return List.of(LIGHTNING, PRISM);
   }

   private void renderBolts(RenderTarget renderTarget, Vec3 position, Matrix4f matrix, double coordinate) {
      int value = 0;
      if (!this.bolts.isEmpty()) {
         value = this.bolts.size() * 7 * 6;
         value *= DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
         PrimitiveTopology tRIANGLES = PrimitiveTopology.TRIANGLES;
         VertexFormat pOSITION_TEX_COLOR = DefaultVertexFormat.POSITION_TEX_COLOR;
         MeshBuffer.Writer writer = bufferBuilder -> {
            for (ThunderPipelines.Bolt bolt : this.bolts) {
               Vector4f vector4f = CameraViewPoseHelper.toViewSpace(bolt.from, position, matrix);
               Vector4f vector4f2 = CameraViewPoseHelper.toViewSpace(bolt.to, position, matrix);
               float x = vector4f2.x - vector4f.x;
               float y = vector4f2.y - vector4f.y;
               float computedValue = (float)Math.hypot(x, y);
               float computedValue2 = computedValue > 1.0E-5F ? -y / computedValue : 1.0F;
               float computedValue3 = computedValue > 1.0E-5F ? x / computedValue : 0.0F;
               float[] floatValues = new float[8];
               float[] floatValues2 = new float[8];
               float[] floatValues3 = new float[8];
               int roundedDown = (int)Math.floor(coordinate * 16.0);

               for (int value2 = 0; value2 <= 7; value2++) {
                  float computedValue4 = value2 / 7.0F;
                  float computedValue5 = fractSin(bolt.seed * 71.13F + value2 * 31.71F + roundedDown * 1.41F);
                  float computedValue6 = value2 != 0 && value2 != 7 ? (computedValue5 - 0.5F) * Math.min(0.17F, computedValue * 0.19F) : 0.0F;
                  floatValues[value2] = vector4f.x + x * computedValue4 + computedValue2 * computedValue6;
                  floatValues2[value2] = vector4f.y + y * computedValue4 + computedValue3 * computedValue6;
                  floatValues3[value2] = vector4f.z + (vector4f2.z - vector4f.z) * computedValue4;
               }

               int computedValue7 = Math.min(255, Math.max(0, Math.round((bolt.argb >>> 24 & 0xFF) * bolt.strength)));
               int value3 = bolt.argb & 16777215 | computedValue7 << 24;

               for (int value4 = 0; value4 < 7; value4++) {
                  float value5 = floatValues[value4];
                  float value6 = floatValues2[value4];
                  float computedValue8 = floatValues[value4 + 1];
                  float computedValue9 = floatValues2[value4 + 1];
                  float computedValue10 = computedValue8 - value5;
                  float computedValue11 = computedValue9 - value6;
                  float computedValue12 = (float)Math.hypot(computedValue10, computedValue11);
                  float computedValue13 = computedValue12 > 1.0E-5F ? -computedValue11 / computedValue12 : computedValue2;
                  float computedValue14 = computedValue12 > 1.0E-5F ? computedValue10 / computedValue12 : computedValue3;
                  float width = bolt.width;
                  float computedValue15 = value4 / 7.0F;
                  float computedValue16 = (value4 + 1) / 7.0F;
                  boltVertex(bufferBuilder, value5 + computedValue13 * width, value6 + computedValue14 * width, floatValues3[value4], computedValue15, 0.0F, value3);
                  boltVertex(bufferBuilder, value5 - computedValue13 * width, value6 - computedValue14 * width, floatValues3[value4], computedValue15, 1.0F, value3);
                  boltVertex(bufferBuilder, computedValue8 - computedValue13 * width, computedValue9 - computedValue14 * width, floatValues3[value4 + 1], computedValue16, 1.0F, value3);
                  boltVertex(bufferBuilder, value5 + computedValue13 * width, value6 + computedValue14 * width, floatValues3[value4], computedValue15, 0.0F, value3);
                  boltVertex(bufferBuilder, computedValue8 - computedValue13 * width, computedValue9 - computedValue14 * width, floatValues3[value4 + 1], computedValue16, 1.0F, value3);
                  boltVertex(bufferBuilder, computedValue8 + computedValue13 * width, computedValue9 + computedValue14 * width, floatValues3[value4 + 1], computedValue16, 0.0F, value3);
               }
            }
         };
         MeshBuffer meshBuffer = MeshBuffer.build(value, tRIANGLES, pOSITION_TEX_COLOR, writer);

         try {
            this.draw(meshBuffer, this.lightningGpu, renderTarget, LIGHTNING, "Thunder GLSL Lightning");
         } catch (Throwable failure) {
            if (meshBuffer != null) {
               try {
                  meshBuffer.close();
               } catch (Throwable failure2) {
                  failure.addSuppressed(failure2);
               }
            }

            throw failure;
         }

         if (meshBuffer != null) {
            meshBuffer.close();
         }
      }
   }

   private void renderGlass(RenderTarget renderTarget, Vec3 position, Matrix4f matrix) {
      int value = 0;
      if (!this.glass.isEmpty()) {
         value = this.glass.size() * 3 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
         MeshBuffer meshBuffer = MeshBuffer.build(value, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR, bufferBuilder -> {
            for (ThunderPipelines.Glass glass2 : this.glass) {
               Vector4f vector4f = CameraViewPoseHelper.toViewSpace(glass2.tip, position, matrix);
               Vector4f vector4f2 = CameraViewPoseHelper.toViewSpace(glass2.left, position, matrix);
               Vector4f vector4f3 = CameraViewPoseHelper.toViewSpace(glass2.right, position, matrix);
               int computedValue = Math.max(0, Math.min(255, (int)((glass2.argb >>> 24) * glass2.strength)));
               int value2 = glass2.argb & 16777215 | computedValue << 24;
               glassVertex(bufferBuilder, vector4f, 0.5F, 0.0F, value2);
               glassVertex(bufferBuilder, vector4f2, 0.0F, 1.0F, value2);
               glassVertex(bufferBuilder, vector4f3, 1.0F, 1.0F, value2);
            }
         });

         try {
            this.draw(meshBuffer, this.prismGpu, renderTarget, PRISM, "Thunder Glass Prisms");
         } catch (Throwable failure) {
            if (meshBuffer != null) {
               try {
                  meshBuffer.close();
               } catch (Throwable failure2) {
                  failure.addSuppressed(failure2);
               }
            }

            throw failure;
         }

         if (meshBuffer != null) {
            meshBuffer.close();
         }
      }
   }

   public ThunderPipelines() {
      this.prismGpu = new UploadHelper("Skycore Thunder Prisms");
      this.bolts = new ArrayList<>(128);
      this.glass = new ArrayList<>(48);
   }

   private static float fractSin(float value) {
      double intermediate = value * 12.9898;
      double computedValue = Math.sin(intermediate) * 43758.5453;
      return (float)(computedValue - Math.floor(computedValue));
   }

   public void glass(Vec3 position, Vec3 position2, Vec3 position3, int value, float value2) {
      if (value2 > 0.002F) {
         if (this.glass.size() < 110) {
            this.glass.add(new ThunderPipelines.Glass(position, position2, position3, value, value2));
         }
      }
   }

   public void release() {
      this.beginFrame();
      this.lightningGpu.close();
      this.prismGpu.close();
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }
   }

   public void beginFrame() {
      this.bolts.clear();
      this.glass.clear();
   }

   private void draw(MeshBuffer meshBuffer, UploadHelper uploadHelper, RenderTarget renderTarget, RenderPipeline renderPipeline, String text) {
      if (meshBuffer != null) {
         MeshData meshData = meshBuffer.mesh();
         int value = meshData.vertexBuffer().remaining();
         GpuBuffer gpuBuffer = uploadHelper.upload(meshData);
         CommandEncoder device = RenderSystem.getDevice().createCommandEncoder();
         RenderPass depthTextureView = renderTarget.getDepthTextureView() != null ? device.createRenderPass(() -> {
            return text;
         }, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty()) : device.createRenderPass(() -> {
            return text;
         }, renderTarget.getColorTextureView(), Optional.empty());

         try (depthTextureView) {
            depthTextureView.setPipeline(renderPipeline);
            depthTextureView.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
            depthTextureView.setUniform("ThunderUniforms", this.uniformBuffer);
            depthTextureView.setVertexBuffer(0, gpuBuffer.slice(0L, value));
            depthTextureView.draw(meshData.drawState().vertexCount(), 1, 0, 0);
         }
      }
   }

   private static void glassVertex(BufferBuilder bufferBuilder, Vector4f vector4f, float value, float value2, int color) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setUv(value, value2);
      vertexConsumer.setColor(color);
   }

   private static void boltVertex(BufferBuilder bufferBuilder, float value, float value2, float value3, float value4, float value5, int color) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(value, value2, value3);
      vertexConsumer.setUv(value4, value5).setColor(color);
   }

   public void render(long timestamp) {
      if (!this.bolts.isEmpty() || !this.glass.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.gameRenderer != null && RenderSystem.tryGetDevice() != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            if (renderTarget != null && renderTarget.getColorTextureView() != null) {
               Camera camera = instance.gameRenderer.mainCamera();
               Vec3 position = camera.position();
               Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
               if (this.uniformBuffer == null) {
                  this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> {
                     return "Skycore Thunder UBO";
                  }, 136, 16L);
               }

               double computedValue = timestamp / 1.0E9;

               try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.uniformBuffer.slice(), Std140Builder.onStack(memoryStack, 16).putVec4((float)(computedValue % 4096.0), 0.0F, 0.0F, 0.0F).get());
               }

               this.renderGlass(renderTarget, position, matrix);
               this.renderBolts(renderTarget, position, matrix, computedValue);
            }
         }
      }
   }

   public record Bolt(Vec3 from, Vec3 to, float width, float seed, float strength, int argb) {
      public Vec3 to() {
         return this.to;
      }

      public float width() {
         return this.width;
      }

      public float strength() {
         return this.strength;
      }

      public float seed() {
         return this.seed;
      }

      public int argb() {
         return this.argb;
      }

      public Vec3 from() {
         return this.from;
      }
   }

   public record Glass(Vec3 tip, Vec3 left, Vec3 right, int argb, float strength) {
      public float strength() {
         return this.strength;
      }

      public Vec3 right() {
         return this.right;
      }

      public Vec3 left() {
         return this.left;
      }

      public int argb() {
         return this.argb;
      }

      public Vec3 tip() {
         return this.tip;
      }
   }
}
