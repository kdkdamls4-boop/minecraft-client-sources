package net.skycore.client.setting;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.skycore.client.setting.ActionSetting;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public class Setting<T> {
   private final String description;
   private T value;
   private final String name;
   private BooleanSupplier visibleWhen = () -> {
      return true;
   };
   private final T defaultValue;

   public void reset() {
      this.value = this.defaultValue;
   }

   public String getName() {
      return this.name;
   }

   public Setting(String text, T t) {
      this(text, "", (T)t);
   }

   public T getDefault() {
      return this.defaultValue;
   }

   public String getDescription() {
      if (!this.description.isBlank()) {
         return this.description;
      } else if (this instanceof BooleanSetting) {
         return "Toggle " + this.name + ".";
      } else if (this instanceof NumberSetting) {
         return "Adjust " + this.name + ".";
      } else if (this instanceof ModeSetting) {
         return "Choose " + this.name + ".";
      } else if (this instanceof MultiSelectSetting) {
         return "Choose one or more " + this.name + " options.";
      } else if (this instanceof StringSetting) {
         return "Enter " + this.name + ".";
      } else if (this instanceof ColorSetting) {
         return "Choose " + this.name + ".";
      } else if (this instanceof KeyBindSetting) {
         return "Press a key for " + this.name + ".";
      } else {
         return this instanceof ActionSetting ? "Run " + this.name + "." : "Configure this option.";
      }
   }

   public Setting(String name2, String description2, T t) {
      this.name = Objects.requireNonNull(name2);
      this.description = description2 == null ? "" : description2;
      this.defaultValue = (T)t;
      this.value = (T)t;
   }

   public boolean isVisible() {
      return this.visibleWhen.getAsBoolean();
   }

   public Setting<T> visibleWhen(BooleanSupplier booleanSupplier) {
      BooleanSupplier booleanSupplier2 = booleanSupplier == null ? () -> {
         return true;
      } : booleanSupplier;
      this.visibleWhen = booleanSupplier2;
      return this;
   }

   public T get() {
      return this.value;
   }

   public void set(T element) {
      this.value = (T)element;
   }
}
