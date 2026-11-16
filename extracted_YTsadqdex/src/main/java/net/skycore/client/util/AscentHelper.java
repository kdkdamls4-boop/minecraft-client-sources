package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;

public final class AscentHelper {
   @SerializedName("lineHeight")
   private float lineHeight;
   @SerializedName("descender")
   private float descender;
   @SerializedName("emSize")
   private float emSize;
   @SerializedName("underlineY")
   private float underlineY;
   @SerializedName("ascender")
   private float ascender;
   @SerializedName("underlineThickness")
   private float underlineThickness;

   public float ascent() {
      return this.ascender;
   }

   public float underlineY() {
      return this.underlineY;
   }

   public float descent() {
      return this.descender;
   }

   public float emSize() {
      return this.emSize;
   }

   public float underlineThickness() {
      return this.underlineThickness;
   }

   public float lineHeight() {
      return this.lineHeight;
   }
}
