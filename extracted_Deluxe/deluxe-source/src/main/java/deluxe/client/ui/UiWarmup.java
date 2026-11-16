package deluxe.client.ui;

import deluxe.client.render.FontText;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.ModuleCategory;
import deluxe.client.render.Render2D;

public final class UiWarmup {
   private static final UiWarmup uiWarmup = new UiWarmup();
   private static volatile int field06798 = 5;
   private static volatile boolean field06799;
   private final EventListener<HudRenderEvent> field06805 = hudRenderEvent -> {
      if (field06798 > 0) {
         field06798--;
         if (!field06799) {
            method1944();
         }

         try {
            this.method1945();
         } catch (Throwable throwable) {
         }

         try {
            this.method1948();
         } catch (Throwable throwable2) {
         }

         if (field06798 <= 0) {
            EventBus.getInstance().unsubscribe(uiWarmup);
         }
      }
   };

   public static void method1944() {
      String local3048_0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?-_()[]{}<>/\\|@#$%^&*+=\"'`~АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

      for (FontFace fontFace : FontFace.values()) {
         try {
            FontText.method1396(
               fontFace,
               fontFace == FontFace.ICONS ? "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789αβγδεζηθικλμνξ" : local3048_0,
               8.0F
            );
         } catch (Throwable throwable) {
         }
      }

      field06799 = true;
   }

   private void method1945() {
      Render2D.method4715(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431);
      Render2D.method4691(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 33554431, 33554431, 33554431);
      Render2D.method4769(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 0.5F, 33554431);
      Render2D.method4749(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 0.5F, 33554431);
      Render2D.method4719(-4000.0F, 1.0F, 1.0F, 33554431);
      Render2D.method4664(-4000.0F, 1.0F, 1.0F, 0.5F, 33554431);
      Render2D.method4742(-4000.0F, 0.0F, 2.0F, 1.0F, 0.25F, 2, 33554431);
      Render2D.method4758(-4000.0F, 0.0F, 4.0F, 4.0F, 2.0F, 2.0F, 2.0F, 2.0F, 3.0F, 1.0F, 0.0F, 0.0F, 1.0F, 33554431);
      Render2D.method4720(-4000.0F, 0.0F, 2.0F, 2.0F, 0.003921569F);
      Render2D.method4753(-4000.0F, 0.0F, 2.0F, 2.0F, 33554431, 0.003921569F);
      Render2D.method4676(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 8.0F);
      Render2D.method4725(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 8.0F, 1.0F, 33554431);
      Render2D.method4702(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F);
      Render2D.method4782(-4000.0F, 0.0F, 2.0F, 2.0F, 0.5F, 0.5F, 1.0F, 33554431);
      Render2D.method4680(-4000.0F, 0.0F, 2.0F, 2.0F, 0.5F, 0.5F, 1.0F, 0.5F, 33554431);
      Render2D.method4662(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 0.5F);
      Render2D.method4780(-4000.0F, 1.0F, 33554431, 0.5F, 0.5F);
      Render2D.method4657(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 1.0F, 1.0F, 0.5F, 33554431, 33554431);
      Render2D.method4721(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 33554431, "icons", "a", 1.0F, 2.0F, 2.0F, 0.0F);
      Render2D.method4674(FontFace.SEMI_BOLD.getResourceName(), "Ag", -4000.0F, 0.0F, 6.0F, 0.5F, 0.35F, 0.5F, 0.003921569F);

      for (FontFace fontFace : FontFace.values()) {
         FontText.method1397(fontFace, fontFace == FontFace.ICONS ? "aM" : "Ag", -4000.0F, 0.0F, 6.0F, 33554431);
      }
   }

   private UiWarmup() {
   }

   public static void method1947() {
      field06798 = 5;
      field06799 = false;
      EventBus.getInstance().unsubscribe(uiWarmup);
      EventBus.getInstance().subscribe(uiWarmup);
   }

   private void method1948() {
      for (ModuleCategory moduleCategory : ModuleCategory.values()) {
         Render2D.method4777(ClickGuiStyle.method3186(moduleCategory), -4000.0F, 0.0F, 2.0F, 2.0F, 0.0F, 33554431);
      }

      for (String local3054_4 : new String[]{
         "assets/deluxe/textures/gui/clickgui/search.png",
         "assets/deluxe/textures/gui/clickgui/chev.png",
         "assets/deluxe/textures/gui/clickgui/chevright.png",
         "assets/deluxe/textures/gui/clickgui/check.png",
         "assets/deluxe/textures/gui/clickgui/picker.png",
         "assets/deluxe/textures/gui/clickgui/bindkey.png"
      }) {
         Render2D.method4777(local3054_4, -4000.0F, 0.0F, 2.0F, 2.0F, 0.0F, 33554431);
      }
   }

   public static void method1949() {
      EventBus.getInstance().subscribe(uiWarmup);
   }
}
