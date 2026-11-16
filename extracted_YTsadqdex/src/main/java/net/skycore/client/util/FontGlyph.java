package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;

public final class FontGlyph {
   @SerializedName("planeBounds")
   private BottomHelper planeBounds;
   @SerializedName("advance")
   private float advance;
   @SerializedName("atlasBounds")
   private BottomHelper atlasBounds;
   @SerializedName("unicode")
   private int unicode;

   public float advance() {
      return this.advance;
   }

   public BottomHelper atlasBounds() {
      return this.atlasBounds;
   }

   public int unicode() {
      return this.unicode;
   }

   public BottomHelper planeBounds() {
      return this.planeBounds;
   }
}
