package deluxe.client.render;

import deluxe.client.util.GuiScale;

public final class PixelSnap {
   static final int field11619 = 512;
   static final int field11620 = 2;
   static final int field11621 = 1024;

   static int method3297(float local5383_0) {
      return Math.max(1, Math.round(GuiScale.method0794()));
   }

   static float method3298(float local5384_0, float local5384_1) {
      return local5384_1 > 1.0E-4F ? Math.round(local5384_0 * local5384_1) / local5384_1 : method3300(local5384_0);
   }

   static int method3299(int local5385_0) {
      return Math.max(4, local5385_0 + 2);
   }

   public static float method3300(float local5386_0) {
      float local5386_1 = GuiScale.method0794();
      return Math.round(local5386_0 * local5386_1) / local5386_1;
   }

   private PixelSnap() {
   }
}
