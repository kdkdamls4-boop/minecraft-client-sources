package net.skycore.client.util;

import net.skycore.client.setting.NumberSetting;

import java.util.Locale;

public final class FormatNumberHelper extends DescriptiveHeightHelper {
   private final NumberSetting number;

   private static String formatNumber(double value) {
      if (Math.abs(value - Math.rint(value)) < 1.0E-5) {
         long roundedValue = Math.round(value);
         return Long.toString(roundedValue);
      } else {
         Locale rOOT = Locale.ROOT;
         Object[] objectValues = new Object[1];
         objectValues[0] = value;
         String text = String.format(rOOT, "%.2f", objectValues);
         String text2 = text.replaceAll("0+$", "");
         return text2.replaceAll("\\.$", "");
      }
   }

   public FormatNumberHelper(NumberSetting number2) {
      super(number2);
      this.number = number2;
   }

   @Override
   public void render(StringBufferHelper stringBufferHelper, float color, float color2, float coordinate, float color3, float color4, Object value, float color5) {
      float color6 = (coordinate - 8.0F * color3) * 0.5F;
      int value2 = stringBufferHelper.settingTitleColor(color5);
      stringBufferHelper.clippedText(color, color2, color6, 16.0F * color3, 11.0F, stringBufferHelper.localize(this.number.getName()), value2, 500, color3, color4);
      stringBufferHelper.description(color, color2 + 16.0F * color3, color6, stringBufferHelper.localize(this.number.getDescription()), stringBufferHelper.settingDescriptionColor(color5), color3, color4);
      float computedValue = color + coordinate - color6;
      String text = formatNumber(this.number.get());
      String suffix = this.number.getSuffix();
      float width = InterBoldHelper.forWeight(500).getWidth(suffix, 11.0F * color3);
      float width2 = InterBoldHelper.forWeight(500).getWidth(text, 11.0F * color3);
      stringBufferHelper.text(computedValue + color6 - width2 - width, color2, 11.0F, text, stringBufferHelper.settingValueColor(color5), 500, color3, color4);
      if (!suffix.isEmpty()) {
         stringBufferHelper.text(computedValue + color6 - width, color2, 11.0F, suffix, stringBufferHelper.settingDescriptionColor(color5), 500, color3, color4);
      }

      float value3 = stringBufferHelper.motion(this.number, (float)this.number.ratio());
      float value4 = stringBufferHelper.pulse(this.number);
      float computedValue2 = color2 + 18.0F * color3;
      float computedValue3 = 6.0F * color3;
      BeginFrameHelper.rect(computedValue, computedValue2, color6, computedValue3).color(MixColorHelper.alpha(stringBufferHelper.settingTrackColor(color5), color4)).radius(512.0F * color3).draw();
      float computedValue4 = (4.0F + 2.0F * value4) * color3;
      float computedValue5 = computedValue + computedValue4 * 0.5F + (color6 - computedValue4) * value3;
      float maximum = Math.max(0.0F, computedValue5 - computedValue);
      if (maximum > 0.0F) {
         BeginFrameHelper.rect(computedValue, computedValue2, maximum + 1.3F, computedValue3)
            .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.fieldActiveColor(), stringBufferHelper.accentColor(), color5), color4))
            .radius(512.0F * color3)
            .draw();
      }

      BeginFrameHelper.rect(computedValue5 - computedValue4 * 0.5F, computedValue2 + (computedValue3 - computedValue4) * 0.5F, computedValue4, computedValue4)
         .color(MixColorHelper.alpha(MixColorHelper.mixColor(stringBufferHelper.quaternaryColor(), stringBufferHelper.primaryColor(), color5), color4))
         .radius(512.0F * color3)
         .draw();
      stringBufferHelper.addHit(GuiActionType.NUMBER, this.number, null, computedValue, computedValue2 - 7.0F * color3, color6, 20.0F * color3, value);
   }

   @Override
   public float rawHeight() {
      return this.descriptiveHeight(138.33F, 28.0F);
   }
}
