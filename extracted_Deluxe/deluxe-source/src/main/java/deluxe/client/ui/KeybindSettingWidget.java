package deluxe.client.ui;

import deluxe.client.render.FontFace;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.render.FontText;
import deluxe.client.ui.setting.SettingRenderer;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.render.Render2D;
import deluxe.client.setting.Setting;

public final class KeybindSettingWidget implements SettingRenderer {
   @Override
   public boolean supports(Setting<?> local6178_1) {
      return local6178_1 instanceof KeybindSetting;
   }

   @Override
   public float render(
      SettingRenderContext settingRenderContext, Setting<?> local6179_2, float local6179_3, float local6179_4, float local6179_5, float local6179_6, float[] floatArray
   ) {
      KeybindSetting currentKeybindSetting = (KeybindSetting)local6179_2;
      float local6179_9 = settingRenderContext.scale;
      String local6179_10 = settingRenderContext.keybindSetting == currentKeybindSetting ? "Нажми клавишу" : (currentKeybindSetting.isBound() ? currentKeybindSetting.getKeyName() : "—");
      float local6179_11 = FontText.method1396(FontFace.SF, local6179_10, 6.0F * local6179_9)
         + (settingRenderContext.keybindSetting == currentKeybindSetting ? 6.6F * local6179_9 : 0.0F);
      settingRenderContext.method3569(
         currentKeybindSetting.name,
         local6179_4,
         local6179_5,
         6.0F * local6179_9,
         UiTheme.method2881(ClickGuiStyle.field10789, ClickGuiStyle.field10790, local6179_3),
         local6179_6 - local6179_11 - 4.0F * local6179_9
      );
      settingRenderContext.method3573(currentKeybindSetting.description, local6179_4, local6179_5 - 1.0F * local6179_9, local6179_6 * 0.5F, 8.0F * local6179_9);
      if (settingRenderContext.keybindSetting == currentKeybindSetting) {
         String local6179_13 = "Нажми клавишу";
         float local6179_12 = FontText.method1396(FontFace.SF, local6179_13, 6.0F * local6179_9);
         FontText.method1397(
            FontFace.SF,
            local6179_13,
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.method2881(ClickGuiStyle.field10792, ClickGuiStyle.field10793, local6179_3)
         );
         Render2D.method4777(
            "assets/deluxe/textures/gui/clickgui/bindkey.png",
            local6179_4 + local6179_6 - local6179_12 - 6.6F * local6179_9,
            local6179_5 + 0.4F * local6179_9,
            5.1F * local6179_9,
            5.1F * local6179_9,
            0.0F,
            UiTheme.method2881(ClickGuiStyle.field10792, ClickGuiStyle.field10793, local6179_3)
         );
      } else if (currentKeybindSetting.isBound()) {
         float local6179_12 = FontText.method1396(FontFace.SF, currentKeybindSetting.getKeyName(), 6.0F * local6179_9);
         FontText.method1397(
            FontFace.SF,
            currentKeybindSetting.getKeyName(),
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.method2881(ClickGuiStyle.field10791, ClickGuiStyle.field10780, local6179_3)
         );
      } else {
         float local6179_12 = FontText.method1396(FontFace.SF, "—", 6.0F * local6179_9);
         FontText.method1397(
            FontFace.SF,
            "—",
            local6179_4 + local6179_6 - local6179_12,
            local6179_5,
            6.0F * local6179_9,
            UiTheme.method2881(ClickGuiStyle.field10792, ClickGuiStyle.field10793, local6179_3)
         );
      }

      settingRenderContext.method3551(
         local6179_4 + local6179_6 / 2.0F,
         local6179_5 - 1.5F * local6179_9,
         local6179_6 / 2.0F,
         9.0F * local6179_9,
         floatArray,
         () -> settingRenderContext.keybindSetting = currentKeybindSetting,
         () -> currentKeybindSetting.withKeyCode(-1)
      );
      return 7.0F * local6179_9;
   }

   @Override
   public float getHeight(SettingRenderContext settingRenderContext, Setting<?> local6180_2, float local6180_3) {
      return 7.0F * settingRenderContext.scale;
   }
}
