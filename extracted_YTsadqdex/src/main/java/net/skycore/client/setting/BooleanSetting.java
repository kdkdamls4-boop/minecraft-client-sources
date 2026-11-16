package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public final class BooleanSetting extends Setting<Boolean> {
   public BooleanSetting(String text, boolean condition) {
      super(text, condition);
   }

   public void toggle() {
      Boolean get = this.get();
      Boolean condition = !get;
      this.set(condition);
   }

   public BooleanSetting(String text, String text2, boolean condition) {
      super(text, text2, condition);
   }
}
