package net.skycore.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public final class TextHeightHelper {
   private final Identifier textureId;
   private static final Map<Identifier, TextHeightHelper> CACHE = new ConcurrentHashMap<>();
   private TextureSetup textureSetup;
   private final AscentHelper metrics;
   private final DistanceRangeMiddleHelper atlas;
   private final Int2ObjectMap<HeightHelper> glyphs;
   private final Long2FloatMap kernings;
   private final HeightHelper fallbackGlyph;
   private final Map<String, TextHeightHelper.Paragraph> shapeCache = new LinkedHashMap<String, TextHeightHelper.Paragraph>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, TextHeightHelper.Paragraph> entry) {
         return this.size() > 1024;
      }
   };
   private static final int SHAPE_CACHE_CAPACITY = 1024;
   private final String name;

   public static TextHeightHelper load(Identifier texture) {
      Objects.requireNonNull(texture, "id");
      var cACHE = CACHE;
      Function<net.minecraft.resources.Identifier, net.skycore.client.util.TextHeightHelper> function = texture2 -> {
         ReadDataHelper readDataHelper = builder();
         String text = texture2.toString();
         readDataHelper = readDataHelper.name(text);
         readDataHelper = readDataHelper.data(texture2);
         UnaryOperator<String> unaryOperator = text2 -> {
            String text3;
            if (text2.endsWith(".json")) {
               int intermediate = text2.length() - 5;
               String text4 = text2.substring(0, intermediate);
               text3 = text4 + ".png";
            } else {
               text3 = text2 + ".png";
            }

            return text3;
         };
         Identifier texture3 = texture2.withPath(unaryOperator);
         readDataHelper = readDataHelper.atlas(texture3);
         return readDataHelper.build();
      };
      return (TextHeightHelper)cACHE.computeIfAbsent(texture, function);
   }

   public float textHeight(float value) {
      return (this.metrics.ascent() - this.metrics.descent()) * value;
   }

   public float kerningAdvance(int value, int value2) {
      return this.kernings.get(packKerningKey(value, value2));
   }

   public static ReadDataHelper builder() {
      return new ReadDataHelper();
   }

   public String name() {
      return this.name;
   }

   private static NativeImage readAtlasImage(Identifier texture) {

      try {
         InputStream resourceManager = MinecraftContext.mc.getResourceManager().open(texture);

         NativeImage nativeImage;
         try {
            nativeImage = NativeImage.read(resourceManager);
         } catch (Throwable failure) {
            if (resourceManager != null) {
               try {
                  resourceManager.close();
               } catch (Throwable failure2) {
                  failure.addSuppressed(failure2);
               }
            }

            throw failure;
         }

         if (resourceManager != null) {
            resourceManager.close();
         }

         return nativeImage;
      } catch (IOException failure3) {
         String text = "Failed to read MSDF atlas image: " + texture;
         throw new IllegalStateException(text, failure3);
      }
   }

   public DistanceRangeMiddleHelper atlas() {
      return this.atlas;
   }

   public AscentHelper metrics() {
      return this.metrics;
   }

   public float distanceRange() {
      return this.atlas.distanceRange();
   }

   public float atlasSize() {
      return this.atlas.size();
   }

   public float lineHeight(float height) {
      return this.metrics.lineHeight() * height;
   }

   public float ascender(float value) {
      return this.metrics.ascent() * value;
   }

   private TextureSetup createTextureSetup() {
      Supplier<java.lang.String> supplier = () -> {
         return this.textureId.toString();
      };
      DynamicTexture dynamicTexture = new DynamicTexture(supplier, readAtlasImage(this.textureId));
      TextureManager textureManager = MinecraftContext.mc.getTextureManager();
      textureManager.register(this.textureId, dynamicTexture);
      GpuTextureView textureView = dynamicTexture.getTextureView();
      SamplerCache samplerCache = RenderSystem.getSamplerCache();
      GpuSampler clampToEdge = samplerCache.getClampToEdge(FilterMode.LINEAR, false);
      return TextureSetup.singleTexture(textureView, clampToEdge);
   }

   public static long packKerningKey(int keyCode, int keyCode2) {
      return (long)keyCode << 32 | keyCode2 & 4294967295L;
   }

   public float localPxPerSdfUnit(float distance) {
      float computedValue = this.atlas.distanceRange() * distance;
      return computedValue / this.atlas.size();
   }

   public boolean canRender(String text) {
      TextHeightHelper.Paragraph paragraph = this.shape(text);
      return !paragraph.missingGlyphs();
   }

   public TextHeightHelper(String name2, Identifier textureId2, DistanceRangeMiddleHelper atlas2, AscentHelper metrics2, Int2ObjectMap<HeightHelper> glyphs2, Long2FloatMap kernings2) {
      this.name = Objects.requireNonNull(name2, "name");
      this.textureId = Objects.requireNonNull(textureId2, "textureId");
      this.atlas = Objects.requireNonNull(atlas2, "atlas");
      this.metrics = Objects.requireNonNull(metrics2, "metrics");
      this.glyphs = Objects.requireNonNull(glyphs2, "glyphs");
      this.kernings = Objects.requireNonNull(kernings2, "kernings");
      this.fallbackGlyph = (HeightHelper)glyphs2.get(63);
   }

   public HeightHelper glyph(int value) {
      return this.glyphs.get(value);
   }

   public TextureSetup textureSetup() {
      if (this.textureSetup == null) {
         TextureSetup textureSetup2 = this.createTextureSetup();
         this.textureSetup = textureSetup2;
      }

      return this.textureSetup;
   }

   public int atlasHeight() {
      return this.atlas.height();
   }

   public int atlasWidth() {
      return this.atlas.width();
   }

   public float getHeight(float height) {
      return this.textHeight(height);
   }

   private TextHeightHelper.Line shapeLine(String text) {
      HeightHelper[] heightHelperValues = new HeightHelper[text.length()];
      float[] floatValues = new float[text.length()];
      int value = 0;
      float height = 0.0F;
      byte flag = 0;
      int computedValue = -1;
      int value2 = 0;

      while (value2 < text.length()) {
         int count = text.codePointAt(value2);
         HeightHelper heightHelper = this.glyphs.get(count);
         if (heightHelper == null) {
            flag = 1;
            heightHelper = this.fallbackGlyph;
         }

         if (heightHelper != null) {
            if (computedValue != -1) {
               height += this.kerningAdvance(computedValue, count);
            }

            heightHelperValues[value] = heightHelper;
            floatValues[value] = height;
            height += heightHelper.advance();
            value++;
            computedValue = count;
         } else {
            computedValue = -1;
         }

         value2 += Character.charCount(count);
      }

      return new TextHeightHelper.Line(heightHelperValues, floatValues, value, height, (flag != 0));
   }

   public float getWidth(String text, float width) {
      return this.shape(text).maxWidth(width, 0.0F);
   }

   public TextHeightHelper.Paragraph shape(String text) {
      Objects.requireNonNull(text, "text");
      TextHeightHelper.Paragraph paragraph = this.shapeCache.get(text);
      if (paragraph != null) {
         return paragraph;
      }

      ArrayList elements = new ArrayList(1);
      boolean condition = false;
      int value = 0;

      for (int value2 = 0; value2 <= text.length(); value2++) {
         if (value2 == text.length() || text.charAt(value2) == '\n') {
            TextHeightHelper.Line line = this.shapeLine(text.substring(value, value2).replace("\r", ""));
            condition |= line.missingGlyphs();
            elements.add(line);
            value = value2 + 1;
         }
      }

      TextHeightHelper.Paragraph paragraph2 = new TextHeightHelper.Paragraph(List.copyOf(elements), condition);
      this.shapeCache.put(text, paragraph2);
      return paragraph2;
   }

   public static final class Line {
      private final boolean missingGlyphs;
      private final float widthEm;
      private final int glyphCount;
      private final HeightHelper[] glyphs;
      private final float[] penEm;

      private Line(HeightHelper[] glyphs2, float[] penEm2, int glyphCount2, float widthEm2, boolean missingGlyphs2) {
         this.glyphs = glyphs2;
         this.penEm = penEm2;
         this.glyphCount = glyphCount2;
         this.widthEm = widthEm2;
         this.missingGlyphs = missingGlyphs2;
      }

      public HeightHelper glyphAt(int value) {
         return this.glyphs[value];
      }

      public float width(float width, float count) {
         float count2 = this.widthEm * width;
         return count2 + count * Math.max(0, this.glyphCount - 1);
      }

      public boolean missingGlyphs() {
         return this.missingGlyphs;
      }

      public int glyphCount() {
         return this.glyphCount;
      }

      public float penX(int value, float value2, float value3) {
         return this.penEm[value] * value2 + value3 * value;
      }
   }

   public record Paragraph(List<TextHeightHelper.Line> lines, boolean missingGlyphs) {
      public boolean missingGlyphs() {
         return this.missingGlyphs;
      }

      public List<TextHeightHelper.Line> lines() {
         return this.lines;
      }

      public float maxWidth(float width, float width2) {
         float result = 0.0F;
         Iterator iterator = this.lines.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return result;
            }

            TextHeightHelper.Line element = (TextHeightHelper.Line)iterator.next();
            float intermediate = element.width(width, width2);
            result = Math.max(result, intermediate);
         }
      }
   }
}
