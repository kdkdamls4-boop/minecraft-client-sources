package net.skycore.client.util;

import net.skycore.client.setting.StringSetting;

public final class StringSettingWidget extends DescriptiveHeightHelper {
   private final StringSetting string;

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float color2, float coordinate, float color3, float color4, Object value, float color5) {
      int value2 = stringBufferHelper.settingTitleColor(color5);
      stringBufferHelper.clippedText(color, color2, (coordinate - 8.0F * color3) * 0.5F, 16.0F * color3, 11.0F, stringBufferHelper.localize(this.string.getName()), value2, 500, color3, color4);
      stringBufferHelper.description(
         color, color2 + 16.0F * color3, (coordinate - 8.0F * color3) * 0.5F, stringBufferHelper.localize(this.string.getDescription()), stringBufferHelper.settingDescriptionColor(color5), color3, color4
      );
      float computedValue = (coordinate - 8.0F * color3) * 0.5F;
      float computedValue2 = 28.0F * color3;
      float computedValue3 = color + coordinate - computedValue;
      int value3 = stringBufferHelper.editingString() == this.string ? 1 : 0;
      float value4 = stringBufferHelper.motion(this.string, value3 != 0 ? 1.0F : 0.0F);
      float value5 = stringBufferHelper.pulse(this.string);
      BeginFrameHelper.rect(computedValue3, color2 - 7.0F * color3, computedValue, computedValue2)
         .color(
            MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.settingFieldColor(color5), stringBufferHelper.fieldActiveColor(), Math.max(value4, value5 * 0.3F) * color5), color4)
         )
         .radius(12.0F * color3)
         .draw();
      String text = value3 != 0 ? stringBufferHelper.stringBuffer() : this.string.get();
      BeginFrameHelper.pushScissor(computedValue3 + 8.0F * color3, color2 - 7.0F * color3, computedValue - 16.0F * color3, computedValue2);

      try {
         stringBufferHelper.text(
            computedValue3 + (12.0F + value4 * 2.0F) * color3,
            color2,
            10.0F,
            text.isEmpty() ? "Input" : text,
            text.isEmpty() ? stringBufferHelper.settingDescriptionColor(color5) : stringBufferHelper.settingValueColor(color5),
            500,
            color3,
            color4
         );
      } finally {
         BeginFrameHelper.popScissor();
      }

      stringBufferHelper.addHit(GuiActionType.STRING, this.string, null, computedValue3, color2 - 7.0F * color3, computedValue, computedValue2, value);
   }

   public StringSettingWidget(StringSetting string2) {
      super(string2);
      this.string = string2;
   }

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(138.33F, 28.0F);
   }
}
