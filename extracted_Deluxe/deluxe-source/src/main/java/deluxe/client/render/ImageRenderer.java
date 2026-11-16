package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.CachedTexture;
import deluxe.client.render.ImageDrawData;
import deluxe.client.render.TexturedQuadDrawData;
import deluxe.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

public final class ImageRenderer implements AutoCloseable {
   private static volatile ImageRenderer imageRenderer;
   private static final VertexFormat field10741 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field10742 = RenderPipeline.builder()
      .withLocation(method3177("pipeline/image"))
      .withVertexShader(method3177("core/image"))
      .withFragmentShader(method3177("core/image"))
      .withVertexFormat(field10741, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline field10743 = RenderPipeline.builder()
      .withLocation(method3177("pipeline/image_glow"))
      .withVertexShader(method3177("core/image"))
      .withFragmentShader(method3177("core/image"))
      .withVertexFormat(field10741, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, ImageRenderer.Nested0473> field10745 = new HashMap<>();
   private int field10746 = 4;
   private final Map<ImageRenderer.Nested0474, TexturedQuadRenderState> field10747 = new LinkedHashMap<>(32);
   private final List<TexturedQuadDrawData> field10748 = new ArrayList<>(128);
   private final ByteBuffer field10749 = ByteBuffer.allocateDirect(49152).order(ByteOrder.nativeOrder());
   private DrawContext field10750;
   private GpuBuffer field10751;
   private boolean field10752;

   private static int method3159(int local5087_0) {
      return local5087_0;
   }

   private Identifier method3160(String local5088_1) {
      if (local5088_1 != null && !local5088_1.isBlank()) {
         String local5088_2 = local5088_1.trim().replace('\\', '/');
         if (local5088_2.startsWith("/")) {
            local5088_2 = local5088_2.substring(1);
         }

         if (local5088_2.startsWith("assets/")) {
            local5088_2 = local5088_2.substring("assets/".length());
            int local5088_3 = local5088_2.indexOf(47);
            if (local5088_3 >= 0) {
               String local5088_4 = local5088_2.substring(0, local5088_3);
               String local5088_5 = local5088_2.substring(local5088_3 + 1);
               return Identifier.of(local5088_4, local5088_5);
            }
         }

         if (local5088_2.indexOf(58) >= 0) {
            return Identifier.tryParse(local5088_2);
         }

         if (!local5088_2.startsWith("images/")) {
            local5088_2 = "images/" + local5088_2;
         }

         if (!local5088_2.contains(".")) {
            local5088_2 = local5088_2 + ".png";
         }

         return Identifier.of("deluxe", local5088_2);
      } else {
         return null;
      }
   }

   private int method3161() {
      return this.field10748.size() * 6 * 4 * 4;
   }

   public boolean method3162(RenderPipeline renderPipeline) {
      return renderPipeline == field10742 || renderPipeline == field10743;
   }

   private GpuBuffer method3163() {
      if (this.field10751 != null && !this.field10751.isClosed() && this.field10751.size() >= 49152L) {
         return this.field10751;
      }

      this.method3178();

      try {
         this.field10751 = RenderSystem.getDevice().createBuffer(() -> "deluxe_image_params", 136, 49152L);
         return this.field10751;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static float method3164(float local5092_0, float local5092_1, float local5092_2) {
      return Math.max(local5092_1, Math.min(local5092_2, local5092_0));
   }

   private static float method3165(float local5093_0) {
      return !Float.isFinite(local5093_0) ? 0.0F : Math.max(0.001F, local5093_0);
   }

   private static void method3166(ByteBuffer byteBuffer, int local5094_1, int local5094_2) {
      byteBuffer.putFloat(local5094_1, (local5094_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 4, (local5094_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 8, (local5094_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 12, (local5094_2 >>> 24 & 0xFF) / 255.0F);
   }

   public static void method3167() {
      ImageRenderer currentImageRenderer = imageRenderer;
      if (currentImageRenderer != null) {
         currentImageRenderer.close();
         imageRenderer = null;
      }
   }

   public void method3168() {
      if (!this.field10748.isEmpty() && this.field10752) {
         GpuBuffer gpuBuffer = this.method3163();
         if (gpuBuffer != null) {
            try {
               ByteBuffer byteBuffer = this.method3172();
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
               this.field10752 = false;
            } catch (RuntimeException exception) {
               this.field10752 = true;
            }
         }
      }
   }

   private ImageRenderer() {
   }

   public boolean method3169(String local5098_1) {
      return this.method3180(local5098_1) != null;
   }

   @Override
   public void close() {
      this.field10747.clear();
      this.field10748.clear();
      this.field10745.clear();
      this.field10750 = null;
      this.method3178();
   }

   public void method3170() {
      this.field10746 = 4;
      this.field10748.clear();
      this.field10752 = false;
   }

   private GpuBuffer method3171() {
      if (!this.field10752 && this.field10751 != null) {
         return this.field10751;
      }

      this.method3168();
      return !this.field10752 && this.field10751 != null ? this.field10751 : null;
   }

   private ByteBuffer method3172() {
      ByteBuffer byteBuffer = this.field10749;
      byteBuffer.clear();

      for (int index = 0; index < this.field10748.size(); index++) {
         TexturedQuadDrawData texturedQuadDrawData = this.field10748.get(index);
         int local5102_4 = index * 6 * 4 * 4;
         byteBuffer.putFloat(local5102_4, texturedQuadDrawData.radiusTL());
         byteBuffer.putFloat(local5102_4 + 4, texturedQuadDrawData.radiusTR());
         byteBuffer.putFloat(local5102_4 + 8, texturedQuadDrawData.radiusBR());
         byteBuffer.putFloat(local5102_4 + 12, texturedQuadDrawData.radiusBL());
         byteBuffer.putFloat(local5102_4 + 16, texturedQuadDrawData.width());
         byteBuffer.putFloat(local5102_4 + 20, texturedQuadDrawData.height());
         byteBuffer.putFloat(local5102_4 + 24, texturedQuadDrawData.smoothness());
         byteBuffer.putFloat(local5102_4 + 28, 0.0F);
         method3166(byteBuffer, local5102_4 + 32, texturedQuadDrawData.colorTopLeft());
         method3166(byteBuffer, local5102_4 + 48, texturedQuadDrawData.colorTopRight());
         method3166(byteBuffer, local5102_4 + 64, texturedQuadDrawData.colorBottomRight());
         method3166(byteBuffer, local5102_4 + 80, texturedQuadDrawData.colorBottomLeft());
      }

      byteBuffer.limit(this.method3161());
      byteBuffer.position(0);
      return byteBuffer;
   }

   public static ImageRenderer method3174() {
      ImageRenderer currentImageRenderer = imageRenderer;
      if (currentImageRenderer == null) {
         synchronized (ImageRenderer.class) {
            currentImageRenderer = imageRenderer;
            if (currentImageRenderer == null) {
               currentImageRenderer = new ImageRenderer();
               imageRenderer = currentImageRenderer;
            }
         }
      }

      return currentImageRenderer;
   }

   int method3175(TexturedQuadDrawData texturedQuadDrawData) {
      int local5105_2 = this.field10748.size();
      if (local5105_2 == 512) {
         return -1;
      }

      this.field10748.add(texturedQuadDrawData);
      this.field10752 = true;
      return local5105_2;
   }

   private void method3176(DrawContext drawContext, ImageDrawData imageDrawData) {
      if (drawContext != null && imageDrawData != null && imageDrawData.visible()) {
         CachedTexture cachedTexture = this.method3180(imageDrawData.texture());
         if (cachedTexture != null) {
            float local5106_4 = imageDrawData.explicitWidth() > 0.0F ? imageDrawData.explicitWidth() : cachedTexture.drawWidth(imageDrawData.size());
            float local5106_5 = imageDrawData.explicitHeight() > 0.0F ? imageDrawData.explicitHeight() : cachedTexture.drawHeight(imageDrawData.size());
            if (!(local5106_4 <= 0.0F) && !(local5106_5 <= 0.0F)) {
               float local5106_6 = Math.max(0.0F, Math.min(local5106_4, local5106_5) * 0.5F);
               TexturedQuadDrawData texturedQuadDrawData = new TexturedQuadDrawData(
                  imageDrawData.x(),
                  imageDrawData.y(),
                  local5106_4,
                  local5106_5,
                  method3164(imageDrawData.radiusTL(), 0.0F, local5106_6),
                  method3164(imageDrawData.radiusTR(), 0.0F, local5106_6),
                  method3164(imageDrawData.radiusBR(), 0.0F, local5106_6),
                  method3164(imageDrawData.radiusBL(), 0.0F, local5106_6),
                  method3165(imageDrawData.smoothness()),
                  method3159(imageDrawData.colorTopLeft()),
                  method3159(imageDrawData.colorTopRight()),
                  method3159(imageDrawData.colorBottomRight()),
                  method3159(imageDrawData.colorBottomLeft()),
                  method3164(imageDrawData.u0(), 0.0F, 1.0F),
                  method3164(imageDrawData.v0(), 0.0F, 1.0F),
                  method3164(imageDrawData.u1(), 0.0F, 1.0F),
                  method3164(imageDrawData.v1(), 0.0F, 1.0F),
                  Float.isFinite(imageDrawData.rotationDegrees()) ? imageDrawData.rotationDegrees() : 0.0F,
                  Float.isFinite(imageDrawData.rotationOriginX()) ? imageDrawData.rotationOriginX() : 0.0F,
                  Float.isFinite(imageDrawData.rotationOriginY()) ? imageDrawData.rotationOriginY() : 0.0F
               );

               try {
                  GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
                  int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
                  Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
                  ScreenRect screenRect = ScissorStack.method3969();
                  ImageRenderer.Nested0474 local5106_12 = new ImageRenderer.Nested0474(
                     guiRenderState,
                     currentLayerSerial,
                     cachedTexture.id(),
                     imageDrawData.nearest()
                        && Math.abs(texturedQuadDrawData.u1() - texturedQuadDrawData.u0()) * cachedTexture.width()
                           <= local5106_4 * GuiScale.method0794() * matrix3x2f.m00()
                        && Math.abs(texturedQuadDrawData.v1() - texturedQuadDrawData.v0()) * cachedTexture.height()
                           <= local5106_5 * GuiScale.method0794() * matrix3x2f.m00(),
                     ImageRenderer.Nested0475.of(matrix3x2f),
                     screenRect,
                     imageDrawData.glow()
                  );
                  TexturedQuadRenderState texturedQuadRenderState = this.field10747.get(local5106_12);
                  if (texturedQuadRenderState == null) {
                     texturedQuadRenderState = new TexturedQuadRenderState(matrix3x2f, cachedTexture, screenRect, local5106_12.nearest(), imageDrawData.glow());
                     texturedQuadRenderState.add(texturedQuadDrawData);
                     this.field10747.put(local5106_12, texturedQuadRenderState);
                     guiRenderState.addSimpleElement(texturedQuadRenderState);
                  } else {
                     texturedQuadRenderState.add(texturedQuadDrawData);
                  }
               } catch (RuntimeException exception) {
                  DeluxeClient.logger().warn("[ImageRenderer] Failed to submit image: {}", imageDrawData.texture(), exception);
               }
            }
         }
      }
   }

   private static Identifier method3177(String local5107_0) {
      return Identifier.of("deluxe", local5107_0);
   }

   private void method3178() {
      if (this.field10751 != null) {
         this.field10751.close();
         this.field10751 = null;
      }
   }

   public void method3179() {
      this.field10745.clear();
   }

   private CachedTexture method3180(String local5110_1) {
      Identifier identifier = this.method3160(local5110_1);
      if (identifier == null) {
         return null;
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      }

      TextureManager textureManager = minecraftClient.getTextureManager();
      if (textureManager == null) {
         return null;
      }

      if (this.field10746 <= 0 && !this.field10745.containsKey(identifier)) {
         return null;
      }

      AbstractTexture currentTexture = textureManager.getTexture(identifier);
      if (currentTexture != null && currentTexture.getGlTextureView() != null && currentTexture.getGlTexture() != null) {
         ImageRenderer.Nested0473 local5110_6 = this.field10745.get(identifier);
         if (local5110_6 != null && local5110_6.texture() == currentTexture) {
            return local5110_6.value();
         }

         this.field10746--;
         int local5110_7 = Math.max(1, currentTexture.getGlTexture().getWidth(0));
         int local5110_8 = Math.max(1, currentTexture.getGlTexture().getHeight(0));
         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         TextureSetup textureSetup2 = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         CachedTexture cachedTexture = new CachedTexture(identifier, textureSetup, textureSetup2, local5110_7, local5110_8);
         this.field10745.put(identifier, new ImageRenderer.Nested0473(currentTexture, cachedTexture));
         return cachedTexture;
      } else {
         return null;
      }
   }

   public void method3181() {
      this.field10747.clear();
   }

   public void method3182(DrawContext drawContext) {
      if (this.field10750 != drawContext) {
         this.field10747.clear();
      }

      this.field10750 = drawContext;
   }

   public void method3183() {
      this.field10750 = null;
      this.field10747.clear();
   }

   public void method3184(ImageDrawData imageDrawData) {
      this.method3176(this.field10750, imageDrawData);
   }

   public void method3185(RenderPass renderPass) {
      if (renderPass != null && !this.field10748.isEmpty()) {
         GpuBuffer gpuBuffer = this.method3171();
         if (gpuBuffer != null) {
            renderPass.setUniform("ImageParamsArray", gpuBuffer);
         }
      }
   }

   private record Nested0473(AbstractTexture texture, CachedTexture value) {
   }

   private record Nested0474(
      GuiRenderState state, int layerSerial, Identifier texture, boolean nearest, ImageRenderer.Nested0475 pose, ScreenRect scissorArea, boolean glow
   ) {
   }

   private record Nested0475(float m00, float m01, float m10, float m11, float m20, float m21) {
      static ImageRenderer.Nested0475 of(Matrix3x2f matrix3x2f) {
         return new ImageRenderer.Nested0475(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }
}
