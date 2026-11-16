package net.skycore.client.util;

public final class MixColorHelper {
   public static final int BORDER = 234881023;
   public static final int TEXT_PRIMARY = -1;
   public static final int TOGGLER_KNOB = -13355203;
   public static final int CHIP_BORDER = 318767103;
   public static final int TEXT_TERTIARY = -10854809;
   public static final int SURFACE = -15658476;
   public static final int FIELD = -15197923;
   public static final int ELEVATED = -15526633;
   public static final int FIELD_ACTIVE = -14934494;
   public static final int TEXT_SECONDARY = -6183767;
   public static final int CHIP_IDLE = -15197923;
   public static final int TRACK = -14934494;
   public static final int BASE = -15855855;
   public static final int TEXT_QUATERNARY = -13355203;

   public static int mixColor(int value, int value2, float value3) {
      value3 = clamp(value3, 0.0F, 1.0F);
      float computedValue = (value >>> 24 & 0xFF) + ((value2 >>> 24 & 0xFF) - (value >>> 24 & 0xFF)) * value3;
      int roundedValue = Math.round(computedValue);
      float computedValue2 = (value >>> 16 & 0xFF) + ((value2 >>> 16 & 0xFF) - (value >>> 16 & 0xFF)) * value3;
      int roundedValue2 = Math.round(computedValue2);
      int computedValue3 = Math.round((value >>> 8 & 0xFF) + ((value2 >>> 8 & 0xFF) - (value >>> 8 & 0xFF)) * value3);
      int computedValue4 = Math.round((value & 0xFF) + ((value2 & 0xFF) - (value & 0xFF)) * value3);
      return roundedValue << 24 | roundedValue2 << 16 | computedValue3 << 8 | computedValue4;
   }

   public static int alpha(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int computedValue = Math.round((value >>> 24 & 0xFF) * value2);
      return computedValue << 24 | value & 16777215;
   }

   public static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   private MixColorHelper() {
   }
}
