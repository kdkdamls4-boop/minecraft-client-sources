package net.skycore.client.util;

import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;

import java.util.List;
import net.skycore.client.setting.Setting;

public final class TextDescriptionLayout {
   public static void render(
      StringBufferHelper stringBufferHelper,
      ModeSetting modeSetting,
      MultiSelectSetting multiSelectSetting,
      float coordinate,
      float coordinate2,
      float coordinate3,
      float color,
      float color2,
      Object value,
      float color3
   ) {
      var modes = modeSetting != null ? modeSetting.getModes() : multiSelectSetting.getModes();
      float index = coordinate;
      float index2 = coordinate2;

      for (String text : modes) {
         float width = InterBoldHelper.forWeight(500).getWidth(text, 10.0F * color);
         float minimum = Math.min(coordinate3, width + 12.0F * color);
         if (index + minimum > coordinate + coordinate3 + 0.1F) {
            index = coordinate;
            index2 += 24.0F * color;
         }

         boolean selected = modeSetting != null ? modeSetting.is(text) : multiSelectSetting.isSelected(text);
         Setting setting = modeSetting != null ? modeSetting : multiSelectSetting;
         float color4 = stringBufferHelper.motionNamed("chip:" + System.identityHashCode(setting) + ":" + text, selected ? 1.0F : 0.0F);
         int color5 = MixColorHelper.mixColor(stringBufferHelper.fieldActiveColor(), stringBufferHelper.accentColor(), color3);
         int color6 = MixColorHelper.mixColor(stringBufferHelper.surfaceColor(), stringBufferHelper.fieldColor(), color3);
         BeginFrameHelper.rect(index, index2, minimum, 20.0F * color)
            .color(MixColorHelper.alpha(MixColorHelper.mixColor(color6, color5, color4), color2))
            .border(Math.max(0.5F, color), MixColorHelper.alpha(318767103, color2))
            .radius(4.0F * color)
            .draw();
         int color7 = MixColorHelper.mixColor(stringBufferHelper.quaternaryColor(), stringBufferHelper.tertiaryColor(), color3);
         int color8 = MixColorHelper.mixColor(stringBufferHelper.tertiaryColor(), stringBufferHelper.primaryColor(), color3);
         float color9 = index + (minimum - width) * 0.5F;
         stringBufferHelper.text(color9, index2 + 4.0F * color, 10.0F, stringBufferHelper.localize(text), MixColorHelper.mixColor(color7, color8, color4), 500, color, color2);
         stringBufferHelper.addHit(
            modeSetting != null ? GuiActionType.MODE : GuiActionType.MULTI, modeSetting != null ? modeSetting : multiSelectSetting, text, index, index2, minimum, 20.0F * color, value
         );
         index += minimum + 4.0F * color;
      }
   }

   private TextDescriptionLayout() {
   }

   public static float rawHeight(List<String> strings) {
      float index = 0.0F;
      int value = 1;

      for (String text : strings) {
         float width = InterBoldHelper.forWeight(500).getWidth(text, 10.0F) + 12.0F;
         if (index > 0.0F && index + 4.0F + width > 176.0F) {
            value++;
            index = width;
         } else {
            index += (index > 0.0F ? 4.0F : 0.0F) + width;
         }
      }

      return 22.0F + value * 20.0F + Math.max(0, value - 1) * 4.0F;
   }
}
