package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class RenderLinesHelper {
   private final UploadHelper gpu = new UploadHelper("Skycore Cubes Lines");
   private final WorldParticleRenderer glowRenderer = new WorldParticleRenderer();
   private static final int[][] EDGES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private static final float[][] CORNERS = new float[][]{
      {-0.5F, -0.5F, -0.5F},
      {0.5F, -0.5F, -0.5F},
      {0.5F, -0.5F, 0.5F},
      {-0.5F, -0.5F, 0.5F},
      {-0.5F, 0.5F, -0.5F},
      {0.5F, 0.5F, -0.5F},
      {0.5F, 0.5F, 0.5F},
      {-0.5F, 0.5F, 0.5F}
   };
   private static final RenderPipeline LINES = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/cubes_lines"))
      .withVertexShader(Identifier.parse("skycore:core/world_color"))
      .withFragmentShader(Identifier.parse("skycore:core/world_color"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int[][] DIAGONALS = new int[][]{{0, 6}, {1, 7}, {3, 5}, {2, 4}};
   private static final Identifier BLOOM = Identifier.parse("skycore:icons/world_render/bloom.png");
   private static final float HALF_LINE_WIDTH = 0.008F;

   private void renderLines(List<RenderLinesHelper.Cube> cubes, int alpha) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gameRenderer != null && instance.level != null) {
         RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
         if (renderTarget != null && renderTarget.getColorTextureView() != null && renderTarget.getDepthTextureView() != null) {
            Camera camera = instance.gameRenderer.mainCamera();
            Vec3 position = camera.position();
            Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);

            try (MeshBuffer vertexSize = MeshBuffer.build(
                  cubes.size() * (EDGES.length + DIAGONALS.length) * 6 * DefaultVertexFormat.POSITION_COLOR.getVertexSize(),
                  PrimitiveTopology.TRIANGLES,
                  DefaultVertexFormat.POSITION_COLOR,
                  bufferBuilder -> {
                     for (RenderLinesHelper.Cube cube : cubes) {
                        Quaternionf rotation = new Quaternionf().rotationXYZ((float)cube.rotation().x, (float)cube.rotation().y, (float)cube.rotation().z);
                        Vector4f[] vector4fValues = new Vector4f[CORNERS.length];

                        for (int value = 0; value < CORNERS.length; value++) {
                           float[] floatValues = CORNERS[value];
                           Vector3f vector = new Vector3f(floatValues[0], floatValues[1], floatValues[2]).rotate(rotation).mul(cube.size());
                           vector4fValues[value] = CameraViewPoseHelper.toViewSpace(cube.position().add(vector.x, vector.y, vector.z), position, matrix);
                        }

                        int index = withAlpha(alpha, cube.alpha() * 0.4F);
                        int index2 = withAlpha(alpha, cube.alpha() * 0.8F);

                        for (int[] intValues : DIAGONALS) {
                           appendLine(bufferBuilder, vector4fValues[intValues[0]], vector4fValues[intValues[1]], index);
                        }

                        for (int[] intValues2 : EDGES) {
                           appendLine(bufferBuilder, vector4fValues[intValues2[0]], vector4fValues[intValues2[1]], index2);
                        }
                     }
                  }
               )) {
               if (vertexSize == null) {
                  return;
               }

               MeshData meshData = vertexSize.mesh();
               int value2 = meshData.drawState().vertexCount();
               int value3 = meshData.vertexBuffer().remaining();
               GpuBuffer gpuBuffer = this.gpu.upload(meshData);

               try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore Cubes Lines Pass";
                  }, renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                  device.setPipeline(LINES);
                  device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  device.setVertexBuffer(0, gpuBuffer.slice(0L, value3));
                  device.draw(value2, 1, 0, 0);
               }
            }
         }
      }
   }

   public static RenderPipeline pipeline() {
      return LINES;
   }

   public void render(List<RenderLinesHelper.Cube> cubes, int alpha) {
      if (!cubes.isEmpty()) {
         ArrayList elements = new ArrayList(cubes.size());
         Iterator iterator = cubes.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               this.glowRenderer.render(elements, true);
               this.renderLines(cubes, alpha);
               return;
            }

            RenderLinesHelper.Cube element = (RenderLinesHelper.Cube)iterator.next();
            Identifier bLOOM = BLOOM;
            Vec3 position = element.position();
            float intermediate = element.size() * 2.0F;
            float alpha2 = element.alpha() * 0.4F;
            int intermediate2 = withAlpha(alpha, alpha2);
            WorldParticleRenderer.Sprite sprite = new WorldParticleRenderer.Sprite(bLOOM, position, intermediate, 0.0F, intermediate2);
            elements.add(sprite);
         }
      }
   }

   private static void vertex(BufferBuilder bufferBuilder, float value, float value2, float value3, int color) {
      bufferBuilder.addVertex(value, value2, value3).setColor(color);
   }

   private static void appendLine(BufferBuilder bufferBuilder, Vector4f vector4f, Vector4f vector4f2, int value) {
      float x = vector4f2.x - vector4f.x;
      float y = vector4f2.y - vector4f.y;
      float squareRoot = (float)Math.sqrt(x * x + y * y);
      if (!(squareRoot < 1.0E-5F)) {
         float computedValue = -y / squareRoot * 0.008F;
         float computedValue2 = x / squareRoot * 0.008F;
         vertex(bufferBuilder, vector4f.x + computedValue, vector4f.y + computedValue2, vector4f.z, value);
         vertex(bufferBuilder, vector4f.x - computedValue, vector4f.y - computedValue2, vector4f.z, value);
         vertex(bufferBuilder, vector4f2.x - computedValue, vector4f2.y - computedValue2, vector4f2.z, value);
         vertex(bufferBuilder, vector4f.x + computedValue, vector4f.y + computedValue2, vector4f.z, value);
         vertex(bufferBuilder, vector4f2.x - computedValue, vector4f2.y - computedValue2, vector4f2.z, value);
         vertex(bufferBuilder, vector4f2.x + computedValue, vector4f2.y + computedValue2, vector4f2.z, value);
      }
   }

   private static int withAlpha(int value, float value2) {
      int value3 = 1;
      value3 = (byte)-1;
      value3 = (byte)1;
      float intermediate = value2 * 255.0F;
      int roundedValue = Math.round(intermediate);
      value3 = Math.max(0, roundedValue);
      int minimum = Math.min(255, value3);
      return minimum << 24 | value & 16777215;
   }

   public record Cube(Vec3 position, Vec3 rotation, float size, float alpha) {
      public Vec3 position() {
         return this.position;
      }

      public float alpha() {
         return this.alpha;
      }

      public Vec3 rotation() {
         return this.rotation;
      }

      public float size() {
         return this.size;
      }
   }
}
