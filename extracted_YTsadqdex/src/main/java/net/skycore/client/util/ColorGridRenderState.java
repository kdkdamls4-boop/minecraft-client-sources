package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

public final class ColorGridRenderState extends ScissorAreaHelper {
   private final int color;
   private final float x;
   private final ColorGridRenderState.ColorGridTexture texture;
   private final float y;
   private final int rows;
   private final float hoveredThickness;
   private final int selectedIndex;
   private final int hoveredIndex;
   private final int columns;
   private final float cellSize;
   private final float radius;
   private final float selectedThickness;

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float computedValue = this.cellSize * this.columns;
      float computedValue2 = this.cellSize * this.rows;
      float computedValue3 = PackSizeHelper.packDual12Raw(this.selectedIndex + 1, this.hoveredIndex + 1);
      int computedValue4 = PackSizeHelper.packU8Pair(Math.round(this.selectedThickness * 8.0F), Math.round(this.hoveredThickness * 8.0F));
      int value = PackSizeHelper.packU8Pair(this.columns, this.rows);
      int value2 = PackSizeHelper.packSize(computedValue);
      int value3 = PackSizeHelper.packSize(computedValue2);
      this.addVertex(vertexConsumer, this.x, this.y, 0.0F, 0.0F, computedValue3, computedValue4, value, value2, value3);
      this.addVertex(vertexConsumer, this.x, this.y + computedValue2, 0.0F, computedValue2, computedValue3, computedValue4, value, value2, value3);
      this.addVertex(vertexConsumer, this.x + computedValue, this.y + computedValue2, computedValue, computedValue2, computedValue3, computedValue4, value, value2, value3);
      this.addVertex(vertexConsumer, this.x + computedValue, this.y, computedValue, 0.0F, computedValue3, computedValue4, value, value2, value3);
   }

   public ColorGridRenderState(
      Matrix3x2fc matrix3x2fc,
      float x2,
      float y2,
      float value,
      int color2,
      float value2,
      int color3,
      int selectedIndex2,
      float value3,
      int hoveredIndex2,
      float value4,
      int[] intValues,
      ScreenRectangle screenRectangle
   ) {
      super(matrix3x2fc, screenRectangle, x2, y2, Math.max(1.0F, value) * Math.max(1, color2), Math.max(1.0F, value) * Math.max(1, intValues.length / Math.max(1, color2)));
      this.texture = ColorGridRenderState.ColorGridTexture.load(color2, intValues);
      this.x = x2;
      this.y = y2;
      this.cellSize = Math.max(1.0F, value);
      this.columns = Math.max(1, color2);
      this.rows = Math.max(1, intValues.length / this.columns);
      this.radius = Math.max(0.0F, value2);
      this.color = color3;
      this.selectedIndex = selectedIndex2;
      this.selectedThickness = Math.max(0.0F, value3);
      this.hoveredIndex = hoveredIndex2;
      this.hoveredThickness = Math.max(0.0F, value4);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.texture.textureSetup();
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.COLOR_GRID;
   }

   private void addVertex(VertexConsumer vertexConsumer, float value, float value2, float color2, float color3, float value3, int value4, int value5, int color4, int color5) {
      float value6 = this.transformX(value, value2);
      float intermediate = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value6, intermediate, value3);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setColor(this.color).setUv(color2, color3).setUv1(color4, color5);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setUv2(value4, value5).setNormal(0.0F, 0.0F, 1.0F);
      vertexConsumer4.setLineWidth(this.radius);
   }

   static final class ColorGridTexture {
      private TextureSetup textureSetup;
      private static final Map<ColorGridRenderState.ColorGridTexture.Key, ColorGridRenderState.ColorGridTexture> CACHE = new ConcurrentHashMap<>();
      private final ColorGridRenderState.ColorGridTexture.Key key;

      private static ColorGridRenderState.ColorGridTexture load(int color, int[] intValues) {
         var cACHE = CACHE;
         ColorGridRenderState.ColorGridTexture.Key key2 = new ColorGridRenderState.ColorGridTexture.Key(color, intValues);
         return cACHE.computeIfAbsent(key2, ColorGridRenderState.ColorGridTexture::new);
      }

      private TextureSetup textureSetup() {
         if (this.textureSetup == null) {
            Identifier texture = Identifier.parse("skycore:generated/color_grid/" + Integer.toHexString(this.key.hashCode()));
            Supplier<java.lang.String> supplier = () -> {
               return texture.toString();
            };
            DynamicTexture dynamicTexture = new DynamicTexture(supplier, createImage(this.key.columns, this.key.colors));
            MinecraftContext.mc.getTextureManager().register(texture, dynamicTexture);
            this.textureSetup = TextureSetup.singleTexture(dynamicTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, false));
         }

         return this.textureSetup;
      }

      private static NativeImage createImage(int value, int[] intValues) {
         int value2 = 1;
         value2 = (byte)1;
         value2 = (byte)1;
         int maximum = Math.max(1, value);
         int maximum2 = Math.max(1, intValues.length / maximum);
         value2 = (byte)1;
         int intermediate = -1;
         NativeImage nativeImage = new NativeImage(Format.RGBA, maximum, maximum2, false);

         for (int value3 = 0; value3 < intValues.length; value3++) {
            value2 = value3 % maximum;
            int intermediate2 = value3 / maximum;
            intermediate = intValues[value3];
            nativeImage.setPixel(value2, intermediate2, intermediate);
         }

         return nativeImage;
      }

      private ColorGridTexture(ColorGridRenderState.ColorGridTexture.Key key2) {
         this.key = key2;
      }

      public record Key(int columns, int[] colors) {
         public int[] colors() {
            return this.colors;
         }

         @Override
         public int hashCode() {
            int value = 0;
            value = 31 * this.columns;
            return value + Arrays.hashCode(this.colors);
         }

         public int columns() {
            return this.columns;
         }

         public Key(int columns, int[] colors) {
            columns = Math.max(1, columns);
            colors = Arrays.copyOf(colors, colors.length);
            this.columns = columns;
            this.colors = colors;
         }

         @Override
         public boolean equals(Object value) {
            if (value instanceof ColorGridRenderState.ColorGridTexture.Key key) {
               if (this.columns == key.columns) {
                  if (Arrays.equals(this.colors, key.colors)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }
}
