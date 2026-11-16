package net.skycore.client.setting;

import java.util.Arrays;
import java.util.List;
import net.skycore.client.setting.Setting;

public final class ModeSetting extends Setting<String> {
   private final List<String> modes;

   public List<String> getModes() {
      return this.modes;
   }

   public void cycle(int value) {
      int value2 = 0;
      if (value != 0) {
         var modes2 = this.modes;
         int value3 = modes2.indexOf(this.get());
         value2 = value3 + Integer.signum(value);
         int computedValue = Math.floorMod(value2, this.modes.size());
         String text = this.modes.get(computedValue);
         super.set(text);
      }
   }

   private String find(String text) {
      if (text == null) {
         return null;
      }

      for (String text2 : this.modes) {
         if (text2.equalsIgnoreCase(text)) {
            return text2;
         }
      }

      if (text.equalsIgnoreCase("Siyanie") || text.equalsIgnoreCase("Сияние")) {
         for (String text3 : this.modes) {
            if (text3.equalsIgnoreCase("Aurora")) {
               return text3;
            }
         }
      }

      return null;
   }

   public ModeSetting(String text, String text2, String... stringValues) {
      super(text, text2);
      if (stringValues != null && stringValues.length != 0) {
         this.modes = List.copyOf(Arrays.asList(stringValues));
         if (this.modes.isEmpty()) {
            throw new IllegalArgumentException("Mode list cannot be empty");
         }

         if (!this.contains(text2)) {
            throw new IllegalArgumentException("Default mode is not declared: " + text2);
         }

         super.set(this.find(text2));
      } else {
         throw new IllegalArgumentException("Mode list cannot be empty");
      }
   }

   private boolean contains(String text) {
      return this.find(text) != null;
   }

   public void set(String text) {
      String text2 = this.find(text);
      if (text2 != null) {
         super.set(text2);
      }
   }

   public boolean is(String text) {
      if (text != null) {
         String text2 = this.get();
         if (text2.equalsIgnoreCase(text)) {
            return true;
         }
      }

      return false;
   }
}
