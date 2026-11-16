package deluxe.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;

public final class ThemePaletteBuffer {
   public static final int field07300 = 6;
   private static final int[] field07302 = new int[]{8150271, 6069759, 16739229};
   private static GpuBuffer field07305;
   private static int[] field07306 = new int[]{16777215};
   private static int field07307 = 1;
   private static float field07308;
   private static long field07309;
   private static float field07310;
   private static float field07311;
   private static float field07312 = -1.0F;
   private static float field07313;

   private static int[] method2140(int[] intArray, int local3366_1) {
      int[] intArray2 = new int[local3366_1];
      if (intArray.length != 1) {
         for (int index = 0; index < local3366_1; index++) {
            float local3366_4 = local3366_1 <= 1 ? 0.0F : (float)index / (local3366_1 - 1);
            float local3366_5 = local3366_4 * (intArray.length - 1);
            int local3366_6 = (int)local3366_5;
            if (local3366_6 > intArray.length - 2) {
               local3366_6 = intArray.length - 2;
            }

            intArray2[index] = method2150(intArray[local3366_6], intArray[local3366_6 + 1], local3366_5 - local3366_6);
         }

         return intArray2;
      } else {
         int local3366_3 = intArray[0] & 16777215;

         for (int index2 = 0; index2 < local3366_1; index2++) {
            intArray2[index2] = local3366_3;
         }

         return intArray2;
      }
   }

   public static int[] method2141() {
      return field07306;
   }

   private ThemePaletteBuffer() {
   }

   public static float method2142() {
      return field07310;
   }

