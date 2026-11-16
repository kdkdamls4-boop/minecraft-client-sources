package net.skycore.client.util;

import com.google.gson.annotations.SerializedName;

public final class Unicode2Helper {
   @SerializedName("advance")
   private float advance;
   @SerializedName("unicode1")
   private int unicode1;
   @SerializedName("unicode2")
   private int unicode2;

   public int unicode2() {
      return this.unicode2;
   }

   public float advance() {
      return this.advance;
   }

   public int unicode1() {
      return this.unicode1;
   }
}
