package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.IconDrawData;
import deluxe.client.render.TextureReference;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class EffectIconRenderer implements AutoCloseable {
   private static volatile EffectIconRenderer effectIconRenderer;
   private static final VertexFormat field04125 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final RenderPipeline field04126 = RenderPipeline.builder()
      .withLocation(method1375("pipeline/effect_icon"))
      .withVertexShader(method1375("core/effect_icon"))
      .withFragmentShader(method1375("core/effect_icon"))
      .withVertexFormat(field04125, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, EffectIconRenderer.Nested0380> field04127 = new HashMap<>();
   private final Map<EffectIconRenderer.Nested0382, IconRenderState> field04128 = new LinkedHashMap<>(16);
   private DrawContext field04129;

   public void method1364(DrawContext drawContext) {
      if (this.field04129 != drawContext) {
         this.field04128.clear();
      }

      this.field04129 = drawContext;
   }

   public void method1365() {
      this.field04127.clear();
   }

   public static EffectIconRenderer method1366() {
      EffectIconRenderer currentEffectIconRenderer = effectIconRenderer;
      if (currentEffectIconRenderer == null) {
         synchronized (EffectIconRenderer.class) {
            currentEffectIconRenderer = effectIconRenderer;
            if (currentEffectIconRenderer == null) {
               currentEffectIconRenderer = new EffectIconRenderer();
               effectIconRenderer = currentEffectIconRenderer;
            }
         }
      }

      return currentEffectIconRenderer;
   }

   @Override
   public void close() {
      this.field04128.clear();
      this.field04127.clear();
      this.field04129 = null;
   }

   public void method1367() {
      this.field04129 = null;
      this.field04128.clear();
   }

   private EffectIconRenderer() {
   }

   public void method1368(EffectIconDraw effectIconDraw) {
      this.method1374(this.field04129, effectIconDraw);
   }

   private TextureReference method1369(Identifier identifier) {
      if (identifier == null) {
         return null;
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.getTextureManager() != null) {
         AbstractTexture currentTexture = minecraftClient.getTextureManager().getTexture(identifier);
         if (currentTexture != null && currentTexture.getGlTextureView() != null) {
            EffectIconRenderer.Nested0380 local1964_4 = this.field04127.get(identifier);
            if (local1964_4 != null && local1964_4.texture() == currentTexture) {
               return local1964_4.value();
            }

            TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            TextureReference textureReference = new TextureReference(identifier, textureSetup);
            this.field04127.put(identifier, new EffectIconRenderer.Nested0380(currentTexture, textureReference));
            return textureReference;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void method1370() {
      this.field04128.clear();
   }

   private static int method1371(int local1966_0) {
      return local1966_0;
   }

   public static void method1372() {
      EffectIconRenderer currentEffectIconRenderer = effectIconRenderer;
      if (currentEffectIconRenderer != null) {
         currentEffectIconRenderer.close();
         effectIconRenderer = null;
      }
   }

   private Sprite method1373(EffectIconDraw effectIconDraw) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && effectIconDraw.effect() != null) {
         SpriteAtlasTexture atlasTexture = minecraftClient.getAtlasManager().getAtlasTexture(Atlases.GUI);
         Identifier identifier = InGameHud.getEffectTexture(effectIconDraw.effect());
         return atlasTexture.getSprite(identifier);
      } else {
         return null;
      }
   }

   private void method1374(DrawContext drawContext, EffectIconDraw effectIconDraw) {
      if (drawContext != null && effectIconDraw != null && effectIconDraw.visible()) {
         Sprite sprite = this.method1373(effectIconDraw);
         if (sprite != null) {
            TextureReference textureReference = this.method1369(sprite.getAtlasId());
            if (textureReference != null) {
               IconDrawData iconDrawData = new IconDrawData(
                  effectIconDraw.x(),
                  effectIconDraw.y(),
                  effectIconDraw.size(),
                  sprite.getMinU(),
                  sprite.getMinV(),
                  sprite.getMaxU(),
                  sprite.getMaxV(),
                  method1371(effectIconDraw.color())
               );

               try {
                  GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
                  int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
                  Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
                  IconRenderState iconRenderState = new IconRenderState(matrix3x2f, textureReference);
                  iconRenderState.add(iconDrawData);
                  guiRenderState.addSimpleElement(iconRenderState);
               } catch (RuntimeException exception) {
                  DeluxeClient.logger().warn("[EffectIconRenderer] Failed to submit effect icon", exception);
               }
            }
         }
      }
   }

   private static Identifier method1375(String local1971_0) {
      return Identifier.of("deluxe", local1971_0);
   }

   private record Nested0380(AbstractTexture texture, TextureReference value) {
   }

   private record Nested0381(float m00, float m01, float m10, float m11, float m20, float m21) {
      static EffectIconRenderer.Nested0381 of(Matrix3x2f matrix3x2f) {
         return new EffectIconRenderer.Nested0381(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private record Nested0382(GuiRenderState state, int layerSerial, Identifier texture, EffectIconRenderer.Nested0381 pose) {
   }
}
