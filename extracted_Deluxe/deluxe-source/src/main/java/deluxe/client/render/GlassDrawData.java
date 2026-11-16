package deluxe.client.render;

import deluxe.client.render.GlassRenderer;

import net.minecraft.client.gui.DrawContext;

public record GlassDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int color,
   float globalAlpha,
   float fresnelPower,
   int fresnelColor,
   float baseAlpha,
   boolean fresnelInvert,
   float fresnelMix,
   float distortStrength,
   float squirt,
   float z,
   float blurRadius,
   int secondColor,
   float colorOffset,
   int splitIndex
) {
   private static float radiusValue(float[] floatArray, int local5484_1) {
      return floatArray != null && local5484_1 >= 0 && local5484_1 < floatArray.length ? floatArray[local5484_1] : 0.0F;
   }

   public GlassDrawData withBlurRadius(float local5489_1) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         local5489_1,
         this.secondColor,
         this.colorOffset
      );
   }

   public GlassDrawData withRainbow(float local5493_1, float local5493_2) {
      int local5493_3 = Math.max(0, Math.min(255, Math.round(local5493_1 * 255.0F)));
      int local5493_4 = Math.max(0, Math.min(255, Math.round(local5493_2 * 255.0F)));
      int local5493_5 = 0xFF000000 | local5493_3 << 16 | local5493_4 << 8;
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         1.0F,
         this.blurRadius,
         local5493_5,
         this.colorOffset
      );
   }

   public GlassDrawData withSplitIndex(int local5496_1) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         this.secondColor,
         this.colorOffset,
         local5496_1
      );
   }

   public GlassDrawData(
      float local5497_1,
      float local5497_2,
      float local5497_3,
      float local5497_4,
      float local5497_5,
      float local5497_6,
      float local5497_7,
      float local5497_8,
      int local5497_9,
      float local5497_10,
      float local5497_11,
      int local5497_12,
      float local5497_13,
      boolean local5497_14,
      float local5497_15,
      float local5497_16,
      float local5497_17,
      float local5497_18
   ) {
      this(
         local5497_1,
         local5497_2,
         local5497_3,
         local5497_4,
         local5497_5,
         local5497_6,
         local5497_7,
         local5497_8,
         local5497_9,
         local5497_10,
         local5497_11,
         local5497_12,
         local5497_13,
         local5497_14,
         local5497_15,
         local5497_16,
         local5497_17,
         local5497_18,
         30.0F,
         local5497_9,
         0.0F
      );
   }

   public void render(DrawContext drawContext) {
      GlassRenderer.method2703().method2709(drawContext, this);
   }

   public GlassDrawData(
      float local5503_1,
      float local5503_2,
      float local5503_3,
      float local5503_4,
      float local5503_5,
      float local5503_6,
      float local5503_7,
      float local5503_8,
      int local5503_9,
      float local5503_10,
      float local5503_11,
      int local5503_12,
      float local5503_13,
      boolean local5503_14,
      float local5503_15,
      float local5503_16,
      float local5503_17,
      float local5503_18,
      float local5503_19,
      int local5503_20,
      float local5503_21
   ) {
      this(
         local5503_1,
         local5503_2,
         local5503_3,
         local5503_4,
         local5503_5,
         local5503_6,
         local5503_7,
         local5503_8,
         local5503_9,
         local5503_10,
         local5503_11,
         local5503_12,
         local5503_13,
         local5503_14,
         local5503_15,
         local5503_16,
         local5503_17,
         local5503_18,
         local5503_19,
         local5503_20,
         local5503_21,
         0
      );
   }

   public GlassDrawData withSecondColor(int local5505_1, float local5505_2) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         local5505_1,
         local5505_2
      );
   }

   public GlassDrawData(
      float local5508_1,
      float local5508_2,
      float local5508_3,
      float local5508_4,
      float[] floatArray,
      int local5508_6,
      float local5508_7,
      float local5508_8,
      int local5508_9,
      float local5508_10,
      boolean local5508_11,
      float local5508_12,
      float local5508_13,
      float local5508_14,
      float local5508_15
   ) {
      this(
         local5508_1,
         local5508_2,
         local5508_3,
         local5508_4,
         radiusValue(floatArray, 0),
         radiusValue(floatArray, 1),
         radiusValue(floatArray, 2),
         radiusValue(floatArray, 3),
         local5508_6,
         local5508_7,
         local5508_8,
         local5508_9,
         local5508_10,
         local5508_11,
         local5508_12,
         local5508_13,
         local5508_14,
         local5508_15
      );
   }

   public boolean visible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.globalAlpha > 0.0F
         && (this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0 || this.color >>> 24 != 0);
   }
}
