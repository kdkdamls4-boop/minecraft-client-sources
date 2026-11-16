package net.skycore.client.util;

import net.minecraft.util.Mth;

public class CapeVector3 {
   public float z;
   public float x;
   public float y;

   public CapeVector3(float x2, float y2, float z2) {
      this.x = x2;
      this.y = y2;
      this.z = z2;
   }

   public CapeVector3 normalize() {
      float x2 = this.x * this.x;
      float y2 = x2 + this.y * this.y;
      float squareRoot = Mth.sqrt(y2 + this.z * this.z);
      if (squareRoot < 1.0E-4F) {
         this.x = 0.0F;
         this.y = 0.0F;
         this.z = 0.0F;
      } else {
         this.x /= squareRoot;
         float y3 = this.y / squareRoot;
         this.y = y3;
         this.z /= squareRoot;
      }

      return this;
   }

   public CapeVector3 mul(float value) {
      this.x *= value;
      this.y *= value;
      float z2 = this.z * value;
      this.z = z2;
      return this;
   }

   public CapeVector3 div(float value) {
      float x2 = this.x / value;
      this.x = x2;
      this.y /= value;
      float z2 = this.z / value;
      this.z = z2;
      return this;
   }

   public CapeVector3 rotateDegrees(float value) {
      float x2 = this.x;
      float y2 = this.y;
      value = (float)Math.toRadians(value);
      float computedValue = Mth.cos(value) * x2;
      double value2 = value;
      float computedValue2 = computedValue - Mth.sin(value2) * y2;
      this.x = computedValue2;
      double value3 = value;
      float computedValue3 = Mth.sin(value3) * x2;
      double value4 = value;
      this.y = computedValue3 + Mth.cos(value4) * y2;
      return this;
   }

   public CapeVector3 clone() {
      return new CapeVector3(this.x, this.y, this.z);
   }

   @Override
   public String toString() {
      return "Vector3 [x=" + this.x + ", y=" + this.y + ", z=" + this.z + "]";
   }

   public CapeVector3 add(CapeVector3 normalizeHelper3) {
      this.x = this.x + normalizeHelper3.x;
      this.y = this.y + normalizeHelper3.y;
      float z2 = this.z + normalizeHelper3.z;
      this.z = z2;
      return this;
   }

   public CapeVector3 subtract(CapeVector3 normalizeHelper3) {
      float x2 = this.x - normalizeHelper3.x;
      this.x = x2;
      float y2 = this.y - normalizeHelper3.y;
      this.y = y2;
      this.z = this.z - normalizeHelper3.z;
      return this;
   }

   public void copy(CapeVector3 x2) {
      this.x = x2.x;
      this.y = x2.y;
      this.z = x2.z;
   }

   public float sqrMagnitude() {
      float x2 = this.x * this.x + this.y * this.y;
      return x2 + this.z * this.z;
   }
}
