package net.skycore.client.util;

import net.skycore.client.module.ModuleCategory;

public final class IconGlyphs {
   public static final String SWORD = "\ue010";
   public static final String NEW_CONFIG = "\ue00b";
   public static final String COMBAT = "\ue005";
   public static final String BIND = "\ue004";
   public static final String CONFIGS = "\ue006";
   public static final String TRASH = "\ue011";
   public static final String MISC = "\ue009";
   public static final String STAR = "\ue00f";
   public static final String SEARCH = "\ue00e";
   public static final String COSMETICA = "\ue007";
   public static final String AUTOBUY_RESET = "\ue003";
   public static final String MOVEMENT = "\ue00a";
   public static final String RENDER = "\ue00d";
   public static final String ACCESSORIES = "\ue001";
   public static final String FILLED_STAR = "\ue008";
   public static final String AUTOBUY = "\ue002";
   public static final String PLAYER = "\ue00c";

   public static String category(ModuleCategory moduleCategory) {
      return switch (moduleCategory) {
         case COMBAT -> "\ue005";
         case MOVEMENT -> "\ue00a";
         case RENDER -> "\ue00d";
         case PLAYER -> "\ue00c";
         case MISC -> "\ue009";
         default -> {
            throw new MatchException(null, null);
         }
      };
   }

   private IconGlyphs() {
   }
}
