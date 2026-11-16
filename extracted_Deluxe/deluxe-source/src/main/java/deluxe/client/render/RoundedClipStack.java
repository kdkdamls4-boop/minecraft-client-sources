package deluxe.client.render;

import java.util.ArrayDeque;

public final class RoundedClipStack {
   private static float field16171;
   private static float field16172;
   private static float field16173;
   private static float field16174;
   private static float field16175;
   private static float field16176;
   private static float field16177;
   private static float field16178;
   private static boolean field16179;
   private static final ArrayDeque<float[]> field16180 = new ArrayDeque<>();

   public static float method4825() {
      return field16176;
   }

   public static float method4826() {
      return field16174;
   }

   public static float method4827() {
      return field16175;
   }

   public static float method4828() {
      return field16172;
   }

   public static void method4829() {
      field16180.clear();
      field16179 = false;
   }

   public static float method4830() {
      return field16177;
   }

   public static float method4831() {
      return field16173;
   }

   public static float method4832() {
      return field16178;
   }

   public static float method4833(float local8029_0, float local8029_1) {
      if (!field16179) {
         return 1.0F;
      }

      float local8029_2 = field16171 + field16173 * 0.5F;
      float local8029_3 = field16172 + field16174 * 0.5F;
      boolean local8029_4 = local8029_0 < local8029_2;
      boolean local8029_5 = local8029_1 < local8029_3;
      float local8029_6 = local8029_5 ? (local8029_4 ? field16175 : field16176) : (local8029_4 ? field16178 : field16177);
      float local8029_7 = Math.abs(local8029_0 - local8029_2) - (field16173 * 0.5F - local8029_6);
      float local8029_8 = Math.abs(local8029_1 - local8029_3) - (field16174 * 0.5F - local8029_6);
      float local8029_9 = Math.max(local8029_7, 0.0F);
      float local8029_10 = Math.max(local8029_8, 0.0F);
      float local8029_11 = Math.min(Math.max(local8029_7, local8029_8), 0.0F)
         + (float)Math.sqrt(local8029_9 * local8029_9 + local8029_10 * local8029_10)
         - local8029_6;
      return Math.max(0.0F, Math.min(1.0F, -local8029_11));
   }

   private RoundedClipStack() {
   }

   public static float method4834() {
      return field16171;
   }

   public static int method4835(int local8033_0, float local8033_1, float local8033_2) {
      float local8033_3 = method4833(local8033_1, local8033_2);
      if (local8033_3 >= 0.999F) {
         return local8033_0;
      }

      if (local8033_3 <= 0.001F) {
         return local8033_0 & 16777215;
      }

      int local8033_4 = Math.round((local8033_0 >>> 24 & 0xFF) * local8033_3);
      return local8033_4 << 24 | local8033_0 & 16777215;
   }

   public static void method4836(
      float local8034_0, float local8034_1, float local8034_2, float local8034_3, float local8034_4, float local8034_5, float local8034_6, float local8034_7
   ) {
      if (field16179) {
         field16180.push(new float[]{field16171, field16172, field16173, field16174, field16175, field16176, field16177, field16178});
      }

      field16171 = local8034_0;
      field16172 = local8034_1;
      field16173 = local8034_2;
      field16174 = local8034_3;
      field16175 = local8034_4;
      field16176 = local8034_5;
      field16177 = local8034_6;
      field16178 = local8034_7;
      field16179 = true;
   }

   public static boolean method4837() {
      return field16179;
   }

   public static void method4838() {
      float[] floatArray = field16180.poll();
      if (floatArray != null) {
         field16171 = floatArray[0];
         field16172 = floatArray[1];
         field16173 = floatArray[2];
         field16174 = floatArray[3];
         field16175 = floatArray[4];
         field16176 = floatArray[5];
         field16177 = floatArray[6];
         field16178 = floatArray[7];
      } else {
         field16179 = false;
      }
   }
}
