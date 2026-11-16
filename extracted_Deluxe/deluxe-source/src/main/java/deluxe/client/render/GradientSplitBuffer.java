package deluxe.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryStack;

public final class GradientSplitBuffer {
   public static final int field10696 = 16;
   private static final float[] field10701 = new float[192];
   private static int field10702;
   private static GpuBuffer field10703;

   public static void method3124() {
      field10702 = 0;
      Arrays.fill(field10701, 0.0F);
   }

   public static GpuBuffer method3125() {
      return method3129();
   }

   public static void method3126() {
      GpuBuffer gpuBuffer = method3129();
      if (gpuBuffer != null) {
         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(768);

               for (int index = 0; index < 192; index++) {
                  byteBuffer.putFloat(index * 4, field10701[index]);
               }

               byteBuffer.position(0);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 768L), byteBuffer);
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

   private GradientSplitBuffer() {
   }

   public static void method3128() {
      if (field10703 != null) {
         field10703.close();
         field10703 = null;
      }
   }

   private static GpuBuffer method3129() {
      if (field10703 != null && !field10703.isClosed() && field10703.size() >= 768L) {
         return field10703;
      }

      method3128();

      try {
         field10703 = RenderSystem.getDevice().createBuffer(() -> "deluxe_client_splits", 136, 768L);
         return field10703;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public static int method3130(
      float local4990_0,
      float local4990_1,
      float local4990_2,
      float local4990_3,
      float local4990_4,
      float local4990_5,
      float local4990_6,
      float local4990_7,
      float local4990_8,
      float local4990_9
   ) {
      if (field10702 >= 16) {
         return 0;
      }

      int local4990_10 = field10702++;
      int local4990_11 = local4990_10 * 3 * 4;
      field10701[local4990_11] = local4990_0;
      field10701[local4990_11 + 1] = local4990_1;
      field10701[local4990_11 + 2] = local4990_2;
      field10701[local4990_11 + 3] = local4990_3;
      field10701[local4990_11 + 4] = local4990_4;
      field10701[local4990_11 + 5] = local4990_5;
      field10701[local4990_11 + 6] = local4990_6;
      field10701[local4990_11 + 7] = local4990_7;
      field10701[local4990_11 + 8] = local4990_8;
      field10701[local4990_11 + 9] = local4990_9;
      return local4990_10 + 1;
   }
}
