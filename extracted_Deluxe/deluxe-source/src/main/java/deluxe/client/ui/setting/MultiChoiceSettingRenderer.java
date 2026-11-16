package deluxe.client.ui.setting;

import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;
import deluxe.client.setting.MultiChoiceSetting;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

public final class MultiChoiceSettingRenderer implements SettingRenderer {
   @Override
   public boolean supports(Setting<?> local1725_1) {
      return local1725_1 instanceof MultiChoiceSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local1727_2, float local1727_3, float local1727_4, float local1727_5, float local1727_6, float[] floatArray
   ) {
      MultiChoiceSetting multiChoiceSetting = (MultiChoiceSetting)local1727_2;
      float local1727_9 = settingRenderContext.scale;
      settingRenderContext.method3569(
         multiChoiceSetting.name,
         local1727_4,
         local1727_5,
         6.0F * local1727_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local1727_3),
         local1727_6 - 8.0F * local1727_9
      );
      settingRenderContext.method3573(multiChoiceSetting.description, local1727_4, local1727_5 - 1.0F * local1727_9, local1727_6 * 0.6F, 8.0F * local1727_9);
      FontText.method1397(
         FontFace.SF,
         "Выбрано: " + multiChoiceSetting.getSelectedCount(),
         local1727_4,
         local1727_5 + 7.5F * local1727_9,
         5.5F * local1727_9,
         UiTheme.method2881(ClickGuiStyle.field10792, ClickGuiStyle.field10793, local1727_3)
      );
      float local1727_10 = settingRenderContext.method3560("mspop:" + System.identityHashCode(multiChoiceSetting), settingRenderContext.field12427 == multiChoiceSetting ? 1.0F : 0.0F);
      float local1727_11 = local1727_4 + local1727_6 - 2.2F * local1727_9;
      float local1727_12 = local1727_5 + 6.0F * local1727_9;
      Render2D.method4658(
         "assets/deluxe/textures/gui/clickgui/chevright.png",
         local1727_11 - 2.75F * local1727_9,
         local1727_12 - 2.75F * local1727_9,
         5.5F * local1727_9,
         0.0F,
         180.0F - local1727_10 * 180.0F,
         local1727_11,
         local1727_12,
         UiTheme.method2881(ClickGuiStyle.field10787, ClickGuiStyle.field10788, local1727_3)
      );
      settingRenderContext.method3554(
         local1727_4,
         local1727_5 - 1.5F * local1727_9,
         local1727_6,
         15.0F * local1727_9,
         floatArray,
         () -> settingRenderContext.field12427 = settingRenderContext.field12427 == multiChoiceSetting ? null : multiChoiceSetting
      );
      if (local1727_10 > 0.01F) {
         SettingOptionPopup.method4314(
            settingRenderContext,
            local1727_4 + local1727_6,
            local1727_5,
            multiChoiceSetting.getOptions(),
            multiChoiceSetting::isIndexSelected,
            multiChoiceSetting::toggleIndex,
            false,
            local1727_10,
            settingRenderContext.field12427 == multiChoiceSetting
         );
      }

      return 13.5F * local1727_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local1728_2, float local1728_3) {
      return 13.5F * settingRenderContext.scale;
   }
}
