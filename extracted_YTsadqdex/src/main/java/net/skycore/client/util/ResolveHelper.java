package net.skycore.client.util;

import net.minecraft.util.Mth;

public final class ResolveHelper {

   public static float[] resolve(String text, float value, float value2, float[] floatValues, float[] floatValues2) {
      float computedValue = Mth.wrapDegrees(value);
      float value3 = value2;
      float value4 = wrap(computedValue);
      float computedValue2 = Math.abs(value4);
      float value5 = 1.5F;
      float value6 = 1.5F;
      if (computedValue2 > 45.0F) {
         computedValue2 = 90.0F - computedValue2;
      }

      float value7;
      if ((value7 = Math.abs(wrap(value3))) > 45.0F) {
         value7 = 90.0F - value7;
      }

      if (text.equals("ReallyWorld 1.16.5")) {
         if (computedValue2 <= 5.0F && value7 <= 5.0F) {
            value5 = 1.6F;
         } else if (computedValue2 <= 10.0F && value7 <= 10.0F) {
            value5 = 1.63F;
         } else if (computedValue2 <= 15.0F && value7 <= 15.0F) {
            value5 = 1.66F;
         } else if (computedValue2 <= 20.0F && value7 <= 20.0F) {
            value5 = 1.71F;
         } else if (computedValue2 <= 25.0F && value7 <= 25.0F) {
            value5 = 1.73F;
         } else if (computedValue2 <= 30.0F && value7 <= 30.0F) {
            value5 = 1.81F;
         } else if (computedValue2 <= 35.0F && value7 <= 35.0F) {
            value5 = 1.81F;
         } else if (computedValue2 <= 40.0F && value7 <= 40.0F) {
            value5 = 1.81F;
         } else if (computedValue2 <= 45.0F || value7 <= 45.0F) {
            value5 = 1.81F;
         }

         if ((
               value6 = value7 <= 5.0F
                  ? 1.6F
                  : (
                     value7 <= 10.0F
                        ? 1.62F
                        : (
                           value7 <= 15.0F
                              ? 1.6F
                              : (
                                 value7 <= 20.0F
                                    ? 1.62F
                                    : (
                                       value7 <= 25.0F
                                          ? 1.64F
                                          : (
                                             value7 <= 30.0F
                                                ? 1.68F
                                                : (value7 <= 35.0F ? 1.75F : (value7 <= 40.0F ? 1.9F : (value7 >= 43.0F && value7 <= 45.0F ? 2.26F : 2.16F)))
                                          )
                                    )
                              )
                        )
                  )
            )
            > value5) {
            value5 = value6;
         }
      }

      if (text.equals("ReallyWorld 1.21.11")) {
         value5 = computedValue2 <= 30.0F && value7 <= 30.0F ? 1.53F : (computedValue2 <= 40.0F && value7 <= 40.0F ? 2.04F : 2.059F);
         value6 = value7 <= 30.0F ? 1.53F : 2.02F;
         value5 = Math.max(value5, value6);
      }

      if (text.equals("BravoHvH")) {
         if (computedValue2 <= 5.0F && value7 <= 5.0F) {
            value5 = 1.64F;
         } else if (computedValue2 <= 10.0F && value7 <= 10.0F) {
            value5 = 1.67F;
         } else if (computedValue2 <= 15.0F && value7 <= 15.0F) {
            value5 = 1.68F;
         } else if (computedValue2 <= 20.0F && value7 <= 20.0F) {
            value5 = 1.73F;
         } else if (computedValue2 <= 25.0F && value7 <= 25.0F) {
            value5 = 1.77F;
         } else if (computedValue2 <= 30.0F && value7 <= 30.0F) {
            value5 = 1.87F;
         } else if (computedValue2 <= 35.0F && value7 <= 35.0F) {
            value5 = 2.01F;
         } else if (computedValue2 <= 40.0F && value7 <= 40.0F) {
            value5 = 2.05F;
         } else if (computedValue2 <= 45.0F || value7 <= 45.0F) {
            value5 = 2.05F;
         }

         value6 = value7 <= 5.0F
            ? 1.65F
            : (
               value7 <= 10.0F
                  ? 1.7F
                  : (
                     value7 <= 15.0F
                        ? 1.71F
                        : (
                           value7 <= 20.0F
                              ? 1.74F
                              : (value7 <= 25.0F ? 1.79F : (value7 <= 30.0F ? 1.91F : (value7 <= 35.0F ? 2.01F : (value7 <= 40.0F ? 2.02F : 2.02F))))
                        )
                  )
            );
      }

      if (text.equals("Custom")) {
         if (computedValue2 <= 5.0F && value7 <= 5.0F) {
            value5 = floatValues[0];
         } else if (computedValue2 <= 10.0F && value7 <= 10.0F) {
            value5 = floatValues[1];
         } else if (computedValue2 <= 15.0F && value7 <= 15.0F) {
            value5 = floatValues[2];
         } else if (computedValue2 <= 20.0F && value7 <= 20.0F) {
            value5 = floatValues[3];
         } else if (computedValue2 <= 25.0F && value7 <= 25.0F) {
            value5 = floatValues[4];
         } else if (computedValue2 <= 30.0F && value7 <= 30.0F) {
            value5 = floatValues[5];
         } else if (computedValue2 <= 35.0F && value7 <= 35.0F) {
            value5 = floatValues[6];
         } else if (computedValue2 <= 40.0F && value7 <= 40.0F) {
            value5 = floatValues[7];
         } else if (computedValue2 <= 45.0F || value7 <= 45.0F) {
            value5 = floatValues[8];
         }

         if ((
               value6 = value7 <= 5.0F
                  ? floatValues2[0]
                  : (
                     value7 <= 10.0F
                        ? floatValues2[1]
                        : (
                           value7 <= 15.0F
                              ? floatValues2[2]
                              : (
                                 value7 <= 20.0F
                                    ? floatValues2[3]
                                    : (value7 <= 25.0F ? floatValues2[4] : (value7 <= 30.0F ? floatValues2[5] : (value7 <= 35.0F ? floatValues2[6] : (value7 <= 40.0F ? floatValues2[7] : floatValues2[8]))))
                              )
                        )
                  )
            )
            > value5) {
            value5 = value6;
         }
      }

      return new float[]{value5, value6, value5};
   }

   private static float wrap(float value) {
      float result = value % 180.0F;
      if (result > 90.0F) {
         result -= 180.0F;
      } else if (result < -90.0F) {
         result += 180.0F;
      }

      return result;
   }
}
