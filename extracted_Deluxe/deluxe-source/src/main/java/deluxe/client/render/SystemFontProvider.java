package deluxe.client.render;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class SystemFontProvider {
   private static final FontRenderContext field10613 = new FontRenderContext(
      new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF
   );
   private final String field10614;
   private final Supplier<Font> field10615;
   private final List<Supplier<Font>> field10616;
   private final List<Font> field10617;
   private final IntFunction<List<Font>> field10618;
   private final Map<Integer, Font> field10619 = new HashMap<>();
   private Font field10620;

   private static boolean method3103(Font font, int local4942_1) {
      try {
         if (font != null && font.canDisplay(local4942_1)) {
            GlyphVector glyphVector = font.createGlyphVector(field10613, new String(Character.toChars(local4942_1)));
            return glyphVector.getNumGlyphs() > 0 && glyphVector.getGlyphCode(0) != font.getMissingGlyphCode();
         } else {
            return false;
         }
      } catch (IllegalArgumentException exception) {
         return false;
      }
   }

   public SystemFontProvider(String local4943_1, Supplier<Font> local4943_2, List<Supplier<Font>> local4943_3, IntFunction<List<Font>> local4943_4) {
      this.field10614 = local4943_1;
      this.field10615 = local4943_2;
      this.field10616 = new ArrayList<>(local4943_3);
      this.field10617 = new ArrayList<>(local4943_3.size());

      for (int index = 0; index < local4943_3.size(); index++) {
         this.field10617.add(null);
      }

      this.field10618 = local4943_4;
   }

   static String method3104(Font font) {
      return font == null ? "missing" : font.getPSName() + "/" + font.getFontName(Locale.ROOT);
   }

   Font method3105(int local4945_1) {
      return this.field10619.computeIfAbsent(local4945_1, this::method3106);
   }

   private Font method3106(int local4946_1) {
      if (!Character.isValidCodePoint(local4946_1)) {
         return this.method3107();
      }

      Font font = this.method3107();
      if (method3103(font, local4946_1)) {
         return font;
      }

      for (int index = 0; index < this.field10616.size(); index++) {
         Font font2 = this.field10617.get(index);
         if (font2 == null) {
            font2 = this.field10616.get(index).get();
            this.field10617.set(index, font2);
         }

         if (method3103(font2, local4946_1)) {
            return font2;
         }
      }

      if (this.field10618 != null) {
         for (Font font3 : this.field10618.apply(local4946_1)) {
            if (method3103(font3, local4946_1)) {
               return font3;
            }
         }
      }

      return font;
   }

   Font method3107() {
      if (this.field10620 == null) {
         this.field10620 = this.field10615.get();
      }

      return this.field10620;
   }

   public String method3108() {
      return this.field10614;
   }
}
