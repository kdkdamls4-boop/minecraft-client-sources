package deluxe.client.render;

import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.GradientSplitBuffer;
import deluxe.client.render.GlowDrawBatch;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.PaletteTransition;
import deluxe.client.render.GlowCompositeDrawData;

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
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlowCompositeRenderer implements AutoCloseable {
   private static volatile GlowCompositeRenderer glowCompositeRenderer;
   public static final RenderPipeline field05960 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_composite"))
      .withVertexShader(method1831("ui/glow/glow_composite"))
      .withFragmentShader(method1831("ui/glow/glow_composite"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field05961 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_shape"))
      .withVertexShader(method1831("ui/glow/glow_shape"))
      .withFragmentShader(method1831("ui/glow/glow_shape"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field05962 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_source"))
      .withVertexShader(method1831("ui/glow/glow_source"))
      .withFragmentShader(method1831("ui/glow/glow_shape"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field05963 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_blur_down"))
      .withVertexShader(method1831("ui/glow/glow_blur"))
      .withFragmentShader(method1831("ui/glow/glow_blur_down"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field05964 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_blur_up"))
      .withVertexShader(method1831("ui/glow/glow_blur"))
      .withFragmentShader(method1831("ui/glow/glow_blur_up"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field05965 = RenderPipeline.builder()
      .withLocation(method1831("pipeline/glow_cutout"))
      .withVertexShader(method1831("ui/glow/glow_blur"))
      .withFragmentShader(method1831("ui/glow/glow_cutout"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<SimpleFramebuffer[]> field05966 = new ArrayList<>(224);
   private final List<SimpleFramebuffer[]> field05967 = new ArrayList<>(224);
   private final List<SimpleFramebuffer> field05968 = new ArrayList<>(224);
   private final List<SimpleFramebuffer> field05969 = new ArrayList<>(224);
   private final List<SimpleFramebuffer> field05970 = new ArrayList<>(224);
   private int field05971;
   private long field05972;
   private final List<GlowCompositeRenderer.Nested0410> field05973 = new ArrayList<>(32);
   private final List<GlowCompositeDrawData> field05974 = new ArrayList<>(32);
   private final List<GlowDrawBatch> field05975 = new ArrayList<>(32);
   private int[] field05976 = new int[224];
   private int[] field05977 = new int[224];
   private int[] field05978 = new int[224];
   private int[] field05979 = new int[224];
   private DrawContext field05980;
   private GpuBuffer field05981;
   private GpuBuffer field05982;
   private GpuBuffer field05983;
   private GpuBuffer field05984;
   private GpuBuffer field05985;
   private ByteBuffer field05986;
   private ByteBuffer field05987;
   private boolean field05988 = true;
   private boolean field05989;
   private final GlowDrawBatch glowDrawBatch = new GlowDrawBatch();

   public void method1813() {
      this.field05974.clear();
      this.field05975.clear();
      this.field05988 = false;
      this.field05989 = false;
   }

   private ByteBuffer method1814(int local2783_1) {
      if (this.field05986 == null || this.field05986.capacity() < local2783_1) {
         if (this.field05986 != null) {
            MemoryUtil.memFree(this.field05986);
         }

         this.field05986 = MemoryUtil.memAlloc(Math.max(local2783_1, 64 * VertexFormats.POSITION.getVertexSize()));
      }

      this.field05986.clear();
      return this.field05986;
   }

   public void method1816(DrawContext drawContext) {
      this.field05980 = drawContext;
   }

   @Override
   public void close() {
      this.field05974.clear();
      this.field05980 = null;
      this.field05971 = 0;
      this.method1826();
      this.method1848();
      if (this.field05983 != null) {
         this.field05983.close();
         this.field05983 = null;
      }

      if (this.field05984 != null) {
         this.field05984.close();
         this.field05984 = null;
      }

      if (this.field05985 != null) {
         this.field05985.close();
         this.field05985 = null;
      }

      if (this.field05986 != null) {
         MemoryUtil.memFree(this.field05986);
         this.field05986 = null;
      }

      if (this.field05987 != null) {
         MemoryUtil.memFree(this.field05987);
         this.field05987 = null;
      }

      for (SimpleFramebuffer simpleFramebuffer : this.field05968) {
         if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
         }
      }

      for (SimpleFramebuffer simpleFramebuffer2 : this.field05969) {
         if (simpleFramebuffer2 != null) {
            simpleFramebuffer2.delete();
         }
      }

      for (SimpleFramebuffer simpleFramebuffer3 : this.field05970) {
         if (simpleFramebuffer3 != null) {
            simpleFramebuffer3.delete();
         }
      }

      this.field05968.clear();
      this.field05969.clear();
      this.field05970.clear();
      method1845(this.field05966);
      method1845(this.field05967);
   }

   private GpuBuffer method1817() {
      if (this.field05981 != null && !this.field05981.isClosed() && this.field05981.size() >= 17920L) {
         return this.field05981;
      }

      this.method1826();

      try {
         this.field05981 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_params", 136, 17920L);
         return this.field05981;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static void method1819(ByteBuffer byteBuffer, int local2789_1, float local2789_2, float local2789_3) {
      byteBuffer.putFloat(local2789_1, local2789_2);
      byteBuffer.putFloat(local2789_1 + 4, local2789_3);
      byteBuffer.putFloat(local2789_1 + 8, 0.0F);
   }

   private SimpleFramebuffer method1820(int local2790_1, int local2790_2, int local2790_3) {
      while (this.field05969.size() <= local2790_1) {
         this.field05969.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.method1860(this.field05969.get(local2790_1), "deluxe_glow_blur_" + local2790_1, local2790_2, local2790_3);
      this.field05969.set(local2790_1, simpleFramebuffer);
      return simpleFramebuffer;
   }

   public void method1821(DrawContext drawContext, GlowCompositeDrawData glowCompositeDrawData) {
      this.method1816(drawContext);
      this.method1859(glowCompositeDrawData);
      this.method1849();
   }

   private SimpleFramebuffer method1822(int local2792_1, int local2792_2, int local2792_3) {
      while (this.field05968.size() <= local2792_1) {
         this.field05968.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.method1860(this.field05968.get(local2792_1), "deluxe_glow_shape_" + local2792_1, local2792_2, local2792_3);
      this.field05968.set(local2792_1, simpleFramebuffer);
      return simpleFramebuffer;
   }

   private void method1823(DrawContext drawContext, GlowCompositeDrawData glowCompositeDrawData) {
      if (drawContext != null && glowCompositeDrawData != null && glowCompositeDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            GlowDrawBatch currentGlowDrawBatch = new GlowDrawBatch();
            if (this.field05973.size() < 224) {
               currentGlowDrawBatch.field13609 = this.field05973.size();
               this.field05973.add(new GlowCompositeRenderer.Nested0410(glowCompositeDrawData, currentGlowDrawBatch, matrix3x2f));
            }

            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlowCompositeRenderState(matrix3x2f, glowCompositeDrawData, ScissorStack.method3969(), currentGlowDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private int method1824(int local2794_1) {
      int local2794_2 = this.field05973.size();
      if (this.field05976.length < local2794_2) {
         this.field05976 = new int[local2794_2];
         this.field05977 = new int[local2794_2];
         this.field05978 = new int[local2794_2];
         this.field05979 = new int[local2794_2];
      }

      float local2794_3 = 8.0F;

      for (int index = 0; index < local2794_2; index++) {
         local2794_3 = Math.max(local2794_3, this.field05973.get(index).glow().effectivePad());
      }

      int local2794_4 = Math.max(8, Math.round(local2794_3 * 1.0F));
      int local2794_5 = local2794_4;
      int local2794_6 = local2794_4;
      int local2794_7 = 0;

      for (int index2 = 0; index2 < local2794_2; index2++) {
         GlowCompositeDrawData glowCompositeDrawData = this.field05973.get(index2).glow();
         float local2794_10 = glowCompositeDrawData.effectivePad();
         int local2794_11 = Math.max(Math.round((glowCompositeDrawData.width() + local2794_10 * 2.0F) * 1.0F), 1);
         int local2794_12 = Math.max(Math.round((glowCompositeDrawData.height() + local2794_10 * 2.0F) * 1.0F), 1);
         if (local2794_5 + local2794_11 + local2794_4 > local2794_1) {
            local2794_5 = local2794_4;
            local2794_6 += local2794_7 + local2794_4;
            local2794_7 = 0;
         }

         if (local2794_11 + local2794_4 * 2 <= local2794_1 && local2794_6 + local2794_12 + local2794_4 <= 2048) {
            this.field05976[index2] = local2794_5;
            this.field05977[index2] = local2794_6;
            this.field05978[index2] = local2794_11;
            this.field05979[index2] = local2794_12;
            local2794_5 += local2794_11 + local2794_4;
            local2794_7 = Math.max(local2794_7, local2794_12);
         } else {
            this.field05978[index2] = 0;
            this.field05979[index2] = 0;
         }
      }

      return local2794_6 + local2794_7 + local2794_4;
   }

   private void method1825(SimpleFramebuffer simpleFramebuffer, int local2795_2, int local2795_3) {
      int local2795_4 = this.field05973.size();
      if (simpleFramebuffer != null && local2795_4 != 0) {
         int local2795_5 = Math.max(1, RenderSystem.getDevice().getUniformOffsetAlignment());
         int local2795_6 = (1248 + local2795_5 - 1) / local2795_5 * local2795_5;
         int local2795_7 = 4 * VertexFormats.POSITION.getVertexSize();
         ByteBuffer byteBuffer = this.method1814(local2795_4 * local2795_7);
         ByteBuffer byteBuffer2 = this.method1863(local2795_4 * local2795_6);
         int local2795_10 = 0;

         for (int index = 0; index < local2795_4; index++) {
            GlowCompositeRenderer.Nested0410 local2795_12 = this.field05973.get(index);
            GlowDrawBatch currentGlowDrawBatch = local2795_12.capture();
            if (this.field05978[index] > 0 && this.field05979[index] > 0) {
               GlowCompositeDrawData glowCompositeDrawData = local2795_12.glow();
               int local2795_15 = this.field05976[index];
               int local2795_16 = this.field05977[index];
               float local2795_17 = glowCompositeDrawData.effectivePad();
               float local2795_18 = Math.min((glowCompositeDrawData.width() + local2795_17 * 2.0F) * 1.0F, this.field05978[index]);
               float local2795_19 = Math.min((glowCompositeDrawData.height() + local2795_17 * 2.0F) * 1.0F, this.field05979[index]);
               currentGlowDrawBatch.field13605 = (float)local2795_15 / local2795_2;
               currentGlowDrawBatch.field13606 = (float)local2795_16 / local2795_3;
               currentGlowDrawBatch.field13607 = Math.max(local2795_18 / local2795_2, 1.0E-6F);
               currentGlowDrawBatch.field13608 = Math.max(local2795_19 / local2795_3, 1.0E-6F);
               float local2795_20 = (float)local2795_15 / local2795_2 * 2.0F - 1.0F;
               float local2795_21 = (float)local2795_16 / local2795_3 * 2.0F - 1.0F;
               float local2795_22 = (local2795_15 + local2795_18) / local2795_2 * 2.0F - 1.0F;
               float local2795_23 = (local2795_16 + local2795_19) / local2795_3 * 2.0F - 1.0F;
               int local2795_24 = local2795_10 * local2795_7;
               method1819(byteBuffer, local2795_24, local2795_20, local2795_21);
               method1819(byteBuffer, local2795_24 + 12, local2795_20, local2795_23);
               method1819(byteBuffer, local2795_24 + 24, local2795_22, local2795_23);
               method1819(byteBuffer, local2795_24 + 36, local2795_22, local2795_21);
               ByteBuffer byteBuffer3 = byteBuffer2.slice(local2795_10 * local2795_6, 1248).order(byteBuffer2.order());
               MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer3), 0, 1248L);
               method1856(byteBuffer3, 0, glowCompositeDrawData, 0.0F, 0.0F, 1.0F, 1.0F);
               byteBuffer3.putFloat(48, glowCompositeDrawData.splitIndex());
               method1851(byteBuffer3, glowCompositeDrawData);
               method1846(byteBuffer3);
               local2795_10++;
            } else {
               currentGlowDrawBatch.field13605 = 0.0F;
               currentGlowDrawBatch.field13606 = 0.0F;
               currentGlowDrawBatch.field13607 = 0.0F;
               currentGlowDrawBatch.field13608 = 0.0F;
            }
         }

         if (local2795_10 != 0) {
            int local2795_11 = local2795_10 * local2795_7;
            int local2795_12 = local2795_10 * local2795_6;

            try {
               CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
               GpuBuffer gpuBuffer = this.method1841(local2795_11);
               GpuBuffer gpuBuffer2 = this.method1862(local2795_12);
               byteBuffer.position(0).limit(local2795_11);
               commandEncoder.writeToBuffer(gpuBuffer.slice(0L, local2795_11), byteBuffer);
               byteBuffer2.position(0).limit(local2795_12);
               commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, local2795_12), byteBuffer2);
               byteBuffer.clear();
               byteBuffer2.clear();
               ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6 * local2795_10);

               try (RenderPass renderPass = commandEncoder.createRenderPass(
                     () -> "deluxe_glow_source", simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  renderPass.setPipeline(field05962);
                  GpuBuffer gpuBuffer3 = GradientSplitBuffer.method3125();
                  if (gpuBuffer3 != null) {
                     renderPass.setUniform("SplitParams", gpuBuffer3);
                  }

                  renderPass.setVertexBuffer(0, gpuBuffer);
                  renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());

                  for (int index2 = 0; index2 < local2795_10; index2++) {
                     renderPass.setUniform("GlowParamsArray", gpuBuffer2.slice((long)index2 * local2795_6, 1248L));
                     renderPass.drawIndexed(0, index2 * 6, 6, 1);
                  }
               }
            } catch (RuntimeException exception) {
               byteBuffer.clear();
               byteBuffer2.clear();
            }
         }
      }
   }

   private void method1826() {
      if (this.field05981 != null) {
         this.field05981.close();
         this.field05981 = null;
      }
   }

   private BuiltBuffer method1828(BufferAllocator bufferAllocator) {
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, -1.0F, 0.0F);
      return bufferBuilder.end();
   }

   private GpuBuffer method1829() {
      if (this.field05982 != null && !this.field05982.isClosed() && this.field05982.size() >= 48L) {
         return this.field05982;
      }

      this.method1848();

      try {
         this.field05982 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_kawase_params", 136, 48L);
         return this.field05982;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static float method1830(float local2800_0, float local2800_1, float local2800_2) {
      return Math.max(local2800_1, Math.min(local2800_2, local2800_0));
   }

   private static Identifier method1831(String local2801_0) {
      return Identifier.of("deluxe", local2801_0);
   }

   private GlowCompositeRenderer() {
   }

   int method1832(GlowCompositeDrawData glowCompositeDrawData, GlowDrawBatch newGlowDrawBatch) {
      int local2803_3 = this.field05974.size();
      if (local2803_3 >= 224) {
         return -1;
      }

      this.field05974.add(glowCompositeDrawData);
      this.field05975.add(newGlowDrawBatch);
      this.field05988 = true;
      return local2803_3;
   }

   private ByteBuffer method1835(MemoryStack memoryStack) {
      int local2806_2 = Math.max(1, this.field05974.size());
      int local2806_3 = local2806_2 * 5 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(local2806_3);

      for (int index = 0; index < this.field05974.size(); index++) {
         GlowCompositeDrawData glowCompositeDrawData = this.field05974.get(index);
         GlowDrawBatch currentGlowDrawBatch = index < this.field05975.size() ? this.field05975.get(index) : null;
         float local2806_8 = currentGlowDrawBatch != null ? currentGlowDrawBatch.field13605 : 0.0F;
         float local2806_9 = currentGlowDrawBatch != null ? currentGlowDrawBatch.field13606 : 0.0F;
         float local2806_10 = currentGlowDrawBatch != null ? currentGlowDrawBatch.field13607 : 1.0F;
         float local2806_11 = currentGlowDrawBatch != null ? currentGlowDrawBatch.field13608 : 1.0F;
         method1856(byteBuffer, index, glowCompositeDrawData, local2806_8, local2806_9, local2806_10, local2806_11);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void method1836(SimpleFramebuffer simpleFramebuffer) {
      if (simpleFramebuffer != null) {
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         RenderPass renderPass = commandEncoder.createRenderPass(
            () -> "deluxe_glow_clear", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
         );
         if (renderPass != null) {
            renderPass.close();
         }
      }
   }

   public void method1838() {
      if (!this.field05973.isEmpty()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.getWindow() != null) {
            int local2809_2 = this.method1824(2048);
            int local2809_3 = 2048;
            int local2809_4 = Math.max(256, Math.min(2048, (local2809_2 + 256 - 1) / 256 * 256));
            long local2809_5 = System.nanoTime();
            if (local2809_4 >= this.field05971) {
               this.field05971 = local2809_4;
               this.field05972 = local2809_5;
            } else if (local2809_5 - this.field05972 > 3000000000L) {
               this.field05971 = local2809_4;
               this.field05972 = local2809_5;
            }

            int local2809_7 = this.field05971;
            SimpleFramebuffer simpleFramebuffer = this.method1822(0, local2809_3, local2809_7);
            SimpleFramebuffer simpleFramebuffer2 = this.method1820(0, local2809_3, local2809_7);
            SimpleFramebuffer simpleFramebuffer3 = this.method1864(0, local2809_3, local2809_7);
            if (simpleFramebuffer != null && simpleFramebuffer2 != null && simpleFramebuffer3 != null) {
               GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
               this.method1836(simpleFramebuffer);
               float local2809_12 = 0.0F;

               for (int index = 0; index < this.field05973.size(); index++) {
                  local2809_12 = Math.max(local2809_12, this.field05973.get(index).glow().blurRadius());
               }

               this.method1825(simpleFramebuffer, local2809_3, local2809_7);
               int local2809_13 = 3;
               float local2809_14 = method1830(local2809_12 / 20.0F, 0.5F, 4.0F) * 1.0F;
               this.method1843(0, local2809_3, local2809_7, local2809_13);
               SimpleFramebuffer[] simpleFramebufferArray = this.field05966.get(0);
               SimpleFramebuffer[] simpleFramebufferArray2 = this.field05967.get(0);
               GpuTextureView gpuTextureView = simpleFramebuffer.getColorAttachmentView();
               int local2809_18 = simpleFramebuffer.textureWidth;
               int local2809_19 = simpleFramebuffer.textureHeight;

               for (int index2 = 0; index2 < local2809_13; index2++) {
                  this.method1850(
                     field05963, gpuTextureView, local2809_18, local2809_19, simpleFramebufferArray[index2], local2809_14, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler
                  );
                  gpuTextureView = simpleFramebufferArray[index2].getColorAttachmentView();
                  local2809_18 = simpleFramebufferArray[index2].textureWidth;
                  local2809_19 = simpleFramebufferArray[index2].textureHeight;
               }

               for (int index3 = local2809_13 - 2; index3 >= 0; index3--) {
                  this.method1850(
                     field05964, gpuTextureView, local2809_18, local2809_19, simpleFramebufferArray2[index3], local2809_14, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler
                  );
                  gpuTextureView = simpleFramebufferArray2[index3].getColorAttachmentView();
                  local2809_18 = simpleFramebufferArray2[index3].textureWidth;
                  local2809_19 = simpleFramebufferArray2[index3].textureHeight;
               }

               this.method1850(field05964, gpuTextureView, local2809_18, local2809_19, simpleFramebuffer2, local2809_14, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
               this.method1858(simpleFramebuffer2, simpleFramebuffer, simpleFramebuffer3, gpuSampler);
               TextureSetup textureSetup = TextureSetup.of(simpleFramebuffer3.getColorAttachmentView(), gpuSampler);

               for (int index4 = 0; index4 < this.field05973.size(); index4++) {
                  this.field05973.get(index4).capture().field13610 = textureSetup;
               }

               this.field05973.clear();
            } else {
               this.field05973.clear();
            }
         } else {
            this.field05973.clear();
         }
      }
   }

   private GpuBuffer method1841(int local2812_1) {
      if (this.field05984 == null || this.field05984.isClosed() || this.field05984.size() < local2812_1) {
         if (this.field05984 != null) {
            this.field05984.close();
         }

         int local2812_2 = Math.max(local2812_1, 64 * VertexFormats.POSITION.getVertexSize());
         local2812_2 = Integer.highestOneBit(local2812_2 - 1) << 1;
         this.field05984 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_src_quads", 40, local2812_2);
      }

      return this.field05984;
   }

   public static void method1842() {
      GlowCompositeRenderer currentGlowCompositeRenderer = glowCompositeRenderer;
      if (currentGlowCompositeRenderer != null) {
         currentGlowCompositeRenderer.close();
         glowCompositeRenderer = null;
      }
   }

   private void method1843(int local2814_1, int local2814_2, int local2814_3, int local2814_4) {
      while (this.field05966.size() <= local2814_1) {
         this.field05966.add(new SimpleFramebuffer[4]);
         this.field05967.add(new SimpleFramebuffer[4]);
      }

      SimpleFramebuffer[] simpleFramebufferArray = this.field05966.get(local2814_1);
      SimpleFramebuffer[] simpleFramebufferArray2 = this.field05967.get(local2814_1);
      int local2814_7 = Math.max(local2814_2, 1);
      int local2814_8 = Math.max(local2814_3, 1);

      for (int index = 0; index < local2814_4; index++) {
         simpleFramebufferArray[index] = this.method1860(simpleFramebufferArray[index], "deluxe_glow_down_" + local2814_1 + "_" + index, local2814_7, local2814_8);
         if (index < local2814_4 - 1) {
            simpleFramebufferArray2[index] = this.method1860(simpleFramebufferArray2[index], "deluxe_glow_up_" + local2814_1 + "_" + index, local2814_7, local2814_8);
         }

         local2814_7 = Math.max(local2814_7 / 2, 1);
         local2814_8 = Math.max(local2814_8 / 2, 1);
      }
   }

   private static void method1845(List<SimpleFramebuffer[]> local2816_0) {
      for (SimpleFramebuffer[] simpleFramebufferArray : local2816_0) {
         for (int index = 0; index < simpleFramebufferArray.length; index++) {
            if (simpleFramebufferArray[index] != null) {
               simpleFramebufferArray[index].delete();
               simpleFramebufferArray[index] = null;
            }
         }
      }

      local2816_0.clear();
   }

   private static void method1846(ByteBuffer byteBuffer) {
      int local2817_1 = 1120;
      int local2817_2 = Math.max(1, Math.min(ThemePaletteBuffer.method2147(), 6));
      int[] intArray = ThemePaletteBuffer.method2141();
      byteBuffer.putFloat(local2817_1, local2817_2);
      byteBuffer.putFloat(local2817_1 + 4, ThemePaletteBuffer.method2148());
      byteBuffer.putFloat(local2817_1 + 8, ThemePaletteBuffer.method2142());
      byteBuffer.putFloat(local2817_1 + 12, PaletteTransition.getProgress());
      int local2817_4 = local2817_1 + 112;
      byteBuffer.putFloat(local2817_4, ThemePaletteBuffer.method2146());
      byteBuffer.putFloat(local2817_4 + 4, ThemePaletteBuffer.method2151());

      for (int index = 0; index < local2817_2 && index < intArray.length; index++) {
         int local2817_6 = intArray[index] & 16777215;
         int local2817_7 = local2817_1 + (1 + index) * 4 * 4;
         byteBuffer.putFloat(local2817_7, (local2817_6 >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local2817_7 + 4, (local2817_6 >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local2817_7 + 8, (local2817_6 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local2817_7 + 12, 1.0F);
      }
   }

   private void method1847(int local2818_1, GlowCompositeDrawData glowCompositeDrawData, SimpleFramebuffer simpleFramebuffer) {
      GpuBuffer gpuBuffer = this.method1865();
      if (gpuBuffer != null && simpleFramebuffer != null) {
         GpuBuffer gpuBuffer2 = null;

         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(1248);
               method1856(byteBuffer, 0, glowCompositeDrawData, 0.0F, 0.0F, 1.0F, 1.0F);
               byteBuffer.putFloat(48, glowCompositeDrawData.splitIndex());
               method1851(byteBuffer, glowCompositeDrawData);
               method1846(byteBuffer);
               byteBuffer.position(0);
               gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_shape_params_" + local2818_1, 136, byteBuffer);
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
            return;
         }

         try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "deluxe_glow_shape_" + local2818_1, simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
               )) {
               renderPass.setPipeline(field05961);
               renderPass.setUniform("GlowParamsArray", gpuBuffer2);
               GpuBuffer gpuBuffer3 = GradientSplitBuffer.method3125();
               if (gpuBuffer3 != null) {
                  renderPass.setUniform("SplitParams", gpuBuffer3);
               }

               renderPass.setVertexBuffer(0, gpuBuffer);
               renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
               renderPass.drawIndexed(0, 0, 6, 1);
            }
         } finally {
            gpuBuffer2.close();
         }
      }
   }

   private void method1848() {
      if (this.field05982 != null) {
         this.field05982.close();
         this.field05982 = null;
      }
   }

   public void method1849() {
      this.field05980 = null;
   }

   private void method1850(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int local2822_3,
      int local2822_4,
      SimpleFramebuffer simpleFramebuffer,
      float local2822_6,
      float local2822_7,
      float local2822_8,
      float local2822_9,
      float local2822_10,
      GpuSampler gpuSampler
   ) {
      if (gpuTextureView != null && simpleFramebuffer != null) {
         GpuBuffer gpuBuffer = this.method1865();
         if (gpuBuffer != null) {
            GpuBuffer gpuBuffer2 = this.method1829();
            if (gpuBuffer2 != null) {
               CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
               ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(48);
                  byteBuffer.putFloat(0, local2822_7);
                  byteBuffer.putFloat(4, local2822_8);
                  byteBuffer.putFloat(8, local2822_9);
                  byteBuffer.putFloat(12, local2822_10);
                  byteBuffer.putFloat(16, local2822_6 / Math.max(local2822_3, 1));
                  byteBuffer.putFloat(20, local2822_6 / Math.max(local2822_4, 1));
                  byteBuffer.putFloat(32, 0.0F);
                  byteBuffer.putFloat(36, 0.0F);
                  byteBuffer.putFloat(40, 0.0F);
                  byteBuffer.putFloat(44, 0.0F);
                  byteBuffer.position(0);
                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
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

               try (RenderPass local2822_17x = commandEncoder.createRenderPass(
                     () -> "deluxe_glow_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
                  )) {
                  local2822_17x.setPipeline(renderPipeline);
                  local2822_17x.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                  local2822_17x.setUniform("KawaseParams", gpuBuffer2);
                  local2822_17x.setVertexBuffer(0, gpuBuffer);
                  local2822_17x.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                  local2822_17x.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   private static void method1851(ByteBuffer byteBuffer, GlowCompositeDrawData glowCompositeDrawData) {
      float[] floatArray = glowCompositeDrawData.spans();
      int local2823_3 = glowCompositeDrawData.spanCount();
      if (floatArray != null && local2823_3 > 0) {
         float local2823_4 = glowCompositeDrawData.effectivePad();
         float local2823_5 = Math.min(
            Math.min(glowCompositeDrawData.radiusTopLeft(), glowCompositeDrawData.radiusTopRight()), Math.min(glowCompositeDrawData.radiusBottomRight(), glowCompositeDrawData.radiusBottomLeft())
         );
         int local2823_6 = 80;
         byteBuffer.putFloat(local2823_6, Math.min(local2823_3, 64));
         byteBuffer.putFloat(local2823_6 + 4, local2823_5);
         byteBuffer.putFloat(local2823_6 + 8, glowCompositeDrawData.leftAligned());
         byteBuffer.putFloat(local2823_6 + 12, glowCompositeDrawData.bottomAnchored() > 0.5F ? 0.0F : 1.0F);
         int local2823_7 = local2823_6 + 16;
         int local2823_8 = Math.min(local2823_3, 64);
         float local2823_9 = glowCompositeDrawData.height() + local2823_4 * 2.0F;

         for (int index = 0; index < local2823_8; index++) {
            int local2823_11 = index * 4;
            float local2823_12 = floatArray[local2823_11] + local2823_4;
            float local2823_13 = floatArray[local2823_11 + 1] + local2823_4;
            float local2823_14 = floatArray[local2823_11 + 2] + local2823_4;
            float local2823_15 = floatArray[local2823_11 + 3] + local2823_4;
            float local2823_16 = local2823_9 - local2823_15;
            float local2823_17 = local2823_9 - local2823_14;
            int local2823_18 = local2823_7 + index * 4 * 4;
            byteBuffer.putFloat(local2823_18, local2823_12);
            byteBuffer.putFloat(local2823_18 + 4, local2823_13);
            byteBuffer.putFloat(local2823_18 + 8, local2823_16);
            byteBuffer.putFloat(local2823_18 + 12, local2823_17);
         }
      }
   }

   public void method1852() {
      if (!this.field05974.isEmpty() && this.field05988) {
         GpuBuffer gpuBuffer = this.method1817();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method1835(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field05988 = false;
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
               this.field05988 = true;
            }
         }
      }
   }

   public static GlowCompositeRenderer method1853() {
      GlowCompositeRenderer currentGlowCompositeRenderer = glowCompositeRenderer;
      if (currentGlowCompositeRenderer == null) {
         synchronized (GlowCompositeRenderer.class) {
            currentGlowCompositeRenderer = glowCompositeRenderer;
            if (currentGlowCompositeRenderer == null) {
               currentGlowCompositeRenderer = new GlowCompositeRenderer();
               glowCompositeRenderer = currentGlowCompositeRenderer;
            }
         }
      }

      return currentGlowCompositeRenderer;
   }

   public boolean method1854(RenderPipeline renderPipeline) {
      return renderPipeline == field05960;
   }

   private GpuBuffer method1855() {
      if (!this.field05988 && this.field05981 != null) {
         return this.field05981;
      }

      this.method1852();
      if (!this.field05988 && this.field05981 != null) {
         return this.field05981;
      }

      this.method1826();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         ByteBuffer byteBuffer = this.method1835(memoryStack);
         this.field05981 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_params", 128, byteBuffer);
         this.field05988 = false;
         gpuBuffer = this.field05981;
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

      return gpuBuffer;
   }

   private static void method1856(
      ByteBuffer byteBuffer, int local2828_1, GlowCompositeDrawData glowCompositeDrawData, float local2828_3, float local2828_4, float local2828_5, float local2828_6
   ) {
      int local2828_7 = local2828_1 * 5 * 4 * 4;
      byteBuffer.putFloat(local2828_7, glowCompositeDrawData.radiusTopLeft());
      byteBuffer.putFloat(local2828_7 + 4, glowCompositeDrawData.radiusTopRight());
      byteBuffer.putFloat(local2828_7 + 8, glowCompositeDrawData.radiusBottomRight());
      byteBuffer.putFloat(local2828_7 + 12, glowCompositeDrawData.radiusBottomLeft());
      byteBuffer.putFloat(local2828_7 + 16, glowCompositeDrawData.width());
      byteBuffer.putFloat(local2828_7 + 20, glowCompositeDrawData.height());
      byteBuffer.putFloat(local2828_7 + 24, glowCompositeDrawData.effectivePad());
      byteBuffer.putFloat(local2828_7 + 28, 2.0F);
      int local2828_8 = glowCompositeDrawData.color();
      byteBuffer.putFloat(local2828_7 + 32, (local2828_8 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 36, (local2828_8 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 40, (local2828_8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 44, glowCompositeDrawData.intensity() * glowCompositeDrawData.alpha());
      byteBuffer.putFloat(local2828_7 + 48, local2828_3);
      byteBuffer.putFloat(local2828_7 + 52, local2828_4);
      byteBuffer.putFloat(local2828_7 + 56, local2828_5);
      byteBuffer.putFloat(local2828_7 + 60, local2828_6);
      int local2828_9 = glowCompositeDrawData.secondColor();
      byteBuffer.putFloat(local2828_7 + 64, (local2828_9 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 68, (local2828_9 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 72, (local2828_9 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2828_7 + 76, glowCompositeDrawData.colorOffset());
   }

   public void method1857(RenderPass renderPass) {
      if (renderPass != null && !this.field05974.isEmpty()) {
         GpuBuffer gpuBuffer = this.method1855();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlowParamsArray", gpuBuffer);
         }
      }
   }

   private void method1858(SimpleFramebuffer simpleFramebuffer, SimpleFramebuffer simpleFramebuffer2, SimpleFramebuffer simpleFramebuffer3, GpuSampler gpuSampler) {
      if (simpleFramebuffer != null && simpleFramebuffer2 != null && simpleFramebuffer3 != null) {
         GpuBuffer gpuBuffer = this.method1865();
         GpuBuffer gpuBuffer2 = this.method1829();
         if (gpuBuffer != null && gpuBuffer2 != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(48);
               byteBuffer.putFloat(0, 0.0F);
               byteBuffer.putFloat(4, 0.0F);
               byteBuffer.putFloat(8, 1.0F);
               byteBuffer.putFloat(12, 1.0F);
               byteBuffer.position(0);
               commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
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

            try (RenderPass local2830_10x = commandEncoder.createRenderPass(
                  () -> "deluxe_glow_cutout", simpleFramebuffer3.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
               )) {
               local2830_10x.setPipeline(field05965);
               local2830_10x.bindTexture("Sampler0", simpleFramebuffer.getColorAttachmentView(), gpuSampler);
               local2830_10x.bindTexture("Sampler1", simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
               local2830_10x.setUniform("KawaseParams", gpuBuffer2);
               local2830_10x.setVertexBuffer(0, gpuBuffer);
               local2830_10x.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
               local2830_10x.drawIndexed(0, 0, 6, 1);
            }
         }
      }
   }

   public void method1859(GlowCompositeDrawData glowCompositeDrawData) {
      this.method1823(this.field05980, glowCompositeDrawData);
   }

   private SimpleFramebuffer method1860(SimpleFramebuffer simpleFramebuffer, String local2832_2, int local2832_3, int local2832_4) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(local2832_2, local2832_3, local2832_4, false);
      }

      if (simpleFramebuffer.textureWidth != local2832_3 || simpleFramebuffer.textureHeight != local2832_4) {
         simpleFramebuffer.resize(local2832_3, local2832_4);
      }

      return simpleFramebuffer;
   }

   private GpuBuffer method1862(int local2834_1) {
      if (this.field05985 == null || this.field05985.isClosed() || this.field05985.size() < local2834_1) {
         if (this.field05985 != null) {
            this.field05985.close();
         }

         int local2834_2 = Integer.highestOneBit(Math.max(local2834_1, 2) - 1) << 1;
         this.field05985 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_src_params", 136, local2834_2);
      }

      return this.field05985;
   }

   private ByteBuffer method1863(int local2835_1) {
      if (this.field05987 == null || this.field05987.capacity() < local2835_1) {
         if (this.field05987 != null) {
            MemoryUtil.memFree(this.field05987);
         }

         this.field05987 = MemoryUtil.memAlloc(local2835_1);
      }

      this.field05987.clear();
      return this.field05987;
   }

   private SimpleFramebuffer method1864(int local2836_1, int local2836_2, int local2836_3) {
      while (this.field05970.size() <= local2836_1) {
         this.field05970.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.method1860(this.field05970.get(local2836_1), "deluxe_glow_result_" + local2836_1, local2836_2, local2836_3);
      this.field05970.set(local2836_1, simpleFramebuffer);
      return simpleFramebuffer;
   }

   private GpuBuffer method1865() {
      if (this.field05983 != null) {
         return this.field05983;
      }

      try (
         BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
         BuiltBuffer builtBuffer = this.method1828(bufferAllocator);
      ) {
         this.field05983 = RenderSystem.getDevice().createBuffer(() -> "deluxe_glow_fullscreen_quad", 32, builtBuffer.getBuffer());
      }

      return this.field05983;
   }

   private record Nested0410(GlowCompositeDrawData glow, GlowDrawBatch capture, Matrix3x2f pose) {
   }
}
