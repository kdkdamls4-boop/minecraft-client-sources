package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.HalfIconDrawData;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class RectHalfIconRenderer implements AutoCloseable {
   private static final VertexFormat field13621 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   private static volatile RectHalfIconRenderer rectHalfIconRenderer;
   public static final RenderPipeline field13623 = RenderPipeline.builder()
      .withLocation(method3938("pipeline/rect_half_icon"))
      .withVertexShader(method3938("core/rect_half_icon"))
      .withFragmentShader(method3938("core/rect_half_icon"))
      .withVertexFormat(field13621, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectHalfIconParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<HalfIconDrawData> field13624 = new ArrayList<>(32);
   private final Map<RectHalfIconRenderer.Nested0510, HalfIconRenderState> field13625 = new LinkedHashMap<>(32);
   private DrawContext field13626;
   private GpuBuffer field13627;
   private boolean field13628 = true;

   private static int method3923(int local6502_0, float local6502_1) {
      int local6502_2 = Math.round((local6502_0 >>> 24 & 0xFF) * method3926(local6502_1, 0.0F, 1.0F));
      return local6502_2 << 24 | local6502_0 & 16777215;
   }

   private static float method3924(long local6503_0, int local6503_2, int local6503_3, int local6503_4) {
      long local6503_5 = local6503_0 ^ local6503_2 * -7046029254386353131L ^ local6503_3 * -4658895280553007687L ^ local6503_4 * -7723592293110705685L;
      local6503_5 ^= local6503_5 >>> 30;
      local6503_5 *= -4658895280553007687L;
      local6503_5 ^= local6503_5 >>> 27;
      local6503_5 *= -7723592293110705685L;
      local6503_5 ^= local6503_5 >>> 31;
      return (float)(local6503_5 & 16777215L) / 1.6777215E7F * 2.0F - 1.0F;
   }

   public void method3925() {
      if (!this.field13624.isEmpty() && this.field13628) {
         GpuBuffer gpuBuffer = this.method3942();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.method3929(memoryStack, this.field13624);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.field13628 = false;
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
               this.field13628 = true;
            }
         }
      }
   }

   private static float method3926(float local6505_0, float local6505_1, float local6505_2) {
      return Math.max(local6505_1, Math.min(local6505_2, local6505_0));
   }

   private void method3927(DrawContext drawContext, HalfIconDrawData halfIconDrawData) {
      if (drawContext != null && halfIconDrawData != null && halfIconDrawData.iconsVisible()) {
         try {
            HalfIconDrawData halfIconDrawData2 = this.method3931(halfIconDrawData);
            TextAtlasRenderer.Nested0465 local6506_4 = TextAtlasRenderer.method3016()
               .method3015(halfIconDrawData2.fontName(), halfIconDrawData2.icon(), halfIconDrawData2.iconSize());
            if (local6506_4.empty()) {
               return;
            }

            Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
            GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
            int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            RectHalfIconRenderer.Nested0512 local6506_8 = RectHalfIconRenderer.Nested0512.of(matrix3x2f);

            for (TextAtlasRenderer.Nested0467 local6506_10 : local6506_4.pages()) {
               if (!local6506_10.glyphs().isEmpty()) {
                  List<HalfIconRenderState.Nested0453> local6506_11 = this.method3928(halfIconDrawData2, local6506_4, local6506_10);
                  if (!local6506_11.isEmpty()) {
                     RectHalfIconRenderer.Nested0510 local6506_12 = new RectHalfIconRenderer.Nested0510(
                        guiRenderState, currentLayerSerial, local6506_10.textureSetup(), local6506_8
                     );
                     HalfIconRenderState halfIconRenderState = this.field13625.get(local6506_12);
                     if (halfIconRenderState == null) {
                        halfIconRenderState = new HalfIconRenderState(matrix3x2f, local6506_10.textureSetup());
                        halfIconRenderState.add(halfIconDrawData2, local6506_11);
                        this.field13625.put(local6506_12, halfIconRenderState);
                        guiRenderState.addSimpleElement(halfIconRenderState);
                     } else {
                        halfIconRenderState.add(halfIconDrawData2, local6506_11);
                     }
                  }
               }
            }
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[HalfIconRectangle] Failed to submit icon rectangle", exception);
         }
      }
   }

   private List<HalfIconRenderState.Nested0453> method3928(
      HalfIconDrawData halfIconDrawData, TextAtlasRenderer.Nested0465 local6507_2, TextAtlasRenderer.Nested0467 local6507_3
   ) {
      List<HalfIconRenderState.Nested0453> local6507_4 = new ArrayList<>(64);
      float local6507_5 = Math.max(halfIconDrawData.spacingX(), Math.max(local6507_2.width(), 1.0F));
      float local6507_6 = Math.max(halfIconDrawData.spacingY(), Math.max(local6507_2.height(), 1.0F));
      float local6507_7 = halfIconDrawData.x() + halfIconDrawData.paddingX() + local6507_2.width() * 0.5F;
      float local6507_8 = halfIconDrawData.y() + halfIconDrawData.paddingY() + local6507_2.height() * 0.5F;
      float local6507_9 = halfIconDrawData.x() + halfIconDrawData.width() - halfIconDrawData.paddingX() - local6507_2.width() * 0.5F;
      float local6507_10 = halfIconDrawData.y() + halfIconDrawData.height() - halfIconDrawData.paddingY() - local6507_2.height() * 0.5F;
      if (!(local6507_9 < local6507_7) && !(local6507_10 < local6507_8)) {
         int local6507_11 = Math.min(96, Math.max(1, (int)Math.floor((local6507_10 - local6507_8) / local6507_6) + 1));
         int local6507_12 = Math.min(96, Math.max(1, (int)Math.floor((local6507_9 - local6507_7) / local6507_5) + 1));

         for (int index = 0; index < local6507_11; index++) {
            for (int index2 = 0; index2 < local6507_12; index2++) {
               float local6507_15 = local6507_7
                  + index2 * local6507_5
                  + method3924(halfIconDrawData.seed(), index2, index, 1) * halfIconDrawData.jitterX();
               float local6507_16 = local6507_8
                  + index * local6507_6
                  + method3924(halfIconDrawData.seed(), index2, index, 2) * halfIconDrawData.jitterY();
               float local6507_17 = (float)Math.toRadians(method3924(halfIconDrawData.seed(), index2, index, 3) * halfIconDrawData.rotationMaxDegrees());
               float local6507_18 = (float)Math.sin(local6507_17);
               float local6507_19 = (float)Math.cos(local6507_17);
               float local6507_20 = local6507_15 - local6507_2.width() * 0.5F;
               float local6507_21 = local6507_16 - local6507_2.height() * 0.5F;
               int local6507_22 = method3923(
                  halfIconDrawData.iconColor(), 1.0F - method3926((local6507_16 - halfIconDrawData.y()) / Math.max(halfIconDrawData.height(), 1.0F), 0.0F, 1.0F) * 0.92F
               );

               for (TextAtlasRenderer.Nested0466 local6507_24 : local6507_3.glyphs()) {
                  float local6507_25 = local6507_20 + local6507_24.x0();
                  float local6507_26 = local6507_21 + local6507_24.y0();
                  float local6507_27 = local6507_20 + local6507_24.x1();
                  float local6507_28 = local6507_21 + local6507_24.y1();
                  RectHalfIconRenderer.Nested0511 local6507_29 = method3930(local6507_25, local6507_26, local6507_15, local6507_16, local6507_18, local6507_19);
                  RectHalfIconRenderer.Nested0511 local6507_30 = method3930(local6507_25, local6507_28, local6507_15, local6507_16, local6507_18, local6507_19);
                  RectHalfIconRenderer.Nested0511 local6507_31 = method3930(local6507_27, local6507_28, local6507_15, local6507_16, local6507_18, local6507_19);
                  RectHalfIconRenderer.Nested0511 local6507_32 = method3930(local6507_27, local6507_26, local6507_15, local6507_16, local6507_18, local6507_19);
                  local6507_4.add(
                     new HalfIconRenderState.Nested0453(
                        local6507_29.x(),
                        local6507_29.y(),
                        local6507_24.u0(),
                        local6507_24.v0(),
                        local6507_30.x(),
                        local6507_30.y(),
                        local6507_24.u0(),
                        local6507_24.v1(),
                        local6507_31.x(),
                        local6507_31.y(),
                        local6507_24.u1(),
                        local6507_24.v1(),
                        local6507_32.x(),
                        local6507_32.y(),
                        local6507_24.u1(),
                        local6507_24.v0(),
                        local6507_22
                     )
                  );
               }
            }
         }

         return local6507_4;
      } else {
         return List.of();
      }
   }

   private ByteBuffer method3929(MemoryStack memoryStack, List<HalfIconDrawData> local6508_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(12288, local6508_2.size() * 3 * 4 * 4));

      for (int index = 0; index < local6508_2.size(); index++) {
         HalfIconDrawData halfIconDrawData = local6508_2.get(index);
         int local6508_6 = index * 3 * 4 * 4;
         byteBuffer.putFloat(local6508_6, halfIconDrawData.x());
         byteBuffer.putFloat(local6508_6 + 4, halfIconDrawData.y());
         byteBuffer.putFloat(local6508_6 + 8, halfIconDrawData.width());
         byteBuffer.putFloat(local6508_6 + 12, halfIconDrawData.height());
         byteBuffer.putFloat(local6508_6 + 16, halfIconDrawData.radiusTopLeft());
         byteBuffer.putFloat(local6508_6 + 20, halfIconDrawData.radiusTopRight());
         byteBuffer.putFloat(local6508_6 + 24, halfIconDrawData.radiusBottomRight());
         byteBuffer.putFloat(local6508_6 + 28, halfIconDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local6508_6 + 32, halfIconDrawData.smoothness());
         byteBuffer.putFloat(local6508_6 + 36, 0.0F);
         byteBuffer.putFloat(local6508_6 + 40, 0.0F);
         byteBuffer.putFloat(local6508_6 + 44, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private static RectHalfIconRenderer.Nested0511 method3930(
      float local6509_0, float local6509_1, float local6509_2, float local6509_3, float local6509_4, float local6509_5
   ) {
      float local6509_6 = local6509_0 - local6509_2;
      float local6509_7 = local6509_1 - local6509_3;
      return new RectHalfIconRenderer.Nested0511(
         local6509_2 + local6509_6 * local6509_5 - local6509_7 * local6509_4, local6509_3 + local6509_6 * local6509_4 + local6509_7 * local6509_5
      );
   }

   private HalfIconDrawData method3931(HalfIconDrawData halfIconDrawData) {
      float local6510_2 = Math.max(0.0F, Math.min(halfIconDrawData.width(), halfIconDrawData.height()) * 0.5F);
      float local6510_3 = Math.max(halfIconDrawData.iconSize(), 1.0F);
      return new HalfIconDrawData(
         halfIconDrawData.x(),
         halfIconDrawData.y(),
         halfIconDrawData.width(),
         halfIconDrawData.height(),
         method3926(halfIconDrawData.radiusTopLeft(), 0.0F, local6510_2),
         method3926(halfIconDrawData.radiusTopRight(), 0.0F, local6510_2),
         method3926(halfIconDrawData.radiusBottomRight(), 0.0F, local6510_2),
         method3926(halfIconDrawData.radiusBottomLeft(), 0.0F, local6510_2),
         halfIconDrawData.colorTopLeft(),
         halfIconDrawData.colorTopRight(),
         halfIconDrawData.colorBottomRight(),
         halfIconDrawData.colorBottomLeft(),
         Math.max(halfIconDrawData.smoothness(), 0.0F),
         halfIconDrawData.fontName(),
         halfIconDrawData.icon(),
         local6510_3,
         halfIconDrawData.iconColor(),
         Math.max(halfIconDrawData.spacingX(), local6510_3),
         Math.max(halfIconDrawData.spacingY(), local6510_3),
         Math.max(halfIconDrawData.paddingX(), 0.0F),
         Math.max(halfIconDrawData.paddingY(), 0.0F),
         Math.max(halfIconDrawData.jitterX(), 0.0F),
         Math.max(halfIconDrawData.jitterY(), 0.0F),
         method3926(halfIconDrawData.rotationMaxDegrees(), 0.0F, 90.0F),
         halfIconDrawData.seed()
      );
   }

   private void method3932() {
      if (this.field13627 != null) {
         this.field13627.close();
         this.field13627 = null;
      }
   }

   public static RectHalfIconRenderer method3933() {
      RectHalfIconRenderer currentRectHalfIconRenderer = rectHalfIconRenderer;
      if (currentRectHalfIconRenderer == null) {
         synchronized (RectHalfIconRenderer.class) {
            currentRectHalfIconRenderer = rectHalfIconRenderer;
            if (currentRectHalfIconRenderer == null) {
               currentRectHalfIconRenderer = new RectHalfIconRenderer();
               rectHalfIconRenderer = currentRectHalfIconRenderer;
            }
         }
      }

      return currentRectHalfIconRenderer;
   }

   private GpuBuffer method3935() {
      if (!this.field13628 && this.field13627 != null) {
         return this.field13627;
      }

      this.method3925();
      if (!this.field13628 && this.field13627 != null) {
         return this.field13627;
      }

      this.method3932();
      this.method3925();
      return this.field13627;
   }

   int method3936(HalfIconDrawData halfIconDrawData) {
      int local6515_2 = this.field13624.size();
      if (local6515_2 == 256) {
         return -1;
      }

      this.field13624.add(halfIconDrawData);
      this.field13628 = true;
      return local6515_2;
   }

   public void method3937(RenderPass renderPass) {
      if (renderPass != null && !this.field13624.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3935();
         if (gpuBuffer != null) {
            renderPass.setUniform("RectHalfIconParamsArray", gpuBuffer);
         }
      }
   }

   private static Identifier method3938(String local6517_0) {
      return Identifier.of("deluxe", local6517_0);
   }

   public boolean method3939(RenderPipeline renderPipeline) {
      return renderPipeline == field13623;
   }

   public void method3940(HalfIconDrawData halfIconDrawData) {
      this.method3927(this.field13626, halfIconDrawData);
   }

   public static void method3941() {
      RectHalfIconRenderer currentRectHalfIconRenderer = rectHalfIconRenderer;
      if (currentRectHalfIconRenderer != null) {
         currentRectHalfIconRenderer.close();
         rectHalfIconRenderer = null;
      }
   }

   @Override
   public void close() {
      this.field13624.clear();
      this.field13625.clear();
      this.field13626 = null;
      this.method3932();
   }

   private GpuBuffer method3942() {
      if (this.field13627 != null && !this.field13627.isClosed() && this.field13627.size() >= 12288L) {
         return this.field13627;
      }

      this.method3932();

      try {
         this.field13627 = RenderSystem.getDevice().createBuffer(() -> "deluxe_rect_half_icon_params", 136, 12288L);
         return this.field13627;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private RectHalfIconRenderer() {
   }

   public void method3943(DrawContext drawContext, HalfIconDrawData halfIconDrawData) {
      this.method3945(drawContext);
      this.method3940(halfIconDrawData);
      this.method3946();
   }

   public void method3944() {
      this.field13624.clear();
      this.field13628 = false;
   }

   public void method3945(DrawContext drawContext) {
      if (this.field13626 != drawContext) {
         this.field13625.clear();
      }

      this.field13626 = drawContext;
   }

   public void method3946() {
      this.field13626 = null;
      this.field13625.clear();
   }

   private record Nested0510(GuiRenderState state, int layerSerial, TextureSetup textureSetup, RectHalfIconRenderer.Nested0512 pose) {
   }

   private record Nested0511(float x, float y) {
   }

   private record Nested0512(float m00, float m01, float m10, float m11, float m20, float m21) {
      static RectHalfIconRenderer.Nested0512 of(Matrix3x2f matrix3x2f) {
         return new RectHalfIconRenderer.Nested0512(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }
}
