package deluxe.client.ui;

import deluxe.client.render.ArcColorSpan;
import deluxe.client.render.FontText;
import deluxe.client.render.Outline360DrawData;

import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class UiTheme {
   public static int field09680 = new Color(255, 255, 255, 255).getRGB();
   public static int field09681 = new Color(255, 255, 255, 184).getRGB();
   public static int field09682 = new Color(255, 255, 255, 122).getRGB();
   public static int field09683 = new Color(255, 255, 255, 61).getRGB();
   public static int field09684 = new Color(255, 255, 255, 26).getRGB();
   public static int field09685 = new Color(255, 255, 255, 10).getRGB();
   public static int field09686 = new Color(71, 123, 255, 255).getRGB();
   public static int field09687 = new Color(123, 163, 255, 255).getRGB();
   public static int field09688 = new Color(97, 142, 255, 255).getRGB();
   public static int field09689 = new Color(255, 255, 255, 255).getRGB();
   public static int field09690 = new Color(15, 14, 17, 255).getRGB();
   public static int field09691 = new Color(255, 255, 255, 255).getRGB();
   public static int field09692 = new Color(19, 18, 22, 255).getRGB();
   public static int field09693 = new Color(26, 25, 31, 255).getRGB();
   public static int field09694 = new Color(21, 20, 23, 255).getRGB();
   public static int field09695 = new Color(71, 123, 255, 20).getRGB();
   public static int field09696 = new Color(255, 255, 255, 5).getRGB();
   public static int field09697 = new Color(255, 255, 255, 20).getRGB();
   public static int field09698 = new Color(255, 255, 255, 10).getRGB();
   public static int field09699 = new Color(255, 255, 255, 5).getRGB();
   public static int field09700 = new Color(255, 255, 255, 20).getRGB();
   public static final int field09701 = new Color(248, 197, 111, 255).getRGB();
   public static final int field09702 = new Color(231, 130, 137, 255).getRGB();
   public static int field09703 = new Color(255, 255, 255, 8).getRGB();
   public static int field09704 = new Color(255, 255, 255, 20).getRGB();
   public static int field09705 = new Color(255, 255, 255, 6).getRGB();
   public static int field09706 = new Color(255, 255, 255, 26).getRGB();
   public static int field09707 = new Color(255, 255, 255, 178).getRGB();
   public static int field09708 = new Color(255, 255, 255, 128).getRGB();
   public static int field09709 = new Color(255, 255, 255, 102).getRGB();
   public static int field09710 = new Color(255, 255, 255, 38).getRGB();
   public static int field09711 = new Color(255, 255, 255, 8).getRGB();
   public static int field09712 = new Color(255, 255, 255, 20).getRGB();
   public static int field09713 = new Color(102, 144, 255, 255).getRGB();
   public static int field09714 = new Color(255, 81, 81, 255).getRGB();
   public static final String field09715 = "assets/deluxe/textures/gui/mainmenu/background/menu.png";
   public static final String field09716 = "assets/deluxe/textures/gui/mainmenu/background/menu_blur.png";
   public static final String field09717 = "assets/deluxe/textures/gui/mainmenu/background/loading.png";
   public static final String field09718 = "assets/deluxe/textures/gui/mainmenu/background/welcome.png";
   public static final String field09719 = "assets/deluxe/textures/gui/mainmenu/icons/logo.png";
   public static final String field09720 = "assets/deluxe/textures/gui/mainmenu/icons/arrow.png";
   public static final String field09721 = "assets/deluxe/textures/gui/mainmenu/icons/person.png";
   public static final String field09722 = "assets/deluxe/textures/gui/mainmenu/icons/telegram.png";
   public static final String field09723 = "assets/deluxe/textures/gui/mainmenu/icons/discord.png";
   public static final String field09724 = "assets/deluxe/textures/gui/mainmenu/icons/chevron.png";
   public static final String field09725 = "assets/deluxe/textures/gui/mainmenu/icons/exit.png";
   public static final String field09726 = "assets/deluxe/textures/gui/mainmenu/icons/settings.png";
   public static final String field09727 = "assets/deluxe/textures/gui/mainmenu/icons/accounts.png";
   public static final String field09728 = "assets/deluxe/textures/gui/mainmenu/icons/singleplayer.png";
   public static final String field09729 = "assets/deluxe/textures/gui/mainmenu/icons/bell.png";
   public static final String field09730 = "assets/deluxe/textures/gui/mainmenu/icons/pattern.png";
   public static final String field09731 = "assets/deluxe/textures/gui/mainmenu/icons/search.png";
   public static final String field09732 = "assets/deluxe/textures/gui/mainmenu/icons/back.png";
   public static final String field09733 = "assets/deluxe/textures/gui/mainmenu/icons/dice.png";
   public static final String field09734 = "assets/deluxe/textures/gui/mainmenu/icons/plus.png";
   public static final String field09735 = "assets/deluxe/textures/gui/mainmenu/icons/user.png";
   public static final String field09736 = "assets/deluxe/textures/gui/mainmenu/icons/delete.png";
   public static final String field09737 = "assets/deluxe/textures/gui/mainmenu/icons/rename.png";
   public static final String field09738 = "assets/deluxe/textures/gui/mainmenu/icons/grass.png";
   public static final String field09739 = "assets/deluxe/textures/gui/mainmenu/icons/deluxe_serif.png";
   public static final String field09740 = "assets/deluxe/textures/gui/mainmenu/icons/cos_menu.png";
   public static boolean field09741;
   public static final String TEXT_ICONS = "icons";
   public static final String field09742 = "α";
   public static final String field09743 = "β";
   public static final String field09744 = "γ";
   public static final String field09745 = "δ";
   public static final String field09746 = "ε";
   public static final String field09747 = "b";
   public static final String field09748 = "9";
   public static final String field09749 = "B";
   public static final String field09750 = "e";
   public static final String field09751 = "d";
   public static final String field09752 = "ζ";
   public static final String field09753 = "E";
   public static final String field09754 = "S";
   public static final String field09755 = "W";
   public static final String field09756 = "λ";
   public static final String field09757 = "μ";
   public static final String field09758 = "θ";
   public static final String field09759 = "η";
   public static final String field09760 = "N";
   public static final String field09761 = "O";
   public static final String field09762 = "P";
   public static final String field09763 = "ι";
   public static final String field09764 = "R";

   public static void method2871(
      float local4499_0, float local4499_1, float local4499_2, float local4499_3, float local4499_4, int local4499_5, int local4499_6
   ) {
      Render2D.method4715(local4499_0, local4499_1, local4499_2, local4499_3, local4499_4, local4499_5);
      Render2D.method4769(local4499_0, local4499_1, local4499_2, local4499_3, local4499_4, 0.5F, local4499_6);
   }

   public static void method2872(float local4500_0, float local4500_1, float local4500_2, float local4500_3, int local4500_4) {
      long local4500_5 = System.currentTimeMillis();
      float local4500_7 = -((float)(local4500_5 % 1100L) / 1100.0F * 360.0F);
      float local4500_8 = (float)(local4500_5 % 1800L) / 1800.0F;
      local4500_8 = local4500_8 < 0.5F ? method2875(local4500_8 * 2.0F) : 1.0F - method2875((local4500_8 - 0.5F) * 2.0F);
      float local4500_9 = 55.0F + 230.0F * local4500_8;
      Render2D.method4699(
         new Outline360DrawData(
               local4500_0 - local4500_2,
               local4500_1 - local4500_2,
               local4500_2 * 2.0F,
               local4500_2 * 2.0F,
               local4500_2,
               local4500_3,
               Render2D.method4724(method2877(field09680, 0.051F)),
               List.of(ArcColorSpan.of(local4500_7, local4500_7 + local4500_9, Render2D.method4724(local4500_4)))
            )
            .withBlendDegrees(Math.min(48.0F, local4500_9 * 0.42F))
      );
   }

   public static List<String> method2873(FontFace fontFace, String local4501_1, float local4501_2, float local4501_3) {
      List<String> local4501_4 = new ArrayList<>();
      StringBuilder stringBuilder = new StringBuilder();

      for (String local4501_9 : local4501_1.split(" ")) {
         String local4501_10 = stringBuilder.length() == 0 ? local4501_9 : stringBuilder + " " + local4501_9;
         if (stringBuilder.length() != 0 && !(FontText.method1396(fontFace, local4501_10, local4501_2) <= local4501_3)) {
            local4501_4.add(stringBuilder.toString());
            stringBuilder = new StringBuilder(local4501_9);
         } else {
            stringBuilder = new StringBuilder(local4501_10);
         }
      }

      if (stringBuilder.length() > 0) {
         local4501_4.add(stringBuilder.toString());
      }

      return local4501_4;
   }

   public static float method2874(float local4502_0) {
      local4502_0 = Math.max(0.0F, Math.min(1.0F, local4502_0));
      return 1.0F + 2.70158F * (local4502_0 - 1.0F) * (local4502_0 - 1.0F) * (local4502_0 - 1.0F) + 1.70158F * (local4502_0 - 1.0F) * (local4502_0 - 1.0F);
   }

   public static float method2875(float local4503_0) {
      local4503_0 = Math.max(0.0F, Math.min(1.0F, local4503_0));
      return 1.0F - (1.0F - local4503_0) * (1.0F - local4503_0);
   }

   public static void method2876(String local4504_0, float local4504_1, float local4504_2, float local4504_3, float local4504_4, int local4504_5) {
      Render2D.method4777(
         local4504_0, local4504_1 - local4504_3 / 2.0F, local4504_2 - local4504_4 / 2.0F, local4504_3, local4504_4, 0.0F, local4504_5
      );
   }

   public static int method2877(int local4505_0, float local4505_1) {
      return Math.round((local4505_0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, local4505_1))) << 24 | local4505_0 & 16777215;
   }

   private UiTheme() {
   }

   public static void method2878(long local4508_0, float local4508_2, float local4508_3) {
      float local4508_4 = Math.min(local4508_2 / 960.0F, local4508_3 / 540.0F);
      float local4508_5 = (local4508_3 - 540.0F * local4508_4) / 2.0F;
      long local4508_6 = System.currentTimeMillis();
      float local4508_8 = method2874(method2885(local4508_0, 0L, 520L));
      float local4508_9 = local4508_2 / 2.0F;
      float local4508_10 = local4508_5 + 229.0F * local4508_4;
      Render2D.method4705(Math.min(1.0F, local4508_8 * 2.0F));
      method2872(local4508_9, local4508_10, 28.5F * local4508_4 * local4508_8, 2.7F * local4508_4 * Math.max(0.001F, local4508_8), field09686);
      float local4508_11 = 1.0F + 0.045F * (float)Math.sin((float)(local4508_6 % 1600L) / 1600.0F * Math.PI * 2.0);
      method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/logo.png",
         local4508_9,
         local4508_10,
         15.5F * local4508_4 * local4508_8 * local4508_11,
         12.0F * local4508_4 * local4508_8 * local4508_11,
         field09686
      );
      Render2D.method4650();
      float local4508_12 = method2875(method2885(local4508_0, 140L, 480L));
      float local4508_13 = FontText.method1396(FontFace.SEMI, "Запускаем Deluxe...", 15.0F * local4508_4);
      Render2D.method4705(local4508_12);
      FontText.method1397(
         FontFace.SEMI,
         "Запускаем Deluxe" + ".".repeat(1 + (int)(local4508_6 / 450L % 3L)),
         local4508_9 - local4508_13 / 2.0F,
         local4508_5 + 283.0F * local4508_4 + (1.0F - local4508_12) * 10.0F * local4508_4,
         15.0F * local4508_4,
         field09680
      );
      Render2D.method4650();
      float local4508_14 = method2875(method2885(local4508_0, 240L, 480L));
      Render2D.method4705(local4508_14);
      float local4508_15 = local4508_5 + 313.0F * local4508_4 + (1.0F - local4508_14) * 10.0F * local4508_4;

      for (String local4508_17 : method2873(
         FontFace.SEMI,
         "Загружаем необходимые компоненты и применяем ваши настройки. Ещё немного - и Deluxe будет готов к работе.",
         7.5F * local4508_4,
         190.0F * local4508_4
      )) {
         FontText.method1397(
            FontFace.SEMI,
            local4508_17,
            local4508_9 - FontText.method1396(FontFace.SEMI, local4508_17, 7.5F * local4508_4) / 2.0F,
            local4508_15,
            7.5F * local4508_4,
            field09708
         );
         local4508_15 += 9.0F * local4508_4;
      }

      Render2D.method4650();
   }

   public static boolean method2879(float local4509_0, float local4509_1, float local4509_2, float local4509_3, float local4509_4, float local4509_5) {
      return local4509_0 >= local4509_2 && local4509_0 <= local4509_2 + local4509_4 && local4509_1 >= local4509_3 && local4509_1 <= local4509_3 + local4509_5;
   }

   public static void method2880(String local4510_0, String local4510_1, float local4510_2, float local4510_3, float local4510_4, int local4510_5) {
      float[] floatArray = Render2D.method4655(local4510_0, local4510_1, local4510_4);
      Render2D.method4746(
         local4510_0,
         local4510_1,
         local4510_2 - (floatArray[0] + floatArray[2]) / 2.0F,
         local4510_3 - (floatArray[1] + floatArray[3]) / 2.0F + 1.0F,
         local4510_4,
         local4510_5
      );
   }

   public static int method2881(int local4511_0, int local4511_1, float local4511_2) {
      float local4511_3 = Math.max(0.0F, Math.min(1.0F, local4511_2));
      return method2883(local4511_0 >>> 24 & 0xFF, local4511_1 >>> 24 & 0xFF, local4511_3) << 24
         | method2883(local4511_0 >>> 16 & 0xFF, local4511_1 >>> 16 & 0xFF, local4511_3) << 16
         | method2883(local4511_0 >>> 8 & 0xFF, local4511_1 >>> 8 & 0xFF, local4511_3) << 8
         | method2883(local4511_0 & 0xFF, local4511_1 & 0xFF, local4511_3);
   }

   public static void method2882(String local4512_0, float local4512_1, float local4512_2, float local4512_3) {
      float local4512_4 = Math.max(local4512_1 / 960.0F, local4512_2 / 540.0F);
      Render2D.method4777(
         local4512_0,
         (local4512_1 - 960.0F * local4512_4) / 2.0F,
         (local4512_2 - 540.0F * local4512_4) / 2.0F,
         960.0F * local4512_4,
         540.0F * local4512_4,
         0.0F,
         method2877(field09680, local4512_3)
      );
   }

   private static int method2883(int local4513_0, int local4513_1, float local4513_2) {
      return Math.round(local4513_0 + (local4513_1 - local4513_0) * local4513_2);
   }

   public static void method2884(String local4514_0, float local4514_1, float local4514_2, float local4514_3, int local4514_4) {
      method2880("icons", local4514_0, local4514_1, local4514_2, local4514_3, local4514_4);
   }

   public static float method2885(long local4515_0, long local4515_2, long local4515_4) {
      return Math.max(0.0F, Math.min(1.0F, (float)(System.currentTimeMillis() - local4515_0 - local4515_2) / (float)local4515_4));
   }
}
