package net.skycore.client.combat;

import java.util.concurrent.ThreadLocalRandom;

public final class AttackInputFreeze {
   private static final long MIN_HOLD_MS = 60L;
   private static long lastImminentNanos = Long.MIN_VALUE;
   private static final long MAX_HOLD_MS = 91L;
   private static long holdMillis = 75L;
   private static final long DEFAULT_HOLD_MS = 75L;

   public static boolean shouldFreezeMovementInput() {
      if (lastImminentNanos != Long.MIN_VALUE) {
         if (System.nanoTime() - lastImminentNanos < holdMillis * 1000000L) {
            return true;
         }
      }

      return false;
   }

   private AttackInputFreeze() {
   }

   public static void markAttackImminent() {
      lastImminentNanos = System.nanoTime();
   }

   public static void reset() {
      lastImminentNanos = Long.MIN_VALUE;
      holdMillis = 75L;
   }

   public static void onAttack() {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      holdMillis = threadLocalRandom.nextLong(60L, 92L);
      long nowNanos = System.nanoTime();
      lastImminentNanos = nowNanos;
   }
}
