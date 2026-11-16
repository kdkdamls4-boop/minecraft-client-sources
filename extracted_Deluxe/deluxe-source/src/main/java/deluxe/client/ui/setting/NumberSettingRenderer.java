package deluxe.client.ui.setting;

import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;
import deluxe.client.setting.NumberSetting;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

public final class NumberSettingRenderer implements SettingRenderer {
   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local577_2, float local577_3, float local577_4, float local577_5, float local577_6, float[] floatArray
   ) {
      NumberSetting currentNumberSetting = (NumberSetting)local577_2;
      float local577_9 = settingRenderContext.scale;
      String displayValue = currentNumberSetting.getDisplayValue();
      settingRenderContext.method3569(
         currentNumberSetting.name,
         local577_4,
         local577_5,
         6.0F * local577_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local577_3),
         local577_6 - FontText.method1396(FontFace.SF, displayValue, 6.0F * local577_9) - 4.0F * local577_9
      );
      settingRenderContext.method3573(currentNumberSetting.description, local577_4, local577_5 - 1.0F * local577_9, local577_6 * 0.6F, 8.0F * local577_9);
      FontText.method1397(
         FontFace.SF,
         displayValue,
         local577_4 + local577_6 - FontText.method1396(FontFace.SF, displayValue, 6.0F * local577_9),
         local577_5,
         6.0F * local577_9,
         UiTheme.method2881(
            UiTheme.method2881(ClickGuiStyle.field10791, ClickGuiStyle.field10780, local577_3),
            -43691,
            settingRenderContext.method3560("sliderDanger:" + System.identityHashCode(currentNumberSetting), currentNumberSetting.isAutomatic() ? 1.0F : 0.0F)
         )
      );
      float local577_11 = local577_5 + 9.65F * local577_9;
      float local577_12 = 1.63F * local577_9;
      Render2D.method4715(
         local577_4,
         local577_11,
         local577_6,
         local577_12,
         local577_12 / 2.0F,
         UiTheme.method2881(ClickGuiStyle.field10808, ClickGuiStyle.field10810, local577_3)
      );
      float local577_13 = settingRenderContext.method3560("slider:" + System.identityHashCode(currentNumberSetting), currentNumberSetting.getFraction());
      float local577_14 = Math.max(3.0F * local577_9, local577_13 * local577_6);
      Render2D.method4715(
         local577_4,
         local577_11,
         local577_14,
         local577_12,
         local577_12 / 2.0F,
         UiTheme.method2881(ClickGuiStyle.field10809, ClickGuiStyle.field10780, local577_3)
      );
      float local577_15 = 2.97F * local577_9;
      Render2D.method4715(
         local577_4 + local577_14 - local577_15 / 2.0F - 0.5F * local577_9,
         local577_11 + local577_12 / 2.0F - local577_15 / 2.0F,
         local577_15,
         local577_15,
         local577_15 / 2.0F,
         UiTheme.field09680
      );
      settingRenderContext.field12415.put(currentNumberSetting, new float[]{local577_4, local577_6});
      settingRenderContext.method3551(
         local577_4,
         local577_11 - 2.5F * local577_9,
         local577_6,
         local577_12 + 5.0F * local577_9,
         floatArray,
         () -> settingRenderContext.numberSetting = currentNumberSetting,
         () -> currentNumberSetting.setValue(settingRenderContext.method3563(currentNumberSetting, currentNumberSetting.floatValue()))
      );
      return 13.3F * local577_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local579_2, float local579_3) {
      return 13.3F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local581_1) {
      return local581_1 instanceof NumberSetting;
   }
}
