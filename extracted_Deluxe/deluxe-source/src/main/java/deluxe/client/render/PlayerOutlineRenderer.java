package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.render.WorldProjection;
import deluxe.client.cosmetic.BedrockVertexCollector;
import deluxe.client.module.render.TagsModule;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class PlayerOutlineRenderer {
   private static PlayerOutlineRenderer playerOutlineRenderer;
   private static final RenderPipeline field14930 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/player_outline"))
      .withVertexShader(DeluxeClient.id("core/player_outline"))
      .withFragmentShader(DeluxeClient.id("core/player_outline"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("PlayerOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field14931 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/player_outline_blur"))
      .withVertexShader(DeluxeClient.id("core/player_outline_blur"))
      .withFragmentShader(DeluxeClient.id("core/player_outline_blur"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("PlayerOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field14932 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/hitbox_mask"))
      .withVertexShader(DeluxeClient.id("core/hitbox_mask"))
      .withFragmentShader(DeluxeClient.id("core/hitbox_mask"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("HitboxMaskParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field14933 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/cosmetic_mask"))
      .withVertexShader(DeluxeClient.id("core/cosmetic_mask"))
      .withFragmentShader(DeluxeClient.id("core/cosmetic_mask"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("HitboxMaskParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final int[][] field14934 = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
   private SimpleFramebuffer field14939;
   private GpuBuffer field14940;
   private GpuBuffer field14941;
   private GpuBuffer field14942;
   private GpuBuffer field14943;
   private GpuBuffer field14944;
   private BufferAllocator field14945;
   private BufferAllocator field14946;
   private final ArrayList<Box> field14947 = new ArrayList<>();
   private final Matrix4f field14948 = new Matrix4f();
   private final Vector4f field14949 = new Vector4f();
   private boolean field14950;
   private boolean field14951;
   private float field14952;
   private float field14953;
   private float field14954;
   private float field14955;
   private boolean field14956;
   private float field14957;
   private float field14958;
   private float field14959;
   private float field14960;

   public void method4375() {
      if (this.field14939 != null) {
         this.field14939.delete();
         this.field14939 = null;
      }

      if (this.field14945 != null) {
         this.field14945.close();
         this.field14945 = null;
      }

      if (this.field14946 != null) {
         this.field14946.close();
         this.field14946 = null;
      }

      this.field14947.clear();
      this.field14950 = false;
      this.field14951 = false;
      if (this.field14940 != null && !this.field14940.isClosed()) {
         this.field14940.close();
      }

      this.field14940 = null;
      if (this.field14941 != null && !this.field14941.isClosed()) {
         this.field14941.close();
      }

      this.field14941 = null;
      if (this.field14942 != null && !this.field14942.isClosed()) {
         this.field14942.close();
      }

      this.field14942 = null;
      if (this.field14943 != null && !this.field14943.isClosed()) {
         this.field14943.close();
      }

      this.field14943 = null;
      if (this.field14944 != null && !this.field14944.isClosed()) {
         this.field14944.close();
      }

      this.field14944 = null;
      BedrockVertexCollector.method2287();
   }

   private PlayerOutlineRenderer() {
   }

   public void method4376(Framebuffer framebuffer) {
      int local7385_2 = BedrockVertexCollector.method2295();
      this.field14950 = false;
      this.field14951 = false;
      if (local7385_2 != 0 && framebuffer != null && framebuffer.getColorAttachmentView() != null && TagsModule.method4106().method4095()) {
         if (this.field14942 == null || this.field14942.isClosed()) {
            this.field14942 = RenderSystem.getDevice().createBuffer(() -> "deluxe_hitbox_mask_params", 136, 80L);
         }

         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(80);
            this.field14948.set(WorldProjection.method1120()).mul(WorldProjection.method1126()).get(0, byteBuffer);
            byteBuffer.putFloat(64, 1.0F);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14942.slice(0L, 80L), byteBuffer);
         } catch (Throwable throwable) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable throwable2) {
                  throwable.addSuppressed(throwable2);
               }
            }

            throw throwable;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         if (this.field14946 == null) {
            this.field14946 = new BufferAllocator(Math.max(256, local7385_2) * 4 * VertexFormats.POSITION_TEXTURE.getVertexSize());
         }

         CloseableBuffer local7385_3x = this.field14946.getAllocated();
         if (local7385_3x != null) {
            local7385_3x.close();
         }

         BufferBuilder bufferBuilder = new BufferBuilder(this.field14946, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

         for (int index = 0; index < local7385_2 * 4; index++) {
            int local7385_6 = index * 5;
            bufferBuilder.vertex(
                  BedrockVertexCollector.method2290(local7385_6), BedrockVertexCollector.method2290(local7385_6 + 1), BedrockVertexCollector.method2290(local7385_6 + 2)
               )
               .texture(BedrockVertexCollector.method2290(local7385_6 + 3), BedrockVertexCollector.method2290(local7385_6 + 4));
            this.method4384(
               BedrockVertexCollector.method2290(local7385_6), BedrockVertexCollector.method2290(local7385_6 + 1), BedrockVertexCollector.method2290(local7385_6 + 2)
            );
         }

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            if (this.field14944 != null && !this.field14944.isClosed() && this.field14944.size() >= builtBuffer.getBuffer().remaining()) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToBuffer(this.field14944.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
            } else {
               if (this.field14944 != null && !this.field14944.isClosed()) {
                  this.field14944.close();
               }

               this.field14944 = RenderSystem.getDevice().createBuffer(() -> "deluxe_cosmetic_mask_mesh", 40, builtBuffer.getBuffer());
            }

            for (int index2 = 0; index2 < BedrockVertexCollector.method2293(); index2++) {
               if (BedrockVertexCollector.method2292(index2) != 0) {
                  GpuTextureView glTextureView = MinecraftClient.getInstance()
                     .getTextureManager()
                     .getTexture(BedrockVertexCollector.method2286(index2))
                     .getGlTextureView();
                  if (glTextureView != null) {
                     try (RenderPass renderPass = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "deluxe_cosmetic_mask", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        renderPass.setPipeline(field14933);
                        renderPass.bindTexture("Sampler0", glTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        renderPass.setUniform("HitboxMaskParams", this.field14942);
                        renderPass.setVertexBuffer(0, this.field14944);
                        renderPass.setIndexBuffer(
                           RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local7385_2 * 6),
                           RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                        );
                        renderPass.drawIndexed(0, BedrockVertexCollector.method2289(index2) * 6, BedrockVertexCollector.method2292(index2) * 6, 1);
                     }
                  }
               }
            }
         }

         BedrockVertexCollector.method2287();
      } else {
         BedrockVertexCollector.method2287();
      }
   }

   public void method4378(Framebuffer framebuffer, boolean local7387_2) {
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && MinecraftClient.getInstance().world != null
         && MinecraftClient.getInstance().gameRenderer != null) {
         ArrayList<Box> local7387_3 = this.field14947;
         local7387_3.clear();

         for (Entity entity : MinecraftClient.getInstance().world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (abstractClientPlayerEntity != MinecraftClient.getInstance().player || !MinecraftClient.getInstance().options.getPerspective().isFirstPerson())
               && TagsModule.method4106().method4105(abstractClientPlayerEntity)) {
               local7387_3.add(
                  abstractClientPlayerEntity.getBoundingBox()
                     .offset(
                        abstractClientPlayerEntity.getLerpedPos(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false))
                           .subtract(abstractClientPlayerEntity.getEntityPos())
                           .subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos())
                     )
               );
            }
         }

         if (!local7387_3.isEmpty()) {
            if (this.field14942 == null || this.field14942.isClosed()) {
               this.field14942 = RenderSystem.getDevice().createBuffer(() -> "deluxe_hitbox_mask_params", 136, 80L);
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(80);
               this.field14948.set(WorldProjection.method1120()).mul(WorldProjection.method1126()).get(0, byteBuffer);
               byteBuffer.putFloat(64, 1.0F);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14942.slice(0L, 80L), byteBuffer);
            } catch (Throwable throwable) {
               if (memoryStack != null) {
                  try {
                     memoryStack.close();
                  } catch (Throwable throwable2) {
                     throwable.addSuppressed(throwable2);
                  }
               }

               throw throwable;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }

            if (this.field14945 == null) {
               this.field14945 = new BufferAllocator(384 * VertexFormats.POSITION.getVertexSize());
            }

            CloseableBuffer local7387_4x = this.field14945.getAllocated();
            if (local7387_4x != null) {
               local7387_4x.close();
            }

            BufferBuilder bufferBuilder = new BufferBuilder(this.field14945, DrawMode.QUADS, VertexFormats.POSITION);

            for (Box box : local7387_3) {
               for (int[] intArray : field14934) {
                  for (int local7387_15 : intArray) {
                     bufferBuilder.vertex(
                        (float)(local7387_15 >> 2 == 0 ? box.minX : box.maxX),
                        (float)((local7387_15 >> 1 & 1) == 0 ? box.minY : box.maxY),
                        (float)((local7387_15 & 1) == 0 ? box.minZ : box.maxZ)
                     );
                  }
               }
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               if (this.field14943 != null && !this.field14943.isClosed() && this.field14943.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.field14943.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.field14943 != null && !this.field14943.isClosed()) {
                     this.field14943.close();
                  }

                  this.field14943 = RenderSystem.getDevice().createBuffer(() -> "deluxe_hitbox_mask_mesh", 40, builtBuffer.getBuffer());
               }

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_hitbox_mask",
                        framebuffer.getColorAttachmentView(),
                        local7387_2 ? OptionalInt.of(0) : OptionalInt.empty(),
                        null,
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field14932);
                  renderPass.setUniform("HitboxMaskParams", this.field14942);
                  renderPass.setVertexBuffer(0, this.field14943);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local7387_3.size() * 36),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local7387_3.size() * 36, 1);
               }
            }
         }
      }
   }

   private void method4382(RenderPass renderPass, int[] intArray) {
      if (!this.field14956) {
         renderPass.enableScissor(intArray[0], intArray[1], intArray[2], intArray[3]);
      }

      renderPass.setVertexBuffer(0, this.field14940);
      renderPass.setIndexBuffer(
         RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
      );
      renderPass.drawIndexed(0, 0, 6, 1);
   }

   private void method4384(float local7394_1, float local7394_2, float local7394_3) {
      if (!this.field14951) {
         this.field14949.set(local7394_1, local7394_2, local7394_3, 1.0F);
         this.field14948.transform(this.field14949);
         if (this.field14949.w <= 0.05F) {
            this.field14951 = true;
         } else {
            float local7394_4 = this.field14949.x / this.field14949.w;
            float local7394_5 = this.field14949.y / this.field14949.w;
            if (!this.field14950) {
               this.field14952 = this.field14954 = local7394_4;
               this.field14953 = this.field14955 = local7394_5;
               this.field14950 = true;
            } else {
               this.field14952 = Math.min(this.field14952, local7394_4);
               this.field14954 = Math.max(this.field14954, local7394_4);
               this.field14953 = Math.min(this.field14953, local7394_5);
               this.field14955 = Math.max(this.field14955, local7394_5);
            }
         }
      }
   }

   private int[] method4385(int local7395_1, int local7395_2, int local7395_3) {
      if (this.field14956) {
         return new int[]{0, 0, local7395_2, local7395_3};
      }

      float local7395_4 = Math.max(Math.min(this.field14957, 4.0F), -4.0F);
      float local7395_5 = Math.max(Math.min(this.field14959, 4.0F), -4.0F);
      float local7395_6 = Math.max(Math.min(this.field14958, 4.0F), -4.0F);
      float local7395_7 = Math.max(Math.min(this.field14960, 4.0F), -4.0F);
      int local7395_8 = Math.max(0, (int)Math.floor((local7395_4 * 0.5F + 0.5F) * local7395_2) - local7395_1);
      int local7395_9 = Math.min(local7395_2, (int)Math.ceil((local7395_5 * 0.5F + 0.5F) * local7395_2) + local7395_1);
      int local7395_10 = Math.max(0, (int)Math.floor((local7395_6 * 0.5F + 0.5F) * local7395_3) - local7395_1);
      int local7395_11 = Math.min(local7395_3, (int)Math.ceil((local7395_7 * 0.5F + 0.5F) * local7395_3) + local7395_1);
      return local7395_9 > local7395_8 && local7395_11 > local7395_10
         ? new int[]{local7395_8, local7395_10, local7395_9 - local7395_8, local7395_11 - local7395_10}
         : null;
   }

   public static PlayerOutlineRenderer method4386() {
      if (playerOutlineRenderer == null) {
         playerOutlineRenderer = new PlayerOutlineRenderer();
      }

      return playerOutlineRenderer;
   }

   private boolean method4390() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      this.field14956 = this.field14951;
      if (this.field14956) {
         return true;
      }

      boolean local7400_2 = this.field14950;
      if (local7400_2) {
         this.field14957 = this.field14952;
         this.field14959 = this.field14954;
         this.field14958 = this.field14953;
         this.field14960 = this.field14955;
      }

      if (minecraftClient.world != null && minecraftClient.gameRenderer != null) {
         this.field14948.set(WorldProjection.method1120()).mul(WorldProjection.method1126());
         float tickProgress = minecraftClient.getRenderTickCounter().getTickProgress(false);
         double local7400_4 = minecraftClient.gameRenderer.getCamera().getCameraPos().x;
         double local7400_6 = minecraftClient.gameRenderer.getCamera().getCameraPos().y;
         double local7400_8 = minecraftClient.gameRenderer.getCamera().getCameraPos().z;

         for (Entity entity : minecraftClient.world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (abstractClientPlayerEntity != minecraftClient.player || !minecraftClient.options.getPerspective().isFirstPerson())
               && TagsModule.method4106().method4105(abstractClientPlayerEntity)) {
               Box boundingBox = abstractClientPlayerEntity.getBoundingBox();
               double local7400_14 = abstractClientPlayerEntity.getLerpedPos(tickProgress).x - abstractClientPlayerEntity.getEntityPos().x - local7400_4;
               double local7400_16 = abstractClientPlayerEntity.getLerpedPos(tickProgress).y - abstractClientPlayerEntity.getEntityPos().y - local7400_6;
               double local7400_18 = abstractClientPlayerEntity.getLerpedPos(tickProgress).z - abstractClientPlayerEntity.getEntityPos().z - local7400_8;
               float local7400_20 = (float)(boundingBox.minX + local7400_14 - 2.5);
               float local7400_21 = (float)(boundingBox.maxX + local7400_14 + 2.5);
               float local7400_22 = (float)(boundingBox.minY + local7400_16 - 2.0);
               float local7400_23 = (float)(boundingBox.maxY + local7400_16 + 2.0);
               float local7400_24 = (float)(boundingBox.minZ + local7400_18 - 2.5);
               float local7400_25 = (float)(boundingBox.maxZ + local7400_18 + 2.5);

               for (int index = 0; index < 8; index++) {
                  this.field14949
                     .set(
                        (index & 4) == 0 ? local7400_20 : local7400_21,
                        (index & 2) == 0 ? local7400_22 : local7400_23,
                        (index & 1) == 0 ? local7400_24 : local7400_25,
                        1.0F
                     );
                  this.field14948.transform(this.field14949);
                  if (this.field14949.w <= 0.05F) {
                     this.field14956 = true;
                     return true;
                  }

                  float local7400_27 = this.field14949.x / this.field14949.w;
                  float local7400_28 = this.field14949.y / this.field14949.w;
                  if (!local7400_2) {
                     this.field14957 = this.field14959 = local7400_27;
                     this.field14958 = this.field14960 = local7400_28;
                     local7400_2 = true;
                  } else {
                     this.field14957 = Math.min(this.field14957, local7400_27);
                     this.field14959 = Math.max(this.field14959, local7400_27);
                     this.field14958 = Math.min(this.field14958, local7400_28);
                     this.field14960 = Math.max(this.field14960, local7400_28);
                  }
               }
            }
         }

         return local7400_2;
      } else {
         this.field14956 = true;
         return true;
      }
   }

   public void method4391(Framebuffer framebuffer) {
      if (TagsModule.method4106().method4095()) {
         Framebuffer framebuffer2 = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer2 != null && framebuffer2.getColorAttachmentView() != null && framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            if (framebuffer.textureWidth == framebuffer2.textureWidth && framebuffer.textureHeight == framebuffer2.textureHeight) {
               this.field14956 = true;
               this.field14950 = false;
               this.field14951 = false;
               int[] intArray = this.method4385(50, framebuffer2.textureWidth, framebuffer2.textureHeight);
               if (intArray != null) {
                  int[] intArray2 = this.method4385(100, framebuffer2.textureWidth, framebuffer2.textureHeight);
                  if (intArray2 != null) {
                     if (this.field14940 == null || this.field14940.isClosed()) {
                        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                           BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                           bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                           bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                           bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                           bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                           try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                              this.field14940 = RenderSystem.getDevice().createBuffer(() -> "deluxe_player_outline_quad", 32, builtBuffer.getBuffer());
                           }
                        }
                     }

                     if (this.field14941 == null || this.field14941.isClosed()) {
                        this.field14941 = RenderSystem.getDevice().createBuffer(() -> "deluxe_player_outline_params", 136, 48L);
                     }

                     MemoryStack memoryStack = MemoryStack.stackPush();

                     try {
                        ByteBuffer byteBuffer = memoryStack.calloc(48);
                        byteBuffer.putFloat(0, (TagsModule.method4106().method4101() >> 16 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(4, (TagsModule.method4106().method4101() >> 8 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(8, (TagsModule.method4106().method4101() & 0xFF) / 255.0F);
                        byteBuffer.putFloat(12, TagsModule.method4106().field13993.getValue());
                        byteBuffer.putFloat(24, TagsModule.method4106().field13991.getValue());
                        byteBuffer.putFloat(28, TagsModule.method4106().field13992.getValue());
                        byteBuffer.putFloat(32, 1.0F / framebuffer2.textureWidth);
                        byteBuffer.putFloat(36, 1.0F / framebuffer2.textureHeight);
                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14941.slice(0L, 48L), byteBuffer);
                     } catch (Throwable throwable) {
                        if (memoryStack != null) {
                           try {
                              memoryStack.close();
                           } catch (Throwable throwable2) {
                              throwable.addSuppressed(throwable2);
                           }
                        }

                        throw throwable;
                     }

                     if (memoryStack != null) {
                        memoryStack.close();
                     }

                     if (this.field14939 == null) {
                        this.field14939 = new SimpleFramebuffer("deluxe_player_outline_blur", framebuffer2.textureWidth, framebuffer2.textureHeight, false);
                     }

                     if (this.field14939.textureWidth != framebuffer2.textureWidth || this.field14939.textureHeight != framebuffer2.textureHeight) {
                        this.field14939.resize(framebuffer2.textureWidth, framebuffer2.textureHeight);
                     }

                     try (RenderPass local7401_5x = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "deluxe_player_outline_blur", this.field14939.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        local7401_5x.setPipeline(field14931);
                        local7401_5x.bindTexture("Sampler0", framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local7401_5x.setUniform("PlayerOutlineParams", this.field14941);
                        this.method4382(local7401_5x, intArray2);
                     }

                     try (RenderPass local7401_5x = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "deluxe_player_outline", framebuffer2.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        local7401_5x.setPipeline(field14930);
                        local7401_5x.bindTexture("Sampler0", this.field14939.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local7401_5x.bindTexture("Sampler1", framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local7401_5x.setUniform("PlayerOutlineParams", this.field14941);
                        this.method4382(local7401_5x, intArray);
                     }
                  }
               }
            }
         }
      }
   }
}
