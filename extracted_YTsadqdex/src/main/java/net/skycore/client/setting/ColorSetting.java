package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public final class ColorSetting extends Setting<Integer> {
   private final boolean alpha;

   public void set(Integer value) {
      int alpha2 = -1;
      alpha2 = (byte)1;
      int value2 = value == null ? this.getDefault() : value;
      alpha2 = this.alpha ? value2 : value2 | 0xFF000000;
      super.set(alpha2);
   }

   public ColorSetting(String text, int value, boolean alpha2) {
      super(text, value);
      this.alpha = alpha2;
      this.set(value);
   }

   public boolean hasAlpha() {
      return this.alpha;
   }
}
