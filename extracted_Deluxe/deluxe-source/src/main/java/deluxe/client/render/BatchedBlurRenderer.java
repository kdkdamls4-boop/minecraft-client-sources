package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.BlurDrawBatch;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.GuiEffectsState;
import deluxe.client.render.BlurDrawData;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class BatchedBlurRenderer implements AutoCloseable {
   private static volatile BatchedBlurRenderer batchedBlurRenderer;
   private static float field15231 = 0.55F;
   private static float field15232 = 0.65F;
   private static float field15233 = 0.78F;
   private static float field15234;
   public static final RenderPipeline field15235 = RenderPipeline.builder()
      .withLocation(method4512("pipeline/batched_blur"))
      .withVertexShader(method4512("ui/batched_blur/batched_blur"))
      .withFragmentShader(method4512("ui/batched_blur/batched_blur"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline field15236 = RenderPipeline.builder()
      .withLocation(method4512("pipeline/kawase_down"))
      .withVertexShader(method4512("ui/kawase/down"))
      .withFragmentShader(method4512("ui/kawase/down"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline field15237 = RenderPipeline.builder()
      .withLocation(method4512("pipeline/kawase_up"))
      .withVertexShader(method4512("ui/kawase/down"))
      .withFragmentShader(method4512("ui/kawase/up"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline field15238 = RenderPipeline.builder()
      .withLocation(method4512("pipeline/gaussian_blur"))
      .withVertexShader(method4512("ui/kawase/down"))
      .withFragmentShader(method4512("ui/kawase/gaussian"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BatchedBlurRenderer.Nested0528> field15250 = new ArrayList<>(32);
   private final List<BatchedBlurRenderer.Nested0526> field15251 = new ArrayList<>(32);
   private DrawContext field15252;
   private final BatchedBlurRenderer.Nested0527 field15253 = new BatchedBlurRenderer.Nested0527("deluxe_blur_result");
   private final BatchedBlurRenderer.Nested0527 field15254 = new BatchedBlurRenderer.Nested0527("deluxe_blur_scratch");
   private final SimpleFramebuffer[] field15255 = new SimpleFramebuffer[4];
   private final SimpleFramebuffer[] field15256 = new SimpleFramebuffer[4];
   private SimpleFramebuffer field15257;
   private long field15258;
   private long field15259;
   private boolean field15260;
   private boolean field15261;
   private boolean field15262 = true;
   private long field15263;
   private long field15264;
   private long field15265;
   private long field15266;
   private SimpleFramebuffer field15267;
   private boolean field15268;
   private float field15269;
   private SimpleFramebuffer field15270;
   private boolean field15271;
   private float field15272;
   private int field15273;
   private SimpleFramebuffer field15274;
   private SimpleFramebuffer field15275;
   private SimpleFramebuffer field15276;
   private GpuBuffer field15278;
   private GpuBuffer field15279;
   private GpuBuffer field15280;
   private boolean field15281;
   private boolean field15282;
   private boolean field15283;
   private boolean field15284 = true;
   private int field15285;

   private void method4472(DrawContext drawContext, BlurDrawData newBlurDrawData) {
      if (newBlurDrawData != null && newBlurDrawData.visible()) {
         if (drawContext != null) {
            try {
               BlurDrawData currentBlurDrawData = this.method4484(newBlurDrawData);
               Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
               ScreenRect screenRect = ScissorStack.method3969();
               BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
               this.field15250.add(new BatchedBlurRenderer.Nested0528(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, false));
               ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
            } catch (RuntimeException exception) {
            }
         }
      }
   }

   public void method4473(BlurDrawData newBlurDrawData) {
      DrawContext drawContext = this.field15252;
      if (newBlurDrawData != null && newBlurDrawData.visible() && drawContext != null) {
         try {
            BlurDrawData currentBlurDrawData = this.method4484(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ScreenRect screenRect = ScissorStack.method3969();
            BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
            this.field15250.add(new BatchedBlurRenderer.Nested0528(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, false, true));
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static int method4476(int local7555_0, int local7555_1, int local7555_2) {
      return Math.max(local7555_1, Math.min(local7555_2, local7555_0));
   }

   private void method4477() {
      if (this.field15279 != null) {
         this.field15279.close();
         this.field15279 = null;
      }
   }

   private static int method4478(int local7557_0, int local7557_1) {
      return -Math.floorDiv(-local7557_0, local7557_1) * local7557_1;
   }

   private void method4479(
      int local7558_1, Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, BatchedBlurRenderer.Nested0530 local7558_4, float local7558_5
   ) {
      if (simpleFramebuffer != null && local7558_4 != null) {
         int local7558_6 = this.field15285 > 0 ? this.field15285 : this.method4516(local7558_5);
         this.field15283 = local7558_1 == 65 || local7558_1 == 66;
         this.method4494(simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight, local7558_6);
         SimpleFramebuffer[] simpleFramebufferArray = this.field15255;
         SimpleFramebuffer[] simpleFramebufferArray2 = this.field15256;
         float local7558_9 = 1.0F;
         GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
         int local7558_12 = framebuffer.textureWidth;
         int local7558_13 = framebuffer.textureHeight;
         float local7558_14 = (float)local7558_4.x() / Math.max(framebuffer.textureWidth, 1);
         float local7558_15 = (float)local7558_4.y() / Math.max(framebuffer.textureHeight, 1);
         float local7558_16 = (float)local7558_4.width() / Math.max(framebuffer.textureWidth, 1);
         float local7558_17 = (float)local7558_4.height() / Math.max(framebuffer.textureHeight, 1);

         for (int index = 0; index < local7558_6; index++) {
            SimpleFramebuffer simpleFramebuffer2 = simpleFramebufferArray[index];
            this.method4483(
               field15236,
               gpuTextureView,
               local7558_12,
               local7558_13,
               simpleFramebuffer2,
               local7558_9,
               local7558_14,
               local7558_15,
               local7558_16,
               local7558_17,
               gpuSampler
            );
            gpuTextureView = simpleFramebuffer2.getColorAttachmentView();
            local7558_12 = simpleFramebuffer2.textureWidth;
            local7558_13 = simpleFramebuffer2.textureHeight;
            local7558_14 = 0.0F;
            local7558_15 = 0.0F;
            local7558_16 = 1.0F;
            local7558_17 = 1.0F;
         }

         SimpleFramebuffer simpleFramebuffer3 = simpleFramebufferArray[local7558_6 - 1];
         SimpleFramebuffer simpleFramebuffer4 = this.field15257;
         if (simpleFramebuffer3 != null && simpleFramebuffer4 != null && simpleFramebuffer4.getColorAttachmentView() != null) {
            float local7558_20 = local7558_5 * 0.5F * (Math.max(framebuffer.textureHeight, 1) / 1080.0F);
            float local7558_21 = local7558_20 * ((float)simpleFramebuffer3.textureHeight / Math.max(local7558_4.height(), 1));
            local7558_21 = method4490(local7558_21, 0.6F, 12.0F);
            int local7558_22 = Math.min(31, Math.max(2, (int)Math.ceil(local7558_21 * 3.0F)));
            boolean local7558_23 = local7558_1 == 66;
            this.method4526(
               simpleFramebuffer3.getColorAttachmentView(),
               simpleFramebuffer4.getColorAttachmentView(),
               1.0F / Math.max(simpleFramebuffer3.textureWidth, 1),
               0.0F,
               local7558_21,
               local7558_22,
               1.0F,
               1.0F,
               0.0F,
               gpuSampler
            );
            this.method4526(
               simpleFramebuffer4.getColorAttachmentView(),
               simpleFramebuffer3.getColorAttachmentView(),
               0.0F,
               1.0F / Math.max(simpleFramebuffer3.textureHeight, 1),
               local7558_21,
               local7558_22,
               local7558_23 ? 1.25F : 1.0F,
               local7558_23 ? 1.0F : 1.0F,
               local7558_23 ? 0.78F : 0.0F,
               gpuSampler
            );
            gpuTextureView = simpleFramebuffer3.getColorAttachmentView();
            local7558_12 = simpleFramebuffer3.textureWidth;
            local7558_13 = simpleFramebuffer3.textureHeight;
         }

         for (int index2 = local7558_6 - 2; index2 >= 0; index2--) {
            SimpleFramebuffer simpleFramebuffer5 = simpleFramebufferArray2[index2];
            this.method4483(field15237, gpuTextureView, local7558_12, local7558_13, simpleFramebuffer5, local7558_9, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
            gpuTextureView = simpleFramebuffer5.getColorAttachmentView();
            local7558_12 = simpleFramebuffer5.textureWidth;
            local7558_13 = simpleFramebuffer5.textureHeight;
         }

         this.method4483(field15237, gpuTextureView, local7558_12, local7558_13, simpleFramebuffer, local7558_9, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
      }
   }

   private SimpleFramebuffer method4480(int local7559_1, int local7559_2) {
      int local7559_3 = Math.max(Math.round(local7559_1 * 0.25F), 1);
      int local7559_4 = Math.max(Math.round(local7559_2 * 0.25F), 1);
      this.field15275 = this.method4515(this.field15275, "deluxe_blur_stratum_heavy", local7559_3, local7559_4);
      return this.field15275;
   }

   public void method4481() {
      if (!this.field15250.isEmpty()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachment() != null) {
            this.field15285 = this.method4516(0.0F);
            long local7560_3 = System.nanoTime();
            long local7560_5 = ++this.field15258;
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            this.field15268 = false;
            this.field15271 = false;
            float local7560_8 = -1.0F;
            float local7560_9 = -1.0F;

            for (BatchedBlurRenderer.Nested0528 local7560_11 : this.field15250) {
               if (local7560_11.method4542()) {
                  if (local7560_11.method4539() == 0) {
                     local7560_8 = Math.max(local7560_8, local7560_11.getBlurDrawData().blurRadius());
                  } else {
                     local7560_9 = Math.max(local7560_9, local7560_11.getBlurDrawData().blurRadius());
                  }
               }
            }

            if (local7560_8 >= 0.0F) {
               float local7560_10 = this.method4509(local7560_8, framebuffer.textureWidth, framebuffer.textureHeight);
               int local7560_11 = Math.max(Math.round(framebuffer.textureWidth * local7560_10), 1);
               int local7560_12 = Math.max(Math.round(framebuffer.textureHeight * local7560_10), 1);
               this.field15267 = this.method4515(this.field15267, "deluxe_blur_global", local7560_11, local7560_12);
               this.field15263 = local7560_3;
               if (this.field15267 != null && this.field15267.getColorAttachmentView() != null) {
                  this.field15268 = true;
                  this.field15269 = local7560_8;
                  TextureSetup textureSetup = TextureSetup.of(this.field15267.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.Nested0528 local7560_15 : this.field15250) {
                     if (local7560_15.method4542() && local7560_15.method4539() == 0) {
                        BlurDrawBatch currentBlurDrawBatch = local7560_15.getBlurDrawBatch();
                        currentBlurDrawBatch.field11731 = 0.0F;
                        currentBlurDrawBatch.field11732 = 0.0F;
                        currentBlurDrawBatch.field11733 = framebuffer.textureWidth;
                        currentBlurDrawBatch.field11734 = framebuffer.textureHeight;
                        currentBlurDrawBatch.field11735 = textureSetup;
                     }
                  }

                  this.field15281 = true;
               }
            }

            if (local7560_9 >= 0.0F) {
               float local7560_10 = this.method4509(local7560_9, framebuffer.textureWidth, framebuffer.textureHeight);
               int local7560_11 = Math.max(Math.round(framebuffer.textureWidth * local7560_10), 1);
               int local7560_12 = Math.max(Math.round(framebuffer.textureHeight * local7560_10), 1);
               this.field15270 = this.method4515(this.field15270, "deluxe_blur_global2", local7560_11, local7560_12);
               this.field15264 = local7560_3;
               if (this.field15270 != null && this.field15270.getColorAttachmentView() != null) {
                  this.field15271 = true;
                  this.field15272 = local7560_9;
                  TextureSetup textureSetup2 = TextureSetup.of(this.field15270.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.Nested0528 local7560_15 : this.field15250) {
                     if (local7560_15.method4542() && local7560_15.method4539() == 1) {
                        BlurDrawBatch currentBlurDrawBatch2 = local7560_15.getBlurDrawBatch();
                        currentBlurDrawBatch2.field11731 = 0.0F;
                        currentBlurDrawBatch2.field11732 = 0.0F;
                        currentBlurDrawBatch2.field11733 = framebuffer.textureWidth;
                        currentBlurDrawBatch2.field11734 = framebuffer.textureHeight;
                        currentBlurDrawBatch2.field11735 = textureSetup2;
                     }
                  }

                  this.field15281 = true;
               }
            }

            boolean local7560_10 = false;

            for (BatchedBlurRenderer.Nested0528 local7560_12 : this.field15250) {
               if (local7560_12.method4543()) {
                  local7560_10 = true;
                  break;
               }
            }

            if (local7560_10) {
               SimpleFramebuffer simpleFramebuffer = this.method4480(framebuffer.textureWidth, framebuffer.textureHeight);
               this.field15266 = local7560_3;
               if (simpleFramebuffer != null && simpleFramebuffer.getColorAttachmentView() != null) {
                  this.field15282 = true;
                  TextureSetup textureSetup3 = TextureSetup.of(simpleFramebuffer.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.Nested0528 local7560_14 : this.field15250) {
                     if (local7560_14.method4543()) {
                        BlurDrawBatch currentBlurDrawBatch3 = local7560_14.getBlurDrawBatch();
                        currentBlurDrawBatch3.field11731 = 0.0F;
                        currentBlurDrawBatch3.field11732 = 0.0F;
                        currentBlurDrawBatch3.field11733 = framebuffer.textureWidth;
                        currentBlurDrawBatch3.field11734 = framebuffer.textureHeight;
                        currentBlurDrawBatch3.field11735 = textureSetup3;
                     }
                  }

                  this.field15281 = true;
               }
            }

            for (int index = 0; index < this.field15250.size() && index < 64; index++) {
               BatchedBlurRenderer.Nested0528 local7560_12 = this.field15250.get(index);
               if (!local7560_12.method4542() && !local7560_12.method4543()) {
                  BlurDrawBatch currentBlurDrawBatch4 = local7560_12.getBlurDrawBatch();
                  BatchedBlurRenderer.Nested0530 local7560_14 = this.method4492(local7560_12, minecraftClient, framebuffer);
                  if (local7560_14 != null) {
                     float local7560_15 = this.method4509(local7560_12.getBlurDrawData().blurRadius(), local7560_14.width(), local7560_14.height());
                     int local7560_16 = Math.max(Math.round(local7560_14.width() * local7560_15), 1);
                     int local7560_17 = Math.max(Math.round(local7560_14.height() * local7560_15), 1);
                     SimpleFramebuffer simpleFramebuffer2 = this.field15253.method4531(local7560_16, local7560_17, local7560_5, local7560_3);
                     if (simpleFramebuffer2 != null && simpleFramebuffer2.getColorAttachmentView() != null) {
                        local7560_12.method4534(simpleFramebuffer2);
                        local7560_12.method4538(local7560_14);
                        currentBlurDrawBatch4.field11731 = local7560_14.x();
                        currentBlurDrawBatch4.field11732 = local7560_14.y();
                        currentBlurDrawBatch4.field11733 = local7560_14.width();
                        currentBlurDrawBatch4.field11734 = local7560_14.height();
                        currentBlurDrawBatch4.field11735 = TextureSetup.of(simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
                        this.field15281 = true;
                     }
                  }
               }
            }
         } else {
            this.field15250.clear();
         }
      }
   }

   private GpuBuffer method4482() {
      if (this.field15279 != null && !this.field15279.isClosed() && this.field15279.size() >= 24576L) {
         return this.field15279;
      }

      this.method4477();

      try {
         this.field15279 = RenderSystem.getDevice().createBuffer(() -> "deluxe_blur_params", 136, 24576L);
         return this.field15279;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private void method4483(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int local7562_3,
      int local7562_4,
      SimpleFramebuffer simpleFramebuffer,
      float local7562_6,
      float local7562_7,
      float local7562_8,
      float local7562_9,
      float local7562_10,
      GpuSampler gpuSampler
   ) {
      if (simpleFramebuffer != null) {
         this.method4501(
            renderPipeline,
            gpuTextureView,
            local7562_3,
            local7562_4,
            simpleFramebuffer.getColorAttachmentView(),
            local7562_6,
            local7562_7,
            local7562_8,
            local7562_9,
            local7562_10,
            gpuSampler,
            true,
            1.0F
         );
      }
   }

   private BlurDrawData method4484(BlurDrawData newBlurDrawData) {
      float local7563_2 = Math.max(0.0F, Math.min(newBlurDrawData.width(), newBlurDrawData.height()) * 0.5F);
      float local7563_3 = method4490(newBlurDrawData.radiusTopLeft(), 0.0F, local7563_2);
      float local7563_4 = method4490(newBlurDrawData.radiusTopRight(), 0.0F, local7563_2);
      float local7563_5 = method4490(newBlurDrawData.radiusBottomRight(), 0.0F, local7563_2);
      float local7563_6 = method4490(newBlurDrawData.radiusBottomLeft(), 0.0F, local7563_2);
      float local7563_7 = Math.max(0.0F, newBlurDrawData.smoothness());
      float local7563_8 = Math.max(0.0F, newBlurDrawData.blurRadius());
      return local7563_3 == newBlurDrawData.radiusTopLeft()
            && local7563_4 == newBlurDrawData.radiusTopRight()
            && local7563_5 == newBlurDrawData.radiusBottomRight()
            && local7563_6 == newBlurDrawData.radiusBottomLeft()
            && local7563_7 == newBlurDrawData.smoothness()
            && local7563_8 == newBlurDrawData.blurRadius()
         ? newBlurDrawData
         : new BlurDrawData(
            newBlurDrawData.x(),
            newBlurDrawData.y(),
            newBlurDrawData.width(),
            newBlurDrawData.height(),
            local7563_3,
            local7563_4,
            local7563_5,
            local7563_6,
            local7563_7,
            local7563_8,
            newBlurDrawData.colorTopLeft(),
            newBlurDrawData.colorTopRight(),
            newBlurDrawData.colorBottomRight(),
            newBlurDrawData.colorBottomLeft()
         );
   }

   public void method4485() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && this.field15282
         && this.field15275 != null
         && this.field15275.getColorAttachmentView() != null) {
         if (this.field15262) {
            this.field15262 = false;
            this.method4479(
               66, framebuffer, this.field15275, new BatchedBlurRenderer.Nested0530(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), 72.0F
            );
         }
      }
   }

   public void method4486() {
      this.method4503(Integer.MAX_VALUE, 0);
   }

   public void method4487() {
      if (!this.field15251.isEmpty() && this.field15284) {
         GpuBuffer gpuBuffer = this.method4482();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method4524(memoryStack, this.field15251);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field15284 = false;
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
            } catch (RuntimeException exception) {
               this.field15284 = true;
            }
         }
      }
   }

   public void method4488(DrawContext drawContext, BlurDrawData newBlurDrawData) {
      this.method4491(drawContext);
      this.method4522(newBlurDrawData);
      this.method4527();
   }

   private static float method4490(float local7569_0, float local7569_1, float local7569_2) {
      return Math.max(local7569_1, Math.min(local7569_2, local7569_0));
   }

   public void method4491(DrawContext drawContext) {
      this.field15252 = drawContext;
   }

   private BatchedBlurRenderer.Nested0530 method4492(BatchedBlurRenderer.Nested0528 local7571_1, MinecraftClient minecraftClient, Framebuffer framebuffer) {
      Window window = minecraftClient.getWindow();
      int local7571_5 = Math.max(window.getScaledWidth(), 1);
      int local7571_6 = Math.max(window.getScaledHeight(), 1);
      float local7571_7 = (float)framebuffer.textureWidth / local7571_5;
      float local7571_8 = (float)framebuffer.textureHeight / local7571_6;
      BlurDrawData currentBlurDrawData = local7571_1.getBlurDrawData();
      float local7571_10 = GuiScale.method0800(Math.max(16.0F, currentBlurDrawData.blurRadius() * 2.0F));
      ScreenRect screenRect = new ScreenRect(
            Math.round(currentBlurDrawData.x()), Math.round(currentBlurDrawData.y()), Math.round(currentBlurDrawData.width()), Math.round(currentBlurDrawData.height())
         )
         .transformEachVertex(local7571_1.method4540());
      if (local7571_1.method4536() != null) {
         screenRect = local7571_1.method4536().intersection(screenRect);
         if (screenRect == null) {
            return null;
         }
      }

      float local7571_12 = method4490(screenRect.getLeft() - local7571_10, 0.0F, local7571_5);
      float local7571_13 = method4490(screenRect.getTop() - local7571_10, 0.0F, local7571_6);
      float local7571_14 = method4490(screenRect.getRight() + local7571_10, local7571_12 + 1.0F, local7571_5);
      float local7571_15 = method4490(screenRect.getBottom() + local7571_10, local7571_13 + 1.0F, local7571_6);
      int local7571_16 = 64;
      int local7571_17 = method4476(method4511((int)Math.floor(local7571_12 * local7571_7), local7571_16), 0, framebuffer.textureWidth - 1);
      int local7571_18 = method4476(method4478((int)Math.ceil(local7571_14 * local7571_7), local7571_16), local7571_17 + 1, framebuffer.textureWidth);
      int local7571_19 = method4476(method4511((int)Math.floor((local7571_6 - local7571_15) * local7571_8), local7571_16), 0, framebuffer.textureHeight - 1);
      int local7571_20 = method4476(
         method4478((int)Math.ceil((local7571_6 - local7571_13) * local7571_8), local7571_16), local7571_19 + 1, framebuffer.textureHeight
      );
      int local7571_21 = Math.max(local7571_18 - local7571_17, 1);
      int local7571_22 = Math.max(local7571_20 - local7571_19, 1);
      return new BatchedBlurRenderer.Nested0530(local7571_17, local7571_19, local7571_21, local7571_22);
   }

   private void method4494(int local7573_1, int local7573_2, int local7573_3) {
      long local7573_4 = ++this.field15259;
      long local7573_6 = System.nanoTime();
      int local7573_8 = Math.max(local7573_1, 1);
      int local7573_9 = Math.max(local7573_2, 1);
      int local7573_10 = local7573_8;
      int local7573_11 = local7573_9;
      Arrays.fill(this.field15255, null);
      Arrays.fill(this.field15256, null);

      for (int index = 0; index < local7573_3; index++) {
         this.field15255[index] = this.field15254.method4531(local7573_8, local7573_9, local7573_4, local7573_6);
         if (index < local7573_3 - 1) {
            this.field15256[index] = this.field15254.method4531(local7573_8, local7573_9, local7573_4, local7573_6);
         }

         local7573_10 = local7573_8;
         local7573_11 = local7573_9;
         local7573_8 = Math.max(local7573_8 / 2, 1);
         local7573_9 = Math.max(local7573_9 / 2, 1);
      }

      this.field15257 = this.field15254.method4531(local7573_10, local7573_11, local7573_4, local7573_6);
   }

   public void method4495() {
      this.field15251.clear();
      this.field15260 = false;
      this.field15261 = false;
      this.field15262 = true;
      this.method4517(System.nanoTime());
      this.field15281 = false;
      this.field15282 = false;
      this.field15271 = false;
      this.field15273 = 0;
      this.field15284 = false;
      this.field15285 = 0;
   }

   private GpuBuffer method4496() {
      if (this.field15280 == null || this.field15280.isClosed() || this.field15280.size() < 64L) {
         this.method4499();
         this.field15280 = RenderSystem.getDevice().createBuffer(() -> "deluxe_kawase_params", 136, 64L);
      }

      return this.field15280;
   }

   @Override
   public void close() {
      this.field15250.clear();
      this.field15251.clear();
      this.field15252 = null;
      this.field15281 = false;
      this.method4477();
      this.method4499();
      this.field15253.method4530();
      this.field15254.method4530();
      Arrays.fill(this.field15255, null);
      Arrays.fill(this.field15256, null);
      this.field15257 = null;
      if (this.field15267 != null) {
         this.field15267.delete();
         this.field15267 = null;
      }

      if (this.field15270 != null) {
         this.field15270.delete();
         this.field15270 = null;
      }

      if (this.field15274 != null) {
         this.field15274.delete();
         this.field15274 = null;
      }

      if (this.field15275 != null) {
         this.field15275.delete();
         this.field15275 = null;
      }

      if (this.field15276 != null) {
         this.field15276.delete();
         this.field15276 = null;
      }

      this.field15268 = false;
      if (this.field15278 != null) {
         this.field15278.close();
         this.field15278 = null;
      }
   }

   public void method4497(RenderPass renderPass) {
      if (renderPass != null && !this.field15251.isEmpty()) {
         GpuBuffer gpuBuffer = this.method4518();
         if (gpuBuffer != null) {
            renderPass.setUniform("BlurParamsArray", gpuBuffer);
         }
      }
   }

   private GpuBuffer method4498() {
      if (this.field15278 != null) {
         return this.field15278;
      }

      try (
         BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
         BuiltBuffer builtBuffer = this.method4513(bufferAllocator);
      ) {
         this.field15278 = RenderSystem.getDevice().createBuffer(() -> "deluxe_blur_fullscreen_quad", 32, builtBuffer.getBuffer());
         return this.field15278;
      }
   }

   private void method4499() {
      if (this.field15280 != null) {
         this.field15280.close();
         this.field15280 = null;
      }
   }

   public static void method4500(float local7580_0) {
      field15234 = Math.max(0.0F, Math.min(1.0F, local7580_0));
   }

   private void method4501(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int local7581_3,
      int local7581_4,
      GpuTextureView gpuTextureView2,
      float local7581_6,
      float local7581_7,
      float local7581_8,
      float local7581_9,
      float local7581_10,
      GpuSampler gpuSampler,
      boolean local7581_12,
      float local7581_13
   ) {
      if (gpuTextureView != null && gpuTextureView2 != null) {
         GpuBuffer gpuBuffer = this.method4498();
         if (gpuBuffer != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            label91: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(64);
                  byteBuffer.putFloat(0, local7581_7);
                  byteBuffer.putFloat(4, local7581_8);
                  byteBuffer.putFloat(8, local7581_9);
                  byteBuffer.putFloat(12, local7581_10);
                  byteBuffer.putFloat(16, local7581_6 / Math.max(local7581_3, 1));
                  byteBuffer.putFloat(20, local7581_6 / Math.max(local7581_4, 1));
                  float local7581_20 = field15234 * 0.97F;
                  byteBuffer.putFloat(32, this.field15283 ? local7581_20 : field15231);
                  byteBuffer.putFloat(36, this.field15283 ? local7581_20 : field15232);
                  byteBuffer.putFloat(40, this.field15283 ? local7581_20 : field15233);
                  byteBuffer.putFloat(44, local7581_13);
                  byteBuffer.position(0);
                  GpuBuffer gpuBuffer2 = this.method4496();
                  if (gpuBuffer2 == null) {
                     break label91;
                  }

                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 64L), byteBuffer);

                  try (RenderPass renderPass = commandEncoder.createRenderPass(
                        () -> "deluxe_kawase_blur", gpuTextureView2, local7581_12 ? OptionalInt.of(0) : OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                     renderPass.setPipeline(renderPipeline);
                     renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                     renderPass.setUniform("KawaseParams", gpuBuffer2);
                     renderPass.setVertexBuffer(0, gpuBuffer);
                     renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }
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

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public void method4503(int local7583_1, int local7583_2) {
      boolean local7583_3 = local7583_2 > local7583_1 && (GuiEffectsState.method3978() || GuiEffectsState.method3974());
      this.field15262 = !local7583_3 || this.field15261;
      if (!this.field15281) {
         this.field15250.clear();
      } else {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            if (this.field15268 && this.field15267 != null) {
               this.method4479(
                  64,
                  framebuffer,
                  this.field15267,
                  new BatchedBlurRenderer.Nested0530(0, 0, framebuffer.textureWidth, framebuffer.textureHeight),
                  this.field15269
               );
            }

            if (this.field15282 && (this.field15260 || !local7583_3) && this.field15275 != null && this.field15275.getColorAttachmentView() != null) {
               this.method4479(
                  66, framebuffer, this.field15275, new BatchedBlurRenderer.Nested0530(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), 72.0F
               );
            }

            for (int index = 0; index < this.field15250.size() && index < 64; index++) {
               BatchedBlurRenderer.Nested0528 local7583_7 = this.field15250.get(index);
               if (!local7583_7.method4542() && !local7583_7.method4543()) {
                  BatchedBlurRenderer.Nested0530 local7583_8 = local7583_7.method4537();
                  SimpleFramebuffer simpleFramebuffer = local7583_7.method4532();
                  if (local7583_8 != null && simpleFramebuffer != null) {
                     this.method4479(index, framebuffer, simpleFramebuffer, local7583_8, local7583_7.getBlurDrawData().blurRadius());
                  }
               }
            }

            this.field15250.clear();
         } else {
            this.field15250.clear();
         }
      }
   }

   public static BatchedBlurRenderer method4504() {
      BatchedBlurRenderer currentBatchedBlurRenderer = batchedBlurRenderer;
      if (currentBatchedBlurRenderer == null) {
         synchronized (BatchedBlurRenderer.class) {
            currentBatchedBlurRenderer = batchedBlurRenderer;
            if (currentBatchedBlurRenderer == null) {
               currentBatchedBlurRenderer = new BatchedBlurRenderer();
               batchedBlurRenderer = currentBatchedBlurRenderer;
            }
         }
      }

      return currentBatchedBlurRenderer;
   }

   public static void method4505(float local7585_0, float local7585_1, float local7585_2) {
      if (Float.isFinite(local7585_0) && Float.isFinite(local7585_1) && Float.isFinite(local7585_2)) {
         field15231 = method4490(local7585_0, 0.0F, 1.0F);
         field15232 = method4490(local7585_1, 0.0F, 1.0F);
         field15233 = method4490(local7585_2, 0.0F, 1.0F);
      }
   }

   private BatchedBlurRenderer() {
   }

   public void method4506(BlurDrawData newBlurDrawData) {
      DrawContext drawContext = this.field15252;
      if (newBlurDrawData != null && newBlurDrawData.visible() && drawContext != null) {
         try {
            BlurDrawData currentBlurDrawData = this.method4484(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            ScreenRect screenRect = ScissorStack.method3969();
            BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.Nested0528 local7587_7 = new BatchedBlurRenderer.Nested0528(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, true);
            local7587_7.method4541(this.field15273);
            this.field15250.add(local7587_7);
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   int method4507(BlurDrawData newBlurDrawData, BlurDrawBatch newBlurDrawBatch) {
      int local7588_3 = this.field15251.size();
      if (local7588_3 == 512) {
         return -1;
      }

      this.field15251.add(new BatchedBlurRenderer.Nested0526(newBlurDrawData, newBlurDrawBatch));
      this.field15284 = true;
      return local7588_3;
   }

   public static void method4508(int local7589_0) {
      method4504().field15273 = Math.max(0, Math.min(1, local7589_0));
   }

   private float method4509(float local7590_1, int local7590_2, int local7590_3) {
      return local7590_1 >= 40.0F ? 0.25F : 0.5F;
   }

   public boolean method4510(RenderPipeline renderPipeline) {
      return renderPipeline == field15235;
   }

   private static int method4511(int local7592_0, int local7592_1) {
      return Math.floorDiv(local7592_0, local7592_1) * local7592_1;
   }

   private static Identifier method4512(String local7594_0) {
      return Identifier.of("deluxe", local7594_0);
   }

   private BuiltBuffer method4513(BufferAllocator bufferAllocator) {
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, -1.0F, 0.0F);
      return bufferBuilder.end();
   }

   public void method4514(RenderPipeline renderPipeline, TextureSetup textureSetup, int local7596_3, int local7596_4) {
      if (this.field15282 && renderPipeline == field15235 && textureSetup != null && this.field15275 != null) {
         GpuTextureView colorAttachmentView = this.field15275.getColorAttachmentView();
         if (colorAttachmentView != null && textureSetup.texure0() == colorAttachmentView) {
            if (local7596_3 < local7596_4) {
               this.field15260 = true;
            } else {
               this.field15261 = true;
            }
         }
      }
   }

   private SimpleFramebuffer method4515(SimpleFramebuffer simpleFramebuffer, String local7597_2, int local7597_3, int local7597_4) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(local7597_2, local7597_3, local7597_4, false);
      }

      if (simpleFramebuffer.textureWidth != local7597_3 || simpleFramebuffer.textureHeight != local7597_4) {
         simpleFramebuffer.resize(local7597_3, local7597_4);
      }

      return simpleFramebuffer;
   }

   private int method4516(float local7598_1) {
      return 2;
   }

   private void method4517(long local7599_1) {
      this.field15253.method4529(local7599_1, 5000000000L);
      this.field15254.method4529(local7599_1, 5000000000L);
      if (this.field15267 != null && local7599_1 - this.field15263 > 5000000000L) {
         this.field15267.delete();
         this.field15267 = null;
      }

      if (this.field15270 != null && local7599_1 - this.field15264 > 5000000000L) {
         this.field15270.delete();
         this.field15270 = null;
      }

      if (this.field15274 != null && local7599_1 - this.field15265 > 5000000000L) {
         this.field15274.delete();
         this.field15274 = null;
      }

      if (this.field15275 != null && local7599_1 - this.field15266 > 5000000000L) {
         this.field15275.delete();
         this.field15275 = null;
      }
   }

   private GpuBuffer method4518() {
      if (this.field15284) {
         this.method4487();
      }

      if (!this.field15284 && this.field15279 != null) {
         return this.field15279;
      }

      if (this.field15284) {
         this.method4477();
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = this.method4524(memoryStack, this.field15251);
            this.field15279 = RenderSystem.getDevice().createBuffer(() -> "deluxe_blur_params", 136, byteBuffer);
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

         this.field15284 = false;
         return this.field15279;
      } else {
         if (this.field15279 == null || this.field15279.isClosed() || this.field15279.size() < 24576L) {
            this.method4477();
            this.field15279 = RenderSystem.getDevice().createBuffer(() -> "deluxe_blur_params", 136, 24576L);
         }

         return this.field15279;
      }
   }

   public static void method4519() {
      BatchedBlurRenderer currentBatchedBlurRenderer = batchedBlurRenderer;
      if (currentBatchedBlurRenderer != null) {
         currentBatchedBlurRenderer.close();
         batchedBlurRenderer = null;
      }
   }

   public void method4520() {
      if (this.field15271 && this.field15270 != null && this.field15270.getColorAttachmentView() != null) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            this.method4479(
               67, framebuffer, this.field15270, new BatchedBlurRenderer.Nested0530(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), this.field15272
            );
            this.field15271 = false;
         }
      }
   }

   public void method4521(DrawContext drawContext, BlurDrawData newBlurDrawData, BlurDrawBatch newBlurDrawBatch) {
      if (drawContext != null && newBlurDrawData != null && newBlurDrawBatch != null && newBlurDrawData.visible()) {
         try {
            BlurDrawData currentBlurDrawData = this.method4484(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            this.field15250.add(new BatchedBlurRenderer.Nested0528(currentBlurDrawData, matrix3x2f, ScissorStack.method3969(), newBlurDrawBatch, true));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void method4522(BlurDrawData newBlurDrawData) {
      this.method4472(this.field15252, newBlurDrawData);
   }

   private ByteBuffer method4524(MemoryStack memoryStack, List<BatchedBlurRenderer.Nested0526> local7606_2) {
      int local7606_3 = Math.max(1, local7606_2.size()) * 3 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(local7606_3);

      for (int index = 0; index < local7606_2.size(); index++) {
         BatchedBlurRenderer.Nested0526 local7606_6 = local7606_2.get(index);
         BlurDrawData currentBlurDrawData = local7606_6.blur();
         BlurDrawBatch currentBlurDrawBatch = local7606_6.capture();
         int local7606_9 = index * 3 * 4 * 4;
         byteBuffer.putFloat(local7606_9, currentBlurDrawData.radiusTopLeft());
         byteBuffer.putFloat(local7606_9 + 4, currentBlurDrawData.radiusTopRight());
         byteBuffer.putFloat(local7606_9 + 8, currentBlurDrawData.radiusBottomRight());
         byteBuffer.putFloat(local7606_9 + 12, currentBlurDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local7606_9 + 16, currentBlurDrawData.width());
         byteBuffer.putFloat(local7606_9 + 20, currentBlurDrawData.height());
         byteBuffer.putFloat(local7606_9 + 24, currentBlurDrawData.smoothness());
         byteBuffer.putFloat(local7606_9 + 28, currentBlurDrawData.blurRadius());
         byteBuffer.putFloat(local7606_9 + 32, currentBlurDrawBatch.field11731);
         byteBuffer.putFloat(local7606_9 + 36, currentBlurDrawBatch.field11732);
         byteBuffer.putFloat(local7606_9 + 40, currentBlurDrawBatch.field11733);
         byteBuffer.putFloat(local7606_9 + 44, currentBlurDrawBatch.field11734);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void method4526(
      GpuTextureView gpuTextureView,
      GpuTextureView gpuTextureView2,
      float local7608_3,
      float local7608_4,
      float local7608_5,
      int local7608_6,
      float local7608_7,
      float local7608_8,
      float local7608_9,
      GpuSampler gpuSampler
   ) {
      if (gpuTextureView != null && gpuTextureView2 != null) {
         GpuBuffer gpuBuffer = this.method4498();
         if (gpuBuffer != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            label86: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(64);
                  byteBuffer.putFloat(0, 0.0F);
                  byteBuffer.putFloat(4, 0.0F);
                  byteBuffer.putFloat(8, 1.0F);
                  byteBuffer.putFloat(12, 1.0F);
                  byteBuffer.putFloat(16, local7608_3);
                  byteBuffer.putFloat(20, local7608_4);
                  byteBuffer.putFloat(24, local7608_5);
                  byteBuffer.putFloat(28, local7608_6);
                  float local7608_17 = field15234 * 0.97F;
                  byteBuffer.putFloat(32, this.field15283 ? local7608_17 : field15231);
                  byteBuffer.putFloat(36, this.field15283 ? local7608_17 : field15232);
                  byteBuffer.putFloat(40, this.field15283 ? local7608_17 : field15233);
                  byteBuffer.putFloat(44, 1.0F);
                  byteBuffer.putFloat(48, local7608_7);
                  byteBuffer.putFloat(52, local7608_8);
                  byteBuffer.putFloat(56, local7608_9);
                  byteBuffer.putFloat(60, field15234);
                  byteBuffer.position(0);
                  GpuBuffer gpuBuffer2 = this.method4496();
                  if (gpuBuffer2 == null) {
                     break label86;
                  }

                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 64L), byteBuffer);

                  try (RenderPass renderPass = commandEncoder.createRenderPass(
                        () -> "deluxe_gaussian_blur", gpuTextureView2, OptionalInt.of(0), null, OptionalDouble.empty()
                     )) {
                     renderPass.setPipeline(field15238);
                     renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                     renderPass.setUniform("KawaseParams", gpuBuffer2);
                     renderPass.setVertexBuffer(0, gpuBuffer);
                     renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }
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

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public void method4527() {
      this.field15252 = null;
   }

   public void method4528(float local7610_1, float local7610_2) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
      if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
         float local7610_5 = method4490(local7610_2, 0.0F, 1.0F);
         if (local7610_5 > 0.004F) {
            float local7610_6 = this.method4509(local7610_1, framebuffer.textureWidth, framebuffer.textureHeight);
            int local7610_7 = Math.max(Math.round(framebuffer.textureWidth * local7610_6), 1);
            int local7610_8 = Math.max(Math.round(framebuffer.textureHeight * local7610_6), 1);
            this.field15274 = this.method4515(this.field15274, "deluxe_blur_stratum", local7610_7, local7610_8);
            this.field15265 = System.nanoTime();
            if (this.field15274 != null && this.field15274.getColorAttachmentView() != null) {
               this.method4479(
                  65, framebuffer, this.field15274, new BatchedBlurRenderer.Nested0530(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), local7610_1
               );
               GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
               float local7610_10 = 5.0F * local7610_5 * local7610_5;
               float local7610_11 = local7610_10 / Math.max(framebuffer.textureWidth, 1);
               float local7610_12 = local7610_10 / Math.max(framebuffer.textureHeight, 1);
               this.method4501(
                  field15237,
                  this.field15274.getColorAttachmentView(),
                  this.field15274.textureWidth,
                  this.field15274.textureHeight,
                  framebuffer.getColorAttachmentView(),
                  1.0F,
                  local7610_11,
                  local7610_12,
                  1.0F - 2.0F * local7610_11,
                  1.0F - 2.0F * local7610_12,
                  gpuSampler,
                  false,
                  local7610_5
               );
            }
         }
      }
   }

   private record Nested0526(BlurDrawData blur, BlurDrawBatch capture) {
   }

   private static final class Nested0527 {
      private final String field15354;
      private final List<BatchedBlurRenderer.Nested0529> field15355 = new ArrayList<>();
      private int field15356;

      private void method4529(long local7617_1, long local7617_3) {
         for (int index = this.field15355.size() - 1; index >= 0; index--) {
            BatchedBlurRenderer.Nested0529 local7617_6 = this.field15355.get(index);
            if (local7617_1 - local7617_6.field15368 > local7617_3) {
               local7617_6.field15366.delete();
               this.field15355.remove(index);
            }
         }
      }

      private void method4530() {
         for (BatchedBlurRenderer.Nested0529 local7618_2 : this.field15355) {
            local7618_2.field15366.delete();
         }

         this.field15355.clear();
      }

      private Nested0527(String local7619_1) {
         this.field15354 = local7619_1;
      }

      private SimpleFramebuffer method4531(int local7620_1, int local7620_2, long local7620_3, long local7620_5) {
         if (local7620_1 > 0 && local7620_2 > 0) {
            for (int index = 0; index < this.field15355.size(); index++) {
               BatchedBlurRenderer.Nested0529 local7620_8 = this.field15355.get(index);
               if (local7620_8.field15367 != local7620_3
                  && local7620_8.field15366.textureWidth == local7620_1
                  && local7620_8.field15366.textureHeight == local7620_2) {
                  local7620_8.field15367 = local7620_3;
                  local7620_8.field15368 = local7620_5;
                  return local7620_8.field15366;
               }
            }

            SimpleFramebuffer simpleFramebuffer = new SimpleFramebuffer(this.field15354 + "_" + this.field15356++, local7620_1, local7620_2, false);
            this.field15355.add(new BatchedBlurRenderer.Nested0529(simpleFramebuffer, local7620_3, local7620_5));
            return simpleFramebuffer;
         } else {
            return null;
         }
      }
   }

   private static final class Nested0528 {
      private final BlurDrawData blurDrawData;
      private final Matrix3x2f field15358;
      private final ScreenRect field15359;
      private final BlurDrawBatch blurDrawBatch;
      private final boolean field15361;
      private final boolean field15362;
      private BatchedBlurRenderer.Nested0530 field15363;
      private SimpleFramebuffer field15364;
      private int field15365;

      SimpleFramebuffer method4532() {
         return this.field15364;
      }

      BlurDrawData getBlurDrawData() {
         return this.blurDrawData;
      }

      void method4534(SimpleFramebuffer simpleFramebuffer) {
         this.field15364 = simpleFramebuffer;
      }

      BlurDrawBatch getBlurDrawBatch() {
         return this.blurDrawBatch;
      }

      ScreenRect method4536() {
         return this.field15359;
      }

      BatchedBlurRenderer.Nested0530 method4537() {
         return this.field15363;
      }

      void method4538(BatchedBlurRenderer.Nested0530 local7627_1) {
         this.field15363 = local7627_1;
      }

      int method4539() {
         return this.field15365;
      }

      Matrix3x2f method4540() {
         return this.field15358;
      }

      void method4541(int local7630_1) {
         this.field15365 = local7630_1;
      }

      private Nested0528(BlurDrawData initialBlurDrawData, Matrix3x2f matrix3x2f, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch, boolean local7631_5) {
         this(initialBlurDrawData, matrix3x2f, screenRect, initialBlurDrawBatch, local7631_5, false);
      }

      boolean method4542() {
         return this.field15361;
      }

      boolean method4543() {
         return this.field15362;
      }

      private Nested0528(
         BlurDrawData initialBlurDrawData,
         Matrix3x2f matrix3x2f,
         ScreenRect screenRect,
         BlurDrawBatch initialBlurDrawBatch,
         boolean local7634_5,
         boolean local7634_6
      ) {
         this.blurDrawData = initialBlurDrawData;
         this.field15358 = matrix3x2f;
         this.field15359 = screenRect;
         this.blurDrawBatch = initialBlurDrawBatch;
         this.field15361 = local7634_5;
         this.field15362 = local7634_6;
      }
   }

   private static final class Nested0529 {
      private final SimpleFramebuffer field15366;
      private long field15367;
      private long field15368;

      private Nested0529(SimpleFramebuffer simpleFramebuffer, long local7635_2, long local7635_4) {
         this.field15366 = simpleFramebuffer;
         this.field15367 = local7635_2;
         this.field15368 = local7635_4;
      }
   }

   private record Nested0530(int x, int y, int width, int height) {
   }
}
