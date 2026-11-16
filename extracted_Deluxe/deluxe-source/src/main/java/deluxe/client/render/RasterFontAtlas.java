package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.render.RasterGlyphCache;
import deluxe.client.render.SystemFontProvider;

import java.awt.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

final class RasterFontAtlas implements AutoCloseable {
   private final Map<String, SystemFontProvider> field16102 = new LinkedHashMap<>();
   private final List<Supplier<Font>> field16103 = new ArrayList<>();
   private final Map<String, Font> field16104 = new HashMap<>();
   private final ArrayList<RasterGlyphCache> field16105 = new ArrayList<>();
   private final Map<RasterFontAtlas.Nested0539, RasterGlyphCache> field16106 = new LinkedHashMap<RasterFontAtlas.Nested0539, RasterGlyphCache>(
      64, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<RasterFontAtlas.Nested0539, RasterGlyphCache> local7999_1) {
         if (this.size() <= 160) {
            return false;
         }

         RasterFontAtlas.this.field16105.add(local7999_1.getValue());
         return true;
      }
   };
   private final Font field16107 = new Font("SansSerif", 0, 1);
   private Supplier<Font> field16108;

   void method4785() {
      for (RasterGlyphCache rasterGlyphCache : this.field16105) {
         rasterGlyphCache.close();
      }

      this.field16105.clear();
   }

   @Override
   public void close() {
      for (RasterGlyphCache rasterGlyphCache : this.field16106.values()) {
         rasterGlyphCache.close();
      }

      this.field16106.clear();
      this.field16102.clear();
      this.field16103.clear();
      this.field16104.clear();
   }

   private Font method4786(String local7970_1) {
      return this.method4805(local7970_1, () -> this.field16108 != null ? this.field16108.get() : this.field16107);
   }

   private void method4787() {
      Supplier<Font> local7971_1 = this.method4801("SegoeProDisplay-Semibold.ttf", () -> this.field16107);
      Supplier<Font> local7971_2 = this.method4801("semi_bold.otf", local7971_1);
      Supplier<Font> local7971_3 = this.method4801("open-sans.bold.ttf", local7971_1);
      Supplier<Font> local7971_4 = this.method4801("open-sans.semibold.ttf", local7971_3);
      Supplier<Font> local7971_5 = this.method4801("bold.otf", local7971_3);
      Supplier<Font> local7971_6 = this.method4801("medium.otf", local7971_2);
      Supplier<Font> local7971_7 = this.method4801("ZenterSPDemo-Black.otf", local7971_1);
      Supplier<Font> local7971_8 = this.method4801("icons.ttf", local7971_1);
      Supplier<Font> local7971_9 = this.method4801("NotoSans-Regular.ttf", local7971_1);
      Supplier<Font> local7971_10 = this.method4801("NotoSansSymbols-Regular.ttf", local7971_1);
      Supplier<Font> local7971_11 = this.method4801("NotoSansSymbols2-Regular.ttf", local7971_1);
      Supplier<Font> local7971_12 = this.method4801("NotoNaskhArabic-Regular.ttf", local7971_1);
      this.field16108 = local7971_9;
      this.method4788(local7971_8, local7971_10, local7971_11, local7971_12, local7971_1, local7971_9);
      this.method4796("default", local7971_1);
      this.method4796("segoe", local7971_1);
      this.method4796("segoe_semibold", local7971_1);
      this.method4796("semi_bold", local7971_2);
      this.method4796("semibold", local7971_2);
      this.method4796("open_sans_bold", local7971_3);
      this.method4796("opensans_bold", local7971_3);
      this.method4796("open_sans_semibold", local7971_4);
      this.method4796("opensans_semibold", local7971_4);
      this.method4796("open_sans", local7971_4);
      this.method4796("opensans", local7971_4);
      this.method4796("bold", local7971_5);
      this.method4796("bold_otf", local7971_5);
      this.method4796("medium", local7971_6);
      this.method4796("medium_otf", local7971_6);
      this.method4796("zenter", local7971_7);
      this.method4796("zenter_black", local7971_7);
      this.method4796("noto", local7971_9);
      this.method4796("noto_sans", local7971_9);
      this.method4796("noto_symbols", local7971_10);
      this.method4796("noto_symbols_1", local7971_10);
      this.method4796("noto_symbols_2", local7971_11);
      this.method4796("noto_symbols2", local7971_11);
      this.method4796("noto_arabic", local7971_12);
      this.method4796("arabic", local7971_12);
      this.method4796("noto_cjk_sc", local7971_9);
      this.method4796("noto_chinese", local7971_9);
      this.method4796("chinese", local7971_9);
      this.method4796("noto_cjk_kr", local7971_9);
      this.method4796("noto_korean", local7971_9);
      this.method4796("korean", local7971_9);
      this.method4796("noto_cjk_jp", local7971_9);
      this.method4796("noto_japanese", local7971_9);
      this.method4796("japanese", local7971_9);
      this.method4796("icons", local7971_8);
      this.method4796("icon", local7971_8);
   }

   RasterFontAtlas() {
      this.method4787();
   }

   @SafeVarargs
   private void method4788(Supplier<Font>... local7973_1) {
      this.field16103.clear();

      for (Supplier<Font> local7973_5 : local7973_1) {
         this.method4802(local7973_5);
      }

      this.method4802(() -> this.field16107);
   }

   private static boolean method4789(int local7974_0) {
      return local7974_0 >= 4352 && local7974_0 <= 4607
         || local7974_0 >= 12592 && local7974_0 <= 12687
         || local7974_0 >= 43360 && local7974_0 <= 43391
         || local7974_0 >= 44032 && local7974_0 <= 55215;
   }

   private static boolean method4792(int local7977_0) {
      return local7977_0 >= 13312 && local7977_0 <= 19903
         || local7977_0 >= 19968 && local7977_0 <= 40959
         || local7977_0 >= 63744 && local7977_0 <= 64255
         || local7977_0 >= 131072 && local7977_0 <= 195103;
   }

   SystemFontProvider method4794(String local7979_1) {
      SystemFontProvider systemFontProvider = this.field16102.get(method4803(local7979_1));
      return systemFontProvider != null ? systemFontProvider : this.field16102.get("default");
   }

   private void method4796(String local7981_1, Supplier<Font> local7981_2) {
      this.field16102.put(method4803(local7981_1), new SystemFontProvider(method4803(local7981_1), local7981_2, this.field16103, this::method4798));
   }

   private Font method4797(String local7982_1, Font font) {
      try (InputStream resourceAsStream = RasterFontAtlas.class.getResourceAsStream("/assets/client/fonts/" + local7982_1)) {
         if (resourceAsStream == null) {
            DeluxeClient.logger().warn("[FontRenderer] Font asset is missing: {}", local7982_1);
            return font;
         } else {
            return Font.createFont(0, resourceAsStream).deriveFont(0, 1.0F);
         }
      } catch (Exception exception) {
         DeluxeClient.logger().warn("[FontRenderer] Failed to load font asset: {}", local7982_1, exception);
         return font;
      }
   }

   private List<Font> method4798(int local7983_1) {
      if (method4789(local7983_1)) {
         return Collections.singletonList(this.method4786("NotoSansCJKkr-Regular.otf"));
      } else if (method4800(local7983_1)) {
         return Collections.singletonList(this.method4786("NotoSansCJKjp-Regular.otf"));
      } else {
         return method4792(local7983_1) ? Collections.singletonList(this.method4786("NotoSansCJKsc-Regular.otf")) : List.of();
      }
   }

   private static boolean method4800(int local7985_0) {
      return local7985_0 >= 12352 && local7985_0 <= 12543 || local7985_0 >= 12784 && local7985_0 <= 12799;
   }

   private Supplier<Font> method4801(String local7986_1, Supplier<Font> local7986_2) {
      return () -> this.method4805(local7986_1, local7986_2);
   }

   private void method4802(Supplier<Font> local7987_1) {
      if (local7987_1 != null) {
         this.field16103.add(local7987_1);
      }
   }

   private static String method4803(String local7988_0) {
      return local7988_0 != null && !local7988_0.isBlank() ? local7988_0.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_') : "default";
   }

   private static float method4804(float local7989_0) {
      return Math.max(1.0F, Math.min(512.0F, Math.round(local7989_0 * 4.0F) / 4.0F));
   }

   private synchronized Font method4805(String local7990_1, Supplier<Font> local7990_2) {
      Font font = this.field16104.get(local7990_1);
      if (font != null) {
         return font;
      }

      Font font2 = this.method4797(local7990_1, local7990_2.get());
      this.field16104.put(local7990_1, font2);
      return font2;
   }

   RasterGlyphCache method4806(String local7991_1, float local7991_2) {
      String local7991_3 = method4803(local7991_1);
      SystemFontProvider systemFontProvider = this.method4794(local7991_3);
      float local7991_5 = method4804(local7991_2);
      RasterFontAtlas.Nested0539 local7991_6 = new RasterFontAtlas.Nested0539(systemFontProvider.method3108(), local7991_5);
      return this.field16106.computeIfAbsent(local7991_6, local7978_2 -> new RasterGlyphCache(systemFontProvider, local7991_5));
   }

   private record Nested0539(String name, float size) {
   }
}
