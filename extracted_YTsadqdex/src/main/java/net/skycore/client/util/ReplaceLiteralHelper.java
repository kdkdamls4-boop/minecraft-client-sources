package net.skycore.client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

public final class ReplaceLiteralHelper {
   private static final Map<Integer, Integer> RANK_COLORS = new HashMap<>();
   private static final Map<Integer, String> MSDF_SYMBOL_REPLACEMENTS = new HashMap<>();

   public static Component replaceLiteral(Component text, String text2, String text3) {
      if (text == null) {
         return null;
      }

      String string = text.getString();
      if (!string.toLowerCase().contains(text2.toLowerCase())) {
         return text;
      }

      string = string.replaceAll("(?i)" + Pattern.quote(text2), text3);
      MutableComponent mutableComponent = Component.empty();
      var styledChars = flatten(text);
      int value = 0;

      for (int value2 = 0; value2 < string.length(); value2++) {
         Style style = value < styledChars.size() ? ((ReplaceLiteralHelper.StyledChar)styledChars.get(value)).style() : Style.EMPTY;
         mutableComponent.append(Component.literal(String.valueOf(string.charAt(value2))).setStyle(style));
         value++;
      }

      return mutableComponent;
   }

   private static void collect(Component text, List<ReplaceLiteralHelper.StyledChar> styledChars) {
      Style style = text.getStyle();
      if (text.getContents() instanceof PlainTextContents plainTextContents) {
         String text2 = plainTextContents.text();

         for (int value = 0; value < text2.length(); value++) {
            styledChars.add(new ReplaceLiteralHelper.StyledChar(text2.charAt(value), style));
         }
      }

      for (Component text3 : text.getSiblings()) {
         collect(text3, styledChars);
      }
   }

   private static void appendSanitizedLiteral(MutableComponent mutableComponent, String text, Style style) {
      Style style2 = style;
      StringBuilder stringBuilder = new StringBuilder();
      int value = 0;

      while (value < text.length()) {
         int count = text.codePointAt(value);
         int value2 = Character.charCount(count);
         if (count == 167 && value + value2 < text.length()) {
            if (!stringBuilder.isEmpty()) {
               mutableComponent.append(Component.literal(stringBuilder.toString()).setStyle(style2));
               stringBuilder.setLength(0);
            }

            char character = text.charAt(value + value2);
            ChatFormatting byCode = ChatFormatting.getByCode(character);
            if (byCode != null) {
               style2 = byCode == ChatFormatting.RESET ? style : style2.applyFormat(byCode);
            }

            value += value2 + 1;
         } else if (!isDisplayChar(count)) {
            value += value2;
         } else {
            stringBuilder.appendCodePoint(count);
            value += value2;
         }
      }

      if (!stringBuilder.isEmpty()) {
         mutableComponent.append(Component.literal(stringBuilder.toString()).setStyle(style2));
      }
   }

   public static boolean isRankCodePoint(int value) {
      if (value >= 42240 && value <= 42559) {
         return true;
      }

      String text = replaceCodePointForMsdf(value);
      return text != null && text.length() > 1;
   }

   public static boolean isVoiceIndicator(int value) {
      return value == 9679
         || value == 9675
         || value == 9673
         || value == 9672
         || value == 8226
         || value == 183
         || value == 8729
         || value == 11044
         || value == 9899
         || value == 9898
         || value == 128994
         || value == 128308
         || value == 128992
         || value == 128993;
   }

   private static void appendReplaced(MutableComponent mutableComponent, Component text, String text2, String text3) {
      ComponentContents contents = text.getContents();
      Style style = text.getStyle();
      if (contents instanceof PlainTextContents plainTextContents) {
         Pattern pattern = Pattern.compile(Pattern.quote(text2), 2);
         Matcher matcher = pattern.matcher(plainTextContents.text());
         String text4 = matcher.replaceAll(text3);
         MutableComponent mutableComponent2 = Component.literal(text4);
         mutableComponent.append(mutableComponent2.setStyle(style));
      }

      Iterator iterator = text.getSiblings().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return;
         }

