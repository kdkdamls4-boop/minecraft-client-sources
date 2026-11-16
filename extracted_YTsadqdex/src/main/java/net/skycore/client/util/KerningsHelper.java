package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class KerningsHelper {
   @SerializedName("kerning")
   private List<Unicode2Helper> kernings;
   @SerializedName("atlas")
   private DistanceRangeMiddleHelper atlas;
   @SerializedName("metrics")
   private AscentHelper metrics;
   @SerializedName("glyphs")
   private List<FontGlyph> glyphs;

   public List<Unicode2Helper> kernings() {
      return this.kernings;
   }

   public DistanceRangeMiddleHelper atlas() {
      return this.atlas;
   }

   public AscentHelper metrics() {
      return this.metrics;
   }

   public List<FontGlyph> glyphs() {
      return this.glyphs;
   }
}
