package net.skycore.client.util;

import net.skycore.client.module.ModuleCategory;

public final class LegacyIconGlyphs {
   public static final String MOVEMENT = "d";
   public static final String SEARCH = "b";
   public static final String CONFIGS = "i";
   public static final String TEXT = "l";
   public static final String COMMAND = "k";
   public static final String LOGO = "a";
   public static final String TRASH = "m";
   public static final String MISC = "g";
   public static final String PLAYER = "e";
   public static final String CHEVRON = "j";
   public static final String THEMES = "h";
   public static final String SKYCORE_LOGO = "a";
   public static final String FILTERS = "o";
   public static final String COMBAT = "c";
   public static final String MONITOR = "f";
   public static final String CHECK = "n";
   public static final String RENDER = "f";

   private LegacyIconGlyphs() {
   }

   public static String category(ModuleCategory moduleCategory) {
      return switch (moduleCategory) {
         case COMBAT -> "c";
         case MOVEMENT -> "d";
         case PLAYER -> "e";
         case RENDER -> "f";
         case MISC -> "g";
         default -> {
            throw new MatchException(null, null);
         }
      };
   }
}
