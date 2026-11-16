package net.skycore.client.util;

import net.skycore.client.setting.ActionSetting;

public final class ActionSettingWidget extends DescriptiveHeightHelper {
   private final ActionSetting button;

   @Override
   public float rawHeight() {
      float height;
      if (this.button.hasSeparateLabel()) {
         height = this.descriptiveHeight(187.88F, 28.0F);
      } else {
         height = 42.0F + 12.0F * Math.max(1, this.descriptionLines(284.67F));
      }

      return height;
   }

   public ActionSettingWidget(ActionSetting button2) {
      super(button2);
      this.button = button2;
   }

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float coordinate, float color2, float color3, float color4, Object value, float color5) {
      float value2 = stringBufferHelper.pulse(this.button);
      if (!this.button.hasSeparateLabel()) {
         String name = stringBufferHelper.localize(this.button.getName());
         float computedValue = 38.0F * color3;
         float computedValue2 = value2 * 1.0F * color3;
         BeginFrameHelper.rect(color, coordinate + computedValue2, color2, computedValue - computedValue2)
            .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.settingFieldColor(color5), stringBufferHelper.accentColor(), value2 * 0.2F * color5), color4))
            .radius(16.0F * color3)
            .draw();
         float width = InterBoldHelper.forWeight(500).getWidth(name, 11.0F * color3);
         stringBufferHelper.text(color + (color2 - width) * 0.5F, coordinate + 12.0F * color3, 11.0F, name, stringBufferHelper.settingValueColor(color5), 500, color3, color4);
         stringBufferHelper.description(color, coordinate + 44.0F * color3, color2, stringBufferHelper.localize(this.button.getDescription()), stringBufferHelper.settingDescriptionColor(color5), color3, color4);
         stringBufferHelper.addHit(GuiActionType.BUTTON, this.button, null, color, coordinate, color2, computedValue, value);
      } else {
         int value3 = stringBufferHelper.settingTitleColor(color5);
         stringBufferHelper.clippedText(color, coordinate, color2 * 0.66F, 16.0F * color3, 11.0F, stringBufferHelper.localize(this.button.getName()), value3, 500, color3, color4);
         stringBufferHelper.description(color, coordinate + 16.0F * color3, color2 * 0.66F, stringBufferHelper.localize(this.button.getDescription()), stringBufferHelper.settingDescriptionColor(color5), color3, color4);
         String actionLabel = stringBufferHelper.localize(this.button.getActionLabel());
         float width2 = InterBoldHelper.forWeight(500).getWidth(actionLabel, 10.0F * color3);
         float maximum = Math.max(54.0F * color3, width2 + 16.0F * color3);
         float computedValue3 = 22.0F * color3;
         float computedValue4 = color + color2 - maximum;
         float computedValue5 = coordinate - 4.0F * color3;
         BeginFrameHelper.rect(computedValue4, computedValue5 + value2 * color3, maximum, computedValue3 - value2 * color3)
            .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.settingFieldColor(color5), stringBufferHelper.accentColor(), value2 * 0.22F * color5), color4))
            .radius(8.0F * color3)
            .draw();
         stringBufferHelper.text(computedValue4 + (maximum - width2) * 0.5F, computedValue5 + 5.0F * color3, 10.0F, actionLabel, stringBufferHelper.settingValueColor(color5), 500, color3, color4);
         stringBufferHelper.addHit(GuiActionType.BUTTON, this.button, null, computedValue4, computedValue5, maximum, computedValue3, value);
      }
   }
}
