package net.skycore.client.util;

import com.mojang.blaze3d.platform.Window;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.skycore.client.profile.LocalProfile;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.windows.WindowsLibrary;

public final class ApplyWindowIconHelper {
   private static boolean dwmResolved;
   private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;
   private static SharedLibrary dwmLibrary;
   private static long iconWindow;
   private static long dwmSetWindowAttribute;
   private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
   private static final String ICON_RESOURCE = "/assets/skycore/textures/tags/main.png";

   private static synchronized void applyWindowIcon(long timestamp) {
      if (timestamp != 0L && iconWindow != timestamp) {
         ByteBuffer byteBuffer = null;
         ByteBuffer byteBuffer2 = null;

         try (InputStream resourceAsStream = ApplyWindowIconHelper.class.getResourceAsStream("/assets/skycore/textures/tags/main.png")) {
            if (resourceAsStream != null) {
               byte[] byteValues = resourceAsStream.readAllBytes();
               byteBuffer = MemoryUtil.memAlloc(byteValues.length);
               byteBuffer.put(byteValues).flip();

               try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                  IntBuffer intBuffer = memoryStack.mallocInt(1);
                  IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                  IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                  byteBuffer2 = STBImage.stbi_load_from_memory(byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
                  if (byteBuffer2 != null) {
                     Buffer buffer = GLFWImage.malloc(1, memoryStack);
                     ((Buffer)buffer.position(0)).width(intBuffer.get(0)).height(intBuffer2.get(0)).pixels(byteBuffer2);
                     GLFW.glfwSetWindowIcon(timestamp, buffer);
                     iconWindow = timestamp;
                  }
               }
            }
         } catch (Throwable failure) {
         } finally {
            if (byteBuffer2 != null) {
               STBImage.stbi_image_free(byteBuffer2);
            }

            if (byteBuffer != null) {
               MemoryUtil.memFree(byteBuffer);
            }
         }
      }
   }

   private ApplyWindowIconHelper() {
   }

   private static synchronized void resolveDwm() {
      if (!dwmResolved) {
         dwmResolved = true;

         try {
            WindowsLibrary windowsLibrary = new WindowsLibrary("dwmapi.dll");
            dwmLibrary = windowsLibrary;
            dwmSetWindowAttribute = dwmLibrary.getFunctionAddress("DwmSetWindowAttribute");
         } catch (Throwable failure) {
            dwmSetWindowAttribute = 0L;
         }
      }
   }

   private static int setDwmAttribute(long dwmAttribute, int dwmAttribute2, int dwmAttribute3) {

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         IntBuffer intBuffer = memoryStack.ints(dwmAttribute3);
         long timestamp = dwmAttribute2;
         long timestamp2 = MemoryUtil.memAddress(intBuffer);
         return JNI.callPPPI(dwmAttribute, timestamp, timestamp2, 4, dwmSetWindowAttribute);
      }
   }

   private static void applyWindows11TitleBar(long timestamp) {
      if (timestamp != 0L && Platform.get() == Platform.WINDOWS) {
         try {
            resolveDwm();
            if (dwmSetWindowAttribute == 0L) {
               return;
            }

            long timestamp2 = GLFWNativeWin32.glfwGetWin32Window(timestamp);
            if (timestamp2 == 0L) {
               return;
            }

            if (setDwmAttribute(timestamp2, 20, 1) != 0) {
               setDwmAttribute(timestamp2, 19, 1);
            }
         } catch (Throwable failure) {
         }
      }
   }

   public static void apply(Minecraft minecraft) {
      if (minecraft != null && minecraft.getWindow() != null) {
         Window window = minecraft.getWindow();
         long windowHandle = window.handle();
         Window window2 = minecraft.getWindow();
         window2.setTitle(LocalProfile.DISPLAY_NAME);
         applyWindows11TitleBar(windowHandle);
         applyWindowIcon(windowHandle);
      }
   }
}
