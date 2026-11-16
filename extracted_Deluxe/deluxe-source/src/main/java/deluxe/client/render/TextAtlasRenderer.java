package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.render.RasterTextDrawData;
import deluxe.client.util.GuiScale;
import deluxe.client.render.RasterGlyphCache;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.RasterGlyphQuad;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.RasterTextLayout;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class TextAtlasRenderer implements AutoCloseable {
   private static volatile TextAtlasRenderer textAtlasRenderer;
   public static final RenderPipeline field10231 = RenderPipeline.builder()
      .withLocation(method3009("pipeline/text_atlas"))
      .withVertexShader(method3009("core/text_atlas"))
      .withFragmentShader(method3009("core/text_atlas"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline field10232 = RenderPipeline.builder()
      .withLocation(method3009("pipeline/text_atlas_fade"))
      .withVertexShader(method3009("core/text_atlas_fade"))
      .withFragmentShader(method3009("core/text_atlas_fade"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final RasterFontAtlas rasterFontAtlas = new RasterFontAtlas();
   private final Map<TextAtlasRenderer.Nested0468, RasterGlyphRenderState> field10234 = new LinkedHashMap<>(64);
   private DrawContext field10235;

   private RasterTextDrawData method3004(RasterTextDrawData rasterTextDrawData) {
      return new RasterTextDrawData(
         rasterTextDrawData.fontName(),
         rasterTextDrawData.text(),
         rasterTextDrawData.x(),
         rasterTextDrawData.y(),
         rasterTextDrawData.size(),
         method3007(rasterTextDrawData.colorTopLeft()),
         method3007(rasterTextDrawData.colorTopRight()),
         method3007(rasterTextDrawData.colorBottomRight()),
         method3007(rasterTextDrawData.colorBottomLeft()),
         rasterTextDrawData.rotationDegrees(),
         rasterTextDrawData.rotationOriginX(),
         rasterTextDrawData.rotationOriginY(),
         rasterTextDrawData.fadeLeft(),
         rasterTextDrawData.fadeRight(),
         rasterTextDrawData.fadeLeftX(),
         rasterTextDrawData.fadeRightX(),
         rasterTextDrawData.fadeWidth(),
         rasterTextDrawData.fadeLeftStrength(),
         rasterTextDrawData.fadeRightStrength(),
         rasterTextDrawData.snapOrigin()
      );
   }

   private static boolean method3005(int local4775_0) {
      return false;
   }

   public float method3006(String local4776_1, String local4776_2, float local4776_3) {
      return local4776_2 != null && !local4776_2.isEmpty() ? this.rasterFontAtlas.method4806(local4776_1, local4776_3).method1089(local4776_2) : 0.0F;
   }

   private static int method3007(int local4777_0) {
      return method3005(local4777_0) ? local4777_0 | 0xFF000000 : local4777_0;
   }

   private boolean method3008(RasterTextDrawData rasterTextDrawData) {
      return method3005(rasterTextDrawData.colorTopLeft())
         || method3005(rasterTextDrawData.colorTopRight())
         || method3005(rasterTextDrawData.colorBottomRight())
         || method3005(rasterTextDrawData.colorBottomLeft());
   }

   private static Identifier method3009(String local4779_0) {
      return Identifier.of("deluxe", local4779_0);
   }

   public void method3010(DrawContext drawContext, RasterTextDrawData rasterTextDrawData) {
      this.method3014(drawContext);
      this.method3012(rasterTextDrawData);
      this.method3011();
   }

   public void method3011() {
      this.field10235 = null;
      this.field10234.clear();
   }

   public void method3012(RasterTextDrawData rasterTextDrawData) {
      this.method3013(this.field10235, rasterTextDrawData);
   }

   private void method3013(DrawContext drawContext, RasterTextDrawData rasterTextDrawData) {
      if (drawContext != null && rasterTextDrawData != null) {
         RasterTextDrawData rasterTextDrawData2 = this.method3008(rasterTextDrawData) ? this.method3004(rasterTextDrawData) : rasterTextDrawData;
         if (rasterTextDrawData2.visible()) {
            try {
               RasterGlyphCache rasterGlyphCache = this.rasterFontAtlas.method4806(rasterTextDrawData2.fontName(), rasterTextDrawData2.size());
               Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
               RasterTextLayout rasterTextLayout = rasterGlyphCache.method1098(rasterTextDrawData2.text());
               if (rasterTextLayout.method4011()) {
                  return;
               }

               rasterGlyphCache.method1082();
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               TextAtlasRenderer.Nested0464 local4783_9 = TextAtlasRenderer.Nested0464.of(matrix3x2f);
               ScreenRect screenRect = ScissorStack.method3969();
               RenderPipeline renderPipeline = rasterTextDrawData2.hasHorizontalFade() ? field10232 : field10231;

               for (RasterTextLayout.Nested0515 local4783_13 : rasterTextLayout.method4014()) {
                  if (!local4783_13.glyphs().isEmpty()) {
                     TextAtlasRenderer.Nested0468 local4783_14 = new TextAtlasRenderer.Nested0468(
                        guiRenderState, currentLayerSerial, local4783_13.page(), local4783_9, screenRect, renderPipeline
                     );
                     RasterGlyphRenderState rasterGlyphRenderState = this.field10234.get(local4783_14);
                     if (rasterGlyphRenderState == null) {
                        rasterGlyphRenderState = new RasterGlyphRenderState(matrix3x2f, local4783_13.page(), screenRect, renderPipeline);
                        this.field10234.put(local4783_14, rasterGlyphRenderState);
                        guiRenderState.addPreparedTextElement(rasterGlyphRenderState);
                     }

                     rasterGlyphRenderState.add(
                        local4783_13.glyphs(),
                        rasterTextDrawData2.x(),
                        rasterTextDrawData2.y(),
                        rasterTextDrawData2.colorTopLeft(),
                        rasterTextDrawData2.colorTopRight(),
                        rasterTextDrawData2.colorBottomRight(),
                        rasterTextDrawData2.colorBottomLeft(),
                        rasterTextDrawData2.rotationDegrees(),
                        rasterTextDrawData2.rotationOriginX(),
                        rasterTextDrawData2.rotationOriginY(),
                        rasterTextDrawData2.fadeLeft(),
                        rasterTextDrawData2.fadeRight(),
                        rasterTextDrawData2.fadeLeftX(),
                        rasterTextDrawData2.fadeRightX(),
                        rasterTextDrawData2.fadeWidth(),
                        rasterTextDrawData2.fadeLeftStrength(),
                        rasterTextDrawData2.fadeRightStrength(),
                        rasterTextDrawData2.snapOrigin()
                     );
                  }
               }
            } catch (RuntimeException exception) {
               DeluxeClient.logger().warn("[FontRenderer] Failed to submit text: {}", rasterTextDrawData2.text(), exception);
            }
         }
      }
   }

   public void method3014(DrawContext drawContext) {
      if (this.field10235 != drawContext) {
         this.field10234.clear();
         this.rasterFontAtlas.method4785();
      }

      this.field10235 = drawContext;
   }

   private TextAtlasRenderer() {
   }

   public TextAtlasRenderer.Nested0465 method3015(String local4786_1, String local4786_2, float local4786_3) {
      if (local4786_2 != null && !local4786_2.isEmpty() && !(local4786_3 <= 0.0F)) {
         try {
            RasterGlyphCache rasterGlyphCache = this.rasterFontAtlas.method4806(local4786_1, local4786_3);
            RasterTextLayout rasterTextLayout = rasterGlyphCache.method1098(local4786_2);
            if (rasterTextLayout.method4011()) {
               return TextAtlasRenderer.Nested0465.EMPTY;
            }

            rasterGlyphCache.method1082();
            List<TextAtlasRenderer.Nested0467> local4786_6 = new ArrayList<>(rasterTextLayout.method4014().size());

            for (RasterTextLayout.Nested0515 local4786_8 : rasterTextLayout.method4014()) {
               List<TextAtlasRenderer.Nested0466> local4786_9 = new ArrayList<>(local4786_8.glyphs().size());

               for (RasterGlyphQuad rasterGlyphQuad : local4786_8.glyphs()) {
                  local4786_9.add(
                     new TextAtlasRenderer.Nested0466(
                        rasterGlyphQuad.x0(),
                        rasterGlyphQuad.y0(),
                        rasterGlyphQuad.x1(),
                        rasterGlyphQuad.y1(),
                        rasterGlyphQuad.u0(),
                        rasterGlyphQuad.v0(),
                        rasterGlyphQuad.u1(),
                        rasterGlyphQuad.v1()
                     )
                  );
               }

               local4786_6.add(new TextAtlasRenderer.Nested0467(local4786_8.page().method3529(), local4786_9));
            }

            return new TextAtlasRenderer.Nested0465(local4786_6, rasterTextLayout.method4013(), rasterTextLayout.method4012());
         } catch (RuntimeException exception) {
            DeluxeClient.logger().warn("[FontRenderer] Failed to layout glyph pattern: {}", local4786_2, exception);
            return TextAtlasRenderer.Nested0465.EMPTY;
         }
      } else {
         return TextAtlasRenderer.Nested0465.EMPTY;
      }
   }

   public static TextAtlasRenderer method3016() {
      TextAtlasRenderer currentTextAtlasRenderer = textAtlasRenderer;
      if (currentTextAtlasRenderer == null) {
         synchronized (TextAtlasRenderer.class) {
            currentTextAtlasRenderer = textAtlasRenderer;
            if (currentTextAtlasRenderer == null) {
               currentTextAtlasRenderer = new TextAtlasRenderer();
               textAtlasRenderer = currentTextAtlasRenderer;
            }
         }
      }

      return currentTextAtlasRenderer;
   }

   public static void method3017() {
      TextAtlasRenderer currentTextAtlasRenderer = textAtlasRenderer;
      if (currentTextAtlasRenderer != null) {
         currentTextAtlasRenderer.close();
         textAtlasRenderer = null;
      }
   }

   @Override
   public void close() {
      this.field10235 = null;
      this.field10234.clear();
      this.rasterFontAtlas.close();
   }

   private record Nested0464(float m00, float m01, float m10, float m11, float m20, float m21) {
      static TextAtlasRenderer.Nested0464 of(Matrix3x2f matrix3x2f) {
         return new TextAtlasRenderer.Nested0464(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   public record Nested0465(List<TextAtlasRenderer.Nested0467> pages, float width, float height) {
      public static final TextAtlasRenderer.Nested0465 EMPTY = new TextAtlasRenderer.Nested0465(List.of(), 0.0F, 0.0F);

      public boolean empty() {
         return this.pages.isEmpty();
      }

      public Nested0465 {
         pages = List.copyOf(pages);
      }
   }

   public record Nested0466(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
   }

   public record Nested0467(TextureSetup textureSetup, List<TextAtlasRenderer.Nested0466> glyphs) {
      public Nested0467 {
         glyphs = List.copyOf(glyphs);
      }
   }

   private record Nested0468(
      GuiRenderState state, int layerSerial, FontTexture page, TextAtlasRenderer.Nested0464 pose, ScreenRect scissorArea, RenderPipeline pipeline
   ) {
   }
}
