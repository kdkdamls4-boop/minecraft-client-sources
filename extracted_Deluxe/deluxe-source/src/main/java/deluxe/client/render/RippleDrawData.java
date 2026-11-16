package deluxe.client.render;

public record RippleDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float smoothness,
   float centerX,
   float centerY,
   float rippleRadius,
   float rippleSmoothness,
   int sourceColor,
   int targetColor,
   boolean isTextured
) {
   public RippleDrawData(
      float local2756_1,
      float local2756_2,
      float local2756_3,
      float local2756_4,
      float local2756_5,
      float local2756_6,
      float local2756_7,
      float local2756_8,
      float local2756_9,
      float local2756_10,
      int local2756_11,
      int local2756_12
   ) {
      this(
         local2756_1,
         local2756_2,
         local2756_3,
         local2756_4,
         local2756_5,
         local2756_5,
         local2756_5,
         local2756_5,
         local2756_6,
         local2756_7,
         local2756_8,
         local2756_9,
         local2756_10,
         local2756_11,
         local2756_12,
         false
      );
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F;
   }
}
