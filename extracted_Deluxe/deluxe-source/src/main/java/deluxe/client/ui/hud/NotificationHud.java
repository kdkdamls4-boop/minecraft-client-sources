package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;

public final class NotificationHud {
   private static final List<NotificationHud.Nested0449> field09213 = new ArrayList<>();
   private static NotificationHud.Nested0449 field09214;

   public static void method2684(boolean local4193_0) {
      long local4193_1 = System.currentTimeMillis();
      boolean local4193_3 = false;

      for (NotificationHud.Nested0449 local4193_5 : field09213) {
         if (local4193_5 != field09214 && local4193_1 - local4193_5.field09270 <= 3220L) {
            local4193_3 = true;
            break;
         }
      }

      if (local4193_0 && !local4193_3) {
         if (field09214 == null || !field09213.contains(field09214)) {
            NotificationHud.Nested0449 local4193_4 = new NotificationHud.Nested0449();
            local4193_4.field09264 = "F";
            local4193_4.field09266 = "Example";
            local4193_4.field09267 = "Notification!";
            local4193_4.field09269 = true;
            field09214 = local4193_4;
            field09213.add(local4193_4);
            method2685();
         }
      } else if (field09214 != null) {
         if (local4193_1 - field09214.field09270 < 3000L) {
            field09214.field09270 = local4193_1 - 3000L;
         }

         field09214 = null;
      }
   }

   private static void method2685() {
      long local4194_0 = System.currentTimeMillis();
      field09213.removeIf(local4195_2 -> local4194_0 - local4195_2.field09270 > 3220L);
      int local4194_2 = 0;

      for (NotificationHud.Nested0449 local4194_4 : field09213) {
         if (local4194_0 - local4194_4.field09270 < 3000L) {
            local4194_2++;
         }
      }

      for (NotificationHud.Nested0449 local4194_4 : field09213) {
         if (local4194_2 <= 6) {
            return;
         }

         if (local4194_0 - local4194_4.field09270 < 3000L) {
            local4194_4.field09270 = local4194_0 - 3000L;
            local4194_2--;
         }
      }
   }

   public static void method2687(float local4196_0, float local4196_1) {
      long local4196_2 = System.currentTimeMillis();
      Iterator<NotificationHud.Nested0449> local4196_4 = field09213.iterator();

      while (local4196_4.hasNext()) {
         if (local4196_2 - local4196_4.next().field09270 > 3220L) {
            local4196_4.remove();
         }
      }

      if (!field09213.isEmpty()) {
         float[] floatArray = HudManager.method3390("toasts", local4196_0 / 2.0F, local4196_1 * 0.62F);
         float local4196_6 = floatArray[1];
         float local4196_7 = local4196_6;
         float local4196_8 = 0.0F;

         for (int index = field09213.size() - 1; index >= 0; index--) {
            NotificationHud.Nested0449 local4196_10 = field09213.get(index);
            long local4196_11 = local4196_2 - local4196_10.field09270;
            float local4196_13 = ThemeColors.method3965((float)local4196_11 / 220.0F);
            float local4196_14 = Math.min(local4196_13, ThemeColors.method3965((float)(3220L - local4196_11) / 220.0F));
            if (!(local4196_14 < 0.01F)) {
               local4196_8 = Math.max(local4196_8, method2690(local4196_10));
               local4196_7 = method2692(local4196_10, floatArray[0], local4196_7, local4196_13, local4196_11, local4196_14);
            }
         }

         if (local4196_7 - local4196_6 > 0.5F) {
            HudManager.method3399(
               "toasts", floatArray[0] - local4196_8 / 2.0F, local4196_6, local4196_8, local4196_7 - local4196_6, floatArray[0], local4196_6
            );
         }
      }
   }

   public static void method2688(String local4197_0, String local4197_1, String local4197_2, boolean local4197_3) {
      NotificationHud.Nested0449 local4197_4 = new NotificationHud.Nested0449();
      local4197_4.field09264 = local4197_0;
      local4197_4.field09266 = local4197_1;
      local4197_4.field09267 = local4197_2;
      local4197_4.field09269 = local4197_3;
      field09213.add(local4197_4);
      method2685();
   }