   public static void method2143() {
      int[] intArray = method2140(field07302, 6);
      field07306 = intArray;
      field07307 = 6;
      field07313 = 0.0F;
      field07309 = System.currentTimeMillis();
      field07310 = 1.0F;
      if (field07312 >= 0.0F && field07310 != field07312) {
         field07311 = field07312;
         PaletteTransition.start();
      }

      field07312 = field07310;
      GpuBuffer gpuBuffer = method2149();
      if (gpuBuffer != null) {
         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(128);
               byteBuffer.putFloat(0, field07307);
               byteBuffer.putFloat(4, field07308);
               byteBuffer.putFloat(8, field07310);
               byteBuffer.putFloat(12, PaletteTransition.getProgress());
               byteBuffer.putFloat(112, field07311);
               byteBuffer.putFloat(116, field07313);

               for (int index = 0; index < field07307; index++) {
                  int local3370_5 = intArray[index] & 16777215;
                  int local3370_6 = (1 + index) * 4 * 4;
                  byteBuffer.putFloat(local3370_6, (local3370_5 >>> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(local3370_6 + 4, (local3370_5 >>> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(local3370_6 + 8, (local3370_5 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(local3370_6 + 12, 1.0F);
               }

               byteBuffer.position(0);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 128L), byteBuffer);
            } catch (Throwable throwable) {
               if (memoryStack != null) {
                  try {
                     memoryStack.close();
                  } catch (Throwable throwable2) {
                     throwable.addSuppressed(throwable2);
                  }
               }

               throw throwable;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         } catch (RuntimeException exception) {
         }
      }
   }

   public static boolean method2144() {
      return false;
   }

   private static float method2145(int[] intArray, int local3373_1, int local3373_2) {
      int local3373_3 = intArray[Math.max(0, Math.min(intArray.length - 1, local3373_1))];
      return (local3373_3 >> 16 - local3373_2 * 8 & 0xFF) / 255.0F;
   }

   public static float method2146() {
      return field07311;
   }

   public static int method2147() {
      return field07307;
   }

   public static float method2148() {
      return field07308;
   }

   private static GpuBuffer method2149() {
      if (field07305 != null && !field07305.isClosed() && field07305.size() >= 128L) {
         return field07305;
      }

      method2153();

      try {
         field07305 = RenderSystem.getDevice().createBuffer(() -> "deluxe_client_palette", 136, 128L);
         return field07305;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static int method2150(int local3378_0, int local3378_1, float local3378_2) {
      float local3378_3 = local3378_2 < 0.0F ? 0.0F : Math.min(local3378_2, 1.0F);
      int local3378_4 = local3378_0 >> 16 & 0xFF;
      int local3378_5 = local3378_0 >> 8 & 0xFF;
      int local3378_6 = local3378_0 & 0xFF;
      int local3378_7 = local3378_1 >> 16 & 0xFF;
      int local3378_8 = local3378_1 >> 8 & 0xFF;
      int local3378_9 = local3378_1 & 0xFF;
      int local3378_10 = Math.round(local3378_4 + (local3378_7 - local3378_4) * local3378_3);
      int local3378_11 = Math.round(local3378_5 + (local3378_8 - local3378_5) * local3378_3);
      int local3378_12 = Math.round(local3378_6 + (local3378_9 - local3378_6) * local3378_3);
      return local3378_10 << 16 | local3378_11 << 8 | local3378_12;
   }

   public static float method2151() {
      return field07313;
   }

   public static GpuBuffer method2152() {
      return method2149();
   }

   public static void method2153() {
      if (field07305 != null) {
         field07305.close();
         field07305 = null;
      }
   }

   private static float[] method2154(float local3382_0) {
      int[] intArray = field07306;
      int local3382_2 = field07307;
      if (local3382_2 <= 1) {
         return new float[]{method2145(intArray, 0, 0), method2145(intArray, 0, 1), method2145(intArray, 0, 2)};
      }

      float local3382_3 = Math.max(0.0F, Math.min(1.0F, local3382_0));
      float local3382_4 = local3382_3 * (local3382_2 - 1);
      int local3382_5 = Math.max(0, Math.min(local3382_2 - 1, (int)Math.floor(local3382_4)));
      int local3382_6 = Math.min(local3382_5 + 1, local3382_2 - 1);
      float local3382_7 = Math.max(0.0F, Math.min(1.0F, local3382_4 - local3382_5));
      local3382_7 = local3382_7 * local3382_7 * (3.0F - 2.0F * local3382_7);
      float[] floatArray = new float[3];

      for (int index = 0; index < 3; index++) {
         float local3382_10 = method2145(intArray, local3382_5, index);
         float local3382_11 = method2145(intArray, local3382_6, index);
         floatArray[index] = local3382_10 + (local3382_11 - local3382_10) * local3382_7;
      }

      return floatArray;
   }

   public static int method2155(float local3383_0) {
      int[] intArray = field07306;
      int local3383_2 = field07307;
      if (intArray == null || intArray.length == 0) {
         return 16777215;
      }

      if (local3383_2 <= 1) {
         return intArray[0] & 16777215;
      }

      float local3383_3 = local3383_0 - (float)Math.floor(local3383_0);
      float local3383_4 = local3383_3 * local3383_2;
      int local3383_5 = Math.max(0, Math.min(local3383_2 - 1, (int)Math.floor(local3383_4)));
      float local3383_6 = Math.max(0.0F, Math.min(1.0F, local3383_4 - local3383_5));
      int local3383_7 = local3383_5 - 1;
      if (local3383_7 < 0) {
         local3383_7 += local3383_2;
      }

      int local3383_8 = local3383_5 + 1;
      if (local3383_8 >= local3383_2) {
         local3383_8 -= local3383_2;
      }

      int local3383_9 = local3383_5 + 2;
      if (local3383_9 >= local3383_2) {
         local3383_9 -= local3383_2;
      }

      float local3383_10 = local3383_6 * local3383_6;
      float local3383_11 = local3383_10 * local3383_6;
      float local3383_12 = 0.5F - 0.5F * (float)Math.cos((float) (Math.PI * 2) * local3383_3);
      float[] floatArray = method2154(local3383_12);
      float local3383_14 = Math.max(0.0F, Math.min(1.0F, field07313));
      int local3383_15 = 0;

      for (int index = 0; index < 3; index++) {
         float local3383_17 = method2145(intArray, local3383_7, index);
         float local3383_18 = method2145(intArray, local3383_5, index);
         float local3383_19 = method2145(intArray, local3383_8, index);
         float local3383_20 = method2145(intArray, local3383_9, index);
         float local3383_21 = 0.5F
            * (
               2.0F * local3383_18
                  + (-local3383_17 + local3383_19) * local3383_6
                  + (2.0F * local3383_17 - 5.0F * local3383_18 + 4.0F * local3383_19 - local3383_20) * local3383_10
                  + (-local3383_17 + 3.0F * local3383_18 - 3.0F * local3383_19 + local3383_20) * local3383_11
            );
         float local3383_22 = floatArray[index] + (local3383_21 - floatArray[index]) * local3383_14;
         int local3383_23 = Math.round(Math.max(0.0F, Math.min(1.0F, local3383_22)) * 255.0F);
         local3383_15 |= local3383_23 << 16 - index * 8;
      }

      return local3383_15;
   }
}
