package net.skycore.client.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomFloats {
   public static float unit() {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      return (float)((threadLocalRandom.nextLong() >>> 11) * 1.110223E-16F);
   }

   private RandomFloats() {
   }
}
