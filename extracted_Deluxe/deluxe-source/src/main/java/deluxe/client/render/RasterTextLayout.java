package deluxe.client.render;

import deluxe.client.render.FontTexture;

import java.util.List;

public final class RasterTextLayout {
   static final RasterTextLayout rasterTextLayout = new RasterTextLayout(List.of(), 0.0F, 0.0F);
   private final List<RasterTextLayout.Nested0515> field13802;
   private final float field13803;
   private final float field13804;

   public boolean method4011() {
      return this.field13802.isEmpty();
   }

   public float method4012() {
      return this.field13804;
   }

   public RasterTextLayout(List<RasterTextLayout.Nested0515> local6693_1, float local6693_2, float local6693_3) {
      this.field13802 = List.copyOf(local6693_1);
      this.field13803 = local6693_2;
      this.field13804 = local6693_3;
   }

   public float method4013() {
      return this.field13803;
   }

   public List<RasterTextLayout.Nested0515> method4014() {
      return this.field13802;
   }

   public record Nested0515(FontTexture page, List<RasterGlyphQuad> glyphs) {
      public Nested0515 {
         glyphs = List.copyOf(glyphs);
      }
   }
}
