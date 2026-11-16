package deluxe.client.util;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;

public final class NicknameGenerator {
   private static final NicknameGenerator.Nested0445[] nested0445Array = new NicknameGenerator.Nested0445[]{
      new NicknameGenerator.Nested0445(
         new String[]{
            "b",
            "d",
            "f",
            "g",
            "h",
            "k",
            "l",
            "m",
            "n",
            "p",
            "r",
            "s",
            "t",
            "v",
            "w",
            "br",
            "cl",
            "cr",
            "dr",
            "fl",
            "gr",
            "pl",
            "sl",
            "sn",
            "st",
            "tr",
            "th",
            "sh",
            "ch",
            "wh"
         },
         new String[]{"a", "e", "i", "o", "u", "ea", "ee", "oo", "ai", "ay", "ow"},
         new String[]{"", "", "n", "r", "l", "s", "t", "ck", "ng", "st", "nd", "rk", "sh", "ll", "x"}
      ),
      new NicknameGenerator.Nested0445(
         new String[]{"b", "d", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "z", "zh", "sh", "ch", "kr", "gr", "dr", "st", "sk", "sv", "tr", "pr", "br"},
         new String[]{"a", "e", "i", "o", "u", "ya", "yo", "yu", "ei", "ai"},
         new String[]{"", "", "", "n", "r", "l", "k", "s", "v", "m", "sh", "ch", "x", "y"}
      ),
      new NicknameGenerator.Nested0445(
         new String[]{"k", "z", "x", "v", "r", "n", "q", "j", "kr", "zr", "vr", "sk", "dr", "th", "ph", "y"},
         new String[]{"a", "e", "i", "o", "y", "ae", "io"},
         new String[]{"", "x", "z", "k", "n", "r", "th", "rn", "ks", "q", "sh"}
      ),
      new NicknameGenerator.Nested0445(
         new String[]{"l", "m", "n", "r", "s", "t", "v", "d", "k", "b", "p", "f", "c", "g"},
         new String[]{"a", "e", "i", "o", "u", "ia", "io", "ea", "ie"},
         new String[]{"", "", "", "", "n", "l", "r", "s", "m"}
      )
   };
   private final SecureRandom field08964 = new SecureRandom();
   private final HashSet<String> field08965 = new HashSet<>();

   private String method2615(String[] stringArray) {
      return stringArray[this.field08964.nextInt(stringArray.length)];
   }

   private int method2616(int... intArray) {
      int local4077_2 = 0;

      for (int local4077_6 : intArray) {
         local4077_2 += local4077_6;
      }

      int local4077_3 = this.field08964.nextInt(local4077_2);

      for (int index = 0; index < intArray.length; index++) {
         local4077_3 -= intArray[index];
         if (local4077_3 < 0) {
            return index;
         }
      }

      return intArray.length - 1;
   }

   private String method2617() {
      StringBuilder stringBuilder = new StringBuilder();
      int local4078_2 = 3 + this.field08964.nextInt(3);

      for (int index = 0; index < local4078_2; index++) {
         stringBuilder.append(this.method2628(index % 2 == 1 ? "aeiouy" : "bdfgklmnprstvxz"));
      }

      return stringBuilder + (this.method2626(50) ? String.valueOf(this.field08964.nextInt(100)) : "");
   }

   private String method2618(String local4079_1) {
      String local4079_2 = local4079_1;
      if (this.method2626(22) && Character.isLetter(local4079_2.charAt(local4079_2.length() - 1))) {
         local4079_2 = local4079_2 + this.method2622() + this.method2624();
      }

      if (this.method2626(7)) {
         local4079_2 = this.method2621(local4079_2);
      }

      if (this.method2626(6)) {
         local4079_2 = this.method2619(local4079_2);
      }
      local4079_2 = switch (this.method2616(50, 30, 17, local4079_2.length() <= 8 ? 3 : 0)) {
         case 0 -> local4079_2.toLowerCase(Locale.ROOT);
         case 1 -> Character.toUpperCase(local4079_2.charAt(0)) + local4079_2.substring(1).toLowerCase(Locale.ROOT);
         case 2 -> this.method2625(local4079_2);
         default -> local4079_2.toUpperCase(Locale.ROOT);
      };
      if (this.method2626(3)) {
         local4079_2 = "x" + local4079_2 + "x";
      }

      local4079_2 = local4079_2.replaceAll("[^A-Za-z0-9_]", "").replaceAll("_+", "_").replaceAll("^_|_$", "");
      return local4079_2.length() > 16 ? local4079_2.substring(0, 16).replaceAll("_$", "") : local4079_2;
   }

