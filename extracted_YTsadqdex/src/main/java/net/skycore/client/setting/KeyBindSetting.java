package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public final class KeyBindSetting extends Setting<Integer> {
   public static final int UNBOUND = -1;

   public KeyBindSetting(String text, int keyCode) {
      super(text, keyCode);
   }
}
