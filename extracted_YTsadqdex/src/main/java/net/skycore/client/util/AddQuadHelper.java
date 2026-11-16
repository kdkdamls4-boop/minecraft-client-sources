package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class AddQuadHelper {
   private static final int[][] BOX_EDGES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private static final RenderPipeline FILL_THROUGH_WALLS = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/box_fill_through_walls"))
      .withVertexShader(Identifier.parse("skycore:core/world_color"))
      .withFragmentShader(Identifier.parse("skycore:core/world_color"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final float OUTLINE_HALF_PIXELS = 0.85F;
   private static final List<AddQuadHelper.Quad> QUADS = new ArrayList<>(64);
   private static final UploadHelper GPU = new UploadHelper("Skycore World Box Vertices");
   private static final List<AddQuadHelper.Segment> SCREEN_SEGMENTS = new ArrayList<>(128);

   private static void addQuad(
      double value,
      double value2,
      double value3,
      double value4,
      double value5,
      double value6,
      double value7,
      double value8,
      double value9,
      double value10,
      double value11,
      double value12,
      int value13
   ) {
      var qUADS = QUADS;
      Vec3 position = new Vec3(value, value2, value3);
      Vec3 position2 = new Vec3(value4, value5, value6);
      Vec3 position3 = new Vec3(value7, value8, value9);
      qUADS.add(new AddQuadHelper.Quad(position, position2, position3, new Vec3(value10, value11, value12), value13));
   }

   public static void addScreenSpaceLine(Vec3 position, Vec3 position2, int value) {
      if (position != null && position2 != null) {
         if (!(position.distanceToSqr(position2) < 1.0E-10)) {
            var sCREEN_SEGMENTS = SCREEN_SEGMENTS;
            AddQuadHelper.Segment segment = new AddQuadHelper.Segment(position, position2, value);
            sCREEN_SEGMENTS.add(segment);
            return;
         }
      }
   }

   private AddQuadHelper() {
   }

   public static void addFacet(Vec3 position, Vec3 position2, Vec3 position3, int value) {
      var qUADS = QUADS;
      AddQuadHelper.Quad quad = new AddQuadHelper.Quad(position, position2, position3, position3, value);
      qUADS.add(quad);
   }

   public static void addFilledBox(AABB bounds, int value) {
      double minX = bounds.minX;
      double minY = bounds.minY;
      double minZ = bounds.minZ;
      double maxX = bounds.maxX;
      double maxY = bounds.maxY;
      double maxZ = bounds.maxZ;
      addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, value);
      addQuad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, value);
      addQuad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, value);
      addQuad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, value);
      addQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, value);
      addQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, value);
   }

   private static void addViewTriangle(
      BufferBuilder bufferBuilder, float value, float value2, float value3, float value4, float value5, float value6, float value7, float value8, float value9, int color
   ) {
      VertexConsumer vertexConsumer = bufferBuilder.addVertex(value, value2, value3);
      vertexConsumer.setColor(color);
      VertexConsumer vertexConsumer2 = bufferBuilder.addVertex(value4, value5, value6);
      vertexConsumer2.setColor(color);
      bufferBuilder.addVertex(value7, value8, value9).setColor(color);
   }

   public static void addBodyDiagonals(AABB bounds, int value, float value2) {
      double computedValue = value2 * 0.01;
      double maximum = Math.max(0.008, computedValue);
      addEdge(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, maximum, value);
      addEdge(bounds.maxX, bounds.minY, bounds.minZ, bounds.minX, bounds.maxY, bounds.maxZ, maximum, value);
      addEdge(bounds.minX, bounds.minY, bounds.maxZ, bounds.maxX, bounds.maxY, bounds.minZ, maximum, value);
      addEdge(bounds.maxX, bounds.minY, bounds.maxZ, bounds.minX, bounds.maxY, bounds.minZ, maximum, value);
   }

   public static void addBeam(Vec3 position, Vec3 position2, double value, int value2) {
      if (position != null && position2 != null && !(position.distanceToSqr(position2) < 1.0E-8)) {
         double x = position.x;
         double y = position.y;
         double z = position.z;
         double x2 = position2.x;
         double y2 = position2.y;
         double z2 = position2.z;
         double minimum = Math.min(0.06, value);
         double maximum = Math.max(0.001, minimum);
         addEdge(x, y, z, x2, y2, z2, maximum, value2);
      }
   }

   public static void beginFrame() {
      QUADS.clear();
      SCREEN_SEGMENTS.clear();
   }

   public static RenderPipeline pipeline() {
      return FILL_THROUGH_WALLS;
   }

   private static void appendScreenSegment(
      BufferBuilder bufferBuilder, AddQuadHelper.Segment segment, Vec3 position, Matrix4f matrix, Matrix4f matrix2, int value, int value2, boolean condition
   ) {
      if (value > 0 && value2 > 0 && !(Math.abs(matrix2.m00()) < 1.0E-5F) && !(Math.abs(matrix2.m11()) < 1.0E-5F)) {
         Vector4f vector4f = CameraViewPoseHelper.toViewSpace(segment.a, position, matrix);
         Vector4f vector4f2 = CameraViewPoseHelper.toViewSpace(segment.b, position, matrix);
         Vector4f vector4f3 = new Vector4f(vector4f).mul(matrix2);
         Vector4f vector4f4 = new Vector4f(vector4f2).mul(matrix2);
         float z = vector4f3.z + (condition ? 0.0F : vector4f3.w);
         float z2 = vector4f4.z + (condition ? 0.0F : vector4f4.w);
         if (!(z < 0.0F) || !(z2 < 0.0F)) {
            if (z < 0.0F) {
               vector4f.lerp(vector4f2, z / (z - z2));
               vector4f3.set(vector4f).mul(matrix2);
            } else if (z2 < 0.0F) {
               vector4f2.lerp(vector4f, z2 / (z2 - z));
               vector4f4.set(vector4f2).mul(matrix2);
            }

            if (!(vector4f3.w <= 1.0E-4F) && !(vector4f4.w <= 1.0E-4F)) {
               float x = (vector4f4.x / vector4f4.w - vector4f3.x / vector4f3.w) * value * 0.5F;
               float y = (vector4f4.y / vector4f4.w - vector4f3.y / vector4f3.w) * value2 * 0.5F;
               float squareRoot = (float)Math.sqrt(x * x + y * y);
               if (!(squareRoot < 0.001F) && Float.isFinite(squareRoot)) {
                  float computedValue = -y / squareRoot * (1.7F / value);
                  float computedValue2 = x / squareRoot * (1.7F / value2);
                  float computedValue3 = computedValue * vector4f3.w / matrix2.m00();
                  float computedValue4 = computedValue2 * vector4f3.w / matrix2.m11();
                  float computedValue5 = computedValue * vector4f4.w / matrix2.m00();
                  float computedValue6 = computedValue2 * vector4f4.w / matrix2.m11();
                  addViewTriangle(
                     bufferBuilder, vector4f.x + computedValue3, vector4f.y + computedValue4, vector4f.z, vector4f.x - computedValue3, vector4f.y - computedValue4, vector4f.z, vector4f2.x - computedValue5, vector4f2.y - computedValue6, vector4f2.z, segment.color
                  );
                  addViewTriangle(
                     bufferBuilder, vector4f.x + computedValue3, vector4f.y + computedValue4, vector4f.z, vector4f2.x - computedValue5, vector4f2.y - computedValue6, vector4f2.z, vector4f2.x + computedValue5, vector4f2.y + computedValue6, vector4f2.z, segment.color
                  );
               }
            }
         }
      }
   }

   public static void addScreenSpaceBoxOutline(AABB bounds, int value) {
      Vec3[] vec3Values = new Vec3[8];
      vec3Values[0] = new Vec3(bounds.minX, bounds.minY, bounds.minZ);
      vec3Values[1] = new Vec3(bounds.maxX, bounds.minY, bounds.minZ);
      vec3Values[2] = new Vec3(bounds.maxX, bounds.minY, bounds.maxZ);
      vec3Values[3] = new Vec3(bounds.minX, bounds.minY, bounds.maxZ);
      vec3Values[4] = new Vec3(bounds.minX, bounds.maxY, bounds.minZ);
      vec3Values[5] = new Vec3(bounds.maxX, bounds.maxY, bounds.minZ);
      vec3Values[6] = new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ);
      vec3Values[7] = new Vec3(bounds.minX, bounds.maxY, bounds.maxZ);
      Vec3[] vec3Values2 = vec3Values;

      for (int[] intValues : BOX_EDGES) {
         Vec3 position = vec3Values2[intValues[0]];
         Vec3 position2 = vec3Values2[intValues[1]];
         SCREEN_SEGMENTS.add(new AddQuadHelper.Segment(position, position2, value));
      }
   }

   public static void endFrame() {
      if (!QUADS.isEmpty() || !SCREEN_SEGMENTS.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.gameRenderer != null) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
            if (colorTextureView == null) {
               QUADS.clear();
               SCREEN_SEGMENTS.clear();
            } else {
               Camera camera = instance.gameRenderer.mainCamera();
               Vec3 position = camera.position();
               Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
               int size = QUADS.size();
               int size2 = SCREEN_SEGMENTS.size();
               var quads = List.copyOf(QUADS);
               var segments = List.copyOf(SCREEN_SEGMENTS);
               QUADS.clear();
               SCREEN_SEGMENTS.clear();
               Matrix4f matrix2 = CaptureLevelProjectionHelper.levelProjectionCopy();
               int device = matrix2 != null && RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1 : 0;
               int width = renderTarget.width;
               int height = renderTarget.height;

               try (MeshBuffer vertexSize = MeshBuffer.build(
                     (size + size2) * 6 * DefaultVertexFormat.POSITION_COLOR.getVertexSize(),
                     PrimitiveTopology.TRIANGLES,
                     DefaultVertexFormat.POSITION_COLOR,
                     bufferBuilder -> {
                        for (AddQuadHelper.Quad quad : quads) {
                           Vector4f vector4f = CameraViewPoseHelper.toViewSpace(quad.p0, position, matrix);
                           Vector4f vector4f2 = CameraViewPoseHelper.toViewSpace(quad.p1, position, matrix);
                           Vector4f vector4f3 = CameraViewPoseHelper.toViewSpace(quad.p2, position, matrix);
                           Vector4f vector4f4 = CameraViewPoseHelper.toViewSpace(quad.p3, position, matrix);
                           bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setColor(quad.color0);
                           bufferBuilder.addVertex(vector4f2.x, vector4f2.y, vector4f2.z).setColor(quad.color1);
                           bufferBuilder.addVertex(vector4f3.x, vector4f3.y, vector4f3.z).setColor(quad.color2);
                           bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setColor(quad.color0);
                           bufferBuilder.addVertex(vector4f3.x, vector4f3.y, vector4f3.z).setColor(quad.color2);
                           bufferBuilder.addVertex(vector4f4.x, vector4f4.y, vector4f4.z).setColor(quad.color3);
                        }

                        if (matrix2 != null && !segments.isEmpty()) {
                           for (AddQuadHelper.Segment segment : segments) {
                              appendScreenSegment(bufferBuilder, segment, position, matrix, matrix2, width, height, device != 0);
                           }
                        }
                     }
                  )) {
                  if (vertexSize == null) {
                     return;
                  }

                  MeshData meshData = vertexSize.mesh();
                  int value = meshData.drawState().vertexCount();
                  int value2 = meshData.vertexBuffer().remaining();
                  GpuBuffer gpuBuffer = GPU.upload(meshData);

                  try (RenderPass device2 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                        return "Skycore World Box Pass";
                     }, colorTextureView, Optional.empty())) {
                     device2.setPipeline(FILL_THROUGH_WALLS);
                     device2.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     device2.setVertexBuffer(0, gpuBuffer.slice(0L, value2));
                     device2.draw(value, 1, 0, 0);
                  }
               }
            }
         } else {
            QUADS.clear();
            SCREEN_SEGMENTS.clear();
         }
      }
   }

   private static void addEdge(double value, double value2, double value3, double value4, double value5, double value6, double value7, int value8) {
      Vec3 position = new Vec3(value, value2, value3);
      Vec3 position2 = new Vec3(value4, value5, value6);
      Vec3 position3 = position2.subtract(position);
      double length = position3.length();
      if (!(length < 1.0E-4)) {
         Vec3 position4 = position3.scale(1.0 / length);
         Vec3 position5 = Math.abs(position4.y) < 0.9
            ? position4.cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(value7)
            : position4.cross(new Vec3(1.0, 0.0, 0.0)).normalize().scale(value7);
         Vec3 position6 = position4.cross(position5).normalize().scale(value7);
         Vec3 position7 = position.add(position5).add(position6);
         Vec3 position8 = position.subtract(position5).add(position6);
         Vec3 position9 = position2.subtract(position5).add(position6);
         Vec3 position10 = position2.add(position5).add(position6);
         QUADS.add(new AddQuadHelper.Quad(position7, position8, position9, position10, value8));
         Vec3 position11 = position.add(position5).subtract(position6);
         Vec3 position12 = position2.add(position5).subtract(position6);
         Vec3 position13 = position2.subtract(position5).subtract(position6);
         Vec3 position14 = position.subtract(position5).subtract(position6);
         QUADS.add(new AddQuadHelper.Quad(position11, position12, position13, position14, value8));
      }
   }

   public static void addFacet(Vec3 position, Vec3 position2, Vec3 position3, int value, int value2, int value3) {
      var qUADS = QUADS;
      AddQuadHelper.Quad quad = new AddQuadHelper.Quad(position, position2, position3, position3, value, value2, value3, value3);
      qUADS.add(quad);
   }

   public static void addBoxOutline(AABB bounds, int value, float value2) {
      double minX = bounds.minX;
      double minY = bounds.minY;
      double minZ = bounds.minZ;
      double maxX = bounds.maxX;
      double maxY = bounds.maxY;
      double maxZ = bounds.maxZ;
      double maximum = Math.max(0.008, value2 * 0.01);
      addEdge(minX, minY, minZ, maxX, minY, minZ, maximum, value);
      addEdge(maxX, minY, minZ, maxX, minY, maxZ, maximum, value);
      addEdge(maxX, minY, maxZ, minX, minY, maxZ, maximum, value);
      addEdge(minX, minY, maxZ, minX, minY, minZ, maximum, value);
      addEdge(minX, maxY, minZ, maxX, maxY, minZ, maximum, value);
      addEdge(maxX, maxY, minZ, maxX, maxY, maxZ, maximum, value);
      addEdge(maxX, maxY, maxZ, minX, maxY, maxZ, maximum, value);
      addEdge(minX, maxY, maxZ, minX, maxY, minZ, maximum, value);
      addEdge(minX, minY, minZ, minX, maxY, minZ, maximum, value);
      addEdge(maxX, minY, minZ, maxX, maxY, minZ, maximum, value);
      addEdge(maxX, minY, maxZ, maxX, maxY, maxZ, maximum, value);
      addEdge(minX, minY, maxZ, minX, maxY, maxZ, maximum, value);
   }

   public record Quad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color0, int color1, int color2, int color3) {
      public Vec3 p0() {
         return this.p0;
      }

      public int color2() {
         return this.color2;
      }

      public Vec3 p2() {
         return this.p2;
      }

      public Vec3 p3() {
         return this.p3;
      }

      public int color1() {
         return this.color1;
      }

      private Quad(Vec3 position, Vec3 position2, Vec3 position3, Vec3 position4, int value) {
         this(position, position2, position3, position4, value, value, value, value);
      }

      public Vec3 p1() {
         return this.p1;
      }

      public int color0() {
         return this.color0;
      }

      public int color3() {
         return this.color3;
      }
   }

   public record Segment(Vec3 a, Vec3 b, int color) {
      public Vec3 a() {
         return this.a;
      }

      public Vec3 b() {
         return this.b;
      }

      public int color() {
         return this.color;
      }
   }
}
