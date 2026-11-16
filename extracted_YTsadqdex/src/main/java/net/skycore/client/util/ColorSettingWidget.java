package net.skycore.client.util;

import net.skycore.client.setting.ColorSetting;

public final class ColorSettingWidget extends DescriptiveHeightHelper {
   private final ColorSetting color;

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color2, float color3, float coordinate, float color4, float color5, Object value, float color6) {
      int color7 = stringBufferHelper.settingTitleColor(color6);
      stringBufferHelper.clippedText(color2, color3, coordinate - 95.0F * color4, 16.0F * color4, 11.0F, stringBufferHelper.localize(this.color.getName()), color7, 500, color4, color5);
      stringBufferHelper.description(
         color2, color3 + 16.0F * color4, coordinate - 95.0F * color4, stringBufferHelper.localize(this.color.getDescription()), stringBufferHelper.settingDescriptionColor(color6), color4, color5
      );
      float color8 = 87.0F * color4;
      float color9 = 22.0F * color4;
      float color10 = color2 + coordinate - color8;
      float color11 = color3 - 4.0F * color4;
      float value2 = stringBufferHelper.pulse(this.color);
      BeginFrameHelper.rect(color10, color11, color8, color9)
         .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.settingFieldColor(color6), stringBufferHelper.accentColor(), value2 * 0.16F * color6), color5))
         .radius(8.0F * color4)
         .draw();
      int color12 = this.color.get();
      String text = String.format("%06X", color12 & 16777215);
      stringBufferHelper.text(color10 + 6.0F * color4, color11 + 4.0F * color4, 11.0F, "#", stringBufferHelper.settingDescriptionColor(color6), 400, color4, color5);
      stringBufferHelper.text(color10 + 14.0F * color4, color11 + 4.0F * color4, 11.0F, text, stringBufferHelper.settingValueColor(color6), 500, color4, color5);
      float computedValue = (12.0F + value2) * color4;
      BeginFrameHelper.rect(color10 + color8 - (18.0F + value2 * 0.5F) * color4, color11 + (5.0F - value2 * 0.5F) * color4, computedValue, computedValue)
         .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.quaternaryColor(), color12, color6), color5))
         .radius(512.0F * color4)
         .draw();
      stringBufferHelper.addHit(GuiActionType.COLOR, this.color, null, color10, color11, color8, color9, value);
   }

   public ColorSettingWidget(ColorSetting color2) {
      super(color2);
      this.color = color2;
   }

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(189.67F, 28.0F);
   }
}
