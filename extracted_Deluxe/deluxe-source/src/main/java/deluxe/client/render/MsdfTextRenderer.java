package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.MsdfTextDrawData;
import deluxe.client.render.MsdfGlyph;
import deluxe.client.render.RoundedClipStack;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class MsdfTextRenderer implements AutoCloseable {
   private static volatile MsdfTextRenderer msdfTextRenderer;
   private static final VertexFormat field08322 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   private static final VertexFormat field08323 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field08324 = RenderPipeline.builder()
      .withLocation(method2383("pipeline/msdf_text"))
      .withVertexShader(method2383("core/msdf_text"))
      .withFragmentShader(method2383("core/msdf_text"))
      .withVertexFormat(field08322, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   public static final RenderPipeline field08325 = RenderPipeline.builder()
      .withLocation(method2383("pipeline/msdf_shimmer"))
      .withVertexShader(method2383("core/msdf_shimmer"))
      .withFragmentShader(method2383("core/msdf_shimmer"))
      .withVertexFormat(field08323, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   private final Map<MsdfTextRenderer.Nested0430, MsdfTextRenderState> field08326 = new LinkedHashMap<>(32);
   private final Map<MsdfTextRenderer.Nested0431, RasterTextRenderState> field08327 = new LinkedHashMap<>(32);
   private final Map<Identifier, MsdfTextRenderer.Nested0432> field08328 = new HashMap<>();
   private final SmallTextAtlasBaker smallTextAtlasBaker = new SmallTextAtlasBaker();
   private DrawContext field08330;
   private boolean field08331;
   private int field08332;

   public float method2359(String local3659_1, String local3659_2, float local3659_3) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(local3659_1);
      return fontAtlas == null ? 0.0F : fontAtlas.method1713(local3659_2, local3659_3);
   }

   public void method2360() {
      this.smallTextAtlasBaker.method3589();
      this.field08330 = null;
      this.field08326.clear();
      this.field08327.clear();
   }

   public boolean method2362() {
      String[] stringArray = FontRegistry.getWarmupNames();
      if (this.field08332 >= stringArray.length) {
         return false;
      }

      FontAtlas fontAtlas = FontRegistry.getAtlas(stringArray[this.field08332++]);
      if (fontAtlas != null) {
         this.smallTextAtlasBaker.method3615(fontAtlas);
      }

      return true;
   }

   public static void method2363() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer != null) {
         currentMsdfTextRenderer.close();
         msdfTextRenderer = null;
      }
   }

   public void method2364() {
      this.smallTextAtlasBaker.method3592();
   }

   private static int method2365(float local3665_0) {
      return Math.round(method2377(local3665_0) * 255.0F);
   }

   private int method2366(int local3666_1, float local3666_2, MsdfTextDrawData msdfTextDrawData) {
      if (!msdfTextDrawData.hasHorizontalFade()) {
         return local3666_1;
      }

      float local3666_4 = 1.0F;
      if (msdfTextDrawData.fadeLeftStrength() > 0.001F) {
         float local3666_5 = method2391(method2377((local3666_2 - msdfTextDrawData.fadeLeftX()) / msdfTextDrawData.fadeWidth()));
         local3666_4 = Math.min(local3666_4, 1.0F - msdfTextDrawData.fadeLeftStrength() * (1.0F - local3666_5));
      }

      if (msdfTextDrawData.fadeRightStrength() > 0.001F) {
         float local3666_5 = method2391(method2377((msdfTextDrawData.fadeRightX() - local3666_2) / msdfTextDrawData.fadeWidth()));
         local3666_4 = Math.min(local3666_4, 1.0F - msdfTextDrawData.fadeRightStrength() * (1.0F - local3666_5));
      }

      int local3666_5 = local3666_1 >>> 24 & 0xFF;
      return Math.round(local3666_5 * local3666_4) << 24 | local3666_1 & 16777215;
   }

   public void method2367(MsdfTextDrawData msdfTextDrawData) {
      this.method2390(this.field08330, msdfTextDrawData, false, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   private static int method2368(int local3668_0, float local3668_1) {
      if (local3668_1 >= 0.999F) {
         return local3668_0;
      }

      if (local3668_1 <= 0.001F) {
         return local3668_0 & 16777215;
      }

      int local3668_2 = Math.round((local3668_0 >>> 24 & 0xFF) * local3668_1);
      return local3668_2 << 24 | local3668_0 & 16777215;
   }

   public float[] method2369(String local3669_1, String local3669_2, float local3669_3) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(local3669_1);
      return fontAtlas == null ? new float[]{0.0F, 0.0F} : fontAtlas.method1704(local3669_2, local3669_3);
   }

   private boolean method2371(
      GuiRenderState guiRenderState,
      int local3671_2,
      Matrix3x2f matrix3x2f,
      ScreenRect screenRect,
      FontAtlas fontAtlas,
      MsdfTextDrawData msdfTextDrawData
   ) {
      float local3671_7 = msdfTextDrawData.size();
      float local3671_8 = SmallTextAtlasBaker.method3611(local3671_7);
      float local3671_9 = GuiScale.method0794();
      int local3671_10 = GuiScale.method0797();
      float local3671_11 = matrix3x2f.m20() * local3671_10;
      float local3671_12 = matrix3x2f.m21() * local3671_10;
      float local3671_13 = msdfTextDrawData.x();
      float local3671_14 = local3671_13;
      int local3671_15 = -1;
      String local3671_16 = msdfTextDrawData.text();
      int local3671_17 = 0;

      while (local3671_17 < local3671_16.length()) {
         int local3671_18 = local3671_16.codePointAt(local3671_17);
         local3671_17 += Character.charCount(local3671_18);
         if (local3671_18 != 10) {
            boolean local3671_19 = !fontAtlas.method1711(local3671_18) && SmallTextAtlasBaker.method3582(local3671_18);
            MsdfGlyph msdfGlyph = fontAtlas.method1714(local3671_18);
            if (local3671_15 != -1) {
               local3671_14 += fontAtlas.method1707(local3671_15, local3671_18) * local3671_7;
            }

            if (msdfGlyph.drawable() || local3671_19) {
               float local3671_21 = local3671_14 * local3671_9 + local3671_11;
               int local3671_22 = Math.round((local3671_21 - (int)Math.floor(local3671_21)) * 4.0F);
               if (local3671_22 == 4) {
                  local3671_22 = 0;
               }

               if (local3671_19
                  ? !this.smallTextAtlasBaker.method3597(local3671_18, local3671_8, local3671_22)
                  : !this.smallTextAtlasBaker.method3594(fontAtlas, local3671_18, msdfGlyph, local3671_8, local3671_22)) {
                  return false;
               }
            }

            local3671_14 += (local3671_19 ? SmallTextAtlasBaker.method3605(local3671_18) : msdfGlyph.advance()) * local3671_7;
            local3671_15 = local3671_18;
         } else {
            local3671_14 = local3671_13;
            local3671_15 = -1;
         }
      }

      float local3671_17x = msdfTextDrawData.y() + fontAtlas.method1710(local3671_7);
      float local3671_18 = fontAtlas.method1708(local3671_7);
      MsdfTextRenderer.Nested0428 local3671_19 = MsdfTextRenderer.Nested0428.of(matrix3x2f);
      float local3671_20 = local3671_13;
      float local3671_21 = Math.round(local3671_17x * local3671_9 + local3671_12);
      int local3671_22 = 0;
      int local3671_23 = -1;
      String local3671_24 = msdfTextDrawData.text();
      int local3671_25 = 0;

      while (local3671_25 < local3671_24.length()) {
         int local3671_26 = local3671_24.codePointAt(local3671_25);
         local3671_25 += Character.charCount(local3671_26);
         if (local3671_26 == 10) {
            local3671_20 = local3671_13;
            local3671_21 = Math.round((local3671_17x + ++local3671_22 * local3671_18) * local3671_9 + local3671_12);
            local3671_23 = -1;
         } else {
            boolean local3671_27 = !fontAtlas.method1711(local3671_26) && SmallTextAtlasBaker.method3582(local3671_26);
            MsdfGlyph msdfGlyph2 = fontAtlas.method1714(local3671_26);
            if (local3671_23 != -1) {
               local3671_20 += fontAtlas.method1707(local3671_23, local3671_26) * local3671_7;
            }

            if (msdfGlyph2.drawable() || local3671_27) {
               float local3671_29 = local3671_20 * local3671_9 + local3671_11;
               int local3671_30 = (int)Math.floor(local3671_29);
               int local3671_31 = Math.round((local3671_29 - local3671_30) * 4.0F);
               if (local3671_31 == 4) {
                  local3671_31 = 0;
                  local3671_30++;
               }

               SmallTextAtlasBaker.Nested0496 local3671_32 = local3671_27
                  ? this.smallTextAtlasBaker.method3588(local3671_26, local3671_8, local3671_31)
                  : this.smallTextAtlasBaker.method3612(fontAtlas, local3671_26, msdfGlyph2, local3671_8, local3671_31);
               if (local3671_32.drawable()) {
                  float local3671_33 = local3671_30;
                  float local3671_34 = (local3671_33 + local3671_32.offsetX() - local3671_11) / local3671_9;
                  float local3671_35 = (local3671_21 - local3671_32.offsetY() - local3671_12) / local3671_9;
                  float local3671_36 = local3671_34 + local3671_32.width() / local3671_9;
                  float local3671_37 = local3671_35 + local3671_32.height() / local3671_9;
                  MsdfTextRenderer.Nested0431 local3671_38 = new MsdfTextRenderer.Nested0431(
                     guiRenderState, local3671_2, local3671_32.page(), local3671_19, screenRect
                  );
                  RasterTextRenderState rasterTextRenderState = this.field08327.get(local3671_38);
                  if (rasterTextRenderState == null) {
                     rasterTextRenderState = new RasterTextRenderState(matrix3x2f, local3671_32.page().method3621(), screenRect);
                     this.field08327.put(local3671_38, rasterTextRenderState);
                     guiRenderState.addPreparedTextElement(rasterTextRenderState);
                  }

                  RasterTextRenderState rasterTextRenderState2 = rasterTextRenderState;
                  method2379(
                     local3671_34,
                     local3671_35,
                     local3671_36,
                     local3671_37,
                     local3671_32.u0(),
                     local3671_32.v0(),
                     local3671_32.u1(),
                     local3671_32.v1(),
                     (local3661_3, local3661_4, local3661_5, local3661_6, local3661_7, local3661_8, local3661_9, local3661_10, local3661_11, local3661_12) -> {
                        int local3661_13 = method2368(this.method2366(msdfTextDrawData.colorTopLeft(), local3661_3, msdfTextDrawData), local3661_11);
                        int local3661_14 = method2368(this.method2366(msdfTextDrawData.colorTopRight(), local3661_5, msdfTextDrawData), local3661_11);
                        int local3661_15 = method2368(this.method2366(msdfTextDrawData.colorBottomRight(), local3661_5, msdfTextDrawData), local3661_12);
                        int local3661_16 = method2368(this.method2366(msdfTextDrawData.colorBottomLeft(), local3661_3, msdfTextDrawData), local3661_12);
                        if ((local3661_13 | local3661_14 | local3661_15 | local3661_16) >>> 24 != 0) {
                           rasterTextRenderState2.add(
                              local3661_3,
                              local3661_4,
                              local3661_5,
                              local3661_6,
                              local3661_7,
                              local3661_8,
                              local3661_9,
                              local3661_10,
                              local3661_13,
                              local3661_14,
                              local3661_15,
                              local3661_16
                           );
                        }
                     }
                  );
               }
            }

            local3671_20 += (local3671_27 ? SmallTextAtlasBaker.method3605(local3671_26) : msdfGlyph2.advance()) * local3671_7;
            local3671_23 = local3671_26;
         }
      }

      return true;
   }

   private TextureSetup method2372(Identifier identifier) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      }

      TextureManager textureManager = minecraftClient.getTextureManager();
      if (textureManager == null) {
         return null;
      }

      AbstractTexture currentTexture = textureManager.getTexture(identifier);
      if (currentTexture != null && currentTexture.getGlTextureView() != null) {
         MsdfTextRenderer.Nested0432 local3672_5 = this.field08328.get(identifier);
         if (local3672_5 != null && local3672_5.texture() == currentTexture) {
            return local3672_5.setup();
         }

         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         this.field08328.put(identifier, new MsdfTextRenderer.Nested0432(currentTexture, textureSetup));
         return textureSetup;
      } else {
         return null;
      }
   }

   private static int method2373(float local3673_0, float local3673_1, float local3673_2, float local3673_3) {
      int local3673_4 = method2365(local3673_3);
      int local3673_5 = method2365((local3673_0 + 0.5F) / 2.0F);
      int local3673_6 = method2365(local3673_1);
      int local3673_7 = method2365(local3673_2);
      return local3673_4 << 24 | local3673_5 << 16 | local3673_6 << 8 | local3673_7;
   }

   private void method2374(
      MsdfTextRenderState msdfTextRenderState,
      GuiRenderState guiRenderState,
      int local3674_3,
      Matrix3x2f matrix3x2f,
      ScreenRect screenRect,
      FontAtlas fontAtlas,
      MsdfTextDrawData msdfTextDrawData,
      boolean local3674_8,
      int local3674_9
   ) {
      float local3674_10 = msdfTextDrawData.size();
      float local3674_11 = msdfTextDrawData.x();
      float local3674_12 = msdfTextDrawData.y() + fontAtlas.method1710(local3674_10);
      float local3674_13 = fontAtlas.method1708(local3674_10);
      float[] floatArray = this.method2382(fontAtlas, msdfTextDrawData.text(), local3674_10);
      float local3674_15 = local3674_11;
      float local3674_16 = local3674_12;
      int local3674_17 = 0;
      int local3674_18 = -1;
      String local3674_19 = msdfTextDrawData.text();
      int local3674_20 = 0;

      while (local3674_20 < local3674_19.length()) {
         int local3674_21 = local3674_19.codePointAt(local3674_20);
         local3674_20 += Character.charCount(local3674_21);
         if (local3674_21 == 10) {
            local3674_15 = local3674_11;
            local3674_16 += local3674_13;
            local3674_17++;
            local3674_18 = -1;
         } else {
            MsdfGlyph msdfGlyph = fontAtlas.method1714(local3674_21);
            if (local3674_18 != -1) {
               local3674_15 += fontAtlas.method1707(local3674_18, local3674_21) * local3674_10;
            }

            if (!fontAtlas.method1711(local3674_21) && SmallTextAtlasBaker.method3582(local3674_21)) {
               if (!this.field08331 && !local3674_8 && Math.abs(msdfTextDrawData.rotationDegrees()) < 0.001F) {
                  SmallTextAtlasBaker.Nested0496 local3674_23 = this.smallTextAtlasBaker.method3588(local3674_21, SmallTextAtlasBaker.method3611(local3674_10), 0);
                  if (local3674_23.drawable()) {
                     float local3674_24 = local3674_15 + local3674_23.offsetX() / GuiScale.method0794();
                     float local3674_25 = local3674_16 - local3674_23.offsetY() / GuiScale.method0794();
                     MsdfTextRenderer.Nested0431 local3674_26 = new MsdfTextRenderer.Nested0431(
                        guiRenderState, local3674_3, local3674_23.page(), MsdfTextRenderer.Nested0428.of(matrix3x2f), screenRect
                     );
                     RasterTextRenderState rasterTextRenderState = this.field08327.get(local3674_26);
                     if (rasterTextRenderState == null) {
                        rasterTextRenderState = new RasterTextRenderState(matrix3x2f, local3674_23.page().method3621(), screenRect);
                        this.field08327.put(local3674_26, rasterTextRenderState);
                        guiRenderState.addPreparedTextElement(rasterTextRenderState);
                     }

                     RasterTextRenderState rasterTextRenderState2 = rasterTextRenderState;
                     method2379(
                        local3674_24,
                        local3674_25,
                        local3674_24 + local3674_23.width() / GuiScale.method0794(),
                        local3674_25 + local3674_23.height() / GuiScale.method0794(),
                        local3674_23.u0(),
                        local3674_23.v0(),
                        local3674_23.u1(),
                        local3674_23.v1(),
                        (local3677_3, local3677_4, local3677_5, local3677_6, local3677_7, local3677_8, local3677_9, local3677_10, local3677_11, local3677_12) -> {
                           int local3677_13 = method2368(this.method2366(msdfTextDrawData.colorTopLeft(), local3677_3, msdfTextDrawData), local3677_11);
                           int local3677_14 = method2368(this.method2366(msdfTextDrawData.colorTopRight(), local3677_5, msdfTextDrawData), local3677_11);
                           int local3677_15 = method2368(this.method2366(msdfTextDrawData.colorBottomRight(), local3677_5, msdfTextDrawData), local3677_12);
                           int local3677_16 = method2368(this.method2366(msdfTextDrawData.colorBottomLeft(), local3677_3, msdfTextDrawData), local3677_12);
                           if ((local3677_13 | local3677_14 | local3677_15 | local3677_16) >>> 24 != 0) {
                              rasterTextRenderState2.add(
                                 local3677_3,
                                 local3677_4,
                                 local3677_5,
                                 local3677_6,
                                 local3677_7,
                                 local3677_8,
                                 local3677_9,
                                 local3677_10,
                                 local3677_13,
                                 local3677_14,
                                 local3677_15,
                                 local3677_16
                              );
                           }
                        }
                     );
                  }
               }

               local3674_15 += SmallTextAtlasBaker.method3605(local3674_21) * local3674_10;
               local3674_18 = local3674_21;
            } else {
               if (msdfGlyph.drawable()) {
                  float local3674_23 = local3674_15 + msdfGlyph.planeLeft() * local3674_10;
                  float local3674_24 = local3674_15 + msdfGlyph.planeRight() * local3674_10;
                  float local3674_25 = local3674_16 - msdfGlyph.planeTop() * local3674_10;
                  float local3674_26 = local3674_16 - msdfGlyph.planeBottom() * local3674_10;
                  float local3674_27 = local3674_17 < floatArray.length ? floatArray[local3674_17] : 1.0F;
                  float local3674_28 = local3674_8 ? method2377((local3674_23 - local3674_11) / local3674_27) : 0.0F;
                  float local3674_29 = local3674_8 ? method2377((local3674_24 - local3674_11) / local3674_27) : 0.0F;
                  if (local3674_8) {
                     int local3674_30 = msdfTextDrawData.colorTopLeft();
                     local3674_28 = method2375(local3674_28, local3674_30);
                     local3674_29 = method2375(local3674_29, local3674_30);
                  }

                  boolean local3674_30 = Math.abs(msdfTextDrawData.rotationDegrees()) >= 0.001F;
                  if (!local3674_8 && !local3674_30) {
                     MsdfTextRenderState msdfTextRenderState2 = msdfTextRenderState;
                     float local3674_32 = local3674_28;
                     float local3674_33 = local3674_29;
                     method2379(
                        local3674_23,
                        local3674_25,
                        local3674_24,
                        local3674_26,
                        msdfGlyph.u0(),
                        msdfGlyph.v0(),
                        msdfGlyph.u1(),
                        msdfGlyph.v1(),
                        (local3670_5, local3670_6, local3670_7, local3670_8, local3670_9, local3670_10, local3670_11, local3670_12, local3670_13, local3670_14) -> {
                           int local3670_15 = method2368(this.method2366(msdfTextDrawData.colorTopLeft(), local3670_5, msdfTextDrawData), local3670_13);
                           int local3670_16 = method2368(this.method2366(msdfTextDrawData.colorTopRight(), local3670_7, msdfTextDrawData), local3670_13);
                           int local3670_17 = method2368(this.method2366(msdfTextDrawData.colorBottomRight(), local3670_7, msdfTextDrawData), local3670_14);
                           int local3670_18 = method2368(this.method2366(msdfTextDrawData.colorBottomLeft(), local3670_5, msdfTextDrawData), local3670_14);
                           if ((local3670_15 | local3670_16 | local3670_17 | local3670_18) >>> 24 != 0) {
                              msdfTextRenderState2.addGlyph(
                                 local3670_5,
                                 local3670_6,
                                 local3670_7,
                                 local3670_8,
                                 local3670_9,
                                 local3670_10,
                                 local3670_11,
                                 local3670_12,
                                 local3670_15,
                                 local3670_16,
                                 local3670_17,
                                 local3670_18,
                                 local3674_32,
                                 local3674_33,
                                 0.0F,
                                 0.0F,
                                 0.0F
                              );
                           }
                        }
                     );
                  } else {
                     int local3674_31 = local3674_8
                        ? local3674_9
                        : RoundedClipStack.method4835(this.method2366(msdfTextDrawData.colorTopLeft(), local3674_23, msdfTextDrawData), local3674_23, local3674_25);
                     int local3674_32 = local3674_8
                        ? local3674_9
                        : RoundedClipStack.method4835(this.method2366(msdfTextDrawData.colorTopRight(), local3674_24, msdfTextDrawData), local3674_24, local3674_25);
                     int local3674_33 = local3674_8
                        ? local3674_9
                        : RoundedClipStack.method4835(this.method2366(msdfTextDrawData.colorBottomRight(), local3674_24, msdfTextDrawData), local3674_24, local3674_26);
                     int local3674_34 = local3674_8
                        ? local3674_9
                        : RoundedClipStack.method4835(this.method2366(msdfTextDrawData.colorBottomLeft(), local3674_23, msdfTextDrawData), local3674_23, local3674_26);
                     if (local3674_8 || (local3674_31 | local3674_32 | local3674_33 | local3674_34) >>> 24 != 0) {
                        msdfTextRenderState.addGlyph(
                           local3674_23,
                           local3674_25,
                           local3674_24,
                           local3674_26,
                           msdfGlyph.u0(),
                           msdfGlyph.v0(),
                           msdfGlyph.u1(),
                           msdfGlyph.v1(),
                           local3674_31,
                           local3674_32,
                           local3674_33,
                           local3674_34,
                           local3674_28,
                           local3674_29,
                           msdfTextDrawData.rotationDegrees(),
                           msdfTextDrawData.rotationOriginX(),
                           msdfTextDrawData.rotationOriginY()
                        );
                     }
                  }
               }

               local3674_15 += msdfGlyph.advance() * local3674_10;
               local3674_18 = local3674_21;
            }
         }
      }
   }

   private static float method2375(float local3675_0, int local3675_1) {
      int local3675_2 = Math.round(method2377(local3675_0) * 63.0F);
      int local3675_3 = Math.round((local3675_1 >>> 16 & 0xFF) / 255.0F * 63.0F);
      int local3675_4 = Math.round((local3675_1 >>> 8 & 0xFF) / 255.0F * 63.0F);
      int local3675_5 = Math.round((local3675_1 & 0xFF) / 255.0F * 63.0F);
      return local3675_2 + local3675_5 * 64 + local3675_4 * 4096 + local3675_3 * 262144;
   }

   @Override
   public void close() {
      this.method2381();
   }

   private static float method2377(float local3679_0) {
      return Math.max(0.0F, Math.min(1.0F, local3679_0));
   }

   public static void method2378() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer != null) {
         currentMsdfTextRenderer.method2381();
      } else {
         FontRegistry.clearCache();
      }
   }

   private static void method2379(
      float local3681_0,
      float local3681_1,
      float local3681_2,
      float local3681_3,
      float local3681_4,
      float local3681_5,
      float local3681_6,
      float local3681_7,
      MsdfTextRenderer.Nested0429 local3681_8
   ) {
      if (!RoundedClipStack.method4837()) {
         local3681_8.method2392(local3681_0, local3681_1, local3681_2, local3681_3, local3681_4, local3681_5, local3681_6, local3681_7, 1.0F, 1.0F);
      } else {
         float local3681_9 = RoundedClipStack.method4834();
         float local3681_10 = RoundedClipStack.method4828();
         float local3681_11 = local3681_9 + RoundedClipStack.method4831();
         float local3681_12 = local3681_10 + RoundedClipStack.method4826();
         if (!(local3681_0 >= local3681_11)
            && !(local3681_2 <= local3681_9)
            && !(local3681_1 >= local3681_12)
            && !(local3681_3 <= local3681_10)
            && !(local3681_2 <= local3681_0)
            && !(local3681_3 <= local3681_1)) {
            float local3681_13 = local3681_2 - local3681_0;
            float local3681_14 = local3681_3 - local3681_1;
            float local3681_15 = Math.max(local3681_0, local3681_9);
            float local3681_16 = Math.min(local3681_2, local3681_11);
            float local3681_17 = Math.max(local3681_1, local3681_10);
            float local3681_18 = Math.min(local3681_3, local3681_12);
            float local3681_19 = local3681_4 + (local3681_6 - local3681_4) * (local3681_15 - local3681_0) / local3681_13;
            float local3681_20 = local3681_4 + (local3681_6 - local3681_4) * (local3681_16 - local3681_0) / local3681_13;
            float local3681_21 = local3681_10 + 1.0F;
            float local3681_22 = local3681_12 - 1.0F;
            float[] floatArray = new float[]{
               local3681_17,
               Math.min(Math.max(local3681_21, local3681_17), local3681_18),
               Math.min(Math.max(local3681_22, local3681_17), local3681_18),
               local3681_18
            };
            Arrays.sort(floatArray);

            for (int index = 0; index < 3; index++) {
               float local3681_25 = floatArray[index];
               float local3681_26 = floatArray[index + 1];
               if (!(local3681_26 - local3681_25 < 1.0E-4F)) {
                  float local3681_27 = local3681_5 + (local3681_7 - local3681_5) * (local3681_25 - local3681_1) / local3681_14;
                  float local3681_28 = local3681_5 + (local3681_7 - local3681_5) * (local3681_26 - local3681_1) / local3681_14;
                  local3681_8.method2392(
                     local3681_15,
                     local3681_25,
                     local3681_16,
                     local3681_26,
                     local3681_19,
                     local3681_27,
                     local3681_20,
                     local3681_28,
                     method2380(local3681_25, local3681_10, local3681_12),
                     method2380(local3681_26, local3681_10, local3681_12)
                  );
               }
            }
         }
      }
   }

   private static float method2380(float local3682_0, float local3682_1, float local3682_2) {
      float local3682_3 = Math.min((local3682_0 - local3682_1) / 1.0F, (local3682_2 - local3682_0) / 1.0F);
      return Math.max(0.0F, Math.min(1.0F, local3682_3));
   }

   private void method2381() {
      this.field08332 = 0;
      this.field08330 = null;
      this.field08326.clear();
      this.field08327.clear();
      this.field08328.clear();
      this.smallTextAtlasBaker.method3581();
      FontRegistry.clearCache();
   }

   private float[] method2382(FontAtlas fontAtlas, String local3684_2, float local3684_3) {
      int local3684_4 = 1;

      for (int index = 0; index < local3684_2.length(); index++) {
         if (local3684_2.charAt(index) == '\n') {
            local3684_4++;
         }
      }

      float[] floatArray = new float[local3684_4];
      int local3684_6 = 0;
      int local3684_7 = -1;
      int local3684_8 = 0;

      while (local3684_8 < local3684_2.length()) {
         int local3684_9 = local3684_2.codePointAt(local3684_8);
         local3684_8 += Character.charCount(local3684_9);
         if (local3684_9 == 10) {
            floatArray[local3684_6] = Math.max(floatArray[local3684_6], 1.0F);
            local3684_6++;
            local3684_7 = -1;
         } else {
            MsdfGlyph msdfGlyph = fontAtlas.method1714(local3684_9);
            if (local3684_7 != -1) {
               floatArray[local3684_6] += fontAtlas.method1707(local3684_7, local3684_9) * local3684_3;
            }

            floatArray[local3684_6] += (
                  !fontAtlas.method1711(local3684_9) && SmallTextAtlasBaker.method3582(local3684_9)
                     ? SmallTextAtlasBaker.method3605(local3684_9)
                     : msdfGlyph.advance()
               )
               * local3684_3;
            local3684_7 = local3684_9;
         }
      }

      for (int local3684_8x = 0; local3684_8x < floatArray.length; local3684_8x++) {
         floatArray[local3684_8x] = Math.max(floatArray[local3684_8x], 1.0F);
      }

      return floatArray;
   }

   private static Identifier method2383(String local3685_0) {
      return Identifier.of("deluxe", local3685_0);
   }

   public void method2384(MsdfTextDrawData msdfTextDrawData, float local3686_2, float local3686_3, float local3686_4, float local3686_5) {
      this.method2390(this.field08330, msdfTextDrawData, true, local3686_2, local3686_3, local3686_4, local3686_5);
   }

   private MsdfTextRenderer() {
   }

   public void method2385(boolean local3688_1) {
      this.field08331 = local3688_1;
   }

   public void method2386(DrawContext drawContext) {
      if (this.field08330 != drawContext) {
         this.field08326.clear();
         this.field08327.clear();
      }

      this.field08330 = drawContext;
      this.smallTextAtlasBaker.method3610();
   }

   public void method2387() {
      this.field08328.clear();
   }

   public boolean method2388(String local3691_1, int local3691_2) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(local3691_1);
      return fontAtlas != null && fontAtlas.method1711(local3691_2);
   }

   public static MsdfTextRenderer method2389() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer == null) {
         synchronized (MsdfTextRenderer.class) {
            currentMsdfTextRenderer = msdfTextRenderer;
            if (currentMsdfTextRenderer == null) {
               currentMsdfTextRenderer = new MsdfTextRenderer();
               msdfTextRenderer = currentMsdfTextRenderer;
            }
         }
      }

      return currentMsdfTextRenderer;
   }

   private void method2390(
      DrawContext drawContext, MsdfTextDrawData msdfTextDrawData, boolean local3693_3, float local3693_4, float local3693_5, float local3693_6, float local3693_7
   ) {
      if (drawContext != null && msdfTextDrawData != null && msdfTextDrawData.visible()) {
         FontAtlas fontAtlas = FontRegistry.getAtlas(msdfTextDrawData.fontName());
         if (fontAtlas != null) {
            try {
               Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               ScreenRect screenRect = ScissorStack.method3969();
               boolean local3693_13 = !local3693_3 && Math.abs(msdfTextDrawData.rotationDegrees()) < 0.001F && msdfTextDrawData.size() <= 20.0F;
               if (local3693_13
                  && !this.field08331
                  && SmallTextAtlasBaker.method3587(matrix3x2f)
                  && this.smallTextAtlasBaker.method3615(fontAtlas)
                  && this.method2371(guiRenderState, currentLayerSerial, matrix3x2f, screenRect, fontAtlas, msdfTextDrawData)) {
                  return;
               }

               TextureSetup textureSetup = this.method2372(fontAtlas.method1715());
               if (textureSetup == null) {
                  return;
               }

               int local3693_15 = local3693_3 ? method2373(local3693_4, local3693_5, local3693_6, local3693_7) : 0;
               MsdfTextRenderer.Nested0430 local3693_16 = new MsdfTextRenderer.Nested0430(
                  guiRenderState, currentLayerSerial, fontAtlas.method1715(), MsdfTextRenderer.Nested0428.of(matrix3x2f), screenRect, local3693_3, local3693_15
               );
               MsdfTextRenderState msdfTextRenderState = this.field08326.get(local3693_16);
               if (msdfTextRenderState == null) {
                  msdfTextRenderState = new MsdfTextRenderState(matrix3x2f, textureSetup, screenRect, local3693_3, fontAtlas.method1716());
                  this.field08326.put(local3693_16, msdfTextRenderState);
                  guiRenderState.addPreparedTextElement(msdfTextRenderState);
               }

               this.method2374(msdfTextRenderState, guiRenderState, currentLayerSerial, matrix3x2f, screenRect, fontAtlas, msdfTextDrawData, local3693_3, local3693_15);
            } catch (RuntimeException exception) {
               DeluxeClient.logger().warn("[MSDF] Failed to submit text: {}", msdfTextDrawData.text(), exception);
            }
         }
      }
   }

   private static float method2391(float local3694_0) {
      return local3694_0 * local3694_0 * (3.0F - 2.0F * local3694_0);
   }

   private record Nested0428(float m00, float m01, float m10, float m11, float m20, float m21) {
      static MsdfTextRenderer.Nested0428 of(Matrix3x2f matrix3x2f) {
         return new MsdfTextRenderer.Nested0428(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private interface Nested0429 {
      void method2392(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10);
   }

   private record Nested0430(
      GuiRenderState state, int layerSerial, Identifier atlas, MsdfTextRenderer.Nested0428 pose, ScreenRect scissorArea, boolean shimmer, int shimmerParams
   ) {
   }

   private record Nested0431(
      GuiRenderState state, int layerSerial, SmallTextAtlasBaker.Nested0495 page, MsdfTextRenderer.Nested0428 pose, ScreenRect scissorArea
   ) {
   }

   private record Nested0432(AbstractTexture texture, TextureSetup setup) {
   }
}
