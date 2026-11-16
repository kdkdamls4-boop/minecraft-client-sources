package net.skycore.client.util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SvgTextureCache {
   private volatile SVGDocument document;
   private static final int MAX_TEXTURE_SIZE = 2048;
   private final LinkedHashMap<Long, TextureSetup> svgVariants;
   private BufferedImage rasterImage;
   private static final int MAX_VARIANTS = 8;
   private TextureSetup staticSetup;
   private static final Map<Identifier, SvgTextureCache> CACHE = new ConcurrentHashMap<>();
   private final LinkedHashMap<Long, TextureSetup> pngVariants = new LinkedHashMap<Long, TextureSetup>(8, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Long, TextureSetup> entry) {
         if (this.size() <= 8) {
            return false;
         }

         TextureManager textureManager = MinecraftContext.mc.getTextureManager();
         Identifier texture = SvgTextureCache.this.textureId;
         long key = (Long)entry.getKey();
         Identifier texture2 = SvgTextureCache.variantId(texture, key);
         textureManager.release(texture2);
         return true;
      }
   };
   private final Identifier textureId;
   private static final Logger LOGGER = LoggerFactory.getLogger(SvgTextureCache.class);
   private static final int SUPERSAMPLE = 2;
   private final boolean svg;

   public TextureSetup textureSetup() {
      if (!this.svg) {
         return this.staticSetup();
      }

      FloatSize floatSize = this.documentSize();
      return this.textureSetup((float)floatSize.getWidth(), (float)floatSize.getHeight());
   }

   private static int unpackHeight(long timestamp) {
      return (int)timestamp;
   }

   private TextureSetup createSvgSetup(long timestamp) {
      int computedValue = Math.min(2048, unpackWidth(timestamp) * 2);
      int computedValue2 = Math.min(2048, unpackHeight(timestamp) * 2);
      Identifier texture = variantId(this.textureId, timestamp);
      Supplier supplier = texture::toString;
      DynamicTexture dynamicTexture = new DynamicTexture(supplier, rasterizeSvg(this.textureId, this.document(), computedValue, computedValue2));
      dynamicTexture.upload();
      MinecraftContext.mc.getTextureManager().register(texture, dynamicTexture);
      GpuTextureView textureView = dynamicTexture.getTextureView();
      GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, false);
      return TextureSetup.singleTexture(textureView, clampToEdge);
   }

   private BufferedImage rasterImage() {
      if (this.rasterImage == null) {
         BufferedImage image = readBufferedPng(this.textureId);
         this.rasterImage = image;
      }

      return this.rasterImage;
   }

   private static NativeImage toNativeImage(BufferedImage image) {
      int width = image.getWidth();
      int height = image.getHeight();
      NativeImage nativeImage = new NativeImage(Format.RGBA, width, height, false);

      for (int value = 0; value < height; value++) {
         for (int value2 = 0; value2 < width; value2++) {
            int rGB = image.getRGB(value2, value);
            if ((rGB >>> 24 & 0xFF) == 0) {
               rGB = 0;
            }

            nativeImage.setPixel(value2, value, rGB);
         }
      }

      return nativeImage;
   }

   public TextureSetup textureSetup(float value, float value2) {
      if (!this.svg) {
         return this.pngSetup(value, value2);
      }

      long timestamp = packSize(quantize(value), quantize(value2));
      TextureSetup textureSetup = this.svgVariants.get(timestamp);
      if (textureSetup != null) {
         return textureSetup;
      }

      TextureSetup textureSetup2 = this.createSvgSetup(timestamp);
      var svgVariants2 = this.svgVariants;
      Long element = timestamp;
      svgVariants2.put(element, textureSetup2);
      return textureSetup2;
   }

   private static BufferedImage readBufferedPng(Identifier texture) {
      try (InputStream resourceManager = MinecraftContext.mc.getResourceManager().open(texture)) {
         BufferedImage image = ImageIO.read(resourceManager);
         if (image == null) {
            throw new IOException("Unsupported PNG: " + texture);
         } else {
            return image;
         }
      } catch (IOException failure) {
         throw new IllegalStateException("Failed to read GUI texture: " + texture, failure);
      }
   }

   public static SvgTextureCache load(Identifier texture) {
      var cACHE = CACHE;
      Function<Identifier, SvgTextureCache> function = SvgTextureCache::new;
      return (SvgTextureCache)cACHE.computeIfAbsent(texture, function);
   }

   private static long packSize(int value, int value2) {
      return (long)value << 32 | value2 & 4294967295L;
   }

   private static SVGDocument loadDocument(Identifier texture) {
      try (InputStream resourceManager = MinecraftContext.mc.getResourceManager().open(texture)) {
         URI namespace = URI.create("resource://" + texture.getNamespace() + "/" + texture.getPath());
         SVGDocument sVGDocument = new SVGLoader().load(resourceManager, namespace, LoaderContext.createDefault());
         if (sVGDocument == null) {
            throw new IOException("Failed to parse SVG document: " + texture);
         } else {
            return sVGDocument;
         }
      } catch (IOException failure) {
         throw new IllegalStateException("Failed to read GUI texture: " + texture, failure);
      }
   }

   public static void prewarm(Collection<Identifier> identifiers) {
      Runnable runnable = () -> {
         Iterator iterator = identifiers.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            Identifier element = (Identifier)iterator.next();

            try {
               SvgTextureCache textureSetupHelper4 = load(element);
               if (textureSetupHelper4.svg) {
                  textureSetupHelper4.document();
               }
            } catch (Throwable failure) {
               LOGGER.warn("Failed to prewarm GUI texture {}", element, failure);
            }
         }
      };
      Thread thread = new Thread(runnable, "Blade SVG Prewarm");
      thread.setDaemon(true);
      thread.start();
   }

   private SvgTextureCache(Identifier textureId2) {
      this.svgVariants = new LinkedHashMap<Long, TextureSetup>(8, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<Long, TextureSetup> entry) {
            if (this.size() <= 8) {
               return false;
            }

            TextureManager textureManager = MinecraftContext.mc.getTextureManager();
            Identifier texture = SvgTextureCache.this.textureId;
            Long key = (Long)entry.getKey();
            long timestamp = key;
            Identifier texture2 = SvgTextureCache.variantId(texture, timestamp);
            textureManager.release(texture2);
            return true;
         }
      };
      this.textureId = textureId2;
      this.svg = textureId2.getPath().endsWith(".svg");
   }

   private static BufferedImage downscalePng(BufferedImage image, int width, int height) {
      BufferedImage image2 = new BufferedImage(image.getWidth(), image.getHeight(), 3);
      Graphics2D graphics2D = image2.createGraphics();

      try {
         graphics2D.setComposite(AlphaComposite.Src);
         graphics2D.drawImage(image, 0, 0, null);
      } finally {
         graphics2D.dispose();
      }

      while (image2.getWidth() / 2 >= width && image2.getHeight() / 2 >= height) {
         int width2 = Math.max(width, image2.getWidth() / 2);
         int height2 = Math.max(height, image2.getHeight() / 2);
         BufferedImage image3 = new BufferedImage(width2, height2, 3);
         Graphics2D graphics2D2 = image3.createGraphics();

         try {
            graphics2D2.setComposite(AlphaComposite.Src);
            graphics2D2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics2D2.drawImage(image2, 0, 0, width2, height2, null);
         } finally {
            graphics2D2.dispose();
         }

         image2 = image3;
      }

      if (image2.getWidth() == width && image2.getHeight() == height) {
         return image2;
      }

      BufferedImage image4 = new BufferedImage(width, height, 3);
      Graphics2D graphics2D3 = image4.createGraphics();

      try {
         graphics2D3.setComposite(AlphaComposite.Src);
         graphics2D3.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         graphics2D3.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         graphics2D3.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
         graphics2D3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics2D3.drawImage(image2, 0, 0, width, height, null);
      } finally {
         graphics2D3.dispose();
      }

      return image4;
   }

   private static int unpackWidth(long timestamp) {
      return (int)(timestamp >>> 32);
   }

   private static NativeImage readPng(Identifier texture) {

      try {

         try (InputStream resourceManager = MinecraftContext.mc.getResourceManager().open(texture)) {
            return NativeImage.read(resourceManager);
         }
      } catch (IOException failure) {
         String text = "Failed to read GUI texture: " + texture;
         throw new IllegalStateException(text, failure);
      }
   }

   private static int quantize(float value) {
      return Math.max(1, (int)Math.ceil(value));
   }

   private SVGDocument document() {
      SVGDocument document2 = this.document;
      if (document2 == null) {
         document2 = loadDocument(this.textureId);
         this.document = document2;
      }

      return document2;
   }

   private FloatSize documentSize() {
      SVGDocument sVGDocument = this.document();
      return sVGDocument.size();
   }

   private static NativeImage rasterizeSvg(Identifier texture, SVGDocument sVGDocument, int value, int value2) {
      BufferedImage image = new BufferedImage(value, value2, 3);
      Graphics2D graphics2D = image.createGraphics();

      try {
         graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
         graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
         sVGDocument.render(null, graphics2D, new ViewBox(0.0F, 0.0F, value, value2));
      } finally {
         graphics2D.dispose();
      }

      int path = texture.getPath().contains("logo_boot") ? Math.max(1, Math.round(4.0F * value / 512.0F)) : 0;
      NativeImage nativeImage = new NativeImage(Format.RGBA, value, value2, false);

      for (int value3 = 0; value3 < value2; value3++) {
         for (int value4 = 0; value4 < value; value4++) {
            if (path <= 0 || value4 >= path && value4 < value - path && value3 >= path && value3 < value2 - path) {
               int rGB = image.getRGB(value4, value3);
               if ((rGB >> 24 & 0xFF) == 0) {
                  rGB = 0;
               }

               nativeImage.setPixel(value4, value3, rGB);
            } else {
               nativeImage.setPixel(value4, value3, 0);
            }
         }
      }

      return nativeImage;
   }

   private TextureSetup createPngSetup(BufferedImage image, long timestamp) {
      int value = unpackWidth(timestamp);
      int value2 = unpackHeight(timestamp);
      BufferedImage image2 = downscalePng(image, value, value2);
      Identifier texture = variantId(this.textureId, timestamp);
      DynamicTexture dynamicTexture = new DynamicTexture(texture::toString, toNativeImage(image2));
      dynamicTexture.upload();
      MinecraftContext.mc.getTextureManager().register(texture, dynamicTexture);
      GpuTextureView textureView = dynamicTexture.getTextureView();
      SamplerCache samplerCache = RenderSystem.getSamplerCache();
      GpuSampler clampToEdge = samplerCache.getClampToEdge(FilterMode.LINEAR, false);
      return TextureSetup.singleTexture(textureView, clampToEdge);
   }

   private static Identifier variantId(Identifier texture, long timestamp) {
      UnaryOperator<java.lang.String> unaryOperator = text -> {
         return text + "/" + unpackWidth(timestamp) + "x" + unpackHeight(timestamp);
      };
      return texture.withPath(unaryOperator);
   }

   private TextureSetup pngSetup(float value, float value2) {
      int width = quantize(value);
      int height = quantize(value2);
      BufferedImage image = this.rasterImage();
      if (width < image.getWidth() && height < image.getHeight()) {
         int width2 = Math.min(image.getWidth(), Math.min(2048, width));
         int height2 = Math.min(image.getHeight(), Math.min(2048, height));
         long timestamp = packSize(width2, height2);
         TextureSetup textureSetup = this.pngVariants.get(timestamp);
         if (textureSetup != null) {
            return textureSetup;
         }

         TextureSetup textureSetup2 = this.createPngSetup(image, timestamp);
         this.pngVariants.put(timestamp, textureSetup2);
         return textureSetup2;
      } else {
         return this.staticSetup();
      }
   }

   private TextureSetup staticSetup() {
      if (this.staticSetup == null) {
         Supplier<java.lang.String> supplier = () -> {
            return this.textureId.toString();
         };
         DynamicTexture dynamicTexture = new DynamicTexture(supplier, readPng(this.textureId));
         dynamicTexture.upload();
         TextureManager textureManager = MinecraftContext.mc.getTextureManager();
         textureManager.register(this.textureId, dynamicTexture);
         GpuTextureView textureView = dynamicTexture.getTextureView();
         SamplerCache samplerCache = RenderSystem.getSamplerCache();
         GpuSampler clampToEdge = samplerCache.getClampToEdge(FilterMode.LINEAR, false);
         TextureSetup textureSetup = TextureSetup.singleTexture(textureView, clampToEdge);
         this.staticSetup = textureSetup;
      }

      return this.staticSetup;
   }
}
