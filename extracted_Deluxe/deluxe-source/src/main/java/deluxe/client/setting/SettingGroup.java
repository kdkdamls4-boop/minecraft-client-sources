package deluxe.client.setting;

import deluxe.client.module.Module;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SettingGroup {
   public final String field00233;
   public final Object field00234;
   public final List<Setting<?>> field00235;
   private static final Map<Module, SettingGroup.Nested0351> field00236 = new IdentityHashMap<>();

   private SettingGroup(String local614_1, Object local614_2, List<Setting<?>> local614_3) {
      this.field00233 = local614_1;
      this.field00234 = local614_2;
      this.field00235 = local614_3;
   }

   public boolean method0501() {
      return this.field00233 != null;
   }

   public static List<SettingGroup> method0502(Module module) {
      List<Setting<?>> settings = module.getSettings();
      SettingGroup.Nested0351 local617_2 = field00236.get(module);
      int local617_3 = settings.size();
      Setting<?> local617_4 = local617_3 == 0 ? null : settings.get(local617_3 - 1);
      if (local617_2 == null || local617_2.size != local617_3 || local617_2.last != local617_4) {
         local617_2 = new SettingGroup.Nested0351(List.copyOf(method0504(module)), local617_3, local617_4);
         field00236.put(module, local617_2);
      }

      return local617_2.sections;
   }

   public static boolean method0503(List<SettingGroup> local618_0) {
      for (SettingGroup settingGroup : local618_0) {
         for (Setting<?> local618_4 : settingGroup.field00235) {
            if (local618_4.isVisible()) {
               return true;
            }
         }
      }

      return false;
   }

   public static List<SettingGroup> method0504(Module module) {
      List<SettingGroup> local619_1 = new ArrayList<>();
      String local619_2 = null;
      Object module2 = module;
      List<Setting<?>> local619_4 = new ArrayList<>();

      for (Setting<?> local619_6 : module.getSettings()) {
         if (local619_6 instanceof SectionSetting sectionSetting) {
            if (!local619_4.isEmpty() || local619_2 != null) {
               local619_1.add(new SettingGroup(local619_2, module2, local619_4));
            }

            local619_2 = sectionSetting.name;
            module2 = sectionSetting;
            local619_4 = new ArrayList<>();
         } else {
            local619_4.add(local619_6);
         }
      }

      if (!local619_4.isEmpty() || local619_2 != null) {
         local619_1.add(new SettingGroup(local619_2, module2, local619_4));
      }

      return local619_1;
   }

   private record Nested0351(List<SettingGroup> sections, int size, Setting<?> last) {
   }
}
