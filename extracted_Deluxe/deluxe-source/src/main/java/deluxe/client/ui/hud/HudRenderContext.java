package deluxe.client.ui.hud;

import deluxe.client.util.GuiScale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public final class HudRenderContext {
   private static boolean field14867;
   private static boolean field14868;
   private static float field14869;
   private static float field14870;
   private static long field14871;
   private static long field14872 = System.currentTimeMillis();
   private static int field14873;
   private static DrawContext field14874;

   public static void method4333(DrawContext drawContext) {
      field14874 = drawContext;
   }

   static void method4334(MinecraftClient minecraftClient, float local7300_1) {
      field14867 = minecraftClient.currentScreen instanceof ChatScreen;
      long local7300_2 = System.currentTimeMillis();
      if (field14867 != field14868) {
         field14870 = field14869;
         field14871 = local7300_2;
         field14868 = field14867;
      }

      float local7300_4 = Math.min(1.0F, (float)(local7300_2 - field14871) / (field14867 ? 180.0F : 120.0F));
      field14869 = field14870 + ((field14867 ? 1.0F : 0.0F) - field14870) * (1.0F - (float)Math.pow(1.0F - local7300_4, 3.0));
      if (local7300_2 - field14872 >= 1000L) {
         field14872 = local7300_2;
         field14873++;
      }
   }

   public static void method4335(float local7301_0, float local7301_1, float local7301_2, float local7301_3) {
      if (field14874 != null) {
         float local7301_4 = GuiScale.method0800(local7301_0);
         float local7301_5 = GuiScale.method0800(local7301_1);
         field14874.getMatrices().pushMatrix();
         field14874.getMatrices().translate(local7301_4, local7301_5);
         field14874.getMatrices().scale(local7301_2, local7301_3);
         field14874.getMatrices().translate(-local7301_4, -local7301_5);
      }
   }

   public static void method4336(float local7302_0, float local7302_1, float local7302_2) {
      method4335(local7302_0, local7302_1, local7302_2, local7302_2);
   }

   public static void method4337() {
      if (field14874 != null) {
         field14874.getMatrices().popMatrix();
      }
   }

   public static float method4338() {
      return field14869;
   }

   public static int method4339() {
      return field14873;
   }

   public static DrawContext method4340() {
      return field14874;
   }

   public static void method4341(float local7308_0, float local7308_1, float local7308_2) {
      method4336(local7308_0, local7308_1, 1.0F);
   }

   public static boolean method4342() {
      return field14869 > 0.01F;
   }

   public static boolean method4343() {
      return field14867;
   }

   private HudRenderContext() {
   }
}
