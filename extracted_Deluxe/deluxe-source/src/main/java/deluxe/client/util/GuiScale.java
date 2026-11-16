package deluxe.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2f;

public final class GuiScale {
   private static final Matrix3x2f field01432 = new Matrix3x2f();

   public static float method0794() {
      return 2.0F;
   }

   public static Matrix3x2f method0795(DrawContext drawContext) {
      Matrix3x2f matrix3x2f = field01432.set(drawContext.getMatrices());
      method0801(matrix3x2f);
      return matrix3x2f;
   }

   public static float method0796() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return 0.0F;
      }

      Window window = minecraftClient.getWindow();
      return window == null ? 0.0F : window.getFramebufferHeight();
   }

   public static int method0797() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return 1;
      }

      Window window = minecraftClient.getWindow();
      return window == null ? 1 : Math.max(1, window.getScaleFactor());
   }

   public static int method0798(float local1068_0) {
      return Math.round(method0800(local1068_0));
   }

   private GuiScale() {
   }

   public static float method0799() {
      return 2.0F / method0797();
   }

   public static float method0800(float local1072_0) {
      return local1072_0 * method0799();
   }

   public static void method0801(Matrix3x2f matrix3x2f) {
      float local1073_1 = method0799();
      if (local1073_1 != 1.0F) {
         matrix3x2f.scale(local1073_1);
      }
   }
}
