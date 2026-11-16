package net.skycore.client.util;

import net.skycore.client.setting.MultiSelectSetting;

import java.util.List;
import java.util.Set;

public final class MultiChoiceSetting extends MultiSelectSetting {
   public MultiChoiceSetting(String text, List<String> strings, Set<String> strings2) {
      super(text, strings, strings2);
   }
}
