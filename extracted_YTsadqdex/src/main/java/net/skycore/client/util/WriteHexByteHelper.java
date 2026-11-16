package net.skycore.client.util;

import java.awt.Color;
import java.util.Locale;
import java.util.Objects;

public final class WriteHexByteHelper {
   public static final int RED = -65536;
   public static final int BLUE = -16776961;
   public static final int BLACK = -16777216;
   private static final char[] HEX = "0123456789ABCDEF".toCharArray();
   public static final int GREEN = -16711936;
   public static final int WHITE = -1;
   public static final int TRANSPARENT = 0;

   public static int rgba(float value, float value2, float value3, float value4) {
      return pack(toChannel(value), toChannel(value2), toChannel(value3), toChannel(value4));
   }

   private static void writeHexByte(char[] charValues, int value, int value2) {
      charValues[value] = HEX[value2 >>> 4 & 15];
      charValues[value + 1] = HEX[value2 & 15];
   }

   public static int withAlpha(int alpha, float value) {
      int alpha2 = -1;
      alpha2 = (byte)-1;
      alpha2 = (byte)1;
      alpha2 = toChannel(value);
      return withAlpha(alpha, alpha2);
   }

   public static int applyAlpha(int alpha, float alpha2) {
      return withAlpha(alpha, alpha2);
   }

   private static int toChannel(float value) {
      return clamp255(Math.round(clamp01(value) * 255.0F));
   }

   public static float[] hsv(int value) {
      float computedValue = red(value) / 255.0F;
      float computedValue2 = green(value) / 255.0F;
      float computedValue3 = blue(value) / 255.0F;
      float computedValue4 = Math.max(computedValue, Math.max(computedValue2, computedValue3));
      float computedValue5 = Math.min(computedValue, Math.min(computedValue2, computedValue3));
      float computedValue6 = computedValue4 - computedValue5;
      float value2;
      if (computedValue6 == 0.0F) {
         value2 = 0.0F;
      } else if (computedValue4 == computedValue) {
         value2 = (computedValue2 - computedValue3) / computedValue6 % 6.0F;
      } else if (computedValue4 == computedValue2) {
         value2 = (computedValue3 - computedValue) / computedValue6 + 2.0F;
      } else {
         value2 = (computedValue - computedValue2) / computedValue6 + 4.0F;
      }

      value2 /= 6.0F;
      if (value2 < 0.0F) {
         value2++;
      }

      float computedValue7 = computedValue4 == 0.0F ? 0.0F : computedValue6 / computedValue4;
      return new float[]{value2, computedValue7, computedValue4};
   }

   private static Integer named(String text) {
      return switch (text) {
         case "RED" -> -65536;
         case "GREEN", "LIME" -> -16711936;
         case "BLUE" -> -16776961;
         case "WHITE" -> -1;
         case "BLACK" -> -16777216;
         case "YELLOW" -> rgb(255, 255, 0);
         case "CYAN", "AQUA" -> rgb(0, 255, 255);
         case "MAGENTA", "FUCHSIA", "PINK" -> rgb(255, 0, 255);
         case "PURPLE" -> rgb(128, 0, 128);
         case "ORANGE" -> rgb(255, 153, 101);
         case "GRAY", "GREY" -> rgb(128, 128, 128);
         case "TRANSPARENT" -> 0;
         default -> null;
      };
   }

   public static String toHex(int alpha) {
      char[] charValues = new char[9];
      charValues[0] = '#';
      writeHexByte(charValues, 1, red(alpha));
      writeHexByte(charValues, 3, green(alpha));
      writeHexByte(charValues, 5, blue(alpha));
      int intermediate = alpha(alpha);
      writeHexByte(charValues, 7, intermediate);
      return new String(charValues);
   }

   public static int rgb(int value, int value2, int value3) {
      return pack(value, value2, value3, 255);
   }

   public static Integer parse(String text) {
      if (text == null) {
         return null;
      }

      String text2 = normalize(text);
      if (text2.isEmpty()) {
         return null;
      }

      Integer value = named(text2);
      if (value != null) {
         return value;
      }

      try {
         return switch (text2.length()) {
            case 3 -> pack(hexDigit(text2.charAt(0)) * 17, hexDigit(text2.charAt(1)) * 17, hexDigit(text2.charAt(2)) * 17, 255);
            case 4 -> pack(hexDigit(text2.charAt(0)) * 17, hexDigit(text2.charAt(1)) * 17, hexDigit(text2.charAt(2)) * 17, hexDigit(text2.charAt(3)) * 17);
            default -> null;
            case 6, 8 -> fromHex(text2);
         };
      } catch (IllegalArgumentException failure) {
         return null;
      }
   }

   public static int blue(int value) {
      return value & 0xFF;
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }

