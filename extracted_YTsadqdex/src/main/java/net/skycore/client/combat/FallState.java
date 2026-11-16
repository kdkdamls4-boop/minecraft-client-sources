package net.skycore.client.combat;

public final class FallState {
   private FallState() {
   }

   public static boolean isFalling(boolean condition, double value) {
      return !condition && value < 0.0;
   }
}
