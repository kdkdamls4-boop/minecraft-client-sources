package net.skycore.client.util;

import com.mojang.blaze3d.GpuFormat;
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
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.LivingEntityRendererInvokeSetupRotationsAccessor;
import net.skycore.client.mixin.ModelPartGetCubesAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class TargetScanRenderer {
   private LivingEntity lastTarget;
   private final ElapsedMillisHelper appear = new ElapsedMillisHelper(300L, ElapsedMillisHelper.Easing.EASE_OUT_QUAD);
   private static final BindGroupLayout COMPOSITE_LAYOUT = BindGroupLayout.builder()
      .withSampler("MaskSampler")
      .withSampler("DepthSampler")
      .withUniform("ScanUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<TargetScanRenderer.ScanVertex> vertices;
   private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/target_scan"))
      .withVertexShader(Identifier.parse("skycore:core/target_scan"))
      .withFragmentShader(Identifier.parse("skycore:core/target_scan"))
      .withBindGroupLayout(TargetScanRenderer.COMPOSITE_LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private TextureTarget maskTarget;
   private static final RenderPipeline MASK_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/target_scan_mask"))
      .withVertexShader(Identifier.parse("skycore:core/world_color"))
      .withFragmentShader(Identifier.parse("skycore:core/world_color"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private GpuBuffer uniforms;
   private boolean animationTarget;
   private final Matrix4f inverseViewProjection = new Matrix4f();
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().putVec4().get();

   private void updateAnimation(boolean animationTarget2) {
      if (animationTarget2 != this.animationTarget) {
         this.animationTarget = animationTarget2;
         this.appear.animate(this.appear.getValue(), animationTarget2 ? 1.0F : 0.0F, animationTarget2 ? 300L : 240L, ElapsedMillisHelper.Easing.EASE_OUT_QUAD);
      }
   }

   public void reset() {
      this.lastTarget = null;
      this.animationTarget = false;
      this.vertices.clear();
      this.appear.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_OUT_QUAD);
   }

   public void release() {
      this.reset();
      if (this.maskTarget != null) {
         this.maskTarget.destroyBuffers();
         this.maskTarget = null;
      }

      if (this.uniforms != null) {
         this.uniforms.close();
         this.uniforms = null;
      }
   }

   private void appendCube(Cube cube, Pose pose, Vec3 position, Vec3 position2, Matrix4f matrix) {
      for (Polygon polygon : cube.polygons) {
         Vertex[] vertexValues = polygon.vertices();
         if (vertexValues.length >= 3) {
            TargetScanRenderer.ScanVertex scanVertex = this.transform(vertexValues[0], pose, position, position2, matrix);

            for (int value = 1; value + 1 < vertexValues.length; value++) {
               this.vertices.add(scanVertex);
               this.vertices.add(this.transform(vertexValues[value], pose, position, position2, matrix));
               this.vertices.add(this.transform(vertexValues[value + 1], pose, position, position2, matrix));
            }
         }
      }
   }

   private void appendPart(ModelPart modelPart, PoseStack poseStack, Vec3 position, Vec3 position2, Matrix4f matrix) {
      if (modelPart.visible) {
         ModelPartGetCubesAccessor modelPartGetCubesAccessor = (ModelPartGetCubesAccessor)(Object)modelPart;
         var cubes = modelPartGetCubesAccessor.skycore$getCubes();
         var lookup = modelPartGetCubesAccessor.skycore$getChildren();
         if (!cubes.isEmpty() || !lookup.isEmpty()) {
            poseStack.pushPose();
            modelPart.translateAndRotate(poseStack);
            if (!modelPart.skipDraw) {
               Pose pose = poseStack.last();

               for (Cube cube : cubes) {
                  this.appendCube(cube, pose, position, position2, matrix);
               }
            }

            for (ModelPart modelPart2 : lookup.values()) {
               this.appendPart(modelPart2, poseStack, position, position2, matrix);
            }

            poseStack.popPose();
         }
      }
   }

   public TargetScanRenderer() {
      this.vertices = new ArrayList<>(4096);
   }

   private void ensureMaskTarget(int width, int height) {
      if (this.maskTarget != null) {
         if (this.maskTarget.width == width) {
            if (this.maskTarget.height == height) {
               return;
            }
         }
      }

      if (this.maskTarget != null) {
         this.maskTarget.destroyBuffers();
      }

      this.maskTarget = new TextureTarget("skycore-target-scan-mask", width, height, true, GpuFormat.RGBA8_UNORM);
   }

   private static boolean valid(LivingEntity entity) {
      return entity != null && entity.isAlive() && !entity.isRemoved();
   }

   public void render(LivingEntity lastTarget2, float count, int coordinate) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null && instance.gameRenderer != null && RenderSystem.tryGetDevice() != null) {
         boolean valid = valid(lastTarget2);
         if (valid) {
            this.lastTarget = lastTarget2;
         }

         this.updateAnimation(valid);
         float value = this.appear.getValue();
         LivingEntity lastTarget3 = this.lastTarget;
         if (lastTarget3 != null && !(value <= 0.01F)) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            if (renderTarget != null && renderTarget.width > 0 && renderTarget.height > 0 && renderTarget.getColorTextureView() != null) {
               Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
               if (matrix != null) {
                  EntityRenderer renderer = instance.getEntityRenderDispatcher().getRenderer(lastTarget3);
                  if (renderer instanceof LivingEntityRenderer livingEntityRenderer) {
                     if (renderer.createRenderState(lastTarget3, count) instanceof LivingEntityRenderState livingEntityRenderState) {
                        EntityModel model = livingEntityRenderer.getModel();
                        model.setupAnim(livingEntityRenderState);
                        Vec3 position = CameraViewPoseHelper.interpolatedPosition(lastTarget3, count);
                        Camera camera = instance.gameRenderer.mainCamera();
                        Vec3 position2 = camera.position();
                        Matrix4f matrix2 = CameraViewPoseHelper.cameraViewPose(camera);
                        this.inverseViewProjection.set(matrix).mul(matrix2).invert();
                        float count2 = 1.0F;
                        float computedValue = (lastTarget3.tickCount + count) / 20.0F * count2;
                        float computedValue2 = (float)(computedValue % 2.0F * Math.PI);
                        float bbHeight = lastTarget3.getBbHeight();
                        float y = (float)(position.y + bbHeight * 0.5F + Math.sin(computedValue2) * (bbHeight * 0.5F + 0.1F));
                        float cosine = (float)Math.cos(computedValue2);
                        float computedValue3 = cosine > 0.0F ? 1.0F : -1.0F;
                        float computedValue4 = Math.abs(cosine);
                        float computedValue5 = (float)(computedValue % (Math.PI * 10));
                        float x = (float)(position.x - position2.x);
                        float y2 = (float)(position.y - position2.y);
                        float z = (float)(position.z - position2.z);
                        float y3 = (float)(y - position2.y);
                        PoseStack poseStack = new PoseStack();
                        if (livingEntityRenderState.hasPose(net.minecraft.world.entity.Pose.SLEEPING) && livingEntityRenderState.bedOrientation != null) {
                           float computedValue6 = livingEntityRenderState.eyeHeight - 0.1F;
                           poseStack.translate(-livingEntityRenderState.bedOrientation.getStepX() * computedValue6, 0.0F, -livingEntityRenderState.bedOrientation.getStepZ() * computedValue6);
                        }

                        float scale = livingEntityRenderState.scale;
                        poseStack.scale(scale, scale, scale);
                        LivingEntityRendererInvokeSetupRotationsAccessor livingEntityRendererInvokeSetupRotationsAccessor = (LivingEntityRendererInvokeSetupRotationsAccessor)livingEntityRenderer;
                        livingEntityRendererInvokeSetupRotationsAccessor.skycore$invokeSetupRotations(livingEntityRenderState, poseStack, livingEntityRenderState.bodyRot, scale);
                        poseStack.scale(-1.0F, -1.0F, 1.0F);
                        livingEntityRendererInvokeSetupRotationsAccessor.skycore$invokeScale(livingEntityRenderState, poseStack);
                        poseStack.translate(0.0F, -1.501F, 0.0F);
                        this.vertices.clear();
                        this.appendPart(model.root(), poseStack, position, position2, matrix2);
                        if (!this.vertices.isEmpty()) {
                           MeshData meshData = this.buildModelMesh();
                           if (meshData != null) {
                              this.ensureMaskTarget(renderTarget.width, renderTarget.height);
                              if (this.maskTarget != null
                                 && this.maskTarget.getColorTexture() != null
                                 && this.maskTarget.getDepthTexture() != null
                                 && this.maskTarget.getColorTextureView() != null
                                 && this.maskTarget.getDepthTextureView() != null) {
                                 GpuBuffer gpuBuffer = null;

                                 try {
                                    gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Skycore Target Scan Mask Vertices", 32, meshData.vertexBuffer());
                                    RenderSystem.getDevice()
                                       .createCommandEncoder()
                                       .clearColorAndDepthTextures(
                                          this.maskTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), this.maskTarget.getDepthTexture(), 0.0
                                       );

                                    try (RenderPass device = RenderSystem.getDevice()
                                          .createCommandEncoder()
                                          .createRenderPass(
                                             () -> {
                                                return "Skycore Target Scan Mask";
                                             },
                                             this.maskTarget.getColorTextureView(),
                                             Optional.empty(),
                                             this.maskTarget.getDepthTextureView(),
                                             OptionalDouble.empty()
                                          )) {
                                       device.setPipeline(MASK_PIPELINE);
                                       device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                                       device.setVertexBuffer(0, gpuBuffer.slice());
                                       device.draw(meshData.drawState().vertexCount(), 1, 0, 0);
                                    }
                                 } finally {
                                    if (gpuBuffer != null) {
                                       gpuBuffer.close();
                                    }

                                    meshData.close();
                                 }

                                 this.ensureUniforms();
                                 float device2 = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
                                 int computedValue7 = coordinate | 0xFF000000;

                                 try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                                    ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE)
                                       .putMat4f(this.inverseViewProjection)
                                       .putVec4(x, y2, z, y3)
                                       .putVec4(computedValue3, computedValue4, computedValue5, value)
                                       .putVec4(1.0F, 1.0F, device2, 0.0F)
                                       .putVec4(
                                          WriteHexByteHelper.red(computedValue7) / 255.0F,
                                          WriteHexByteHelper.green(computedValue7) / 255.0F,
                                          WriteHexByteHelper.blue(computedValue7) / 255.0F,
                                          1.0F
                                       )
                                       .get();
                                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
                                 }

                                 GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                                 GpuSampler clampToEdge2 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

                                 try (RenderPass device3 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                                       return "Skycore Target Scan Composite";
                                    }, renderTarget.getColorTextureView(), Optional.empty())) {
                                    device3.setPipeline(COMPOSITE_PIPELINE);
                                    device3.setUniform("ScanUniforms", this.uniforms);
                                    device3.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), clampToEdge);
                                    device3.bindTexture("DepthSampler", this.maskTarget.getDepthTextureView(), clampToEdge2);
                                    device3.setVertexBuffer(0, EnsureHelper.buffer().slice());
                                    device3.draw(EnsureHelper.vertexCount(), 1, 0, 0);
                                 }
                              } else {
                                 meshData.close();
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            if (!valid) {
               this.lastTarget = null;
            }
         }
      } else {
         this.reset();
      }
   }

   public static List<RenderPipeline> pipelines() {
      return List.of(MASK_PIPELINE, COMPOSITE_PIPELINE);
   }

   private MeshData buildModelMesh() {
      int size = this.vertices.size();
      size *= DefaultVertexFormat.POSITION_COLOR.getVertexSize();
      BufferBuilder bufferBuilder = new BufferBuilder(ByteBufferBuilder.exactlySized(size), PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      Iterator iterator = this.vertices.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return bufferBuilder.build();
         }

         TargetScanRenderer.ScanVertex element = (TargetScanRenderer.ScanVertex)iterator.next();
         VertexConsumer vertexConsumer = bufferBuilder.addVertex(element.x, element.y, element.z);
         vertexConsumer.setColor(-1);
      }
   }

   private void ensureUniforms() {
      if (this.uniforms == null) {
         GpuDevice device = RenderSystem.getDevice();
         Supplier<java.lang.String> supplier = () -> {
            return "Skycore Target Scan UBO";
         };
         long uNIFORM_SIZE = UNIFORM_SIZE;
         GpuBuffer gpuBuffer = device.createBuffer(supplier, 136, uNIFORM_SIZE);
         this.uniforms = gpuBuffer;
      }
   }

   private TargetScanRenderer.ScanVertex transform(Vertex vertex, Pose pose, Vec3 position, Vec3 position2, Matrix4f matrix) {
      Matrix4f matrix2 = pose.pose();
      float value = vertex.worldX();
      float value2 = vertex.worldY();
      float intermediate = vertex.worldZ();
      Vector3f vector = new Vector3f();
      Vector3f vector2 = matrix2.transformPosition(value, value2, intermediate, vector);
      double x = vector2.x;
      Vec3 position3 = position.add(x, vector2.y, vector2.z);
      Vector4f vector4f = CameraViewPoseHelper.toViewSpace(position3, position2, matrix);
      return new TargetScanRenderer.ScanVertex(vector4f.x, vector4f.y, vector4f.z);
   }

   public record ScanVertex(float x, float y, float z) {

      public float x() {
         return this.x;
      }

      public float z() {
         return this.z;
      }

      public float y() {
         return this.y;
      }
   }
}
