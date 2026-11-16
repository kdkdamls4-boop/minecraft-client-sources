package deluxe.client.setting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiChoiceSetting extends Setting<MultiChoiceSetting> {
   private final List<String> options = new ArrayList<>();
   private final Set<String> selectedOptions = new LinkedHashSet<>();
   private int minimumSelection;
   public boolean expanded;

   private String findOption(String local4923_1) {
      for (String local4923_3 : this.options) {
         if (local4923_3.equalsIgnoreCase(local4923_1)) {
            return local4923_3;
         }
      }

      return null;
   }

   protected MultiChoiceSetting(String name, String local4924_2, String[] stringArray, String... stringArray2) {
      super(name, local4924_2);

      for (String local4924_8 : stringArray) {
         this.options.add(local4924_8);
      }

      for (String local4924_8 : stringArray2) {
         String local4924_9 = this.findOption(local4924_8);
         if (local4924_9 != null) {
            this.selectedOptions.add(local4924_9);
         }
      }
   }

   public boolean isSelected(String local4925_1) {
      String local4925_2 = this.findOption(local4925_1);
      return local4925_2 != null && this.selectedOptions.contains(local4925_2);
   }

   public MultiChoiceSetting(String name, String local4926_2) {
      super(name, local4926_2);
   }

   public MultiChoiceSetting withOptions(String... stringArray) {
      this.options.clear();

      for (String local4927_5 : stringArray) {
         this.options.add(local4927_5);
      }

      return this;
   }

   public int getSelectedCount() {
      return this.selectedOptions.size();
   }

   public List<String> getSelectedOptions() {
      return new ArrayList<>(this.selectedOptions);
   }

   public MultiChoiceSetting withSelection(String... stringArray) {
      this.selectedOptions.clear();

      for (String local4930_5 : stringArray) {
         String local4930_6 = this.findOption(local4930_5);
         if (local4930_6 != null) {
            this.selectedOptions.add(local4930_6);
         }
      }

      return this;
   }

   public void toggleOption(String local4931_1) {
      String local4931_2 = this.findOption(local4931_1);
      if (local4931_2 != null) {
         if (this.selectedOptions.contains(local4931_2)) {
            if (this.getSelectedCount() > this.minimumSelection) {
               this.selectedOptions.remove(local4931_2);
               this.notifyChanged();
            }
         } else {
            this.selectedOptions.add(local4931_2);
            this.notifyChanged();
         }
      }
   }

   public Set<String> getSelectedSet() {
      return new LinkedHashSet<>(this.selectedOptions);
   }

   public void toggleIndex(int local4933_1) {
      if (local4933_1 >= 0 && local4933_1 < this.options.size()) {
         this.toggleOption(this.options.get(local4933_1));
      }
   }

   public MultiChoiceSetting withMinimumSelection(int local4934_1) {
      this.minimumSelection = Math.max(0, local4934_1);
      return this;
   }

   public boolean isIndexSelected(int local4935_1) {
      return local4935_1 >= 0 && local4935_1 < this.options.size() && this.selectedOptions.contains(this.options.get(local4935_1));
   }

   public List<String> getOptions() {
      return this.options;
   }

   public String getSelectionSummary() {
      return this.getSelectedCount() + "/" + this.options.size();
   }

   public boolean matchesOption(String local4938_1) {
      return this.isSelected(local4938_1);
   }
}
