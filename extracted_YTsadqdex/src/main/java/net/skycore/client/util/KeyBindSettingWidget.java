package net.skycore.client.util;

import net.skycore.client.setting.KeyBindSetting;

import net.skycore.client.module.KeyBinds;

public final class KeyBindSettingWidget extends DescriptiveHeightHelper {
   private final KeyBindSetting keybind;

   public KeyBindSettingWidget(KeyBindSetting keybind2) {
      super(keybind2);
      this.keybind = keybind2;
   }

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float coordinate, float coordinate2, float color2, float color3, Object value, float color4) {
      int value2 = stringBufferHelper.settingTitleColor(color4);
      stringBufferHelper.clippedText(color, coordinate, coordinate2 * 0.66F, 16.0F * color2, 11.0F, stringBufferHelper.localize(this.keybind.getName()), value2, 500, color2, color3);
      stringBufferHelper.description(color, coordinate + 16.0F * color2, coordinate2 * 0.66F, stringBufferHelper.localize(this.keybind.getDescription()), stringBufferHelper.settingDescriptionColor(color4), color2, color3);
      int value3 = stringBufferHelper.bindingSetting() == this.keybind ? 1 : 0;
      int computedValue = this.keybind.get() != -1 ? 1 : 0;
      float color5 = stringBufferHelper.motion(this.keybind, value3 != 0 ? 1.0F : 0.0F);
      float value4 = stringBufferHelper.pulse(this.keybind);
      String text = value3 != 0 ? "..." : (computedValue != 0 ? KeyBinds.name(this.keybind.get()) : "n/a");
      int value5 = computedValue;
      float width = InterBoldHelper.forWeight(500).getWidth(text, 10.0F * color2);
      float computedValue2 = value5 != 0 ? 14.0F * color2 : 0.0F;
      float maximum = Math.max(27.0F * color2, width + computedValue2 + 12.0F * color2);
      float computedValue3 = 20.0F * color2;
      float computedValue4 = color + coordinate2 - maximum;
      float computedValue5 = coordinate - 4.0F * color2;
      int color6 = computedValue != 0 ? stringBufferHelper.fieldActiveColor() : stringBufferHelper.surfaceColor();
      int color7 = computedValue != 0 ? stringBufferHelper.accentColor() : stringBufferHelper.fieldColor();
      int color8 = MixColorHelper.mixColor(color6, color7, color4);
      if (color5 > 0.0F || value4 > 0.0F) {
         color8 = MixColorHelper.mixColor(color8, computedValue != 0 ? stringBufferHelper.accentColor() : stringBufferHelper.fieldActiveColor(), Math.max(color5, value4 * 0.22F));
      }

      BeginFrameHelper.rect(computedValue4, computedValue5, maximum, computedValue3).color(MixColorHelper.alpha(color8, color3)).radius(8.0F * color2).draw();
      float computedValue6 = width + computedValue2;
      float computedValue7 = computedValue4 + (maximum - computedValue6) * 0.5F;
      int color9 = computedValue != 0 ? stringBufferHelper.secondaryColor() : stringBufferHelper.tertiaryColor();
      int color10 = computedValue != 0 ? stringBufferHelper.primaryColor() : stringBufferHelper.secondaryColor();
      int color11 = MixColorHelper.mixColor(color9, color10, color4);
      if (color5 > 0.0F) {
         color11 = MixColorHelper.mixColor(color11, computedValue != 0 ? stringBufferHelper.primaryColor() : stringBufferHelper.secondaryColor(), color5);
      }

      stringBufferHelper.text(computedValue7, computedValue5 + 4.0F * color2, 10.0F, text, color11, 500, color2, color3);
      if (value5 != 0) {
         float computedValue8 = 10.0F * color2;
         float computedValue9 = computedValue7 + width + 4.0F * color2;
         BeginFrameHelper.text(computedValue9, computedValue5 + 5.0F * color2, computedValue8, "\ue004")
            .font(InterBoldHelper.csGuiIcons())
            .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.tertiaryColor(), stringBufferHelper.primaryColor(), Math.max(color4, color5)), color3))
            .draw();
      }

      stringBufferHelper.addHit(GuiActionType.KEYBIND, this.keybind, null, computedValue4, computedValue5, maximum, computedValue3, value);
   }

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(187.88F, 28.0F);
   }
}
