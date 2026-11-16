package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class KeybindsHud {
   private static final String[] field09113 = new String[]{"A", "B", "C", "D", "E"};
   private static final KeybindsHud.Nested0446 field09114 = new KeybindsHud.Nested0446(null, "Example Module");
   private static final Map<Module, KeybindsHud.Nested0446> field09115 = new IdentityHashMap<>();
   private static final List<KeybindsHud.Nested0446> field09116 = new ArrayList<>();
   private static final Animation field09117 = new Animation();
   private static final Animation field09118 = new Animation();
   private static final Animation field09119 = new Animation();
   private static final Animation field09120 = new Animation();
   private static float field09121;
   private static float field09122;

   private KeybindsHud() {
   }

   public static float method2647(float local4126_0, float local4126_1) {
      method2649();
      float local4126_2 = HudStyle.method4823("Hotkeys");
      float local4126_3 = 0.0F;
      float local4126_4 = 0.0F;
      float local4126_5 = 13.0F;

      for (KeybindsHud.Nested0446 local4126_7 : field09116) {
         local4126_3 = Math.max(local4126_3, HudStyle.method4809(10.0F, method2651(local4126_7)));
         local4126_4 = Math.max(local4126_4, HudStyle.method4822(method2653(local4126_7)));
         local4126_5 += 14.0F * local4126_7.visibilityAnimation.floatValue();
      }

      float local4126_6 = local4126_3 + (local4126_3 > 0.0F && local4126_4 > 0.0F ? 2.0F : 0.0F) + local4126_4;
      float local4126_7 = Math.max(local4126_2, Math.max(local4126_6, 1.0F));
      boolean local4126_8 = !field09116.isEmpty() || HudRenderContext.method4342();
      field09117.update();
      field09117.animateTo(local4126_8 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      field09118.update();
      if (!field09116.isEmpty()) {
         field09118.animateTo(Math.max(local4126_7, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field09119.update();
      if (!field09116.isEmpty()) {
         field09119.animateTo(Math.max(local4126_7, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field09120.update();
      if (!field09116.isEmpty()) {
         field09120.animateTo(Math.max(local4126_3, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field09121 = field09118.floatValue();
      field09122 = Math.max(local4126_5, 1.0F);
      float local4126_9 = field09117.floatValue();
      if (local4126_9 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.method3390("keybinds", local4126_0, local4126_1 - field09122);
      float local4126_11 = floatArray[0];
      float local4126_12 = floatArray[1];
      HudManager.method3418("keybinds", local4126_11, local4126_12, field09121, field09122);
      HudRenderContext.method4341(local4126_11 + field09121 / 2.0F, local4126_12 + field09122 / 2.0F, local4126_9);
      HudStyle.method4824(local4126_11, local4126_12, Math.max(field09119.floatValue(), 1.0F), local4126_9, HudIcons.KEYBINDS, "Hotkeys");
      float local4126_13 = local4126_12 + 15.0F;

      for (KeybindsHud.Nested0446 local4126_15 : field09116) {
         method2652(local4126_11, local4126_13, local4126_9, local4126_15);
         local4126_13 += 13.0F * local4126_15.visibilityAnimation.floatValue();
      }

      HudRenderContext.method4337();
      return field09122 * local4126_9;
   }

   static {
      field09118.resetTo(1.0);
   }

   private static void method2649() {
      field09116.clear();

      for (Module currentModule : ModuleManager.getModules()) {
         boolean local4129_2 = currentModule.hasKeybind() && currentModule.isEnabled();
         KeybindsHud.Nested0446 local4129_3 = field09115.get(currentModule);
         if (local4129_3 == null) {
            if (!local4129_2) {
               continue;
            }

            local4129_3 = new KeybindsHud.Nested0446(currentModule, currentModule.name);
            field09115.put(currentModule, local4129_3);
         }

         local4129_3.field09165 = currentModule.hasKeybind() ? currentModule.getKeyName() : local4129_3.field09165;
         local4129_3.visible = local4129_2;
         if (local4129_2 || local4129_3.visibilityAnimation.floatValue() > 0.001) {
            field09116.add(local4129_3);
         }
      }

      field09115.entrySet()
         .removeIf(
            local4130_0 -> !local4130_0.getValue().visible
               && local4130_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local4130_0.getValue().visibilityAnimation.isFinished()
         );
      field09114.visible = field09116.isEmpty() && HudRenderContext.method4342();
      if (field09114.visible) {
         field09114.field09165 = field09113[HudRenderContext.method4339() % field09113.length];
      }

      if (field09114.visible || field09114.visibilityAnimation.floatValue() > 0.001) {
         field09116.add(field09114);
      }

      field09116.sort(Comparator.<KeybindsHud.Nested0446>comparingDouble(local4128_0 -> method2653(local4128_0)).reversed());

      for (KeybindsHud.Nested0446 local4129_1 : field09116) {
         local4129_1.visibilityAnimation.update();
         local4129_1.visibilityAnimation.animateTo(local4129_1.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
         boolean local4129_2 = local4129_1.module == null || local4129_1.module.isEnabled();
         local4129_1.animation.update();
         if (local4129_1.field09167) {
            local4129_1.animation.animateTo(local4129_2 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
         } else {
            local4129_1.field09167 = true;
            local4129_1.animation.resetTo(local4129_2 ? 1.0 : 0.0);
         }
      }
   }

   private static float method2651(KeybindsHud.Nested0446 local4131_0) {
      return FontText.method1396(FontFace.SEMI_BOLD, local4131_0.field09165, 6.0F);
   }

   private static void method2652(float local4132_0, float local4132_1, float local4132_2, KeybindsHud.Nested0446 local4132_3) {
      float local4132_4 = local4132_3.visibilityAnimation.floatValue() * local4132_2;
      if (!(local4132_4 <= 0.001F)) {
         float local4132_5 = HudStyle.method4814(local4132_4);
         float local4132_6 = method2651(local4132_3);
         float local4132_7 = HudStyle.method4809(10.0F, local4132_6);
         float local4132_8 = HudStyle.method4822(method2653(local4132_3));
         float local4132_9 = local4132_0 - local4132_5;
         float local4132_10 = local4132_0 + field09120.floatValue() + 2.0F - local4132_5;
         float local4132_11 = HudStyle.method4816(local4132_1);
         HudStyle.method4821(local4132_9, local4132_1, local4132_7, 11.0F, local4132_4);
         float local4132_12 = local4132_9 + 2.0F + 10.0F + 1.0F;
         float local4132_13 = local4132_12 + 0.25F;
         HudStyle.method4810(local4132_12, local4132_1 + 5.5F, local4132_4, 3.0F);
         float local4132_14 = local4132_9 + (local4132_13 - local4132_9 - 10.0F) / 2.0F;
         KeybindAnimationSprite.TOGGLE
            .method1720(
               local4132_14,
               local4132_1 + 5.5F - 5.0F,
               10.0F,
               10.0F,
               UiTheme.method2877(ThemeColors.field13669, local4132_4),
               local4132_3.animation.floatValue()
            );
         FontText.method1397(
            FontFace.SEMI_BOLD,
            local4132_3.field09165,
            local4132_13 + (local4132_9 + local4132_7 - local4132_13 - local4132_6) / 2.0F,
            local4132_11,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local4132_4)
         );
         HudStyle.method4821(local4132_10, local4132_1, local4132_8, 11.0F, local4132_4);
         FontText.method1397(
            FontFace.SEMI_BOLD,
            local4132_3.field09164,
            local4132_10 + 4.0F,
            local4132_11,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local4132_4)
         );
      }
   }

   private static float method2653(KeybindsHud.Nested0446 local4133_0) {
      return FontText.method1396(FontFace.SEMI_BOLD, local4133_0.field09164, 6.0F);
   }

   private static final class Nested0446 extends DraggableHudElement {
      final Module module;
      final String field09164;
      String field09165 = "";
      final Animation animation = new Animation();
      boolean field09167;

      Nested0446(Module initialModule, String local4134_2) {
         this.module = initialModule;
         this.field09164 = local4134_2;
      }
   }
}
