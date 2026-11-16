package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;

public final class BottomHelper {
   @SerializedName("left")
   private float left;
   @SerializedName("bottom")
   private float bottom;
   @SerializedName("right")
   private float right;
   @SerializedName("top")
   private float top;

   public float bottom() {
      return this.bottom;
   }

   public float right() {
      return this.right;
   }

   public float top() {
      return this.top;
   }

   public float left() {
      return this.left;
   }
}
