package deluxe.client.setting;

import deluxe.client.setting.Setting;

import java.awt.Color;

public final class ColorSetting extends Setting<ColorSetting> {
   private int argb;

   private float[] getHsb() {
      Color color = new Color(this.argb, true);
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
   }

   public int getArgb() {
      return this.argb;
   }

   public int getOpaqueRgb() {
      return 0xFF000000 | this.argb & 16777215;
   }

   public float getSaturation() {
      return this.getHsb()[1];
   }

   public ColorSetting withHsb(float local5434_1, float local5434_2, float local5434_3) {
      int alpha = this.getAlpha();
      int local5434_5 = Color.HSBtoRGB(local5434_1, local5434_2, local5434_3) & 16777215;
      return this.setValue(alpha << 24 | local5434_5);
   }

   public float getBrightness() {
      return this.getHsb()[2];
   }

   public ColorSetting(String name, String local5436_2, Color color) {
      super(name, local5436_2);
      this.argb = color.getRGB();
   }

   public ColorSetting(String name, String local5437_2) {
      this(name, local5437_2, new Color(255, 255, 255, 255));
   }

   public ColorSetting withArgb(int newArgb) {
      return this.setValue(newArgb);
   }

   public int getValue() {
      return this.argb;
   }

   public ColorSetting withAlpha(float local5440_1) {
      int local5440_2 = Math.round(Math.max(0.0F, Math.min(1.0F, local5440_1)) * 255.0F);
      return this.setValue(local5440_2 << 24 | this.argb & 16777215);
   }

   public int getAlpha() {
      return this.argb >> 24 & 0xFF;
   }

   public ColorSetting setValue(int newArgb) {
      if (newArgb != this.argb) {
         this.argb = newArgb;
         this.notifyChanged();
      }

      return this;
   }

   public float getHue() {
      return this.getHsb()[0];
   }
}
