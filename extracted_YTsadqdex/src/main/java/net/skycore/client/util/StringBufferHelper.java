package net.skycore.client.util;

import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.StringSetting;

import net.skycore.client.setting.Setting;

public interface StringBufferHelper {
   public String stringBuffer();

   public KeyBindSetting bindingSetting();

   default void clippedText(float value, float value2, float value3, float value4, float value5, String text, int value6, int value7, float value8, float value9) {
      BeginFrameHelper.pushScissor(value, value2 - 2.0F * value8, Math.max(0.0F, value3), Math.max(0.0F, value4));

      try {
         this.text(value, value2, value5, text, value6, value7, value8, value9);
      } finally {
         BeginFrameHelper.popScissor();
      }
   }

   default int settingTrackColor(float tingTrackColor) {
      return MixColorHelper.mixColor(this.fieldActiveColor(), this.trackColor(), tingTrackColor);
   }

   public void triggerPulse(Object value);

   public int accentColor();

   public int quaternaryColor();

   public boolean transparentStyle();

   default int settingFieldColor(float tingFieldColor) {
      return MixColorHelper.mixColor(this.fieldColor(), this.fieldActiveColor(), tingFieldColor);
   }

   public int surfaceColor();

   default void description(float value, float value2, float width, String text, int value3, float value4, float value5) {
      if (text != null && !text.isBlank()) {
         String[] stringValues = text.trim().split("\\s+");
         StringBuilder stringBuilder = new StringBuilder();
         int value6 = 0;

         for (String text2 : stringValues) {
            String text3 = stringBuilder.length() == 0 ? text2 : stringBuilder + " " + text2;
            if (!(InterBoldHelper.forWeight(400).getWidth(text3, 10.0F * value4) <= width) && stringBuilder.length() != 0) {
               this.text(value, value2 + value6 * 12.0F * value4, 10.0F, stringBuilder.toString(), value3, 400, value4, value5);
               if (++value6 >= 2) {
                  return;
               }

               stringBuilder.setLength(0);
               stringBuilder.append(text2);
            } else {
               if (stringBuilder.length() > 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(text2);
            }
         }

         if (value6 < 2 && stringBuilder.length() > 0) {
            String text4 = stringBuilder.toString();

            while (InterBoldHelper.forWeight(400).getWidth(text4, 10.0F * value4) > width && text4.length() > 3) {
               text4 = text4.substring(0, text4.length() - 2);
            }

            if (!text4.equals(stringBuilder.toString())) {
               text4 = text4 + "…";
            }

            this.text(value, value2 + value6 * 12.0F * value4, 10.0F, text4, value3, 400, value4, value5);
         }
      }
   }

   public void toggleDropdown(Setting<?> setting);

   public int fieldColor();

   public boolean dropdownOpen(Setting<?> setting);

   public void addHit(GuiActionType guiActionType, Object value, String text, float value2, float value3, float value4, float value5, Object value6);

   public StringSetting editingString();

   public int primaryColor();

   public int fieldActiveColor();

   public int trackColor();

   public int secondaryColor();

   default int settingSecondaryValueColor(float tingSecondaryValueColor) {
      return MixColorHelper.mixColor(this.quaternaryColor(), this.secondaryColor(), tingSecondaryValueColor);
   }

   default void iconText(float value, float value2, float value3, String text, int color, float value4, float color2) {
      BeginFrameHelper.text(value, value2 - 5.5F * value4, value3 * value4, text).font(InterBoldHelper.clickGuiIcons()).color(MixColorHelper.alpha(color, color2)).draw();
   }

   public float pulse(Object value);

   default void text(float value, float value2, float value3, String text, int color, int value4, float value5, float color2) {
      BeginFrameHelper.text(value, value2, value3 * value5, text).font(InterBoldHelper.forWeight(value4)).color(MixColorHelper.alpha(color, color2)).draw();
   }

   public float motionNamed(String text, float value);

   public int elevatedColor();

   public String localize(String text);

   default int settingValueColor(float tingValueColor) {
      return MixColorHelper.mixColor(this.tertiaryColor(), this.primaryColor(), tingValueColor);
   }

   default int settingTitleColor(float tingTitleColor) {
      return MixColorHelper.mixColor(this.secondaryColor(), this.primaryColor(), tingTitleColor);
   }

   public int tertiaryColor();

   public float motion(Object value, float value2);

   default int settingDescriptionColor(float tingDescriptionColor) {
      return MixColorHelper.mixColor(this.tertiaryColor(), this.secondaryColor(), tingDescriptionColor);
   }
}
