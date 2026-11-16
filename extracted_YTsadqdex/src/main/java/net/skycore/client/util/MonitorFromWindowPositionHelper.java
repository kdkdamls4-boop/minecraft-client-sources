package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public final class MonitorFromWindowPositionHelper {
   private static long lastCheckTime;
   private static final long CHECK_INTERVAL_NS = 1000000000L;
   private static long lastMonitorHandle;
   private static int lastRefreshRate = 60;

   private static long monitorFromWindowPosition(long timestamp, int value, int value2) {
      int[] intValues = new int[1];
      int[] intValues2 = new int[1];
      GLFW.glfwGetWindowPos(timestamp, intValues, intValues2);
      int width = intValues[0] + value / 2;
      int height = intValues2[0] + value2 / 2;
      long timestamp2 = GLFW.glfwGetPrimaryMonitor();
      PointerBuffer pointerBuffer = GLFW.glfwGetMonitors();
      if (pointerBuffer != null) {
         for (int value3 = 0; value3 < pointerBuffer.limit(); value3++) {
            long timestamp3 = pointerBuffer.get(value3);
            int[] intValues3 = new int[1];
            int[] intValues4 = new int[1];
            GLFW.glfwGetMonitorPos(timestamp3, intValues3, intValues4);
            GLFWVidMode gLFWVidMode = GLFW.glfwGetVideoMode(timestamp3);
            if (gLFWVidMode != null && width >= intValues3[0] && width < intValues3[0] + gLFWVidMode.width() && height >= intValues4[0] && height < intValues4[0] + gLFWVidMode.height()) {
               timestamp2 = timestamp3;
               break;
            }
         }
      }

      return timestamp2;
   }

   private static int refreshRateFromMonitor(long timestamp) {
      GLFWVidMode gLFWVidMode = GLFW.glfwGetVideoMode(timestamp);
      return gLFWVidMode != null ? gLFWVidMode.refreshRate() : 60;
   }

   private MonitorFromWindowPositionHelper() {
   }

   public static void updateDisplayInfo() {
      long nowNanos = System.nanoTime();
      if (nowNanos - lastCheckTime >= 1000000000L) {
         lastCheckTime = nowNanos;
         Minecraft instance = Minecraft.getInstance();
         if (instance != null && instance.getWindow() != null) {
            long windowHandle = instance.getWindow().handle();
            long timestamp = GLFW.glfwGetWindowMonitor(windowHandle);
            if (timestamp == 0L) {
               timestamp = monitorFromWindowPosition(windowHandle, instance.getWindow().getWidth(), instance.getWindow().getHeight());
            }

            if (timestamp != lastMonitorHandle) {
               lastRefreshRate = refreshRateFromMonitor(timestamp);
               lastMonitorHandle = timestamp;
            }
         }
      }
   }

   public static int getRefreshRate() {
      return lastRefreshRate;
   }
}
