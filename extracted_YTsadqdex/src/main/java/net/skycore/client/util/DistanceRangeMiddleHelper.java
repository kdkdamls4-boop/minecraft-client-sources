package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;

public final class DistanceRangeMiddleHelper {
   @SerializedName("distanceRangeMiddle")
   private float distanceRangeMiddle;
   @SerializedName("width")
   private int width;
   @SerializedName("yOrigin")
   private String yOrigin;
   @SerializedName("distanceRange")
   private float distanceRange;
   @SerializedName("type")
   private String type;
   @SerializedName("height")
   private int height;
   @SerializedName("size")
   private float size;

   public float distanceRangeMiddle() {
      return this.distanceRangeMiddle;
   }

   public int width() {
      return this.width;
   }

   public float size() {
      return this.size;
   }

   public String type() {
      return this.type;
   }

   public int height() {
      return this.height;
   }

   public String yOrigin() {
      return this.yOrigin;
   }

   public float distanceRange() {
      return this.distanceRange;
   }
}
