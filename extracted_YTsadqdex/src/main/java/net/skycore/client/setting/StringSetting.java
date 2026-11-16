package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public final class StringSetting extends Setting<String> {
   private final int maxLength;

   public int getMaxLength() {
      return this.maxLength;
   }

   public StringSetting(String text, String text2, int value) {
      super(text, text2 == null ? "" : text2);
      this.maxLength = Math.max(1, value);
      this.set(text2);
   }

   public void set(String text) {
      String text2 = text == null ? "" : text;
      String text3;
      if (text2.length() > this.maxLength) {
         text3 = text2.substring(0, this.maxLength);
      } else {
         text3 = text2;
      }

      super.set(text3);
   }
}
