package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.module.render.JumpCircleModule;
import deluxe.client.ui.UiTheme;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class JumpCircleRenderer {
   private static JumpCircleRenderer jumpCircleRenderer;
   private static final RenderPipeline field14462 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/jump_circle"))
      .withVertexShader(DeluxeClient.id("core/jump_circle"))
      .withFragmentShader(DeluxeClient.id("core/jump_circle"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("JumpCircleParams", UniformType.UNIFORM_BUFFER)
      .build();
   private SimpleFramebuffer field14463;
   private GpuBuffer field14464;
   private GpuBuffer field14465;
   private long field14467;

   public void method4235() {
      if (this.field14463 != null) {
         this.field14463.delete();
         this.field14463 = null;
      }

      if (this.field14464 != null && !this.field14464.isClosed()) {
         this.field14464.close();
      }

      this.field14464 = null;
      if (this.field14465 != null && !this.field14465.isClosed()) {
         this.field14465.close();
      }

      this.field14465 = null;
   }

   private JumpCircleRenderer() {
   }

   public void method4237(Matrix4f matrix4f, Matrix4f matrix4f2) {
      JumpCircleModule jumpCircleModule = JumpCircleModule.method0833();
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (!jumpCircleModule.isEnabled() || minecraftClient.world == null || minecraftClient.player == null) {
         this.method4235();
      } else if (jumpCircleModule.method0831().isEmpty()) {
         if (this.field14467 == 0L) {
            this.field14467 = System.currentTimeMillis();
         } else if (System.currentTimeMillis() - this.field14467 > 5000L) {
            this.method4235();
         }
      } else {
         this.field14467 = 0L;
         Framebuffer framebuffer = minecraftClient.getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.getDepthAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            if (this.field14463 == null) {
               this.field14463 = new SimpleFramebuffer("deluxe_jump_circle_scene", framebuffer.textureWidth, framebuffer.textureHeight, false);
            }

            if (this.field14463.textureWidth != framebuffer.textureWidth || this.field14463.textureHeight != framebuffer.textureHeight) {
               this.field14463.resize(framebuffer.textureWidth, framebuffer.textureHeight);
            }

            if (this.field14464 == null || this.field14464.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.field14464 = RenderSystem.getDevice().createBuffer(() -> "deluxe_jump_circle_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.field14465 == null || this.field14465.isClosed()) {
               this.field14465 = RenderSystem.getDevice().createBuffer(() -> "deluxe_jump_circle_params", 136, 2416L);
            }

            Matrix4f matrix4f3 = new Matrix4f(matrix4f2).mul(matrix4f);
            Vec3d cameraPos = minecraftClient.gameRenderer.getCamera().getCameraPos();
            int local7108_8 = jumpCircleModule.field01495.isChecked() ? jumpCircleModule.colorSetting.getValue() : UiTheme.field09686;
            float local7108_9 = jumpCircleModule.field01489.getValue() + jumpCircleModule.field01490.getValue() * 3.0F + 0.5F;
            Vector4f vector4f = new Vector4f();
            float local7108_11 = 1.0F;
            float local7108_12 = 1.0F;
            float local7108_13 = -1.0F;
            float local7108_14 = -1.0F;
            boolean local7108_15 = false;
            int local7108_16 = 0;
            MemoryStack memoryStack = MemoryStack.stackPush();

            label295: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(2416);
                  new Matrix4f(matrix4f3).invert().get(0, byteBuffer);
                  byteBuffer.putFloat(64, jumpCircleModule.field01489.getValue());
                  byteBuffer.putFloat(68, jumpCircleModule.field01490.getValue());
                  byteBuffer.putFloat(76, jumpCircleModule.field01491.getValue());
                  byteBuffer.putFloat(80, (local7108_8 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(84, (local7108_8 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(88, (local7108_8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(92, 1.0F);
                  byteBuffer.putFloat(96, jumpCircleModule.field01493.isChecked() ? 1.0F : 0.0F);
                  byteBuffer.putFloat(100, jumpCircleModule.modeSetting.isSelected("Вода") ? 1.0F : 0.0F);
                  Iterator iterator = jumpCircleModule.method0831().iterator();

                  label215:
                  while (true) {
                     JumpCircleModule.Nested0362 local7108_20;
                     float local7108_21;
                     float local7108_22;
                     float local7108_23;
                     while (true) {
                        if (!iterator.hasNext()) {
                           break label215;
                        }

                        local7108_20 = (JumpCircleModule.Nested0362)iterator.next();
                        if (local7108_16 >= 16) {
                           break label215;
                        }

                        local7108_21 = (float)(local7108_20.x() - cameraPos.x);
                        local7108_22 = (float)(local7108_20.y() - cameraPos.y);
                        local7108_23 = (float)(local7108_20.z() - cameraPos.z);
                        vector4f.set(local7108_21, local7108_22, local7108_23, 1.0F);
                        matrix4f3.transform(vector4f);
                        if (!(vector4f.w <= 0.05F)) {
                           boolean local7108_24 = false;
                           float local7108_25 = 1.0F;
                           float local7108_26 = 1.0F;
                           float local7108_27 = -1.0F;
                           float local7108_28 = -1.0F;

                           for (int index = 0; index < 8; index++) {
                              vector4f.set(
                                 (index & 1) == 0 ? local7108_21 - local7108_9 : local7108_21 + local7108_9,
                                 (index & 2) == 0 ? local7108_22 - 1.35F : local7108_22 + 1.35F,
                                 (index & 4) == 0 ? local7108_23 - local7108_9 : local7108_23 + local7108_9,
                                 1.0F
                              );
                              matrix4f3.transform(vector4f);
                              if (vector4f.w <= 0.05F) {
                                 local7108_24 = true;
                                 break;
                              }

                              local7108_25 = Math.min(local7108_25, vector4f.x / vector4f.w);
                              local7108_27 = Math.max(local7108_27, vector4f.x / vector4f.w);
                              local7108_26 = Math.min(local7108_26, vector4f.y / vector4f.w);
                              local7108_28 = Math.max(local7108_28, vector4f.y / vector4f.w);
                           }

                           if (local7108_24) {
                              local7108_15 = true;
                              break;
                           }

                           if (!(local7108_27 < -1.0F) && !(local7108_25 > 1.0F) && !(local7108_28 < -1.0F) && !(local7108_26 > 1.0F)) {
                              local7108_11 = Math.min(local7108_11, local7108_25);
                              local7108_13 = Math.max(local7108_13, local7108_27);
                              local7108_12 = Math.min(local7108_12, local7108_26);
                              local7108_14 = Math.max(local7108_14, local7108_28);
                              break;
                           }
                        }
                     }

                     byteBuffer.putFloat(112 + local7108_16 * 16, local7108_21);
                     byteBuffer.putFloat(116 + local7108_16 * 16, local7108_22);
                     byteBuffer.putFloat(120 + local7108_16 * 16, local7108_23);
                     byteBuffer.putFloat(124 + local7108_16 * 16, jumpCircleModule.method0835(local7108_20));

                     for (int index2 = 0; index2 < 32; index2++) {
                        byteBuffer.putFloat(368 + local7108_16 * 128 + index2 * 4, local7108_20.wallDistBlocks()[index2]);
                     }

                     local7108_16++;
                  }

                  if (local7108_16 == 0) {
                     break label295;
                  }

                  byteBuffer.putFloat(72, local7108_16);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14465.slice(0L, 2416L), byteBuffer);
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

               int local7108_17x = 0;
               int local7108_18 = 0;
               int local7108_19 = framebuffer.textureWidth;
               int local7108_20 = framebuffer.textureHeight;
               if (!local7108_15) {
                  local7108_17x = Math.max(0, (int)Math.floor((Math.max(local7108_11, -1.0F) * 0.5F + 0.5F) * framebuffer.textureWidth) - 8);
                  local7108_18 = Math.max(0, (int)Math.floor((Math.max(local7108_12, -1.0F) * 0.5F + 0.5F) * framebuffer.textureHeight) - 8);
                  local7108_19 = Math.min(framebuffer.textureWidth, (int)Math.ceil((Math.min(local7108_13, 1.0F) * 0.5F + 0.5F) * framebuffer.textureWidth) + 8)
                     - local7108_17x;
                  local7108_20 = Math.min(
                        framebuffer.textureHeight, (int)Math.ceil((Math.min(local7108_14, 1.0F) * 0.5F + 0.5F) * framebuffer.textureHeight) + 8
                     )
                     - local7108_18;
                  if (local7108_19 <= 0 || local7108_20 <= 0) {
                     return;
                  }
               }

               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .copyTextureToTexture(
                     framebuffer.getColorAttachment(), this.field14463.getColorAttachment(), 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight
                  );

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(() -> "deluxe_jump_circle", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                  renderPass.setPipeline(field14462);
                  renderPass.enableScissor(local7108_17x, local7108_18, local7108_19, local7108_20);
                  renderPass.bindTexture("Sampler0", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                  renderPass.bindTexture("Sampler1", this.field14463.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                  renderPass.setUniform("JumpCircleParams", this.field14465);
                  renderPass.setVertexBuffer(0, this.field14464);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, 6, 1);
               }

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public static JumpCircleRenderer method4238() {
      if (jumpCircleRenderer == null) {
         jumpCircleRenderer = new JumpCircleRenderer();
      }

      return jumpCircleRenderer;
   }
}
