package net.skycore.client.util;

import net.skycore.client.setting.BooleanSetting;

public final class BooleanSettingWidget extends DescriptiveHeightHelper {
   private final BooleanSetting bool;

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(264.67F, 28.0F);
   }

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float coordinate, float coordinate2, float color2, float color3, Object value, float color4) {
      int value2 = stringBufferHelper.settingTitleColor(color4);
      stringBufferHelper.clippedText(color, coordinate, coordinate2 - 20.0F * color2, 16.0F * color2, 11.0F, stringBufferHelper.localize(this.bool.getName()), value2, 500, color2, color3);
      stringBufferHelper.description(
         color, coordinate + 16.0F * color2, coordinate2 - 20.0F * color2, stringBufferHelper.localize(this.bool.getDescription()), stringBufferHelper.settingDescriptionColor(color4), color2, color3
      );
      float value3 = stringBufferHelper.motion(this.bool, this.bool.get() ? 1.0F : 0.0F);
      float value4 = stringBufferHelper.pulse(this.bool);
      float computedValue = (12.0F + value4) * color2;
      float computedValue2 = color + coordinate2 - computedValue;
      int color5 = stringBufferHelper.settingFieldColor(color4);
      int color6 = stringBufferHelper.fieldActiveColor();
      int color7 = MixColorHelper.mixColor(color6, stringBufferHelper.accentColor(), color4);
      float maximum = Math.max(value3, value4 * 0.35F);
      BeginFrameHelper.rect(computedValue2, coordinate, computedValue, computedValue)
         .color(MixColorHelper.alpha(MixColorHelper.mixColor(color5, color7, maximum), color3))
         .radius(4.0F * color2)
         .draw();
      if (value3 > 0.02F) {
         int value5 = MixColorHelper.mixColor(stringBufferHelper.tertiaryColor(), stringBufferHelper.primaryColor(), color4);
         stringBufferHelper.iconText(computedValue2 + 2.0F * color2, coordinate + 2.0F * color2, 8.0F, "n", value5, color2, color3 * value3);
      }

      stringBufferHelper.addHit(GuiActionType.BOOLEAN, this.bool, null, color, coordinate - 2.0F * color2, coordinate2, 32.0F * color2, value);
   }

   public BooleanSettingWidget(BooleanSetting bool2) {
      super(bool2);
      this.bool = bool2;
   }
}
