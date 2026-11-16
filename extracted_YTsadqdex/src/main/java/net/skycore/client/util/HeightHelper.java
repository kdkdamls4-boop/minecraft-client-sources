package net.skycore.client.util;

public final class HeightHelper {
   private final float maxV;
   private final float right;
   private final float bottom;
   private final float advance;
   private final float maxU;
   private final float minV;
   private final float minU;
   private final float top;
   private final float left;
   private final int code;
   private final boolean hasGeometry;

   public HeightHelper(FontGlyph code2, float value, float value2) {
      this.code = code2.unicode();
      this.advance = code2.advance();
      BottomHelper bottomHelper = code2.atlasBounds();
      BottomHelper bottomHelper2 = code2.planeBounds();
      this.hasGeometry = ((bottomHelper != null && bottomHelper2 != null ? 1 : 0) != 0);
      if (this.hasGeometry) {
         this.minU = bottomHelper.left() / value;
         this.maxU = bottomHelper.right() / value;
         this.minV = 1.0F - bottomHelper.top() / value2;
         this.maxV = 1.0F - bottomHelper.bottom() / value2;
         this.left = bottomHelper2.left();
         this.bottom = bottomHelper2.bottom();
         this.right = bottomHelper2.right();
         this.top = bottomHelper2.top();
      } else {
         this.minU = this.maxU = this.minV = this.maxV = this.left = this.bottom = this.right = this.top = 0.0F;
      }
   }

   public float height() {
      return this.top - this.bottom;
   }

   public float advance() {
      return this.advance;
   }

   public float right() {
      return this.right;
   }

   public float minU() {
      return this.minU;
   }

   public float left() {
      return this.left;
   }

   public int code() {
      return this.code;
   }

   public float maxU() {
      return this.maxU;
   }

   public float bottom() {
      return this.bottom;
   }

   public float maxV() {
      return this.maxV;
   }

   public float bearingY() {
      return this.top;
   }

   public float top() {
      return this.top;
   }

   public float minV() {
      return this.minV;
   }

   public boolean hasGeometry() {
      return this.hasGeometry;
   }

   public float width() {
      return this.right - this.left;
   }
}