      return Math.min(value, 1.0F);
   }

   public static int pack(Color color) {
      int intermediate = -1;
      int value = 0;
      intermediate = (byte)1;
      value = (byte)0;
      Objects.requireNonNull(color, "color");
      value = color.getRed();
      intermediate = color.getGreen();
      int blue = color.getBlue();
      int alpha = color.getAlpha();
      return pack(value, intermediate, blue, alpha);
   }

   private static long parseHexDigits(String text, int index, int index2) {
      long timestamp = 0L;

      for (int index3 = index; index3 < index2; index3++) {
         long timestamp2 = timestamp << 4;
         char character = text.charAt(index3);
         timestamp = timestamp2 | hexDigit(character);
      }

      return timestamp;
   }

   public static int rgba(int value, int value2, int value3, int value4) {
      return pack(value, value2, value3, value4);
   }

   public static int lerp(int alpha, int alpha2, float value) {
      int value2 = 1;
      int value3 = 0;
      value2 = (byte)1;
      value3 = (byte)0;
      int value4 = 1;
      float alpha3 = clamp01(value);
      float value5 = red(alpha);
      value2 = red(alpha2);
      value3 = Math.round(value5 + (value2 - red(alpha)) * alpha3);
      float value6 = green(alpha);
      float computedValue = value6 + (green(alpha2) - green(alpha)) * alpha3;
      int value7 = 1;
      value2 = Math.round(computedValue);
      float computedValue2 = blue(alpha) + (blue(alpha2) - blue(alpha)) * alpha3;
      value4 = Math.round(computedValue2);
      float computedValue3 = alpha(alpha) + (alpha(alpha2) - alpha(alpha)) * alpha3;
      value7 = Math.round(computedValue3);
      return pack(value3, value2, value4, value7);
   }

   public static int multiplyAlpha(int alpha, float value) {
      int alpha2 = -1;
      alpha2 = (byte)-1;
      float computedValue = alpha(alpha) * clamp01(value);
      alpha2 = Math.round(computedValue);
      return withAlpha(alpha, alpha2);
   }

   public static boolean isColorChar(char character) {
      return character == '#' || Character.digit(character, 16) >= 0;
   }

   private static int clamp255(int value) {
      if (value < 0) {
         return 0;
      }

      return Math.min(value, 255);
   }

   public static int multiplyRgb(int alpha, float value) {
      int alpha2 = -1;
      int value2 = 0;
      alpha2 = (byte)-1;
      value2 = (byte)0;
      float value3 = value < 0.0F ? 0.0F : value;
      value2 = Math.round(red(alpha) * value3);
      float computedValue = green(alpha) * value3;
      alpha2 = Math.round(computedValue);
      float intermediate = blue(alpha) * value3;
      int roundedValue = Math.round(intermediate);
      return pack(value2, alpha2, roundedValue, alpha(alpha));
   }

   public static int fromHsv(float value, float value2, float value3, int value4) {
      float roundedDown = value - (float)Math.floor(value);
      float value5 = clamp01(value2);
      float value6 = clamp01(value3);
      if (value5 == 0.0F) {
         int value7 = toChannel(value6);
         return pack(value7, value7, value7, value4);
      }

      float computedValue = roundedDown * 6.0F;
      int roundedDown2 = (int)Math.floor(computedValue);
      float computedValue2 = computedValue - roundedDown2;
      float computedValue3 = value6 * (1.0F - value5);
      float computedValue4 = value6 * (1.0F - value5 * computedValue2);
      float computedValue5 = value6 * (1.0F - value5 * (1.0F - computedValue2));

      return switch (roundedDown2 % 6) {
         case 0 -> pack(toChannel(value6), toChannel(computedValue5), toChannel(computedValue3), value4);
         case 1 -> pack(toChannel(computedValue4), toChannel(value6), toChannel(computedValue3), value4);
         case 2 -> pack(toChannel(computedValue3), toChannel(value6), toChannel(computedValue5), value4);
         case 3 -> pack(toChannel(computedValue3), toChannel(computedValue4), toChannel(value6), value4);
         case 4 -> pack(toChannel(computedValue5), toChannel(computedValue3), toChannel(value6), value4);
         default -> pack(toChannel(value6), toChannel(computedValue3), toChannel(computedValue4), value4);
      };
   }

   private static String normalize(String text) {
      String text2 = text.trim();
      if (text2.startsWith("#")) {
         text2 = text2.substring(1);
      }

      return text2.toUpperCase(Locale.ROOT);
   }

   public static int red(int value) {
      return value >> 16 & 0xFF;
   }

   private WriteHexByteHelper() {
   }

   private static int hexDigit(char character) {
      int result = Character.digit(character, 16);
      if (result < 0) {
         String text = "Invalid hex digit: " + character;
         throw new IllegalArgumentException(text);
      } else {
         return result;
      }
   }

   public static int fade(int value, int value2, int value3, int value4) {
      long now = System.currentTimeMillis();
      int computedValue = (int)((now / Math.max(1, value) + value2) % 360L);
      computedValue = computedValue >= 180 ? 360 - computedValue : computedValue;
      return lerp(value3, value4, computedValue / 180.0F);
   }

   public static int pack(int value, int value2, int value3, int value4) {
      return clamp255(value4) << 24 | clamp255(value) << 16 | clamp255(value2) << 8 | clamp255(value3);
   }

   public static int alpha(int value) {
      return value >>> 24 & 0xFF;
   }

   public static int fromHex(String text) {
      if (text == null) {
         throw new IllegalArgumentException("Hex string cannot be null");
      } else {
         String text2 = normalize(text);
         int length = text2.length();
         if (length != 6 && length != 8) {
            throw new IllegalArgumentException("Expected 6 or 8 hex digits, got " + length);
         } else {
            long timestamp = parseHexDigits(text2, 0, length);
            return length == 6
               ? pack((int)(timestamp >> 16) & 0xFF, (int)(timestamp >> 8) & 0xFF, (int)timestamp & 0xFF, 255)
               : pack((int)(timestamp >> 24) & 0xFF, (int)(timestamp >> 16) & 0xFF, (int)(timestamp >> 8) & 0xFF, (int)timestamp & 0xFF);
         }
      }
   }

   public static int hue(float value) {
      return fromHsv(value, 1.0F, 1.0F, 255);
   }

   public static int withAlpha(int value, int value2) {
      return clamp255(value2) << 24 | value & 16777215;
   }

   public static int green(int value) {
      return value >> 8 & 0xFF;
   }
}
