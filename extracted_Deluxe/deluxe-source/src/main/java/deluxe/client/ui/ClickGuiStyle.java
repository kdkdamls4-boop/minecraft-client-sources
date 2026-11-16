package deluxe.client.ui;

import deluxe.client.render.ThemeColors;

import deluxe.client.module.ModuleCategory;

import java.awt.Color;

public final class ClickGuiStyle {
   public static int field10780 = new Color(71, 123, 255, 255).getRGB();
   public static int field10781 = new Color(9, 16, 32, 194).getRGB();
   public static int field10782 = new Color(71, 123, 255, 15).getRGB();
   public static int field10783 = new Color(255, 255, 255, 128).getRGB();
   public static int field10784 = new Color(71, 123, 255, 13).getRGB();
   public static int field10785 = new Color(255, 255, 255, 5).getRGB();
   public static int field10786 = new Color(255, 255, 255, 89).getRGB();
   public static int field10787 = new Color(255, 255, 255, 26).getRGB();
   public static int field10788 = new Color(71, 123, 255, 51).getRGB();
   public static int field10789 = new Color(255, 255, 255, 77).getRGB();
   public static int field10790 = new Color(71, 123, 255, 128).getRGB();
   public static int field10791 = new Color(255, 255, 255, 153).getRGB();
   public static int field10792 = new Color(255, 255, 255, 51).getRGB();
   public static int field10793 = new Color(71, 123, 255, 51).getRGB();
   public static int field10794 = new Color(255, 255, 255, 26).getRGB();
   public static int field10795 = new Color(255, 255, 255, 51).getRGB();
   public static int field10796 = new Color(71, 123, 255, 26).getRGB();
   public static int field10797 = new Color(255, 255, 255, 15).getRGB();
   public static int field10798 = new Color(255, 255, 255, 20).getRGB();
   public static int field10799 = new Color(71, 123, 255, 26).getRGB();
   public static int field10800 = new Color(71, 123, 255, 41).getRGB();
   public static int field10801 = new Color(255, 255, 255, 38).getRGB();
   public static int field10802 = new Color(255, 255, 255, 38).getRGB();
   public static int field10803 = new Color(255, 255, 255, 20).getRGB();
   public static int field10804 = new Color(255, 255, 255, 15).getRGB();
   public static int field10805 = new Color(255, 255, 255, 5).getRGB();
   public static int field10806 = new Color(255, 255, 255, 153).getRGB();
   public static int field10807 = new Color(255, 255, 255, 51).getRGB();
   public static int field10808 = new Color(255, 255, 255, 26).getRGB();
   public static int field10809 = new Color(255, 255, 255, 153).getRGB();
   public static int field10810 = new Color(71, 123, 255, 26).getRGB();
   public static int field10811 = new Color(255, 255, 255, 5).getRGB();
   public static int field10812 = new Color(255, 255, 255, 5).getRGB();
   public static int field10813 = new Color(255, 255, 255, 38).getRGB();
   public static int field10814 = new Color(255, 255, 255, 20).getRGB();
   public static int field10815 = new Color(255, 255, 255, 15).getRGB();
   public static int field10816 = new Color(255, 255, 255, 5).getRGB();
   public static int field10817 = new Color(0, 0, 0, 33).getRGB();
   public static int field10818 = new Color(0, 0, 0, 26).getRGB();
   public static int field10819 = new Color(188, 188, 188, 255).getRGB();
   public static int field10820 = new Color(15, 14, 17, 96).getRGB();
   public static int field10821 = new Color(15, 14, 17, 245).getRGB();
   public static int field10822 = new Color(15, 14, 17, 80).getRGB();
   public static int field10823 = new Color(255, 255, 255, 3).getRGB();
   public static int field10824 = new Color(15, 14, 17, 110).getRGB();
   public static int field10825 = new Color(16, 15, 19, 204).getRGB();
   public static int field10826 = new Color(255, 255, 255, 3).getRGB();
   public static int field10827 = new Color(255, 255, 255, 5).getRGB();
   public static int field10828 = new Color(71, 123, 255, 20).getRGB();
   public static final String field10829 = "assets/deluxe/textures/gui/clickgui/search.png";
   public static final String field10830 = "assets/deluxe/textures/gui/clickgui/chev.png";
   public static final String field10831 = "assets/deluxe/textures/gui/clickgui/chevright.png";
   public static final String field10832 = "assets/deluxe/textures/gui/clickgui/check.png";
   public static final String field10833 = "assets/deluxe/textures/gui/clickgui/picker.png";
   public static final String field10834 = "assets/deluxe/textures/gui/clickgui/bindkey.png";

   private ClickGuiStyle() {
   }

   public static String method3186(ModuleCategory moduleCategory) {
      return switch (moduleCategory) {
         case COMBAT -> "assets/deluxe/textures/gui/clickgui/cat_combat.png";
         case MOVEMENT -> "assets/deluxe/textures/gui/clickgui/cat_movement.png";
         case RENDER -> "assets/deluxe/textures/gui/clickgui/cat_visuals.png";
         case PLAYER -> "assets/deluxe/textures/gui/clickgui/cat_player.png";
         case MISC -> "assets/deluxe/textures/gui/clickgui/cat_misc.png";
      };
   }

   public static void method3187(int local5147_0) {
      int local5147_1 = 0xFF000000 | local5147_0 & 16777215;
      field10780 = local5147_1;
      field10781 = method3188(UiTheme.method2881(-16777216, local5147_1, 0.126F), 194);
      field10782 = method3188(local5147_1, 15);
      field10784 = method3188(local5147_1, 13);
      field10788 = method3188(local5147_1, 51);
      field10790 = method3188(local5147_1, 128);
      field10793 = method3188(local5147_1, 51);
      field10796 = method3188(local5147_1, 26);
      field10799 = method3188(local5147_1, 26);
      field10800 = method3188(local5147_1, 41);
      field10810 = method3188(local5147_1, 26);
      field10828 = method3188(local5147_1, 20);
      UiTheme.field09686 = local5147_1;
      UiTheme.field09688 = UiTheme.method2881(local5147_1, -1, 0.14F);
      UiTheme.field09687 = UiTheme.method2881(local5147_1, -1, 0.28F);
      UiTheme.field09713 = UiTheme.method2881(local5147_1, -1, 0.17F);
      UiTheme.field09695 = method3188(local5147_1, 20);
      ThemeColors.field13669 = local5147_1;
   }

   private static int method3188(int local5148_0, int local5148_1) {
      return local5148_1 << 24 | local5148_0 & 16777215;
   }
}
