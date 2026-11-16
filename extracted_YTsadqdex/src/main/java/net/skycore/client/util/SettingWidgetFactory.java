package net.skycore.client.util;

import net.skycore.client.setting.ActionSetting;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.skycore.client.setting.Setting;

public final class SettingWidgetFactory {
   public static DescriptiveHeightHelper create(Setting<?> setting) {
      if (setting instanceof ActionSetting actionSetting) {
         return new ActionSettingWidget(actionSetting);
      } else if (setting instanceof BooleanSetting booleanSetting) {
         return new BooleanSettingWidget(booleanSetting);
      } else if (setting instanceof NumberSetting numberSetting) {
         return new FormatNumberHelper(numberSetting);
      } else if (setting instanceof ModeSetting modeSetting) {
         return new ModeSettingWidget(modeSetting);
      } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
         return new MultiSelectSettingWidget(multiSelectSetting);
      } else if (setting instanceof StringSetting stringSetting) {
         return new StringSettingWidget(stringSetting);
      } else if (setting instanceof ColorSetting colorSetting) {
         return new ColorSettingWidget(colorSetting);
      } else if (setting instanceof KeyBindSetting keyBindSetting) {
         return new KeyBindSettingWidget(keyBindSetting);
      } else {
         throw new IllegalArgumentException("Unsupported setting type: " + setting.getClass().getName());
      }
   }

   private SettingWidgetFactory() {
   }

   public static List<DescriptiveHeightHelper> create(List<Setting<?>> element) {
      int size = element.size();
      ArrayList elements = new ArrayList(size);
      Iterator iterator = element.iterator();

      while (iterator.hasNext()) {
         Setting element2 = (Setting)iterator.next();
         DescriptiveHeightHelper descriptiveHeightHelper = create(element2);
         elements.add(descriptiveHeightHelper);
      }

      return elements;
   }
}
