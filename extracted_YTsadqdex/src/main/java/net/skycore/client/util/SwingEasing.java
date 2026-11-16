package net.skycore.client.util;

public final class SwingEasing {
   private SwingEasing() {
   }

   public static double easeSwing(double progress) {
      double computedValue = progress * progress;
      double computedValue2 = computedValue * progress;
      return 3.0 * progress * (1.0 - progress) * (1.0 - progress) * 0.44 + 3.0 * computedValue * (1.0 - progress) * 0.94 + computedValue2;
   }

   public static double easePickaxeSwing(double progress) {
      double computedValue = progress * progress;
      double computedValue2 = computedValue * progress;
      return 3.0 * progress * (1.0 - progress) * (1.0 - progress) * 0.44 + 3.0 * computedValue * (1.0 - progress) * 1.0 + computedValue2;
   }
}