   private String method2619(String local4080_1) {
      int local4080_2 = this.field08964.nextInt(local4080_1.length());
      return Character.isLetter(local4080_1.charAt(local4080_2))
         ? local4080_1.substring(0, local4080_2 + 1) + local4080_1.charAt(local4080_2) + local4080_1.substring(local4080_2 + 1)
         : local4080_1;
   }

   private String method2620() {
      NicknameGenerator.Nested0445 local4081_1 = nested0445Array[this.field08964.nextInt(nested0445Array.length)];

      return switch (this.method2616(30, 20, 18, 12, 10, 10)) {
         case 0 -> this.method2623(local4081_1, 2 + this.field08964.nextInt(2));
         case 1 -> this.method2623(local4081_1, 1 + this.field08964.nextInt(2)) + this.method2622() + this.method2624();
         case 2 -> this.method2623(local4081_1, 1 + this.field08964.nextInt(2))
            + this.method2622()
            + this.method2623(nested0445Array[this.field08964.nextInt(nested0445Array.length)], 1 + this.field08964.nextInt(2));
         case 3 -> this.method2617();
         case 4 -> this.method2623(local4081_1, 1) + this.method2623(local4081_1, 1) + this.field08964.nextInt(10);
         default -> this.method2623(local4081_1, 2) + this.method2628("xyzkqo");
      };
   }

   private String method2621(String local4082_1) {
      StringBuilder stringBuilder = new StringBuilder();

      for (char local4082_6 : local4082_1.toCharArray()) {
         char local4082_7 = switch (Character.toLowerCase(local4082_6)) {
            case 'a' -> '4';
            case 'e' -> '3';
            case 'i' -> '1';
            case 'o' -> '0';
            case 's' -> '5';
            default -> local4082_6;
         };
         stringBuilder.append(this.method2626(45) ? local4082_7 : local4082_6);
      }

      return stringBuilder.toString();
   }

   private String method2622() {
      return this.method2626(25) ? "_" : "";
   }

   private String method2623(NicknameGenerator.Nested0445 local4084_1, int local4084_2) {
      StringBuilder stringBuilder;
      do {
         stringBuilder = new StringBuilder();
         if (this.method2626(8)) {
            stringBuilder.append(this.method2628("aeiou"));
         }

         for (int index = 0; index < local4084_2; index++) {
            stringBuilder.append(this.method2615(local4084_1.onsets()))
               .append(this.method2626(75) ? String.valueOf(this.method2628("aeiou")) : this.method2615(local4084_1.vowels()));
            if (index == local4084_2 - 1 || this.method2626(15)) {
               stringBuilder.append(this.method2615(local4084_1.codas()));
            }
         }
      } while (stringBuilder.length() < 3 || stringBuilder.length() > 3 + local4084_2 * 2);

      return stringBuilder.toString();
   }

   private String method2624() {
      return switch (this.method2616(28, 26, 18, 16, 12)) {
         case 0 -> String.valueOf(2003 + this.field08964.nextInt(13));
         case 1 -> String.valueOf(this.field08964.nextInt(100));
         case 2 -> String.format(Locale.ROOT, "%02d", this.field08964.nextInt(32));
         case 3 -> String.valueOf(100 + this.field08964.nextInt(900));
         default -> String.valueOf(this.field08964.nextInt(10)).repeat(2 + this.field08964.nextInt(2));
      };
   }

   private String method2625(String local4088_1) {
      StringBuilder stringBuilder = new StringBuilder();
      boolean local4088_3 = true;

      for (char local4088_7 : local4088_1.toCharArray()) {
         if (!Character.isLetter(local4088_7)) {
            local4088_3 = local4088_7 == '_';
            stringBuilder.append(local4088_7);
         } else {
            stringBuilder.append(local4088_3 ? Character.toUpperCase(local4088_7) : Character.toLowerCase(local4088_7));
            local4088_3 = false;
         }
      }

      return stringBuilder.toString();
   }

   private boolean method2626(int local4089_1) {
      return this.field08964.nextInt(100) < local4089_1;
   }

   public String method2627() {
      String local4090_1;
      do {
         local4090_1 = this.method2618(this.method2620());
      } while (local4090_1.length() < 3 || local4090_1.length() > 16 || !this.field08965.add(local4090_1.toLowerCase(Locale.ROOT)));

      return local4090_1;
   }

   private char method2628(String local4091_1) {
      return local4091_1.charAt(this.field08964.nextInt(local4091_1.length()));
   }

   private record Nested0445(String[] onsets, String[] vowels, String[] codas) {
   }
}
