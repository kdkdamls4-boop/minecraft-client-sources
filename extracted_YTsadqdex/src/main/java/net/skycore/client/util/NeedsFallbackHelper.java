package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;

public record NeedsFallbackHelper(TextHeightHelper font, float size, List<NeedsFallbackHelper.Glyph> glyphs, float width, boolean needsFallback) {
   public float size() {
      return this.size;
   }

   public TextHeightHelper font() {
      return this.font;
   }

   public boolean needsFallback() {
      return this.needsFallback;
   }

   public float width() {
      return this.width;
   }

   public NeedsFallbackHelper(TextHeightHelper font, float size, List<NeedsFallbackHelper.Glyph> glyphs, float width, boolean needsFallback) {
      glyphs = List.copyOf(glyphs);
      this.font = font;
      this.size = size;
      this.glyphs = glyphs;
      this.width = width;
      this.needsFallback = needsFallback;
   }

   public List<NeedsFallbackHelper.Glyph> glyphs() {
      return this.glyphs;
   }

   public static final class Builder {
      private final float size;
      private float width;
      private final TextHeightHelper font;
      private boolean needsFallback;
      private final List<NeedsFallbackHelper.Glyph> glyphs = new ArrayList<>();

      public void appendSegment(String text, int width2, float width3) {
         if (!text.isEmpty()) {
            TextHeightHelper.Paragraph paragraph = this.font.shape(text);
            if (paragraph.lines().size() != 1) {
               throw new IllegalArgumentException("Expected single-line segment");
            }

            if (paragraph.missingGlyphs()) {
               this.needsFallback = true;
               this.glyphs.add(new NeedsFallbackHelper.Glyph(null, text, this.width, width2));
            } else {
               TextHeightHelper.Line first = paragraph.lines().getFirst();

               for (int value = 0; value < first.glyphCount(); value++) {
                  HeightHelper heightHelper = first.glyphAt(value);
                  this.glyphs
                     .add(new NeedsFallbackHelper.Glyph(heightHelper, new String(Character.toChars(heightHelper.code())), this.width + first.penX(value, this.size, width3), width2));
               }
            }

            this.width = this.width + paragraph.maxWidth(this.size, width3);
         }
      }

      public NeedsFallbackHelper build() {
         return new NeedsFallbackHelper(this.font, this.size, this.glyphs, this.width, this.needsFallback);
      }

      public void append(int value, int width2) {
         char[] charValues = Character.toChars(value);
         String text = new String(charValues);
         TextHeightHelper.Paragraph paragraph = this.font.shape(text);
         HeightHelper heightHelper = this.font.glyph(value);
         boolean missingGlyphs = this.needsFallback | paragraph.missingGlyphs();
         this.needsFallback = missingGlyphs;
         var glyphs2 = this.glyphs;
         NeedsFallbackHelper.Glyph glyph = new NeedsFallbackHelper.Glyph(heightHelper, text, this.width, width2);
         glyphs2.add(glyph);
         float width3 = this.width + paragraph.maxWidth(this.size, 0.0F);
         this.width = width3;
      }

      public Builder(TextHeightHelper font2, float size2) {
         this.font = font2;
         this.size = size2;
      }
   }

   public record Glyph(HeightHelper rendered, String text, float x, int color) {
      public HeightHelper rendered() {
         return this.rendered;
      }

      public float x() {
         return this.x;
      }

      public String text() {
         return this.text;
      }
   }
}
