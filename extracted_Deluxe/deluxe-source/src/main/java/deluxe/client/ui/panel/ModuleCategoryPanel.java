package deluxe.client.ui.panel;

import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.render.RoundedClipStack;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.ui.SmoothScroll;

import deluxe.client.render.FontFace;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleManager;
import deluxe.client.render.Render2D;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModuleCategoryPanel {
   private static final Map<Module, ModuleCategoryPanel.Nested0484> field11577 = new IdentityHashMap<>();

   public static void method3286(
      SettingRenderContext settingRenderContext,
      ModuleCategory moduleCategory,
      SmoothScroll smoothScroll,
      float local5347_3,
      float local5347_4,
      float local5347_5,
      float local5347_6,
      float local5347_7
   ) {
      float local5347_8 = settingRenderContext.scale;
      float local5347_9 = local5347_4 + 21.0F * local5347_8;
      Render2D.method4725(
         local5347_3 + 0.5F * local5347_8,
         local5347_4 + 0.5F * local5347_8,
         local5347_5 - 1.0F * local5347_8,
         local5347_6 - 1.0F * local5347_8,
         8.0F * local5347_8,
         30.0F,
         1.0F,
         -1
      );
      Render2D.method4715(local5347_3, local5347_4, local5347_5, local5347_6, 8.5F * local5347_8, ClickGuiStyle.field10781);
      Render2D.method4769(local5347_3, local5347_4, local5347_5, local5347_6, 8.5F * local5347_8, 0.5F, ClickGuiStyle.field10782);
      FontText.method1397(
         FontFace.SF,
         moduleCategory.getDisplayName().toUpperCase(Locale.ROOT),
         local5347_3 + 11.0F * local5347_8,
         local5347_4 + 9.0F * local5347_8,
         6.3F * local5347_8,
         ClickGuiStyle.field10783
      );
      Render2D.method4777(
         ClickGuiStyle.method3186(moduleCategory),
         local5347_3 + local5347_5 - 17.3F * local5347_8,
         local5347_4 + 9.4F * local5347_8,
         6.4F * local5347_8,
         6.4F * local5347_8,
         0.0F,
         ClickGuiStyle.field10783
      );
      smoothScroll.update(local5347_7);
      float local5347_10 = local5347_4 + 26.5F * local5347_8 - smoothScroll.getPixelAlignedOffset();
      Render2D.method4678(settingRenderContext.drawContext, local5347_3, local5347_9, local5347_5, local5347_6 - 21.0F * local5347_8);
      RoundedClipStack.method4836(
         local5347_3, local5347_9, local5347_5, local5347_6 - 21.0F * local5347_8, 0.0F, 0.0F, 8.5F * local5347_8, 8.5F * local5347_8
      );
      float[] floatArray = null;

      for (Module module : ModuleManager.getModulesByCategory(moduleCategory)) {
         ModuleCategoryPanel.Nested0484 local5347_14 = method3287(module);
         float local5347_15 = settingRenderContext.method3560(
            local5347_14.searchKey,
            !settingRenderContext.field12426.isEmpty()
                  && !local5347_14.lowerName.contains(settingRenderContext.field12426)
                  && !local5347_14.lowerDescription.contains(settingRenderContext.field12426)
               ? 0.0F
               : 1.0F
         );
         if (!(local5347_15 < 0.01F)) {
            if (local5347_15 < 0.999F) {
               Render2D.method4705(local5347_15 * local5347_15);
            }

            float local5347_16 = ModulePanel.method3269(settingRenderContext, module, local5347_5 - 9.2F * local5347_8);
            if (local5347_10 + local5347_16 >= local5347_9 - 2.0F && local5347_10 <= local5347_9 + (local5347_6 - 21.0F * local5347_8) + 2.0F) {
               if (floatArray == null) {
                  floatArray = new float[]{local5347_3, local5347_9, local5347_5, local5347_6 - 21.0F * local5347_8};
               }

               ModulePanel.method3271(
                  settingRenderContext, module, local5347_3 + 4.6F * local5347_8, local5347_10, local5347_5 - 9.2F * local5347_8, floatArray
               );
            }

            if (local5347_15 < 0.999F) {
               Render2D.method4650();
            }

            local5347_10 += (local5347_16 + 1.5F * local5347_8) * local5347_15;
         }
      }

      RoundedClipStack.method4838();
      Render2D.method4661(settingRenderContext.drawContext);
      smoothScroll.maximumOffset = Math.max(0.0F, local5347_10 - local5347_4 + smoothScroll.getPixelAlignedOffset() - local5347_6 + 4.5F * local5347_8);
   }

   private ModuleCategoryPanel() {
   }

   private static ModuleCategoryPanel.Nested0484 method3287(Module module) {
      ModuleCategoryPanel.Nested0484 local5350_1 = field11577.get(module);
      if (local5350_1 == null || local5350_1.name != module.name || local5350_1.description != module.description) {
         String local5350_2 = module.description == null ? "" : module.description;
         local5350_1 = new ModuleCategoryPanel.Nested0484(
            module.name, module.description, module.name.toLowerCase(), local5350_2.toLowerCase(), "search." + module.name
         );
         field11577.put(module, local5350_1);
      }

      return local5350_1;
   }

   private record Nested0484(String name, String description, String lowerName, String lowerDescription, String searchKey) {
   }
}
