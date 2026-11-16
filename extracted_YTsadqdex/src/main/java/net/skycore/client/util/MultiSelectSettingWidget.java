package net.skycore.client.util;

import net.skycore.client.setting.MultiSelectSetting;

import java.util.StringJoiner;

public final class MultiSelectSettingWidget extends DescriptiveHeightHelper {
   private final MultiSelectSetting multi;

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float coordinate, float coordinate2, float color2, float color3, Object value, float color4) {
      boolean dropdownOpen = stringBufferHelper.dropdownOpen(this.multi);
      float value2 = stringBufferHelper.motion(this.multi, dropdownOpen ? 1.0F : 0.0F);
      float value3 = stringBufferHelper.pulse(this.multi);
      int value4 = stringBufferHelper.settingTitleColor(color4);
      stringBufferHelper.clippedText(color, coordinate, (coordinate2 - 8.0F * color2) * 0.5F, 16.0F * color2, 11.0F, stringBufferHelper.localize(this.multi.getName()), value4, 500, color2, color3);
      stringBufferHelper.description(
         color, coordinate + 16.0F * color2, (coordinate2 - 8.0F * color2) * 0.5F, stringBufferHelper.localize(this.multi.getDescription()), stringBufferHelper.settingDescriptionColor(color4), color2, color3
      );
      float computedValue = (coordinate2 - 8.0F * color2) * 0.5F;
      float computedValue2 = 28.0F * color2;
      float computedValue3 = color + coordinate2 - computedValue;
      float computedValue4 = coordinate - 7.0F * color2;
      int color5 = stringBufferHelper.settingFieldColor(color4);
      int color6 = stringBufferHelper.fieldActiveColor();
      int color7 = MixColorHelper.mixColor(color5, color6, value2 * color4);
      color7 = MixColorHelper.mixColor(color7, stringBufferHelper.accentColor(), value3 * 0.18F * color4);
      BeginFrameHelper.rect(computedValue3, computedValue4, computedValue, computedValue2).color(MixColorHelper.alpha(color7, color3)).radius(8.0F * color2).draw();
      StringJoiner stringJoiner = new StringJoiner(", ");

      for (String text : this.multi.get()) {
         stringJoiner.add(stringBufferHelper.localize(text));
      }

      String text2 = stringJoiner.length() == 0 ? stringBufferHelper.localize("None") : stringJoiner.toString();
      if (text2.length() > 18) {
         text2 = stringBufferHelper.localize(this.multi.get().size() + " selected");
      }

      BeginFrameHelper.pushScissor(computedValue3 + 8.0F * color2, computedValue4, computedValue - 32.0F * color2, computedValue2);

      try {
         stringBufferHelper.text(computedValue3 + 10.0F * color2, computedValue4 + 8.0F * color2, 10.0F, text2, stringBufferHelper.settingValueColor(color4), 500, color2, color3);
      } finally {
         BeginFrameHelper.popScissor();
      }

      stringBufferHelper.iconText(
         computedValue3 + computedValue - 18.0F * color2,
         computedValue4 + (10.0F + 1.5F * value2) * color2,
         9.0F,
         "j",
         MixColorHelper.mixColor(stringBufferHelper.quaternaryColor(), stringBufferHelper.tertiaryColor(), color4),
         color2,
         color3
      );
      stringBufferHelper.addHit(GuiActionType.MULTI, this.multi, "__toggle__", computedValue3, computedValue4, computedValue, computedValue2, value);
      if (!(value2 <= 0.001F)) {
         float computedValue5 = computedValue4 + computedValue2 + 4.0F * color2;
         float modes = Math.max(0.0F, this.multi.getModes().size() * 24.0F - 2.0F) * color2 * value2;
         BeginFrameHelper.rect(computedValue3, computedValue5, computedValue, modes).color(MixColorHelper.alpha(stringBufferHelper.settingFieldColor(color4), color3 * value2)).radius(8.0F * color2).draw();
         int value5 = 0;

         for (String text3 : this.multi.getModes()) {
            float computedValue6 = computedValue5 + value5 * 24.0F * color2 * value2;
            boolean selected = this.multi.isSelected(text3);
            float color8 = stringBufferHelper.motionNamed("multi:" + System.identityHashCode(this.multi) + ":" + text3, selected ? 1.0F : 0.0F);
            stringBufferHelper.text(
               computedValue3 + 10.0F * color2,
               computedValue6 + 5.0F * color2,
               10.0F,
               stringBufferHelper.localize(text3),
               MixColorHelper.mixColor(stringBufferHelper.settingDescriptionColor(color4), stringBufferHelper.primaryColor(), color8),
               500,
               color2,
               color3 * value2
            );
            if (color8 > 0.02F) {
               stringBufferHelper.iconText(computedValue3 + computedValue - 18.0F * color2, computedValue6 + 8.0F * color2, 8.0F, "n", stringBufferHelper.primaryColor(), color2, color3 * value2 * color8 * color4);
            }

            if (value2 > 0.92F) {
               stringBufferHelper.addHit(GuiActionType.MULTI, this.multi, text3, computedValue3, computedValue6, computedValue, 22.0F * color2, value);
            }

            value5++;
         }
      }
   }

   public MultiSelectSettingWidget(MultiSelectSetting multi2) {
      super(multi2);
      this.multi = multi2;
   }

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(138.33F, 28.0F);
   }
}
