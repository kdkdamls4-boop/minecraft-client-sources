package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class DarkenHelper {
   private static long lastBiteTime = 0L;
   private final TargetParticleRenderer glowRenderer;
   private static final int SKULL_COUNT = 3;
   private int dissolvedTargetId;
   private final ElapsedMillisHelper appear = new ElapsedMillisHelper(650L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
   private final ElapsedMillisHelper inner = new ElapsedMillisHelper(850L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("BloomSampler")
      .withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private static int biteSkullIndex = 0;
   private static final Random RNG = new Random();
   private float clock;
   private static final int SEAL_PARTICLES = 16;
   private final SpawnFallbackHelper deathDissolve;
   private final List<TargetParticleRenderer.Sprite> glowSprites;
   private static final RenderPipeline ALPHA_THROUGH_WALLS = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/deadhead_alpha"))
      .withVertexShader(Identifier.parse("skycore:core/world_particle"))
      .withFragmentShader(Identifier.parse("skycore:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private long lastFrameNanos;
   private LivingEntity lastTarget;
   private boolean animationTarget;

   private static int darken(int value, float value2) {
      int value3 = value >> 24 & 0xFF;
      int computedValue = (int)((value >> 16 & 0xFF) * value2);
      int computedValue2 = (int)((value >> 8 & 0xFF) * value2);
      int computedValue3 = (int)((value & 0xFF) * value2);
      return value3 << 24 | computedValue << 16 | computedValue2 << 8 | computedValue3;
   }

   private void render3DMesh(Minecraft minecraft, List<DarkenHelper.Quad3D> quad3Ds, Vec3 position, Matrix4f matrix) {
      if (!quad3Ds.isEmpty() && minecraft.level != null) {
         RenderTarget renderTarget = minecraft.gameRenderer.mainRenderTarget();
         GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
         if (colorTextureView != null) {
            AbstractTexture texture = minecraft.getTextureManager().getTexture(ClientTextures.BLOOM);
            GpuTextureView textureView = texture != null ? texture.getTextureView() : null;
            if (textureView != null) {
               BufferBuilder bufferBuilder = new BufferBuilder(
                  ByteBufferBuilder.exactlySized(quad3Ds.size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()),
                  PrimitiveTopology.TRIANGLES,
                  DefaultVertexFormat.POSITION_TEX_COLOR
               );

               for (DarkenHelper.Quad3D quad3D : quad3Ds) {
                  Vector4f vector4f = CameraViewPoseHelper.toViewSpace(quad3D.p0, position, matrix);
                  Vector4f vector4f2 = CameraViewPoseHelper.toViewSpace(quad3D.p1, position, matrix);
                  Vector4f vector4f3 = CameraViewPoseHelper.toViewSpace(quad3D.p2, position, matrix);
                  Vector4f vector4f4 = CameraViewPoseHelper.toViewSpace(quad3D.p3, position, matrix);
                  bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setUv(0.0F, 0.0F).setColor(quad3D.color);
                  bufferBuilder.addVertex(vector4f2.x, vector4f2.y, vector4f2.z).setUv(1.0F, 0.0F).setColor(quad3D.color);
                  bufferBuilder.addVertex(vector4f3.x, vector4f3.y, vector4f3.z).setUv(1.0F, 1.0F).setColor(quad3D.color);
                  bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setUv(0.0F, 0.0F).setColor(quad3D.color);
                  bufferBuilder.addVertex(vector4f3.x, vector4f3.y, vector4f3.z).setUv(1.0F, 1.0F).setColor(quad3D.color);
                  bufferBuilder.addVertex(vector4f4.x, vector4f4.y, vector4f4.z).setUv(0.0F, 1.0F).setColor(quad3D.color);
               }

               MeshData meshData = bufferBuilder.build();
               if (meshData != null) {
                  GpuDevice device = RenderSystem.getDevice();
                  GpuBuffer gpuBuffer = null;
                  GpuBuffer gpuBuffer2 = null;

                  try {
                     gpuBuffer = device.createBuffer(() -> {
                        return "Skycore 3D Deadhead Vertices";
                     }, 40, meshData.vertexBuffer());
                     gpuBuffer2 = this.uploadUniform(1.0F);
                     GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                     RenderPass renderPass = device.createCommandEncoder().createRenderPass(() -> {
                        return "Skycore 3D Deadhead Pass";
                     }, colorTextureView, Optional.empty());

                     try {
                        renderPass.setPipeline(ALPHA_THROUGH_WALLS);
                        renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        renderPass.setUniform("WorldParticleUniforms", gpuBuffer2);
                        renderPass.bindTexture("BloomSampler", textureView, clampToEdge);
                        renderPass.setVertexBuffer(0, gpuBuffer.slice());
                        renderPass.draw(quad3Ds.size() * 6, 1, 0, 0);
                     } finally {
                        renderPass.close();
                     }
                  } finally {
                     if (gpuBuffer2 != null) {
                        gpuBuffer2.close();
                     }

                     if (gpuBuffer != null) {
                        gpuBuffer.close();
                     }

                     meshData.close();
                  }
               }
            }
         }
      }
   }

   public void release() {
      this.reset();
   }

   public void render(LivingEntity lastTarget2, float coordinate, int coordinate2) {
      long nowNanos = System.nanoTime();
      long now = System.currentTimeMillis();
      float computedValue = this.lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowNanos;
      boolean valid = valid(lastTarget2);
      if (valid) {
         this.lastTarget = lastTarget2;
         if (lastTarget2.getId() == this.dissolvedTargetId) {
            this.dissolvedTargetId = Integer.MIN_VALUE;
         }
      }

      if (dead(this.lastTarget) && this.lastTarget.getId() != this.dissolvedTargetId) {
         this.deathDissolve.burst(this.glowSprites, this.lastTarget, coordinate2, now);
         this.dissolvedTargetId = this.lastTarget.getId();
         valid = false;
      }

      this.updateAnimations((boolean)valid);
      float value = this.appear.getValue();
      float value2 = this.inner.getValue();
      if (this.lastTarget != null && !(value <= 0.01F) && !(value2 <= 0.001F)) {
         this.clock += computedValue * 1000.0F;
         float computedValue2 = this.clock / 1000.0F;
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.lastTarget, coordinate);
         float alpha = Math.min(0.95F, value2 * 1.6F) * value;
         int value3 = EasedFactorHelper.blend(coordinate2, this.lastTarget, 1.0F);
         int alpha2 = WriteHexByteHelper.lerp(value3, -16711800, 0.4F);
         this.glowSprites.clear();
         float computedValue3 = (0.85F + 0.12F * Mth.sin(computedValue2 * 3.0F)) * value2;

         for (int value4 = 0; value4 < 16; value4++) {
            float computedValue4 = computedValue2 * 1.8F + (float)(value4 * Math.PI * 2.0 / 16.0);
            this.glowSprites
               .add(
                  new TargetParticleRenderer.Sprite(
                     new Vec3(position.x + computedValue3 * Mth.sin(computedValue4), position.y + 0.05 + 0.03 * Mth.sin(computedValue2 * 4.0F + value4), position.z + computedValue3 * Mth.cos(computedValue4)),
                     0.08F * value2,
                     WriteHexByteHelper.multiplyAlpha(alpha2, alpha * 0.45F)
                  )
               );
         }

         long timestamp = now - lastBiteTime;
         float computedValue5 = timestamp >= 0L && timestamp <= 380L ? (float)timestamp / 380.0F : -1.0F;
         Vec3 bbHeight = position.add(0.0, this.lastTarget.getBbHeight() * 0.55, 0.0);
         Minecraft instance = Minecraft.getInstance();
         Camera camera = instance.gameRenderer.mainCamera();
         Vec3 position2 = camera.position();
         Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
         ArrayList elements = new ArrayList(180);
         float computedValue6 = 1.85F * (0.8F + 0.2F * value2);

         for (int value5 = 0; value5 < 3; value5++) {
            float computedValue7 = (float)(value5 * Math.PI * 2.0 / 3.0);
            float computedValue8 = computedValue2 * 2.2F + computedValue7;
            double sine = 0.4 * Mth.sin(computedValue2 * 3.6F + computedValue7 * 1.4F);
            Vec3 position3 = new Vec3(position.x + computedValue6 * Mth.sin(computedValue8), position.y + this.lastTarget.getBbHeight() * 0.52 + sine, position.z + computedValue6 * Mth.cos(computedValue8));
            float computedValue9 = 1.56F * value2;
            float value6 = 0.0F;
            float computedValue10 = -computedValue8 + (float) (Math.PI / 2);
            if (computedValue5 >= 0.0F && value5 == biteSkullIndex) {
               float sine2 = (float)Math.sin(computedValue5 * Math.PI);
               position3 = position3.lerp(bbHeight, sine2 * 0.88);
               computedValue9 *= 1.0F + sine2 * 0.6F;
               value6 = 0.18F * sine2;
               if (computedValue5 >= 0.35F && computedValue5 <= 0.65F) {
                  for (int value7 = 0; value7 < 4; value7++) {
                     this.glowSprites
                        .add(
                           new TargetParticleRenderer.Sprite(
                              bbHeight.add((RNG.nextFloat() - 0.5) * 0.4, (RNG.nextFloat() - 0.5) * 0.4, (RNG.nextFloat() - 0.5) * 0.4),
                              0.12F * value2,
                              WriteHexByteHelper.multiplyAlpha(-16711800, alpha)
                           )
                        );
                  }
               }
            }

            for (int value8 = 1; value8 <= 8; value8++) {
               float computedValue11 = computedValue8 - value8 * 0.08F;
               double sine3 = 0.4 * Mth.sin(computedValue2 * 3.6F + computedValue7 * 1.4F - value8 * 0.1F);
               float alpha3 = alpha * (1.0F - value8 / 8.0F) * 0.35F;
               if (alpha3 > 0.005F) {
                  this.glowSprites
                     .add(
                        new TargetParticleRenderer.Sprite(
                           new Vec3(position.x + computedValue6 * Mth.sin(computedValue11), position.y + this.lastTarget.getBbHeight() * 0.52 + sine3, position.z + computedValue6 * Mth.cos(computedValue11)),
                           computedValue9 * (1.0F - value8 * 0.04F),
                           WriteHexByteHelper.multiplyAlpha(alpha2, alpha3)
                        )
                     );
               }
            }

            int computedValue12 = packARGB((int)(alpha * 230.0F), 215, 255, 225);
            int computedValue13 = packARGB((int)(alpha * 240.0F), 10, 20, 15);
            int computedValue14 = packARGB((int)(alpha * 240.0F), 245, 255, 250);
            add3DBox(elements, position3, computedValue9 * 0.32F, computedValue9 * 0.28F, computedValue9 * 0.32F, 0.0F, computedValue9 * 0.1F, 0.0F, computedValue10, computedValue12);
            add3DBox(elements, position3, computedValue9 * 0.24F, computedValue9 * 0.14F, computedValue9 * 0.18F, 0.0F, -computedValue9 * 0.08F, computedValue9 * 0.12F, computedValue10, computedValue12);
            add3DBox(elements, position3, computedValue9 * 0.08F, computedValue9 * 0.08F, computedValue9 * 0.05F, -computedValue9 * 0.08F, computedValue9 * 0.1F, computedValue9 * 0.14F, computedValue10, computedValue13);
            add3DBox(elements, position3, computedValue9 * 0.08F, computedValue9 * 0.08F, computedValue9 * 0.05F, computedValue9 * 0.08F, computedValue9 * 0.1F, computedValue9 * 0.14F, computedValue10, computedValue13);
            add3DBox(elements, position3, computedValue9 * 0.05F, computedValue9 * 0.06F, computedValue9 * 0.04F, 0.0F, computedValue9 * 0.01F, computedValue9 * 0.15F, computedValue10, computedValue13);
            add3DBox(elements, position3, computedValue9 * 0.22F, computedValue9 * 0.1F, computedValue9 * 0.24F, 0.0F, -computedValue9 * (0.2F + value6), computedValue9 * 0.06F, computedValue10, computedValue12);
            add3DBox(elements, position3, computedValue9 * 0.18F, computedValue9 * 0.04F, computedValue9 * 0.02F, 0.0F, -computedValue9 * 0.14F, computedValue9 * 0.2F, computedValue10, computedValue14);
            add3DBox(elements, position3, computedValue9 * 0.16F, computedValue9 * 0.04F, computedValue9 * 0.02F, 0.0F, -computedValue9 * (0.16F + value6), computedValue9 * 0.17F, computedValue10, computedValue14);
            Vec3 position4 = rotateOffset(position3, -computedValue9 * 0.08F, computedValue9 * 0.1F, computedValue9 * 0.16F, computedValue10);
            Vec3 position5 = rotateOffset(position3, computedValue9 * 0.08F, computedValue9 * 0.1F, computedValue9 * 0.16F, computedValue10);
            this.glowSprites.add(new TargetParticleRenderer.Sprite(position4, computedValue9 * 0.18F, WriteHexByteHelper.multiplyAlpha(-16711732, alpha * 0.9F)));
            this.glowSprites.add(new TargetParticleRenderer.Sprite(position5, computedValue9 * 0.18F, WriteHexByteHelper.multiplyAlpha(-16711732, alpha * 0.9F)));
         }

         this.render3DMesh(instance, elements, position2, matrix);
         this.glowRenderer.render(this.glowSprites, 1.0F, true, true);
         this.deathDissolve.render(computedValue, now);
      } else {
         if (!valid && value <= 0.01F) {
            this.lastTarget = null;
            this.glowSprites.clear();
         }

         this.deathDissolve.render(computedValue, now);
      }
   }

   public static RenderPipeline pipeline() {
      return ALPHA_THROUGH_WALLS;
   }

   public DarkenHelper() {
      this.glowRenderer = new TargetParticleRenderer();
      this.deathDissolve = new SpawnFallbackHelper();
      this.glowSprites = new ArrayList<>(120);
      this.dissolvedTargetId = Integer.MIN_VALUE;
   }

   private static boolean dead(LivingEntity entity) {
      if (entity != null) {
         if (entity.isDeadOrDying()) {
            return true;
         }

         if (entity.deathTime > 0 || entity.getHealth() <= 0.0F) {
            return true;
         }
      }

      return false;
   }

   private static int packARGB(int value, int value2, int value3, int value4) {
      return value << 24 | value2 << 16 | value3 << 8 | value4;
   }

   private void updateAnimations(boolean animationTarget2) {
      if (animationTarget2 != this.animationTarget) {
         this.animationTarget = animationTarget2;
         float elapsed = animationTarget2 ? 1.0F : 0.0F;
         this.appear
            .animate(
               this.appear.getValue(), elapsed, animationTarget2 ? 650L : 450L, animationTarget2 ? ElapsedMillisHelper.Easing.EASE_OUT_EXPO : ElapsedMillisHelper.Easing.EASE_OUT_QUAD
            );
         this.inner
            .animate(
               this.inner.getValue(), elapsed, animationTarget2 ? 850L : 500L, animationTarget2 ? ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD : ElapsedMillisHelper.Easing.EASE_OUT_QUAD
            );
      }
   }

   private static boolean valid(LivingEntity entity) {
      if (entity != null) {
         if (entity.isAlive() && !entity.isRemoved()) {
            return true;
         }
      }

      return false;
   }

   private GpuBuffer uploadUniform(float value) {
      GpuDevice device = RenderSystem.getDevice();
      Supplier<java.lang.String> supplier = () -> {
         return "Skycore 3D Deadhead UBO";
      };
      GpuBuffer gpuBuffer = device.createBuffer(supplier, 136, UNIFORM_SIZE);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UNIFORM_SIZE).putVec4(value, 0.0F, 0.0F, 0.0F).get();
         device.createCommandEncoder().writeToBuffer(gpuBuffer.slice(), byteBuffer);
      } catch (Throwable failure) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable failure2) {
               failure.addSuppressed(failure2);
            }
         }

         throw failure;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      return gpuBuffer;
   }

   private static void add3DBox(
      List<DarkenHelper.Quad3D> quad3Ds, Vec3 position, float value, float value2, float value3, float value4, float value5, float value6, float value7, int value8
   ) {
      float computedValue = value * 0.5F;
      float computedValue2 = value2 * 0.5F;
      float computedValue3 = value3 * 0.5F;
      Vec3[] vec3Values = new Vec3[]{
         rotateOffset(position, value4 - computedValue, value5 - computedValue2, value6 - computedValue3, value7),
         rotateOffset(position, value4 + computedValue, value5 - computedValue2, value6 - computedValue3, value7),
         rotateOffset(position, value4 + computedValue, value5 + computedValue2, value6 - computedValue3, value7),
         rotateOffset(position, value4 - computedValue, value5 + computedValue2, value6 - computedValue3, value7),
         rotateOffset(position, value4 - computedValue, value5 - computedValue2, value6 + computedValue3, value7),
         rotateOffset(position, value4 + computedValue, value5 - computedValue2, value6 + computedValue3, value7),
         rotateOffset(position, value4 + computedValue, value5 + computedValue2, value6 + computedValue3, value7),
         rotateOffset(position, value4 - computedValue, value5 + computedValue2, value6 + computedValue3, value7)
      };
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[4], vec3Values[5], vec3Values[6], vec3Values[7], value8));
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[1], vec3Values[0], vec3Values[3], vec3Values[2], darken(value8, 0.8F)));
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[3], vec3Values[2], vec3Values[6], vec3Values[7], lighten(value8, 1.15F)));
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[4], vec3Values[5], vec3Values[1], vec3Values[0], darken(value8, 0.65F)));
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[5], vec3Values[1], vec3Values[2], vec3Values[6], darken(value8, 0.88F)));
      quad3Ds.add(new DarkenHelper.Quad3D(vec3Values[0], vec3Values[4], vec3Values[7], vec3Values[3], darken(value8, 0.88F)));
   }

   public void reset() {
      this.lastTarget = null;
      this.dissolvedTargetId = Integer.MIN_VALUE;
      this.animationTarget = false;
      this.lastFrameNanos = 0L;
      this.clock = 0.0F;
      this.glowSprites.clear();
      this.deathDissolve.clear();
      this.appear.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
      this.inner.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
   }

   private static int lighten(int value, float value2) {
      int value3 = 1;
      int value4 = value >> 24 & 0xFF;
      int computedValue = Math.min(255, (int)((value >> 16 & 0xFF) * value2));
      value3 = (int)((value >> 8 & 0xFF) * value2);
      int minimum = Math.min(255, value3);
      value3 = (int)((value & 0xFF) * value2);
      int minimum2 = Math.min(255, value3);
      return value4 << 24 | computedValue << 16 | minimum << 8 | minimum2;
   }

   public static void triggerBite() {
      int value = 0;
      long now = System.currentTimeMillis();
      lastBiteTime = now;
      value = RNG.nextInt(3);
      biteSkullIndex = value;
   }

   private static Vec3 rotateOffset(Vec3 position, float value, float value2, float value3, float value4) {
      float cosine = Mth.cos(value4);
      double value5 = value4;
      float sine = Mth.sin(value5);
      double computedValue = value * cosine - value3 * sine;
      double computedValue2 = value * sine + value3 * cosine;
      double value6 = value2;
      return position.add(computedValue, value6, computedValue2);
   }

   public record Quad3D(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
      public Vec3 p1() {
         return this.p1;
      }

      public Vec3 p0() {
         return this.p0;
      }

      public int color() {
         return this.color;
      }

      public Vec3 p2() {
         return this.p2;
      }

      public Vec3 p3() {
         return this.p3;
      }
   }
}
