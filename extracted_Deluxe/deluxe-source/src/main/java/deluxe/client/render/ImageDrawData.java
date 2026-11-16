package deluxe.client.render;

public record ImageDrawData(
   String texture,
   float x,
   float y,
   float size,
   float radiusTL,
   float radiusTR,
   float radiusBR,
   float radiusBL,
   float smoothness,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float u0,
   float v0,
   float u1,
   float v1,
   float rotationDegrees,
   float rotationOriginX,
   float rotationOriginY,
   float explicitWidth,
   float explicitHeight,
   boolean nearest,
   boolean glow
) {
   public static final int DEFAULT_COLOR = -1;
   public static final float DEFAULT_SMOOTHNESS = 0.0F;

   public boolean visible() {
      return this.texture != null
         && !this.texture.isBlank()
         && (this.size > 0.0F || this.explicitWidth > 0.0F && this.explicitHeight > 0.0F)
         && (
            effectiveAlpha(this.colorTopLeft) > 0
               || effectiveAlpha(this.colorTopRight) > 0
               || effectiveAlpha(this.colorBottomRight) > 0
               || effectiveAlpha(this.colorBottomLeft) > 0
         );
   }

   public ImageDrawData withColors(int... intArray) {
      if (intArray != null && intArray.length != 0) {
         if (intArray.length == 1) {
            return this.withColors(intArray[0], intArray[0], intArray[0], intArray[0]);
         } else if (intArray.length == 2) {
            return this.withColors(intArray[0], intArray[1], intArray[1], intArray[0]);
         } else {
            return intArray.length == 3
               ? this.withColors(intArray[0], intArray[1], intArray[2], intArray[0])
               : this.withColors(intArray[0], intArray[1], intArray[2], intArray[3]);
         }
      } else {
         return this;
      }
   }

   public ImageDrawData withUv(float local3763_1, float local3763_2, float local3763_3, float local3763_4) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         local3763_1,
         local3763_2,
         local3763_3,
         local3763_4,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   private ImageDrawData withColors(int local3764_1, int local3764_2, int local3764_3, int local3764_4) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         local3764_1,
         local3764_2,
         local3764_3,
         local3764_4,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public ImageDrawData(String local3780_1, float local3780_2, float local3780_3, float local3780_4, float local3780_5, int local3780_6) {
      this(
         local3780_1,
         local3780_2,
         local3780_3,
         local3780_4,
         local3780_5,
         local3780_5,
         local3780_5,
         local3780_5,
         0.0F,
         local3780_6,
         local3780_6,
         local3780_6,
         local3780_6,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData(
      String local3782_1,
      float local3782_2,
      float local3782_3,
      float local3782_4,
      float local3782_5,
      float local3782_6,
      float local3782_7,
      float local3782_8,
      int local3782_9,
      int local3782_10,
      int local3782_11,
      int local3782_12
   ) {
      this(
         local3782_1,
         local3782_2,
         local3782_3,
         local3782_4,
         local3782_5,
         local3782_6,
         local3782_7,
         local3782_8,
         0.0F,
         local3782_9,
         local3782_10,
         local3782_11,
         local3782_12,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   private static int effectiveAlpha(int local3784_0) {
      return local3784_0 >>> 24 & 0xFF;
   }

   public ImageDrawData(
      String local3786_1,
      float local3786_2,
      float local3786_3,
      float local3786_4,
      float local3786_5,
      int local3786_6,
      int local3786_7,
      int local3786_8,
      int local3786_9
   ) {
      this(
         local3786_1,
         local3786_2,
         local3786_3,
         local3786_4,
         local3786_5,
         local3786_5,
         local3786_5,
         local3786_5,
         0.0F,
         local3786_6,
         local3786_7,
         local3786_8,
         local3786_9,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData withRotation(float local3787_1, float local3787_2, float local3787_3) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         this.smoothness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         local3787_1,
         local3787_2,
         local3787_3,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public ImageDrawData(String local3790_1, float local3790_2, float local3790_3, float local3790_4, float local3790_5) {
      this(
         local3790_1,
         local3790_2,
         local3790_3,
         local3790_4,
         local3790_5,
         local3790_5,
         local3790_5,
         local3790_5,
         0.0F,
         -1,
         -1,
         -1,
         -1,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         false
      );
   }

   public ImageDrawData withGlow() {
      return this.glow
         ? this
         : new ImageDrawData(
            this.texture,
            this.x,
            this.y,
            this.size,
            this.radiusTL,
            this.radiusTR,
            this.radiusBR,
            this.radiusBL,
            this.smoothness,
            this.colorTopLeft,
            this.colorTopRight,
            this.colorBottomRight,
            this.colorBottomLeft,
            this.u0,
            this.v0,
            this.u1,
            this.v1,
            this.rotationDegrees,
            this.rotationOriginX,
            this.rotationOriginY,
            this.explicitWidth,
            this.explicitHeight,
            this.nearest,
            true
         );
   }

   public ImageDrawData withSmoothness(float local3798_1) {
      return new ImageDrawData(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.radiusTL,
         this.radiusTR,
         this.radiusBR,
         this.radiusBL,
         local3798_1,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.explicitWidth,
         this.explicitHeight,
         this.nearest,
         this.glow
      );
   }

   public float radius() {
      return this.radiusTL;
   }

   public ImageDrawData(String local3800_1, float local3800_2, float local3800_3, float local3800_4, float local3800_5, float local3800_6, int local3800_7) {
      this(
         local3800_1,
         local3800_2,
         local3800_3,
         0.0F,
         local3800_6,
         local3800_6,
         local3800_6,
         local3800_6,
         0.0F,
         local3800_7,
         local3800_7,
         local3800_7,
         local3800_7,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         Math.max(0.0F, local3800_4),
         Math.max(0.0F, local3800_5),
         false,
         false
      );
   }

   public ImageDrawData withNearest() {
      return this.nearest
         ? this
         : new ImageDrawData(
            this.texture,
            this.x,
            this.y,
            this.size,
            this.radiusTL,
            this.radiusTR,
            this.radiusBR,
            this.radiusBL,
            this.smoothness,
            this.colorTopLeft,
            this.colorTopRight,
            this.colorBottomRight,
            this.colorBottomLeft,
            this.u0,
            this.v0,
            this.u1,
            this.v1,
            this.rotationDegrees,
            this.rotationOriginX,
            this.rotationOriginY,
            this.explicitWidth,
            this.explicitHeight,
            true,
            this.glow
         );
   }
}
