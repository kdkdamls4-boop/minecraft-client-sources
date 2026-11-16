package net.skycore.client.setting;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.skycore.client.setting.Setting;

public class MultiSelectSetting extends Setting<Set<String>> {
   private final List<String> modes;

   private static boolean containsIgnoreCase(Set<String> strings, String text) {
      Iterator iterator = strings.iterator();

      while (iterator.hasNext()) {
         String element = (String)iterator.next();
         if (element != null) {
            if (element.equalsIgnoreCase(text)) {
               return true;
            }
         }
      }

      return false;
   }

   public void toggle(String text) {
      String text2 = this.find(text);
      if (text2 != null) {
         LinkedHashSet<String> strings = new LinkedHashSet<>(this.get());
         if (this.isSelected(text2)) {
            strings.removeIf(text3 -> {
               return text3.equalsIgnoreCase(text2);
            });
         } else {
            strings.add(text2);
         }

         this.set(strings);
      }
   }

   private static Set<String> sanitize(List<String> strings, Set<String> strings2) {
      if (strings == null) {
         return Set.of();
      }

      var strings3 = List.copyOf(strings);
      LinkedHashSet entries = new LinkedHashSet();
      if (strings2 != null) {

         for (String text : strings3) {
            if (containsIgnoreCase(strings2, text)) {
               entries.add(text);
            }
         }
      }

      return Set.copyOf(entries);
   }

   public void set(Set<String> element) {
      LinkedHashSet entries = new LinkedHashSet();
      if (element != null) {

         for (String text : this.modes) {
            if (containsIgnoreCase(element, text)) {
               entries.add(text);
            }
         }
      }

      var entries2 = Set.copyOf(entries);
      super.set(entries2);
   }

   public List<String> getModes() {
      return this.modes;
   }

   public boolean isSelected(String text) {
      if (text == null) {
         return false;
      }

      Iterator iterator = this.get().iterator();

      String text2;
      do {
         if (!iterator.hasNext()) {
            return false;
         }

         text2 = (String)iterator.next();
      } while (!text2.equalsIgnoreCase(text));

      return true;
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

      return null;
   }

   public MultiSelectSetting(String text, List<String> strings, Set<String> strings2) {
      super(text, sanitize(strings, strings2));
      if (strings == null) {
         throw new IllegalArgumentException("Mode list cannot be null");
      }

      this.modes = List.copyOf(strings);
      this.set(strings2);
   }
}
