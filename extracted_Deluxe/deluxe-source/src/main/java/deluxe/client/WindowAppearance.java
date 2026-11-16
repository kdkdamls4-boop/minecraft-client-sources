package deluxe.client;

import java.nio.IntBuffer;
import java.util.Locale;
import net.minecraft.SharedConstants;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.WindowsLibrary;

public final class WindowAppearance {
   public static final String field15028 = method4397();
   private static WindowsLibrary field15029;
   private static long field15030;

   private static boolean method4396() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   public static String method4397() {
      return "Deluxe #3.7 - " + method4398();
   }

   public static String method4398() {
      return SharedConstants.getGameVersion().name();
   }

   public static String method4399() {
      return "Deluxe LoL  ·  " + method4398();
   }

   private static long method4400() {
      if (field15030 != 0L) {
         return field15030;
      }

      if (field15029 == null) {
         field15029 = new WindowsLibrary("dwmapi.dll");
      }

      field15030 = field15029.getFunctionAddress("DwmSetWindowAttribute");
      return field15030;
   }

   public static void method4401(long local7413_0) {
      if (method4396() && local7413_0 != 0L) {
         try {
            long local7413_2 = GLFWNativeWin32.glfwGetWin32Window(local7413_0);
            long local7413_4 = method4400();
            if (local7413_2 == 0L || local7413_4 == 0L) {
               return;
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               IntBuffer intBuffer = memoryStack.ints(1);
               long local7413_8 = MemoryUtil.memAddress(intBuffer);
               int local7413_10 = JNI.callPPI(local7413_2, 20, local7413_8, 4, local7413_4);
               if (local7413_10 != 0) {
                  JNI.callPPI(local7413_2, 19, local7413_8, 4, local7413_4);
               }
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
         } catch (Throwable throwable3) {
         }
      }
   }

   private WindowAppearance() {
   }
}
