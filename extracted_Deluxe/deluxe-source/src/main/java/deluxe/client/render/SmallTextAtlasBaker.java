package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.util.GuiScale;
import deluxe.client.render.MsdfGlyph;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.resource.Resource;
import org.joml.Matrix3x2f;

final class SmallTextAtlasBaker implements AutoCloseable {
   static final float field12498 = 20.0F;
   static final int field12499 = 4;
   private static final SmallTextAtlasBaker.Nested0496 field12506 = new SmallTextAtlasBaker.Nested0496(null, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0);
   private static final FontRenderContext field12507 = new FontRenderContext(
      new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF
   );
   private static final Font field12508 = new Font("SansSerif", 0, 32);
   private static final ExecutorService field12510 = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "deluxe-msdf-face-loader");
      thread.setDaemon(true);
      return thread;
   });
   private static final Map<Integer, Float> field12511 = new ConcurrentHashMap<>();
   private final Map<FontAtlas, Optional<SmallTextAtlasBaker.Nested0497>> field12512 = new ConcurrentHashMap<>();
   private final Map<FontAtlas, Optional<Font>> field12513 = new ConcurrentHashMap<>();
   private final Set<FontAtlas> field12514 = ConcurrentHashMap.newKeySet();
   private final Map<FontAtlas, Map<Long, SmallTextAtlasBaker.Nested0496>> field12515 = new HashMap<>();
   private final Map<Long, SmallTextAtlasBaker.Nested0496> field12516 = new HashMap<>();
   private final List<SmallTextAtlasBaker.Nested0495> field12517 = new ArrayList<>();
   private boolean field12518;
   private long field12519;
   private long field12520 = 2000000L;

   void method3581() {
      this.method3583();
      this.field12512.clear();
      this.field12513.clear();
      this.field12514.clear();
      this.field12518 = false;
   }

   static boolean method3582(int local5863_0) {
      return local5863_0 > 32 && !Character.isWhitespace(local5863_0) && Character.isDefined(local5863_0) && field12508.canDisplay(local5863_0);
   }

   private void method3583() {
      for (SmallTextAtlasBaker.Nested0495 local5864_2 : this.field12517) {
         local5864_2.close();
      }

      this.field12517.clear();
      this.field12515.clear();
      this.field12516.clear();
   }

   private SmallTextAtlasBaker.Nested0496 method3585(
      FontAtlas fontAtlas, int local5866_2, MsdfGlyph msdfGlyph, float local5866_4, float local5866_5
   ) {
      Font font = this.method3608(fontAtlas);
      if (font != null) {
         SmallTextAtlasBaker.Nested0496 local5866_7 = this.method3586(font, local5866_2, local5866_4, local5866_5);
         if (local5866_7 != null) {
            return local5866_7;
         }
      }

      return this.method3599(fontAtlas, msdfGlyph, local5866_4, local5866_5);
   }

   private SmallTextAtlasBaker.Nested0496 method3586(Font font, int local5867_2, float local5867_3, float local5867_4) {
      float local5867_5 = local5867_3 * GuiScale.method0794();
      int local5867_6 = 3;
      Font font2 = font.deriveFont(0, local5867_5 * local5867_6);
      GlyphVector glyphVector = font2.createGlyphVector(field12507, new String(Character.toChars(local5867_2)));
      if (glyphVector.getNumGlyphs() == 0) {
         return field12506;
      }

      float local5867_9 = local5867_4 * local5867_6;
      Rectangle pixelBounds = glyphVector.getPixelBounds(field12507, local5867_9, 0.0F);
      if (pixelBounds.width > 0 && pixelBounds.height > 0) {
         int local5867_11 = Math.floorDiv(pixelBounds.x, local5867_6);
         int local5867_12 = Math.floorDiv(pixelBounds.y, local5867_6);
         int local5867_13 = -Math.floorDiv(-(pixelBounds.x + pixelBounds.width), local5867_6);
         int local5867_14 = -Math.floorDiv(-(pixelBounds.y + pixelBounds.height), local5867_6);
         int local5867_15 = local5867_13 - local5867_11 + 4;
         int local5867_16 = local5867_14 - local5867_12 + 4;
         if (local5867_15 <= 1024 && local5867_16 <= 1024) {
            int local5867_17 = local5867_15 * local5867_6;
            int local5867_18 = local5867_16 * local5867_6;
            BufferedImage bufferedImage = new BufferedImage(local5867_17, local5867_18, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            graphics2D.setColor(Color.WHITE);
            graphics2D.drawGlyphVector(glyphVector, (2 - local5867_11) * local5867_6 + local5867_9, (2 - local5867_12) * local5867_6);
            graphics2D.dispose();
            int[] rGB = bufferedImage.getRGB(0, 0, local5867_17, local5867_18, null, 0, local5867_17);
            int[] intArray = new int[local5867_15 * local5867_16];
            int local5867_23 = local5867_6 * local5867_6;

            for (int index = 0; index < local5867_16; index++) {
               for (int index2 = 0; index2 < local5867_15; index2++) {
                  int local5867_26 = 0;
                  int local5867_27 = index * local5867_6 * local5867_17 + index2 * local5867_6;

                  for (int index3 = 0; index3 < local5867_6; index3++) {
                     int local5867_29 = local5867_27 + index3 * local5867_17;

                     for (int index4 = 0; index4 < local5867_6; index4++) {
                        local5867_26 += rGB[local5867_29 + index4] >>> 24 & 0xFF;
                     }
                  }

                  intArray[index * local5867_15 + index2] = method3593((float)local5867_26 / local5867_23);
               }
            }

            SmallTextAtlasBaker.Nested0498 local5867_24 = this.method3613(local5867_15, local5867_16);
            if (local5867_24 == null) {
               return field12506;
            }

            local5867_24.page.method3617(intArray, local5867_15, local5867_16, local5867_24.x, local5867_24.y);
            float local5867_25 = 9.765625E-4F;
            return new SmallTextAtlasBaker.Nested0496(
               local5867_24.page,
               local5867_24.x * local5867_25,
               local5867_24.y * local5867_25,
               (local5867_24.x + local5867_15) * local5867_25,
               (local5867_24.y + local5867_16) * local5867_25,
               local5867_11 - 2,
               -local5867_12 + 2,
               local5867_15,
               local5867_16
            );
         } else {
            return field12506;
         }
      } else {
         return field12506;
      }
   }

   static boolean method3587(Matrix3x2f matrix3x2f) {
      float local5868_1 = GuiScale.method0794();
      int local5868_2 = GuiScale.method0797();
      return Math.abs(matrix3x2f.m01()) < 1.0E-4F
         && Math.abs(matrix3x2f.m10()) < 1.0E-4F
         && Math.abs(matrix3x2f.m00() * local5868_2 - local5868_1) < 0.001F
         && Math.abs(matrix3x2f.m11() * local5868_2 - local5868_1) < 0.001F;
   }

   SmallTextAtlasBaker.Nested0496 method3588(int local5869_1, float local5869_2, int local5869_3) {
      long local5869_4 = (long)local5869_1 << 16 | (long)Math.round(local5869_2 * 4.0F) << 2 | local5869_3;
      SmallTextAtlasBaker.Nested0496 local5869_6 = this.field12516.get(local5869_4);
      if (local5869_6 == null) {
         long local5869_7 = System.nanoTime();
         local5869_6 = this.method3586(field12508, local5869_1, local5869_2, local5869_3 / 4.0F);
         this.field12520 = this.field12520 - (System.nanoTime() - local5869_7);
         this.field12516.put(local5869_4, local5869_6);
      }

      if (local5869_6.page() != null) {
         local5869_6.page().field12575 = this.field12519;
      }

      return local5869_6;
   }

   void method3589() {
      for (SmallTextAtlasBaker.Nested0495 local5870_2 : this.field12517) {
         local5870_2.method3620();
      }
   }

   private SmallTextAtlasBaker.Nested0497 method3591(FontAtlas fontAtlas) {
      Optional<SmallTextAtlasBaker.Nested0497> local5872_2 = this.field12512.get(fontAtlas);
      return local5872_2 == null ? null : local5872_2.orElse(null);
   }

   void method3592() {
      this.field12520 = 2000000L;
      this.field12519++;
   }

   private static int method3593(float local5874_0) {
      double local5874_1 = method3607(local5874_0 / 255.0F);
      return (int)Math.min(255L, Math.round(Math.pow(local5874_1, 0.72) * 255.0));
   }

   boolean method3594(FontAtlas fontAtlas, int local5875_2, MsdfGlyph msdfGlyph, float local5875_4, int local5875_5) {
      Map<Long, SmallTextAtlasBaker.Nested0496> local5875_6 = this.field12515.computeIfAbsent(fontAtlas, fontAtlas2 -> new HashMap<>());
      long local5875_7 = (long)local5875_2 << 16 | (long)Math.round(local5875_4 * 4.0F) << 2 | local5875_5;
      if (local5875_6.containsKey(local5875_7)) {
         return true;
      }

      if (this.field12520 <= 0L) {
         return false;
      }

      long local5875_9 = System.nanoTime();
      local5875_6.put(local5875_7, this.method3585(fontAtlas, local5875_2, msdfGlyph, local5875_4, local5875_5 / 4.0F));
      this.field12520 = this.field12520 - (System.nanoTime() - local5875_9);
      return true;
   }

   boolean method3595() {
      return this.field12520 > 0L;
   }

   private static float method3596(
      SmallTextAtlasBaker.Nested0497 local5877_0,
      FontAtlas fontAtlas,
      MsdfGlyph msdfGlyph,
      float local5877_3,
      float local5877_4,
      float local5877_5,
      float local5877_6,
      float local5877_7,
      float local5877_8,
      float local5877_9
   ) {
      float local5877_10 = msdfGlyph.planeRight() - msdfGlyph.planeLeft();
      float local5877_11 = msdfGlyph.planeTop() - msdfGlyph.planeBottom();
      float local5877_12 = local5877_3 + (local5877_7 - msdfGlyph.planeLeft()) / local5877_10 * (local5877_4 - local5877_3);
      float local5877_13 = local5877_5 + (msdfGlyph.planeTop() - local5877_8) / local5877_11 * (local5877_6 - local5877_5);
      boolean local5877_14 = local5877_7 < msdfGlyph.planeLeft() + local5877_9
         || local5877_7 > msdfGlyph.planeRight() - local5877_9
         || local5877_8 < msdfGlyph.planeBottom() + local5877_9
         || local5877_8 > msdfGlyph.planeTop() - local5877_9;
      local5877_12 = method3598(local5877_12, local5877_3 + 0.5F, local5877_4 - 0.5F);
      local5877_13 = method3598(local5877_13, local5877_5 + 0.5F, local5877_6 - 0.5F);
      float local5877_15 = local5877_0.method3625(local5877_12, local5877_13);
      float local5877_16 = (local5877_15 - 0.5F) * fontAtlas.method1716();
      return local5877_14 ? Math.min(local5877_16, -0.01F) : local5877_16;
   }

   boolean method3597(int local5878_1, float local5878_2, int local5878_3) {
      long local5878_4 = (long)local5878_1 << 16 | (long)Math.round(local5878_2 * 4.0F) << 2 | local5878_3;
      if (this.field12516.containsKey(local5878_4)) {
         return true;
      }

      if (this.field12520 <= 0L) {
         return false;
      }

      long local5878_6 = System.nanoTime();
      this.field12516.put(local5878_4, this.method3586(field12508, local5878_1, local5878_2, local5878_3 / 4.0F));
      this.field12520 = this.field12520 - (System.nanoTime() - local5878_6);
      return true;
   }

   private static float method3598(float local5879_0, float local5879_1, float local5879_2) {
      return Math.max(local5879_1, Math.min(local5879_2, local5879_0));
   }

   private SmallTextAtlasBaker.Nested0496 method3599(FontAtlas fontAtlas, MsdfGlyph msdfGlyph, float local5880_3, float local5880_4) {
      SmallTextAtlasBaker.Nested0497 local5880_5 = this.method3591(fontAtlas);
      if (local5880_5 == null) {
         return field12506;
      }

      float local5880_6 = msdfGlyph.planeRight() - msdfGlyph.planeLeft();
      float local5880_7 = msdfGlyph.planeTop() - msdfGlyph.planeBottom();
      if (!(local5880_6 <= 0.0F) && !(local5880_7 <= 0.0F)) {
         float local5880_8 = msdfGlyph.u0() * local5880_5.field12593;
         float local5880_9 = msdfGlyph.u1() * local5880_5.field12593;
         float local5880_10 = msdfGlyph.v0() * local5880_5.field12594;
         float local5880_11 = msdfGlyph.v1() * local5880_5.field12594;
         float local5880_12 = (local5880_9 - local5880_8) / local5880_6;
         if (local5880_12 <= 0.0F) {
            return field12506;
         }

         float local5880_13 = local5880_3 * GuiScale.method0794();
         int local5880_14 = Math.round(msdfGlyph.planeLeft() * local5880_13 + local5880_4) - 2;
         int local5880_15 = Math.round(msdfGlyph.planeTop() * local5880_13) + 2;
         int local5880_16 = (int)Math.ceil(local5880_6 * local5880_13) + 4 + 2;
         int local5880_17 = (int)Math.ceil(local5880_7 * local5880_13) + 4 + 1;
         if (local5880_16 <= 1024 && local5880_17 <= 1024) {
            float local5880_18 = local5880_13 / local5880_12;
            float local5880_19 = 1.0F / local5880_12;
            int[] intArray = new int[local5880_16 * local5880_17];

            for (int index = 0; index < local5880_17; index++) {
               for (int index2 = 0; index2 < local5880_16; index2++) {
                  float local5880_23 = 0.0F;

                  for (int index3 = 0; index3 < 4; index3++) {
                     for (int index4 = 0; index4 < 4; index4++) {
                        float local5880_26 = (index4 + 0.5F) / 4.0F - 0.5F;
                        float local5880_27 = (index3 + 0.5F) / 4.0F - 0.5F;
                        float local5880_28 = (local5880_14 + index2 + 0.5F + local5880_26 - local5880_4) / local5880_13;
                        float local5880_29 = (local5880_15 - index - 0.5F - local5880_27) / local5880_13;
                        float local5880_30 = method3596(
                           local5880_5,
                           fontAtlas,
                           msdfGlyph,
                           local5880_8,
                           local5880_9,
                           local5880_10,
                           local5880_11,
                           local5880_28,
                           local5880_29,
                           local5880_19
                        );
                        float local5880_31 = local5880_30 * local5880_18;
                        local5880_23 += method3607(local5880_31 * 4.0F + 0.5F);
                     }
                  }

                  intArray[index * local5880_16 + index2] = method3593(local5880_23 / 16.0F * 255.0F);
               }
            }

            SmallTextAtlasBaker.Nested0498 local5880_21 = this.method3613(local5880_16, local5880_17);
            if (local5880_21 == null) {
               return field12506;
            }

            local5880_21.page.method3617(intArray, local5880_16, local5880_17, local5880_21.x, local5880_21.y);
            float local5880_22 = 9.765625E-4F;
            return new SmallTextAtlasBaker.Nested0496(
               local5880_21.page,
               local5880_21.x * local5880_22,
               local5880_21.y * local5880_22,
               (local5880_21.x + local5880_16) * local5880_22,
               (local5880_21.y + local5880_17) * local5880_22,
               local5880_14,
               local5880_15,
               local5880_16,
               local5880_17
            );
         } else {
            return field12506;
         }
      } else {
         return field12506;
      }
   }

   @Override
   public void close() {
      this.method3581();
   }

   private static Font method3600(FontAtlas fontAtlas) {
      String local5882_1 = method3603(fontAtlas);
      if (local5882_1 == null) {
         return null;
      }

      try (InputStream resourceAsStream = SmallTextAtlasBaker.class.getResourceAsStream(local5882_1)) {
         if (resourceAsStream == null) {
            DeluxeClient.logger().warn("[MSDF] Inter OTF not found for small-text bake: {}", local5882_1);
            return null;
         } else {
            return Font.createFont(0, resourceAsStream).deriveFont(0, 1.0F);
         }
      } catch (Exception exception) {
         DeluxeClient.logger().warn("[MSDF] Failed to load Inter OTF {}", local5882_1, exception);
         return null;
      }
   }

   private void method3602(FontAtlas fontAtlas) {
      if (this.field12514.add(fontAtlas)) {
         field12510.submit(
            () -> {
               try {
                  field12508.canDisplay('A');
                  if (MinecraftClient.getInstance() == null) {
                     this.field12514.remove(fontAtlas);
                     return;
                  }

                  Optional<Font> local5865_2 = Optional.ofNullable(method3600(fontAtlas));
                  this.field12512
                     .put(fontAtlas, local5865_2.isPresent() ? Optional.empty() : Optional.ofNullable(SmallTextAtlasBaker.Nested0497.method3623(fontAtlas)));
                  this.field12513.put(fontAtlas, local5865_2);
               } catch (Throwable throwable) {
                  this.field12512.putIfAbsent(fontAtlas, Optional.empty());
                  this.field12513.putIfAbsent(fontAtlas, Optional.empty());
               }
            }
         );
      }
   }

   private static String method3603(FontAtlas fontAtlas) {
      String path = fontAtlas.method1715().getPath();
      String local5885_2 = path.substring(path.lastIndexOf(47) + 1);
      if (local5885_2.endsWith(".png")) {
         local5885_2 = local5885_2.substring(0, local5885_2.length() - 4);
      }

      String local5885_3 = "/assets/deluxe/fonts/inter/";

      return switch (local5885_2.toLowerCase(Locale.ROOT)) {
         case "thin" -> local5885_3 + "Inter-Thin.otf";
         case "mini" -> local5885_3 + "Inter-Light.otf";
         case "semi" -> local5885_3 + "Inter-Medium.otf";
         case "semi_bold" -> local5885_3 + "Inter-SemiBold.otf";
         case "bold" -> local5885_3 + "Inter-Bold.otf";
         default -> null;
      };
   }

   private SmallTextAtlasBaker.Nested0495 method3604() {
      SmallTextAtlasBaker.Nested0495 local5887_1 = null;

      for (SmallTextAtlasBaker.Nested0495 local5887_3 : this.field12517) {
         if (local5887_3.field12575 != this.field12519 && (local5887_1 == null || local5887_3.field12575 < local5887_1.field12575)) {
            local5887_1 = local5887_3;
         }
      }

      if (local5887_1 == null) {
         return null;
      }

      SmallTextAtlasBaker.Nested0495 local5887_2 = local5887_1;

      for (Map<Long, SmallTextAtlasBaker.Nested0496> local5887_4 : this.field12515.values()) {
         local5887_4.values().removeIf(local5861_1 -> local5861_1.page() == local5887_2);
      }

      this.field12516.values().removeIf(local5889_1 -> local5889_1.page() == local5887_2);
      local5887_2.method3619();
      return local5887_2;
   }

   static float method3605(int local5888_0) {
      return field12511.computeIfAbsent(
         local5888_0,
         local5893_0 -> field12508.createGlyphVector(field12507, new String(Character.toChars(local5893_0))).getGlyphMetrics(0).getAdvance() / 32.0F
      );
   }

   private static float method3607(float local5891_0) {
      return Math.max(0.0F, Math.min(1.0F, local5891_0));
   }

   private Font method3608(FontAtlas fontAtlas) {
      Optional<Font> local5892_2 = this.field12513.get(fontAtlas);
      return local5892_2 == null ? null : local5892_2.orElse(null);
   }

   void method3610() {
      if (this.field12518) {
         this.field12518 = false;
         this.method3583();
      }
   }

   static float method3611(float local5895_0) {
      return Math.max(0.5F, Math.round(local5895_0 * 4.0F) / 4.0F);
   }

   SmallTextAtlasBaker.Nested0496 method3612(FontAtlas fontAtlas, int local5896_2, MsdfGlyph msdfGlyph, float local5896_4, int local5896_5) {
      Map<Long, SmallTextAtlasBaker.Nested0496> local5896_6 = this.field12515.computeIfAbsent(fontAtlas, fontAtlas2 -> new HashMap<>());
      long local5896_7 = (long)local5896_2 << 16 | (long)Math.round(local5896_4 * 4.0F) << 2 | local5896_5;
      SmallTextAtlasBaker.Nested0496 local5896_9 = local5896_6.get(local5896_7);
      if (local5896_9 == null) {
         long local5896_10 = System.nanoTime();
         local5896_9 = this.method3585(fontAtlas, local5896_2, msdfGlyph, local5896_4, local5896_5 / 4.0F);
         this.field12520 = this.field12520 - (System.nanoTime() - local5896_10);
         local5896_6.put(local5896_7, local5896_9);
      }

      if (local5896_9.page() != null) {
         local5896_9.page().field12575 = this.field12519;
      }

      return local5896_9;
   }

   private SmallTextAtlasBaker.Nested0498 method3613(int local5897_1, int local5897_2) {
      for (SmallTextAtlasBaker.Nested0495 local5897_4 : this.field12517) {
         SmallTextAtlasBaker.Nested0498 local5897_5 = local5897_4.method3618(local5897_1, local5897_2);
         if (local5897_5 != null) {
            return local5897_5;
         }
      }

      if (this.field12517.size() >= 16) {
         SmallTextAtlasBaker.Nested0495 local5897_3 = this.method3604();
         if (local5897_3 == null) {
            this.field12518 = true;
            DeluxeClient.logger().warn("[MSDF] Small-text atlas overflow ({} pages), reset scheduled", this.field12517.size() + 1);
            return null;
         } else {
            return local5897_3.method3618(local5897_1, local5897_2);
         }
      } else {
         SmallTextAtlasBaker.Nested0495 local5897_3 = new SmallTextAtlasBaker.Nested0495();
         this.field12517.add(local5897_3);
         return local5897_3.method3618(local5897_1, local5897_2);
      }
   }

   boolean method3615(FontAtlas fontAtlas) {
      Optional<Font> local5899_2 = this.field12513.get(fontAtlas);
      Optional<SmallTextAtlasBaker.Nested0497> local5899_3 = this.field12512.get(fontAtlas);
      if (local5899_2 != null && local5899_3 != null) {
         return local5899_2.isPresent() || local5899_3.isPresent();
      }

      this.method3602(fontAtlas);
      return false;
   }

   static final class Nested0495 implements AutoCloseable {
      private static int field12568;
      private final NativeImage field12569;
      private final NativeImageBackedTexture field12570;
      private final TextureSetup field12571;
      private int field12572 = 1;
      private int field12573 = 1;
      private int field12574;
      private long field12575;
      private int field12576 = Integer.MAX_VALUE;
      private int field12577 = Integer.MAX_VALUE;
      private int field12578 = Integer.MIN_VALUE;
      private int field12579 = Integer.MIN_VALUE;

      @Override
      public void close() {
         this.field12570.close();
      }

      private void method3617(int[] intArray, int local5902_2, int local5902_3, int local5902_4, int local5902_5) {
         for (int index = 0; index < local5902_3; index++) {
            for (int index2 = 0; index2 < local5902_2; index2++) {
               int local5902_8 = intArray[index * local5902_2 + index2];
               this.field12569.setColor(local5902_4 + index2, local5902_5 + index, local5902_8 << 24 | 16777215);
            }
         }

         this.field12576 = Math.min(this.field12576, local5902_4);
         this.field12577 = Math.min(this.field12577, local5902_5);
         this.field12578 = Math.max(this.field12578, local5902_4 + local5902_2);
         this.field12579 = Math.max(this.field12579, local5902_5 + local5902_3);
      }

      private SmallTextAtlasBaker.Nested0498 method3618(int local5903_1, int local5903_2) {
         int local5903_3 = local5903_1 + 1;
         int local5903_4 = local5903_2 + 1;
         if (this.field12572 + local5903_3 > 1024) {
            this.field12572 = 1;
            this.field12573 = this.field12573 + this.field12574;
            this.field12574 = 0;
         }

         if (this.field12573 + local5903_4 <= 1024 && local5903_3 <= 1024) {
            int local5903_5 = this.field12572;
            int local5903_6 = this.field12573;
            this.field12572 += local5903_3;
            this.field12574 = Math.max(this.field12574, local5903_4);
            return new SmallTextAtlasBaker.Nested0498(this, local5903_5, local5903_6);
         } else {
            return null;
         }
      }

      private void method3619() {
         this.field12569.fillRect(0, 0, 1024, 1024, 0);
         this.field12572 = 1;
         this.field12573 = 1;
         this.field12574 = 0;
         this.field12576 = 0;
         this.field12577 = 0;
         this.field12578 = 1024;
         this.field12579 = 1024;
      }

      Nested0495() {
         this.field12569 = new NativeImage(1024, 1024, true);
         this.field12570 = new NativeImageBackedTexture(() -> "deluxe_msdf_small_atlas_" + field12568++, this.field12569);
         this.field12571 = TextureSetup.of(this.field12570.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
      }

      private void method3620() {
         if (this.field12578 > this.field12576 && this.field12579 > this.field12577) {
            if (this.field12570.getGlTexture() != null) {
               int local5906_1 = Math.max(0, this.field12576);
               int local5906_2 = Math.max(0, this.field12577);
               int local5906_3 = Math.min(1024, this.field12578) - local5906_1;
               int local5906_4 = Math.min(1024, this.field12579) - local5906_2;
               if (local5906_3 > 0 && local5906_4 > 0) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToTexture(
                        this.field12570.getGlTexture(), this.field12569, 0, 0, local5906_1, local5906_2, local5906_3, local5906_4, local5906_1, local5906_2
                     );
               }

               this.field12576 = Integer.MAX_VALUE;
               this.field12577 = Integer.MAX_VALUE;
               this.field12578 = Integer.MIN_VALUE;
               this.field12579 = Integer.MIN_VALUE;
            }
         }
      }

      TextureSetup method3621() {
         return this.field12571;
      }
   }

   record Nested0496(
      SmallTextAtlasBaker.Nested0495 page,
      float u0,
      float v0,
      float u1,
      float v1,
      int offsetX,
      int offsetY,
      int width,
      int height
   ) {
      boolean drawable() {
         return this.page != null && this.width > 0 && this.height > 0;
      }
   }

   private static final class Nested0497 {
      final int field12593;
      final int field12594;
      private final int[] field12595;

      private float method3622(int local5922_1, int local5922_2, int local5922_3) {
         local5922_1 = Math.max(0, Math.min(this.field12593 - 1, local5922_1));
         local5922_2 = Math.max(0, Math.min(this.field12594 - 1, local5922_2));
         return (this.field12595[local5922_2 * this.field12593 + local5922_1] >>> local5922_3 & 0xFF) / 255.0F;
      }

      static SmallTextAtlasBaker.Nested0497 method3623(FontAtlas fontAtlas) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient == null) {
            return null;
         }

         Optional<Resource> local5923_2 = minecraftClient.getResourceManager().getResource(fontAtlas.method1715());
         if (local5923_2.isEmpty()) {
            DeluxeClient.logger().warn("[MSDF] Atlas PNG not readable for small-text bake: {}", fontAtlas.method1715());
            return null;
         }

         try (InputStream inputStream = local5923_2.get().getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
               return null;
            }

            int currentWidth = bufferedImage.getWidth();
            int currentHeight = bufferedImage.getHeight();
            return new SmallTextAtlasBaker.Nested0497(currentWidth, currentHeight, bufferedImage.getRGB(0, 0, currentWidth, currentHeight, null, 0, currentWidth));
         } catch (Exception exception) {
            DeluxeClient.logger().warn("[MSDF] Failed to load atlas PNG for small-text bake: {}", fontAtlas.method1715(), exception);
            return null;
         }
      }

      private float method3624(int local5924_1, int local5924_2, float local5924_3, float local5924_4, int local5924_5) {
         float local5924_6 = this.method3622(local5924_1, local5924_2, local5924_5);
         float local5924_7 = this.method3622(local5924_1 + 1, local5924_2, local5924_5);
         float local5924_8 = this.method3622(local5924_1, local5924_2 + 1, local5924_5);
         float local5924_9 = this.method3622(local5924_1 + 1, local5924_2 + 1, local5924_5);
         float local5924_10 = local5924_6 + (local5924_7 - local5924_6) * local5924_3;
         float local5924_11 = local5924_8 + (local5924_9 - local5924_8) * local5924_3;
         return local5924_10 + (local5924_11 - local5924_10) * local5924_4;
      }

      float method3625(float local5925_1, float local5925_2) {
         local5925_1 -= 0.5F;
         local5925_2 -= 0.5F;
         int local5925_3 = (int)Math.floor(local5925_1);
         int local5925_4 = (int)Math.floor(local5925_2);
         float local5925_5 = local5925_1 - local5925_3;
         float local5925_6 = local5925_2 - local5925_4;
         float local5925_7 = this.method3624(local5925_3, local5925_4, local5925_5, local5925_6, 16);
         float local5925_8 = this.method3624(local5925_3, local5925_4, local5925_5, local5925_6, 8);
         float local5925_9 = this.method3624(local5925_3, local5925_4, local5925_5, local5925_6, 0);
         return Math.max(Math.min(local5925_7, local5925_8), Math.min(Math.max(local5925_7, local5925_8), local5925_9));
      }

      private Nested0497(int local5926_1, int local5926_2, int[] intArray) {
         this.field12593 = local5926_1;
         this.field12594 = local5926_2;
         this.field12595 = intArray;
      }
   }

   private record Nested0498(SmallTextAtlasBaker.Nested0495 page, int x, int y) {
   }
}
