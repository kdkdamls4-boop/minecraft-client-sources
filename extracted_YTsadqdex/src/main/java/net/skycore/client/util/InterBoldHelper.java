package net.skycore.client.util;

public final class InterBoldHelper {
   public static TextHeightHelper interBold() {
      return InterBoldHelper.Holder.INTER_BOLD;
   }

   public static TextHeightHelper skycoreIcons() {
      return skycore();
   }

   public static TextHeightHelper sfSemibold() {
      return InterBoldHelper.Holder.INTER_SEMIBOLD;
   }

   public static TextHeightHelper suisseMedium() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   public static TextHeightHelper sfMedium() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   public static TextHeightHelper suisseRegular() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   public static TextHeightHelper watermarkLogo() {
      return skycore();
   }

   private InterBoldHelper() {
   }

   public static TextHeightHelper sfRegular() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   public static TextHeightHelper csGuiIcons() {
      return InterBoldHelper.Holder.CS_GUI_ICONS;
   }

   public static TextHeightHelper forWeight(int value) {
      int computedValue = Math.max(1, Math.min(1000, value));
      if (computedValue < 600) {
         return InterBoldHelper.Holder.INTER_MEDIUM;
      }

      if (computedValue < 700) {
         return InterBoldHelper.Holder.INTER_SEMIBOLD;
      }

      if (computedValue < 800) {
         return InterBoldHelper.Holder.INTER_BOLD;
      }

      return InterBoldHelper.Holder.INTER_EXTRABOLD;
   }

   public static TextHeightHelper sfRoundedSemibold() {
      return InterBoldHelper.Holder.INTER_SEMIBOLD;
   }

   public static TextHeightHelper interSemibold() {
      return InterBoldHelper.Holder.INTER_SEMIBOLD;
   }

   public static TextHeightHelper skycore() {
      return InterBoldHelper.Holder.SKYCORE;
   }

   public static TextHeightHelper interMedium() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   public static TextHeightHelper interExtraBold() {
      return InterBoldHelper.Holder.INTER_EXTRABOLD;
   }

   public static TextHeightHelper clickGuiIcons() {
      return InterBoldHelper.Holder.CLICK_GUI_ICONS;
   }

   public static TextHeightHelper sfRoundedMedium() {
      return InterBoldHelper.Holder.INTER_MEDIUM;
   }

   static final class Holder {
      private static final TextHeightHelper CS_GUI_ICONS = TextHeightHelper.builder().name("CSGui Icons").atlas("csgui-icons").data("csgui-icons").build();
      private static final TextHeightHelper SKYCORE = TextHeightHelper.builder().name("Skycore").atlas("skycore").data("skycore").build();
      private static final TextHeightHelper CLICK_GUI_ICONS = TextHeightHelper.builder()
         .name("ClickGUI Icons")
         .atlas("clickgui-icons")
         .data("clickgui-icons")
         .build();
      private static final TextHeightHelper INTER_BOLD = TextHeightHelper.builder().name("Inter Bold").atlas("inter-bold").data("inter-bold").build();
      private static final TextHeightHelper INTER_SEMIBOLD = TextHeightHelper.builder().name("Inter Semibold").atlas("inter-semi").data("inter-semi").build();
      private static final TextHeightHelper INTER_EXTRABOLD = TextHeightHelper.builder()
         .name("Inter ExtraBold")
         .atlas("inter-extrabold")
         .data("inter-extrabold")
         .build();
      private static final TextHeightHelper INTER_MEDIUM = TextHeightHelper.builder().name("Inter Medium").atlas("inter-medium").data("inter-medium").build();

      private Holder() {
      }
   }
}
