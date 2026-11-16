package deluxe.client.ui.setting;

import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.RectDrawData;
import deluxe.client.ui.UiTheme;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

public final class BooleanSettingRenderer implements SettingRenderer {
   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local3031_2, float local3031_3) {
      return 7.0F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local3032_1) {
      return local3032_1 instanceof BooleanSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local3033_2, float local3033_3, float local3033_4, float local3033_5, float local3033_6, float[] floatArray
   ) {
      BooleanSetting booleanSetting = (BooleanSetting)local3033_2;
      float local3033_9 = settingRenderContext.scale;
      float local3033_10 = settingRenderContext.method3544("cb:" + System.identityHashCode(booleanSetting), booleanSetting.isChecked());
      settingRenderContext.method3569(
         booleanSetting.name,
         local3033_4,
         local3033_5,
         6.0F * local3033_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local3033_3),
         local3033_6 - 15.0F * local3033_9
      );
      settingRenderContext.method3573(booleanSetting.description, local3033_4, local3033_5 - 1.0F * local3033_9, local3033_6 - 15.0F * local3033_9, 8.0F * local3033_9);
      float local3033_11 = 10.74F * local3033_9;
      float local3033_12 = 6.64F * local3033_9;
      float local3033_13 = local3033_4 + local3033_6 - local3033_11;
      float local3033_14 = local3033_5;
      int local3033_15 = UiTheme.method2881(ClickGuiStyle.field10797, ClickGuiStyle.field10799, local3033_3);
      int local3033_16 = UiTheme.method2881(ClickGuiStyle.field10801, ClickGuiStyle.field10780, local3033_3);
      Render2D.method4744(
         new RectDrawData(
               local3033_13,
               local3033_14,
               local3033_11,
               local3033_12,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               local3033_12 / 2.0F,
               Render2D.method4724(UiTheme.method2881(local3033_15, local3033_16, local3033_10))
            )
            .withSmoothness(0.5F)
      );
      float local3033_17 = 4.88F * local3033_9;
      float local3033_18 = local3033_13 + 0.93F * local3033_9 + local3033_10 * (local3033_11 - local3033_17 - 1.86F * local3033_9);
      Render2D.method4682(
         local3033_18 + local3033_17 / 2.0F,
         local3033_14 + local3033_12 / 2.0F,
         local3033_17 / 2.0F,
         0.5F,
         Render2D.method4724(
            UiTheme.method2881(
               UiTheme.method2881(ClickGuiStyle.field10798, ClickGuiStyle.field10800, local3033_3),
               UiTheme.field09680,
               local3033_10
            )
         )
      );
      settingRenderContext.method3554(
         local3033_13 - 2.0F * local3033_9,
         local3033_14 - 2.0F * local3033_9,
         local3033_11 + 4.0F * local3033_9,
         local3033_12 + 4.0F * local3033_9,
         floatArray,
         booleanSetting::toggleValue
      );
      return 7.0F * local3033_9;
   }
}
