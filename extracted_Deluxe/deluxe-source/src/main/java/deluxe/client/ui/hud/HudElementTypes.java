package deluxe.client.ui.hud;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.ui.setting.SettingRendererRegistry;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.ui.UiTheme;

import deluxe.client.module.render.InterfaceModule;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

import java.util.ArrayList;

public final class HudElementTypes {
   private static final AnimatedValue animatedValue = new AnimatedValue(180L);
   private static final SettingRenderContext settingRenderContext = new SettingRenderContext();
   private static String field16209;
   private static boolean field16210;
   private static float field16211;
   private static float field16212;
   private static float field16213 = 60.0F;
   private static boolean field16214;
   private static float field16215;
   private static float field16216;
   private static String field16217;
   private static float field16218;
   private static float field16219;

   public static boolean method4840(double local8044_0, double local8044_2, int local8044_4) {
      float[] floatArray = HudManager.method3420(local8044_0, local8044_2);
      float local8044_6 = floatArray[0];
      float local8044_7 = floatArray[1];
      boolean local8044_8 = field16209 != null
         && field16210
         && local8044_6 >= field16211
         && local8044_6 <= field16211 + 103.3F
         && local8044_7 >= field16212
         && local8044_7 <= field16212 + field16213;
      if (local8044_4 == 1) {
         if (local8044_8) {
            for (int index = settingRenderContext.field12417.size() - 1; index >= 0; index--) {
               SettingRenderContext.Nested0493 local8044_10 = settingRenderContext.field12417.get(index);
               if (local8044_10.rightAction() != null
                  && UiTheme.method2879(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.rightAction().run();
                  return true;
               }
            }

            method4842();
            return true;
         } else {
            String local8044_9 = HudManager.method3383(local8044_6, local8044_7);
            if (local8044_9 != null && !method4845(local8044_9).isEmpty()) {
               method4843(local8044_9, local8044_6, local8044_7);
               return true;
            } else if (field16209 != null && field16210) {
               method4842();
               return true;
            } else {
               return false;
            }
         }
      } else if (local8044_4 == 0 && field16209 != null && field16210) {
         if (settingRenderContext.field12427 != null) {
            for (int index2 = settingRenderContext.field12417.size() - 1; index2 >= 0; index2--) {
               SettingRenderContext.Nested0493 local8044_10 = settingRenderContext.field12417.get(index2);
               if (UiTheme.method2879(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.action().run();
                  return true;
               }
            }

            return true;
         } else {
            if (!local8044_8) {
               method4842();
               return false;
            }

            for (int index3 = settingRenderContext.field12417.size() - 1; index3 >= 0; index3--) {
               SettingRenderContext.Nested0493 local8044_10 = settingRenderContext.field12417.get(index3);
               if (UiTheme.method2879(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.action().run();
                  return true;
               }
            }

            field16214 = true;
            field16215 = local8044_6 - field16211;
            field16216 = local8044_7 - field16212;
            return true;
         }
      } else {
         return false;
      }
   }

   static void method4841(float mouseX, float mouseY, boolean local8045_2) {
      animatedValue.method3869(field16210 && local8045_2 && field16209 != null ? 1.0 : 0.0);
      float local8045_3 = (float)animatedValue.method3871();
      if (field16209 != null && !(local8045_3 < 0.01F)) {
         if (field16214) {
            field16211 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.method3387() - 103.3F - 5.0F), mouseX - field16215));
            field16212 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.method3389() - field16213 - 5.0F), mouseY - field16216));
         }

         ArrayList<Setting<?>> local8045_4 = method4845(field16209);
         float local8045_5 = 0.0F;

         for (int index = 0; index < local8045_4.size(); index++) {
            local8045_5 += SettingRendererRegistry.method0943(settingRenderContext, local8045_4.get(index), 90.8F)
               + (index < local8045_4.size() - 1 ? 3.5F : 0.0F);
         }

         float local8045_6 = 5.1F + local8045_5 + 5.1F;
         field16213 = local8045_6;
         settingRenderContext.method3574(HudRenderContext.method4340(), 1.0F, mouseX, mouseY);
         HudRenderContext.method4336(field16211 + 51.65F, field16212 + local8045_6 / 2.0F, 0.96F + 0.04F * local8045_3);
         HudStyle.method4812(field16211, field16212, 103.3F, local8045_6, local8045_3, 4.5F, ShapeDrawing.Nested0502.LOW);
         Render2D.method4705(local8045_3);
         float local8045_7 = field16212 + 5.1F;

         for (Setting<?> local8045_9 : local8045_4) {
            local8045_7 += SettingRendererRegistry.method0942(settingRenderContext, local8045_9, 1.0F, field16211 + 6.3F, local8045_7, 90.8F, null) + 3.5F;
         }

         settingRenderContext.method3552();
         Render2D.method4650();
         HudRenderContext.method4337();
      } else {
         if (!field16210) {
            field16209 = null;
            if (field16217 != null && local8045_2) {
               method4844(field16217, field16218, field16219);
            }
         }
      }
   }

   public static void method4842() {
      field16210 = false;
      field16214 = false;
      field16217 = null;
      settingRenderContext.field12427 = null;
   }

   private HudElementTypes() {
   }

   public static void method4843(String local8048_0, float local8048_1, float local8048_2) {
      if (field16209 != null && field16210 && animatedValue.method3871() > 0.05) {
         field16217 = local8048_0;
         field16218 = local8048_1;
         field16219 = local8048_2;
         field16210 = false;
      } else {
         method4844(local8048_0, local8048_1, local8048_2);
      }
   }

   private static void method4844(String local8049_0, float local8049_1, float local8049_2) {
      field16217 = null;
      field16209 = local8049_0;
      field16210 = true;
      settingRenderContext.field12427 = null;
      field16211 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.method3387() - 103.3F - 5.0F), local8049_1 + 2.0F));
      field16212 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.method3389() - field16213 - 5.0F), local8049_2 + 2.0F));
   }

   private static ArrayList<Setting<?>> method4845(String local8050_0) {
      ArrayList<Setting<?>> local8050_1 = new ArrayList<>();
      if ("watermark".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.method2247().field07919);
         local8050_1.add(InterfaceModule.method2247().field07920);
      }

      if ("potions".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.method2247().field07922);
      }

      if ("target".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.method2247().field07923);
      }

      return local8050_1;
   }

   public static void method4846() {
      field16214 = false;
   }

   public static boolean method4847() {
      return field16209 != null && animatedValue.method3871() > 0.01;
   }
}