         Component element = (Component)iterator.next();
         appendReplaced(mutableComponent, element, text2, text3);
      }
   }

   public static Component truncateAfterPlayerName(Component text, String text2) {
      if (text != null && text2 != null && !text2.isEmpty()) {
         MutableComponent mutableComponent = Component.empty();
         ArrayList<ReplaceLiteralHelper.OrderedChar> orderedChars = new ArrayList();
         text.getVisualOrderText().accept((value, style, value2) -> {
            orderedChars.add(new ReplaceLiteralHelper.OrderedChar(style, value2));
            return true;
         });
         int value3 = 0;
         byte flag = 0;
         byte flag2 = 0;

         for (ReplaceLiteralHelper.OrderedChar orderedChar : orderedChars) {
            int codePoint = orderedChar.codePoint;
            Style style2 = orderedChar.style;
            if (isDisplayChar(codePoint)) {
               String text3 = replaceCodePointForMsdf(codePoint);
               if (text3 != null) {
                  if (!text3.isEmpty()) {
                     appendDisplayCodePoint(mutableComponent, codePoint, style2);
                     value3 = 0;
                     flag2 = 0;
                  }
               } else {
                  String text4 = new String(Character.toChars(codePoint));
                  char character = text4.charAt(0);
                  if (flag == 0) {
                     mutableComponent.append(Component.literal(text4).setStyle(style2));
                     if (character == text2.charAt(value3)) {
                        if (++value3 == text2.length()) {
                           flag = 1;
                           flag2 = 1;
                        }
                     } else {
                        value3 = character == text2.charAt(0) ? 1 : 0;
                     }
                  } else {
                     if (flag2 == 0 || !Character.isDigit(character)) {
                        break;
                     }

                     mutableComponent.append(Component.literal(text4).setStyle(style2));
                  }
               }
            }
         }

         return mutableComponent.getString().isEmpty() ? text : mutableComponent;
      } else {
         return text == null ? Component.empty() : text;
      }
   }

   private static void appendDisplayCodePoint(MutableComponent mutableComponent, int value, Style style) {
      char[] charValues = Character.toChars(value);
      MutableComponent mutableComponent2 = Component.literal(new String(charValues));
      MutableComponent mutableComponent3 = mutableComponent2.setStyle(style);
      mutableComponent.append(mutableComponent3);
   }

   public static Component sanitizeDisplayText(Component text) {
      if (text == null) {
         return Component.empty();
      }

      MutableComponent mutableComponent = Component.empty();
      sanitizeDisplayText(mutableComponent, text);
      return mutableComponent;
   }

   public static boolean isDisplayChar(int value) {
      if (shouldStripDisplayCodePoint(value)) {
         return false;
      } else if (replaceCodePointForMsdf(value) != null) {
         return true;
      } else if (value == 32 || value == 95 || value == 45 || value == 46 || value == 124 || value == 64) {
         return true;
      } else if (value >= 48 && value <= 57) {
         return true;
      } else if (value >= 65 && value <= 90) {
         return true;
      } else {
         return value >= 97 && value <= 122 ? true : value >= 1024 && value <= 1279;
      }
   }

   public static String replaceSymbols(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder stringBuilder = new StringBuilder(text.length() + 8);
         int value = 0;

         while (value < text.length()) {
            int count = text.codePointAt(value);
            int value2 = Character.charCount(count);
            if (count == 42327 && value + value2 < text.length() && text.charAt(value + value2) == 'ꕘ') {
               stringBuilder.append("SPONSOR");
               value += value2 + 1;
            } else {
               String text2 = replaceCodePointForMsdf(count);
               if (text2 != null) {
                  stringBuilder.append(text2);
               } else {
                  stringBuilder.appendCodePoint(count);
               }

               value += value2;
            }
         }

         return stringBuilder.toString();
      } else {
         return text;
      }
   }

   public static String replaceCodePoint(int value) {
      return replaceCodePointForMsdf(value);
   }

   public static Component replace(Component text, String text2, String text3) {
      if (text != null && text2 != null && text3 != null) {
         MutableComponent mutableComponent = Component.empty();
         Style style = text.getStyle();
         MutableComponent mutableComponent2 = mutableComponent.setStyle(style);
         appendReplaced(mutableComponent2, text, text2, text3);
         return mutableComponent2;
      } else {
         return text;
      }
   }

   private static int interpolate(int value, int value2, float value3) {
      int intermediate = -1;
      int value4 = 0;
      int value5 = value >> 16 & 0xFF;
      int value6 = value >> 8 & 0xFF;
      int value7 = value & 0xFF;
      int value8 = value2 >> 16 & 0xFF;
      int value9 = value2 >> 8 & 0xFF;
      int value10 = value2 & 0xFF;
      value4 = Math.round(value5 + (value8 - value5) * value3);
      float computedValue = value6 + (value9 - value6) * value3;
      intermediate = Math.round(computedValue);
      float intermediate2 = value7 + (value10 - value7) * value3;
      return rgb(value4, intermediate, Math.round(intermediate2));
   }

   public static int getGradientColorForReplacement(int value, int value2, int value3, float value4, int value5) {
      int value6 = 1;
      int value7 = 0;
      if (value3 <= 1) {
         return applyOpacity(value5, value4);
      }

      Integer value8 = RANK_COLORS.get(value);
      if (value8 == null) {
         return applyOpacity(value5, value4);
      }

      value7 = value8;
      int value9 = darken(value7, 0.8F);
      float computedValue = (float)value2 / (value3 - 1);
      value6 = value8;
      int value10 = interpolate(value9, value6, computedValue);
      return applyOpacity(value10, value4);
   }

   public static Component replaceSymbols(Component text) {
      if (text.getString().contains("◆")) {
         text = replace(text, "◆", "@");
      }

      if (text.getString().contains("┃")) {
         text = replace(text, "┃", "|");
      }

      if (text.getString().contains("\ud83d\udd25")) {
         text = replace(text, "\ud83d\udd25", "@");
      }

      if (text.getString().contains("ᴀ")) {
         text = replace(text, "ᴀ", "A");
      }

      if (text.getString().contains("ʙ")) {
         text = replace(text, "ʙ", "B");
      }

      if (text.getString().contains("ᴄ")) {
         text = replace(text, "ᴄ", "C");
      }

      if (text.getString().contains("ᴅ")) {
         text = replace(text, "ᴅ", "D");
      }

      if (text.getString().contains("ᴇ")) {
         text = replace(text, "ᴇ", "E");
      }

      if (text.getString().contains("ғ")) {
         text = replace(text, "ғ", "F");
      }

      if (text.getString().contains("ɢ")) {
         text = replace(text, "ɢ", "G");
      }

      if (text.getString().contains("ʜ")) {
         text = replace(text, "ʜ", "H");
      }

      if (text.getString().contains("ɪ")) {
         text = replace(text, "ɪ", "I");
      }

      if (text.getString().contains("ᴊ")) {
         text = replace(text, "ᴊ", "J");
      }

      if (text.getString().contains("ᴋ")) {
         text = replace(text, "ᴋ", "K");
      }

      if (text.getString().contains("ʟ")) {
         text = replace(text, "ʟ", "L");
      }

      if (text.getString().contains("ᴍ")) {
         text = replace(text, "ᴍ", "M");
      }

      if (text.getString().contains("ɴ")) {
         text = replace(text, "ɴ", "N");
      }

      if (text.getString().contains("ꜱ")) {
         text = replace(text, "ꜱ", "S");
      }

      if (text.getString().contains("ᴏ")) {
         text = replace(text, "ᴏ", "O");
      }

      if (text.getString().contains("ᴘ")) {
         text = replace(text, "ᴘ", "P");
      }

      if (text.getString().contains("ǫ")) {
         text = replace(text, "ǫ", "Q");
      }

      if (text.getString().contains("ʀ")) {
         text = replace(text, "ʀ", "R");
      }

      if (text.getString().contains("ᴛ")) {
         text = replace(text, "ᴛ", "T");
      }

      if (text.getString().contains("ᴜ")) {
         text = replace(text, "ᴜ", "U");
      }

      if (text.getString().contains("ᴠ")) {
         text = replace(text, "ᴠ", "V");
      }

      if (text.getString().contains("ᴡ")) {
         text = replace(text, "ᴡ", "W");
      }

      if (text.getString().contains("ꜰ")) {
         text = replace(text, "ꜰ", "F");
      }

      if (text.getString().contains("ʏ")) {
         text = replace(text, "ʏ", "Y");
      }

      if (text.getString().contains("ᴢ")) {
         text = replace(text, "ᴢ", "Z");
      }

      return text;
   }

   public static String toQwerty(String text) {
      String text2 = text.replace("й", "q").replace("ц", "w");
      text2 = text2.replace("у", "e");
      text2 = text2.replace("к", "r");
      text2 = text2.replace("е", "t").replace("н", "y");
      text2 = text2.replace("г", "u");
      text2 = text2.replace("ш", "i").replace("щ", "o").replace("з", "p").replace("х", "[").replace("ъ", "]").replace("ф", "a");
      text2 = text2.replace("ы", "s");
      text2 = text2.replace("в", "d");
      text2 = text2.replace("а", "f").replace("п", "g").replace("р", "h");
      text2 = text2.replace("о", "j").replace("л", "k");
      text2 = text2.replace("д", "l").replace("ж", ";").replace("э", "'").replace("я", "z");
      text2 = text2.replace("ч", "x").replace("с", "c").replace("м", "v");
      text2 = text2.replace("и", "b");
      text2 = text2.replace("т", "n");
      text2 = text2.replace("ь", "m").replace("б", ",");
      text2 = text2.replace("ю", ".").replace("ё", "`").replace("Й", "Q");
      text2 = text2.replace("Ц", "W");
      text2 = text2.replace("У", "E").replace("К", "R").replace("Е", "T");
      text2 = text2.replace("Н", "Y");
      text2 = text2.replace("Г", "U").replace("Ш", "I").replace("Щ", "O").replace("З", "P").replace("Х", "{");
      text2 = text2.replace("Ъ", "}")
         .replace("Ф", "A")
         .replace("Ы", "S")
         .replace("В", "D")
         .replace("А", "F")
         .replace("П", "G")
         .replace("Р", "H")
         .replace("О", "J");
      text2 = text2.replace("Л", "K");
      text2 = text2.replace("Д", "L").replace("Ж", ":");
      text2 = text2.replace("Э", "\"").replace("Я", "Z").replace("Ч", "X").replace("С", "C").replace("М", "V").replace("И", "B");
      text2 = text2.replace("Т", "N");
      text2 = text2.replace("Ь", "M").replace("Б", "<").replace("Ю", ">");
      return text2.replace("Ё", "~");
   }

   private static int darken(int value, float value2) {
      int computedValue = (int)((value >> 16 & 0xFF) * value2);
      int computedValue2 = (int)((value >> 8 & 0xFF) * value2);
      int computedValue3 = (int)((value & 0xFF) * value2);
      return rgb(computedValue, computedValue2, computedValue3);
   }

   public static Component stripRankSymbols(Component text) {
      if (text == null) {
         return Component.empty();
      } else {
         MutableComponent mutableComponent = Component.empty();
         FormattedCharSequence visualOrderText = text.getVisualOrderText();
         FormattedCharSink formattedCharSink = (value, style, value2) -> {
            if (!isRankCodePoint(value2)) {
               appendDisplayCodePoint(mutableComponent, value2, style);
            }

            return true;
         };
         visualOrderText.accept(formattedCharSink);
         return mutableComponent;
      }
   }

   private static List<ReplaceLiteralHelper.StyledChar> flatten(Component text) {
      ArrayList elements = new ArrayList();
      collect(text, elements);
      return elements;
   }

   private static int applyOpacity(int value, float value2) {
      float computedValue = Math.min(1.0F, Math.max(0.0F, value2)) * 255.0F;
      int roundedValue = Math.round(computedValue);
      return roundedValue << 24 | value & 16777215;
   }

   private static boolean shouldStripDisplayCodePoint(int value) {
      if (isVoiceIndicator(value)) {
         return true;
      } else {
         String text = replaceCodePointForMsdf(value);
         if (text != null && text.isEmpty()) {
            return true;
         } else if (text != null) {
            return false;
         } else if (Character.isISOControl(value)) {
            return true;
         } else {
            int type = Character.getType(value);
            if (type == 16 || type == 18 || type == 19) {
               return true;
            } else if (value >= 57344 && value <= 63743) {
               return true;
            } else if (value >= 40960 && value <= 42239) {
               return true;
            } else if (value >= 8203 && value <= 8207) {
               return true;
            } else {
               return value >= 8288 && value <= 8303 ? true : value >= 65024 && value <= 65039;
            }
         }
      }
   }

   private static int rgb(int value, int value2, int value3) {
      return value << 16 | value2 << 8 | value3;
   }

   static {
      MSDF_SYMBOL_REPLACEMENTS.put(9889, "");
      MSDF_SYMBOL_REPLACEMENTS.put(9733, "");
      MSDF_SYMBOL_REPLACEMENTS.put(42240, "PLAYER");
      MSDF_SYMBOL_REPLACEMENTS.put(42244, "HERO");
      MSDF_SYMBOL_REPLACEMENTS.put(42248, "TITAN");
      MSDF_SYMBOL_REPLACEMENTS.put(42258, "AVENGER");
      MSDF_SYMBOL_REPLACEMENTS.put(42262, "OVERLORD");
      MSDF_SYMBOL_REPLACEMENTS.put(42272, "MAGISTER");
      MSDF_SYMBOL_REPLACEMENTS.put(42276, "IMPERATOR");
      MSDF_SYMBOL_REPLACEMENTS.put(42280, "DRAGON");
      MSDF_SYMBOL_REPLACEMENTS.put(42336, "D.HELPER");
      MSDF_SYMBOL_REPLACEMENTS.put(42290, "BULL");
      MSDF_SYMBOL_REPLACEMENTS.put(42294, "TIGER");
      MSDF_SYMBOL_REPLACEMENTS.put(42308, "DRACULA");
      MSDF_SYMBOL_REPLACEMENTS.put(42326, "BUNNY");
      MSDF_SYMBOL_REPLACEMENTS.put(42312, "COBRA");
      MSDF_SYMBOL_REPLACEMENTS.put(42304, "HYDRA");
      MSDF_SYMBOL_REPLACEMENTS.put(42322, "RABBIT");
      MSDF_SYMBOL_REPLACEMENTS.put(42249, "HELPER");
      MSDF_SYMBOL_REPLACEMENTS.put(42259, "ML.MODER");
      MSDF_SYMBOL_REPLACEMENTS.put(42263, "MODER");
      MSDF_SYMBOL_REPLACEMENTS.put(42273, "MODER+");
      MSDF_SYMBOL_REPLACEMENTS.put(42277, "ST.MODER");
      MSDF_SYMBOL_REPLACEMENTS.put(42281, "GL.MODER");
      MSDF_SYMBOL_REPLACEMENTS.put(42291, "ML.ADMIN");
      MSDF_SYMBOL_REPLACEMENTS.put(42295, "ADMIN");
      MSDF_SYMBOL_REPLACEMENTS.put(42241, "MEDIA");
      MSDF_SYMBOL_REPLACEMENTS.put(42245, "YT");
      MSDF_SYMBOL_REPLACEMENTS.put(42305, "GOD");
      MSDF_SYMBOL_REPLACEMENTS.put(42313, "PEGAS");
      MSDF_SYMBOL_REPLACEMENTS.put(42327, "SPONSOR");
      MSDF_SYMBOL_REPLACEMENTS.put(42328, "");
      MSDF_SYMBOL_REPLACEMENTS.put(42242, "D.MODER");
      MSDF_SYMBOL_REPLACEMENTS.put(9670, "@");
      MSDF_SYMBOL_REPLACEMENTS.put(9475, "|");
      MSDF_SYMBOL_REPLACEMENTS.put(128293, "@");
      MSDF_SYMBOL_REPLACEMENTS.put(7424, "A");
      MSDF_SYMBOL_REPLACEMENTS.put(665, "B");
      MSDF_SYMBOL_REPLACEMENTS.put(7428, "C");
      MSDF_SYMBOL_REPLACEMENTS.put(7429, "D");
      MSDF_SYMBOL_REPLACEMENTS.put(7431, "E");
      MSDF_SYMBOL_REPLACEMENTS.put(42800, "F");
      MSDF_SYMBOL_REPLACEMENTS.put(610, "G");
      MSDF_SYMBOL_REPLACEMENTS.put(668, "H");
      MSDF_SYMBOL_REPLACEMENTS.put(618, "I");
      MSDF_SYMBOL_REPLACEMENTS.put(7434, "J");
      MSDF_SYMBOL_REPLACEMENTS.put(7435, "K");
      MSDF_SYMBOL_REPLACEMENTS.put(671, "L");
      MSDF_SYMBOL_REPLACEMENTS.put(7437, "M");
      MSDF_SYMBOL_REPLACEMENTS.put(628, "N");
      MSDF_SYMBOL_REPLACEMENTS.put(7439, "O");
      MSDF_SYMBOL_REPLACEMENTS.put(7448, "P");
      MSDF_SYMBOL_REPLACEMENTS.put(491, "Q");
      MSDF_SYMBOL_REPLACEMENTS.put(640, "R");
      MSDF_SYMBOL_REPLACEMENTS.put(7451, "T");
      MSDF_SYMBOL_REPLACEMENTS.put(7452, "U");
      MSDF_SYMBOL_REPLACEMENTS.put(42801, "S");
      MSDF_SYMBOL_REPLACEMENTS.put(7456, "V");
      MSDF_SYMBOL_REPLACEMENTS.put(7457, "W");
      MSDF_SYMBOL_REPLACEMENTS.put(7521, "X");
      MSDF_SYMBOL_REPLACEMENTS.put(655, "Y");
      MSDF_SYMBOL_REPLACEMENTS.put(7458, "Z");
      RANK_COLORS.put(42240, rgb(120, 120, 120));
      RANK_COLORS.put(42244, rgb(100, 113, 251));
      RANK_COLORS.put(42248, rgb(214, 200, 42));
      RANK_COLORS.put(42258, rgb(101, 189, 56));
      RANK_COLORS.put(42262, rgb(85, 255, 255));
      RANK_COLORS.put(42272, rgb(202, 130, 60));
      RANK_COLORS.put(42276, rgb(202, 60, 60));
      RANK_COLORS.put(42280, rgb(245, 51, 238));
      RANK_COLORS.put(42336, rgb(214, 200, 42));
      RANK_COLORS.put(42290, rgb(121, 81, 202));
      RANK_COLORS.put(42294, rgb(202, 130, 60));
      RANK_COLORS.put(42308, rgb(202, 60, 60));
      RANK_COLORS.put(42326, rgb(68, 65, 66));
      RANK_COLORS.put(42312, rgb(127, 214, 86));
      RANK_COLORS.put(42304, rgb(92, 120, 7));
      RANK_COLORS.put(42322, rgb(120, 120, 120));
      RANK_COLORS.put(42249, rgb(214, 200, 42));
      RANK_COLORS.put(42259, rgb(100, 113, 251));
      RANK_COLORS.put(42263, rgb(100, 113, 251));
      RANK_COLORS.put(42273, rgb(121, 81, 202));
      RANK_COLORS.put(42277, rgb(100, 113, 251));
      RANK_COLORS.put(42281, rgb(121, 81, 202));
      RANK_COLORS.put(42291, rgb(64, 151, 214));
      RANK_COLORS.put(42295, rgb(202, 60, 60));
      RANK_COLORS.put(42241, rgb(121, 81, 202));
      RANK_COLORS.put(42245, rgb(255, 255, 255));
      RANK_COLORS.put(42305, rgb(214, 200, 42));
      RANK_COLORS.put(42313, rgb(238, 171, 32));
      RANK_COLORS.put(42327, rgb(255, 215, 0));
   }

   private ReplaceLiteralHelper() {
   }

   private static void sanitizeDisplayText(MutableComponent mutableComponent, Component text) {
      ComponentContents contents = text.getContents();
      Style style = text.getStyle();
      if (contents instanceof PlainTextContents plainTextContents) {
         String text2 = plainTextContents.text();
         appendSanitizedLiteral(mutableComponent, text2, style);
      }

      Iterator iterator = text.getSiblings().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return;
         }

         Component element = (Component)iterator.next();
         sanitizeDisplayText(mutableComponent, element);
      }
   }

   private static String replaceCodePointForMsdf(int value) {
      var mSDF_SYMBOL_REPLACEMENTS = MSDF_SYMBOL_REPLACEMENTS;
      Integer value2 = value;
      return (String)mSDF_SYMBOL_REPLACEMENTS.get(value2);
   }

   static final class OrderedChar {
      private final int codePoint;
      private final Style style;

      private OrderedChar(Style style2, int codePoint2) {
         this.style = style2;
         this.codePoint = codePoint2;
      }
   }

   public record StyledChar(char c, Style style) {
      public Style style() {
         return this.style;
      }

      public char c() {
         return this.c;
      }
   }
}
