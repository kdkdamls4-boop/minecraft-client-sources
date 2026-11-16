package deluxe.client.ui.setting;

import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;

import deluxe.client.setting.ActionSetting;
import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

public final class ActionSettingRenderer implements SettingRenderer {
   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local3590_2, float local3590_3, float local3590_4, float local3590_5, float local3590_6, float[] floatArray
   ) {
      ActionSetting actionSetting = (ActionSetting)local3590_2;
      float local3590_9 = settingRenderContext.scale;
      float local3590_10 = Math.max(
         18.0F * local3590_9, FontText.method1396(FontFace.SF, actionSetting.getButtonLabel(), 6.0F * local3590_9) + 9.5F * local3590_9
      );
      settingRenderContext.method3569(
         actionSetting.name,
         local3590_4,
         local3590_5 + 1.3F * local3590_9,
         6.0F * local3590_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local3590_3),
         local3590_6 - local3590_10 - 4.0F * local3590_9
      );
      settingRenderContext.method3573(actionSetting.description, local3590_4, local3590_5, local3590_6 * 0.5F, 9.6F * local3590_9);
      float local3590_11 = local3590_4 + local3590_6 - local3590_10;
      float local3590_12 = settingRenderContext.method3544(
         "btn:" + System.identityHashCode(actionSetting),
         UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local3590_11, local3590_5, local3590_10, 9.57F * local3590_9)
      );
      Render2D.method4715(
         local3590_11,
         local3590_5,
         local3590_10,
         9.57F * local3590_9,
         2.5F * local3590_9,
         UiTheme.method2881(
            UiTheme.method2877(ClickGuiStyle.field10780, 0.55F + 0.45F * local3590_3),
            UiTheme.method2881(ClickGuiStyle.field10780, UiTheme.field09680, 0.12F),
            local3590_12
         )
      );
      Render2D.method4769(local3590_11, local3590_5, local3590_10, 9.57F * local3590_9, 2.5F * local3590_9, 0.5F, ClickGuiStyle.field10817);
      FontText.method1397(
         FontFace.SF,
         actionSetting.getButtonLabel(),
         local3590_11 + (local3590_10 - FontText.method1396(FontFace.SF, actionSetting.getButtonLabel(), 6.0F * local3590_9)) / 2.0F,
         local3590_5 + 1.8F * local3590_9,
         6.0F * local3590_9,
         UiTheme.field09680
      );
      settingRenderContext.method3554(local3590_11, local3590_5, local3590_10, 9.57F * local3590_9, floatArray, actionSetting::runAction);
      return 9.6F * local3590_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local3591_2, float local3591_3) {
      return 9.6F * settingRenderContext.scale;
   }

   @Override
   public boolean supports(Setting<?> local3592_1) {
      return local3592_1 instanceof ActionSetting;
   }
}
