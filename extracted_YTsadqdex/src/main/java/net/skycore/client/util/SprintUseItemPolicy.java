package net.skycore.client.util;

public final class SprintUseItemPolicy {
   public static boolean allows(boolean condition, boolean condition2, boolean condition3, boolean condition4) {
      return condition2 || !condition || condition3 || condition4;
   }

   private SprintUseItemPolicy() {
   }
}
