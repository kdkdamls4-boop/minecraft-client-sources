package net.skycore.client.util;

import net.minecraft.util.Mth;

public class CapeVector2 {
   public float y;
   public float x;

   public void copy(CapeVector2 x2) {
      this.x = x2.x;
      this.y = x2.y;
   }

   @Override
   public String toString() {
      return "Vector2 [x=" + this.x + ", y=" + this.y + "]";
   }

   public CapeVector2 normalize() {
      float x2 = this.x * this.x;
      float y2 = x2 + this.y * this.y;
      float squareRoot = Mth.sqrt(y2);
      if (squareRoot < 1.0E-4F) {
         this.x = 0.0F;
         this.y = 0.0F;
      } else {
         float x3 = this.x / squareRoot;
         this.x = x3;
         this.y /= squareRoot;
      }

      return this;
   }

   public CapeVector2 rotateDegrees(float value) {
      float x2 = this.x;
      float y2 = this.y;
      value = (float)Math.toRadians(value);
      double value2 = value;
      float computedValue = Mth.cos(value2) * x2;
      double intermediate = value;
      float computedValue2 = computedValue - Mth.sin(intermediate) * y2;
      this.x = computedValue2;
      float computedValue3 = Mth.sin(value) * x2;
      intermediate = value;
      float computedValue4 = computedValue3 + Mth.cos(intermediate) * y2;
      this.y = computedValue4;
      return this;
   }

   public CapeVector2 clone() {
      return new CapeVector2(this.x, this.y);
   }

   public CapeVector2 div(float value) {
      float x2 = this.x / value;
      this.x = x2;
      float y2 = this.y / value;
      this.y = y2;
      return this;
   }

   public CapeVector2 add(CapeVector2 normalizeHelper2) {
      float x2 = this.x + normalizeHelper2.x;
      this.x = x2;
      this.y = this.y + normalizeHelper2.y;
      return this;
   }

   public CapeVector2 mul(float value) {
      this.x *= value;
      float y2 = this.y * value;
      this.y = y2;
      return this;
   }

   public CapeVector2 subtract(CapeVector2 normalizeHelper2) {
      this.x = this.x - normalizeHelper2.x;
      float y2 = this.y - normalizeHelper2.y;
      this.y = y2;
      return this;
   }

   public CapeVector2(float x2, float y2) {
      this.x = x2;
      this.y = y2;
   }
}
