package deluxe.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
   private static float[] field13210;
   private static float[] field13211;
   private static long field13212 = 0L;
   private static SecureRandom field13213 = new SecureRandom();

   public static float lerp(float local6273_0, float local6273_1, float local6273_2) {
      return local6273_0 + local6273_2 * (local6273_1 - local6273_0);
   }

   public static float clampUnit(float local6274_0) {
      return method3827(0.0F, 1.0F, local6274_0);
   }

   public static int clampInt(int local6275_0, int local6275_1, int local6275_2) {
      return Math.max(local6275_0, Math.min(local6275_1, local6275_2));
   }

   public static float randomFloat(float local6276_0, float local6276_1) {
      method3819(local6276_0, local6276_1);
      return local6276_0 + ThreadLocalRandom.current().nextFloat() * (local6276_1 - local6276_0);
   }

   public static double roundToStep(double local6277_0, double local6277_2) {
      if (local6277_2 <= 0.0) {
         throw new IllegalArgumentException("Step size must be positive");
      }

      long local6277_4 = Math.round(local6277_0 / local6277_2);
      return local6277_4 * local6277_2;
   }

   public static double method3805(double local6278_0, double local6278_2, float local6278_4) {
      return local6278_2 + (local6278_0 - local6278_2) * local6278_4;
   }

   public static float method3806(float local6279_0, float local6279_1) {
      if (local6279_1 <= 0.0F) {
         throw new IllegalArgumentException("Step size must be positive");
      }

      int local6279_2 = Math.round(local6279_0 / local6279_1);
      return local6279_2 * local6279_1;
   }

   public static float method3807(float local6280_0, float local6280_1) {
      method3819(local6280_0, local6280_1);
      return local6280_0 >= local6280_1 ? local6280_0 : (float)ThreadLocalRandom.current().nextDouble(local6280_0, local6280_1);
   }

   public static int method3808(int local6281_0, int local6281_1, float local6281_2) {
      return local6281_0 + (int)(local6281_2 * (local6281_1 - local6281_0));
   }

   public static float method3809(float local6282_0, float local6282_1) {
      float local6282_2 = field13213.nextFloat();
      float local6282_3 = field13213.nextFloat();
      return local6282_0 + (local6282_1 - local6282_0) * ((local6282_2 + local6282_3) / 2.0F);
   }

   public static double method3810(double local6283_0, double local6283_2) {
      method3819(local6283_0, local6283_2);
      return local6283_0 >= local6283_2 ? local6283_0 : ThreadLocalRandom.current().nextDouble(local6283_0, local6283_2);
   }

   public static double method3811(double local6284_0, double local6284_2, double local6284_4) {
      return local6284_0 + local6284_4 * (local6284_2 - local6284_0);
   }

   public static float method3812(float local6285_0) {
      return method3827(0.0F, 360.0F, local6285_0);
   }

   public static float method3813(float local6286_0, float local6286_1) {
      double local6286_2 = (double)Math.round(local6286_0 / local6286_1) * local6286_1;
      BigDecimal bigDecimal = new BigDecimal(local6286_2);
      bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
      return bigDecimal.floatValue();
   }

   public static float method3814(double local6287_0) {
      return (float)local6287_0;
   }

   public static double method3815(double local6288_0, double local6288_2) {
      return Math.abs(local6288_0 - local6288_2);
   }

   public static SecureRandom method3816() {
      return field13213;
   }

   public static float method3817(float local6290_0, float local6290_1) {
      return local6290_0 + (local6290_1 - local6290_0) * field13213.nextFloat();
   }

   public static float method3818(float local6291_0, float local6291_1) {
      double local6291_2 = field13213.nextDouble();
      double local6291_4 = field13213.nextDouble();
      double local6291_6 = field13213.nextGaussian() * 0.02F;
      double local6291_8 = Math.pow(local6291_2, 1.0 + field13213.nextDouble() * 0.7);
      double local6291_10 = (local6291_4 * 0.8 + 0.1) * (Math.log1p(local6291_2 * 3.0) * 0.5 + 0.5);
      return (float)(local6291_0 + (local6291_1 - local6291_0) * local6291_8 * local6291_10 + local6291_6);
   }

   private static void method3819(double local6292_0, double local6292_2) {
      if (local6292_0 > local6292_2) {
         throw new IllegalArgumentException("Minimum value cannot be greater than maximum value.");
      }
   }

   public static double method3820(double local6293_0, int local6293_2) {
      double local6293_3 = Math.pow(10.0, local6293_2);
      return Math.round(local6293_0 * local6293_3) / local6293_3;
   }

   public static double method3821(double local6294_0) {
      return method3824(0.0, 1.0, local6294_0);
   }

   private MathUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static long method3822(long local6296_0, long local6296_2) {
      return field13213.nextLong(local6296_0, local6296_2);
   }

   public static float method3823(float local6297_0, float local6297_1) {
      method3819(local6297_0, local6297_1);
      return local6297_0 + ThreadLocalRandom.current().nextFloat() * (local6297_1 - local6297_0);
   }

   public static double method3824(double local6298_0, double local6298_2, double local6298_4) {
      return Math.max(local6298_0, Math.min(local6298_2, local6298_4));
   }

   public static float method3825(float local6299_0, float local6299_1) {
      switch (field13213.nextInt(4)) {
         case 0:
            return method3809(local6299_0, local6299_1);
         case 1:
            return method3818(local6299_0, local6299_1);
         case 2:
            return method3826(local6299_0, local6299_1);
         default:
            return method3817(local6299_0, local6299_1);
      }
   }

   public static float method3826(float local6300_0, float local6300_1) {
      return local6300_0 + (local6300_1 - local6300_0) * (float)Math.random();
   }

   public static float method3827(float local6302_0, float local6302_1, float local6302_2) {
      return Math.max(local6302_0, Math.min(local6302_1, local6302_2));
   }

   public static float method3828(float local6303_0, float local6303_1, float local6303_2) {
      if (field13210 == null) {
         field13210 = new float[]{(local6303_0 + local6303_1) / 2.0F};
         field13211 = new float[]{randomFloat(local6303_0, local6303_1)};
      }

      if (System.currentTimeMillis() - field13212 > 250.0 + Math.random() * 300.0) {
         field13211[0] = randomFloat(local6303_0, local6303_1);
         field13212 = System.currentTimeMillis();
      }

      field13210[0] = field13210[0] + (field13211[0] - field13210[0]) * local6303_2;
      return field13210[0];
   }

   public static float method3829(float local6304_0, float local6304_1, float local6304_2) {
      return local6304_1 + (local6304_0 - local6304_1) * local6304_2;
   }

   public static double method3830(double local6305_0) {
      return method3824(0.0, 360.0, local6305_0);
   }

   public static double method3831(double local6306_0, double local6306_2, double local6306_4, double local6306_6, double local6306_8, double local6306_10) {
      double local6306_12 = local6306_6 - local6306_0;
      double local6306_14 = local6306_8 - local6306_2;
      double local6306_16 = local6306_10 - local6306_4;
      return Math.sqrt(local6306_12 * local6306_12 + local6306_14 * local6306_14 + local6306_16 * local6306_16);
   }
}
