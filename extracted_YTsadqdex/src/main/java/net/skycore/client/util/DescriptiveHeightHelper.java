package net.skycore.client.util;

import net.skycore.client.setting.Setting;

public abstract class DescriptiveHeightHelper {
   protected final Setting<?> setting;

   protected DescriptiveHeightHelper(Setting<?> setting2) {
      this.setting = setting2;
   }

   protected final float descriptiveHeight(float value, float value2) {
      int value3 = this.descriptionLines(value);
      float computedValue = value3 <= 0 ? 14.0F : 16.0F + 12.0F * value3;
      return Math.max(value2, computedValue);
   }

   public abstract float rawHeight();

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   protected final int descriptionLines(float width) {
      String description = SelectAndSaveHelper.text(this.setting.getDescription());
      if (description != null && !description.isBlank()) {
         String[] stringValues = description.trim().split("\\s+");
         int value = 1;
         StringBuilder stringBuilder = new StringBuilder();

         for (String text : stringValues) {
            String text2 = stringBuilder.length() == 0 ? text : stringBuilder + " " + text;
            if (!(InterBoldHelper.forWeight(400).getWidth(text2, 10.0F) <= width) && stringBuilder.length() != 0) {
               if (value < 2) {
                  value++;
                  stringBuilder.setLength(0);
                  stringBuilder.append(text);
               }
            } else {
               if (stringBuilder.length() > 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(text);
            }
         }

         return Math.min(value, 2);
      } else {
         return 0;
      }
   }

   public Setting<?> setting() {
      return this.setting;
   }

   public final float height(float height) {
      return this.rawHeight() * height;
   }

   public abstract void render(StringBufferHelper stringBufferHelper, float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, Object value, float coordinate6);
}