   public static void method2689(StatusEffectInstance statusEffectInstance, String local4198_1, String local4198_2, boolean local4198_3) {
      NotificationHud.Nested0449 local4198_4 = new NotificationHud.Nested0449();
      local4198_4.field09265 = statusEffectInstance;
      local4198_4.field09266 = local4198_1;
      local4198_4.field09267 = local4198_2;
      local4198_4.field09269 = local4198_3;
      field09213.add(local4198_4);
      method2685();
   }

   private static float method2690(NotificationHud.Nested0449 local4199_0) {
      return 16.0F
         + FontText.method1396(FontFace.SEMI_BOLD, local4199_0.field09266, 5.5F)
         + 1.0F
         + FontText.method1396(FontFace.SEMI_BOLD, local4199_0.field09267, 5.5F)
         + 4.5F
         + 5.25F
         + 4.5F;
   }

   public static void method2691(String local4201_0, String local4201_1, Text text, boolean local4201_3) {
      NotificationHud.Nested0449 local4201_4 = new NotificationHud.Nested0449();
      local4201_4.field09264 = local4201_0;
      local4201_4.field09266 = local4201_1;
      local4201_4.field09267 = text.getString();
      local4201_4.field09268 = text;
      local4201_4.field09269 = local4201_3;
      field09213.add(local4201_4);
      method2685();
   }

   private static float method2692(
      NotificationHud.Nested0449 local4202_0, float local4202_1, float local4202_2, float local4202_3, long local4202_4, float local4202_6
   ) {
      float local4202_7 = method2690(local4202_0);
      float local4202_8 = local4202_1 - local4202_7 / 2.0F;
      float local4202_9 = local4202_2 - 4.0F * (1.0F - local4202_3);
      float local4202_10 = local4202_9 + 7.5F;
      float local4202_11 = ThemeColors.method3965(1.0F - (float)local4202_4 / 3000.0F);
      Render2D.method4705(local4202_6);
      HudStyle.method4817(local4202_8, local4202_9, local4202_7, 15.0F, local4202_6, 5.5F);
      if (local4202_0.field09265 != null) {
         Render2D.method4741(local4202_0.field09265.getEffectType(), local4202_8 + 4.5F, local4202_10 - 3.5F, 7.0F);
      } else {
         ThemeColors.method3961(
            local4202_0.field09264,
            local4202_8 + 4.5F + 3.5F,
            local4202_10,
            7.0F,
            local4202_0.field09269 ? ThemeColors.field13669 : ThemeColors.field13669 & 16777215 | 2046820352
         );
      }

      float local4202_12 = local4202_8 + 4.5F + 7.0F + 2.0F + 0.5F + 2.0F;
      float local4202_13 = local4202_10 - 2.75F;
      FontText.method1397(
         FontFace.SEMI_BOLD,
         local4202_0.field09266,
         local4202_12,
         local4202_13,
         5.5F,
         local4202_0.field09269 ? HudStyle.method4815(1.0F) : HudStyle.method4815(0.6F)
      );
      float local4202_14 = local4202_12 + FontText.method1396(FontFace.SEMI_BOLD, local4202_0.field09266, 5.5F) + 1.0F;
      int local4202_15 = local4202_0.field09269 ? HudStyle.method4820(1.0F) : HudStyle.method4815(1.0F);
      if (local4202_0.field09268 != null) {
         FontText.method1400(FontFace.SEMI_BOLD, local4202_0.field09268, local4202_14, local4202_13, 5.5F, local4202_15);
      } else {
         FontText.method1397(FontFace.SEMI_BOLD, local4202_0.field09267, local4202_14, local4202_13, 5.5F, local4202_15);
      }

      ThemeColors.method3966(
         local4202_8 + local4202_7 - 4.5F - 2.625F,
         local4202_10,
         5.25F,
         local4202_11,
         UiTheme.method2881(
            ThemeColors.field13664, local4202_0.field09269 ? ThemeColors.field13669 : ThemeColors.field13667, local4202_11
         )
      );
      Render2D.method4650();
      return local4202_2 + 18.5F * local4202_6;
   }

   private static final class Nested0449 {
      String field09264;
      StatusEffectInstance field09265;
      String field09266;
      String field09267;
      Text field09268;
      boolean field09269;
      long field09270 = System.currentTimeMillis();
   }
}
