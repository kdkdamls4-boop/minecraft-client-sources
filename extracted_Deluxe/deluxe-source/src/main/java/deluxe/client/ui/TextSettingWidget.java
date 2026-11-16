package deluxe.client.ui;

import deluxe.client.render.FontFace;
import deluxe.client.render.FontText;
import deluxe.client.ui.setting.SettingRenderer;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.setting.Setting;
import deluxe.client.setting.TextSetting;

public final class TextSettingWidget implements SettingRenderer {
   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local1779_2, float local1779_3) {
      return 7.0F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local1780_1) {
      return local1780_1 instanceof TextSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local1781_2, float local1781_3, float local1781_4, float local1781_5, float local1781_6, float[] floatArray
   ) {
      TextSetting currentTextSetting = (TextSetting)local1781_2;
      float local1781_9 = settingRenderContext.scale;
      boolean local1781_10 = settingRenderContext.textSetting == currentTextSetting;
      boolean isEmpty = currentTextSetting.getValue().isEmpty();
      String local1781_12 = currentTextSetting.getPlaceholder().isEmpty() ? "Введите текст" : currentTextSetting.getPlaceholder();
      float local1781_13 = local1781_6 * 0.55F;
      float local1781_14 = Math.min(
         FontText.method1396(FontFace.SF, isEmpty ? local1781_12 : currentTextSetting.getValue(), 6.0F * local1781_9)
            + (local1781_10 ? 2.0F * local1781_9 : 0.0F),
         local1781_13
      );
      settingRenderContext.method3569(
         currentTextSetting.name,
         local1781_4,
         local1781_5,
         6.0F * local1781_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local1781_3),
         local1781_6 - local1781_14 - 5.0F * local1781_9
      );
      settingRenderContext.method3573(currentTextSetting.description, local1781_4, local1781_5 - 1.0F * local1781_9, local1781_6 * 0.4F, 8.0F * local1781_9);
      float local1781_15 = settingRenderContext.method3560("inputHint:" + System.identityHashCode(currentTextSetting), isEmpty ? 1.0F : 0.0F);
      if (local1781_15 > 0.01F) {
         float local1781_16 = Math.min(FontText.method1396(FontFace.SF, local1781_12, 6.0F * local1781_9), local1781_13);
         settingRenderContext.method3569(
            local1781_12,
            local1781_4 + local1781_6 - local1781_16 - (local1781_10 ? 2.0F * local1781_9 : 0.0F),
            local1781_5,
            6.0F * local1781_9,
            UiTheme.method2877(
               UiTheme.method2881(ClickGuiStyle.field10792, ClickGuiStyle.field10793, local1781_3),
               (local1781_10 ? 0.55F : 1.0F) * local1781_15
            ),
            local1781_13
         );
      }

      if (local1781_10) {
         float local1781_16 = Math.min(FontText.method1396(FontFace.SF, currentTextSetting.getValue(), 6.0F * local1781_9), local1781_13);
         settingRenderContext.method3539(
            currentTextSetting,
            local1781_4 + local1781_6 - local1781_16 - 2.0F * local1781_9,
            local1781_5,
            6.0F * local1781_9,
            UiTheme.method2881(ClickGuiStyle.field10791, ClickGuiStyle.field10780, local1781_3)
         );
      } else if (!isEmpty) {
         settingRenderContext.method3569(
            currentTextSetting.getValue(),
            local1781_4 + local1781_6 - local1781_14,
            local1781_5,
            6.0F * local1781_9,
            UiTheme.method2881(ClickGuiStyle.field10791, ClickGuiStyle.field10780, local1781_3),
            local1781_13
         );
      }

      settingRenderContext.method3554(local1781_4 + local1781_6 / 2.0F, local1781_5 - 1.5F * local1781_9, local1781_6 / 2.0F, 9.0F * local1781_9, floatArray, () -> {
         settingRenderContext.textSetting = currentTextSetting;
         currentTextSetting.moveToEnd(false);
      });
      return 7.0F * local1781_9;
   }
}
