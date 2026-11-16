package deluxe.client.module.player;

import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;

@ModuleInfo(name = "NoPush", description = "Не даёт выбранным источникам толкать игрока.", category = ModuleCategory.PLAYER)
public final class NoPushModule extends Module {
   private static NoPushModule noPushModule;
   public final MultiSelectSetting multiSelectSetting = this.addSetting(
      new MultiSelectSetting("Толчки от", "Что не может толкать игрока.", new String[]{"Вода", "Лава", "Блоки", "Сущности", "Граница мира"}, "Вода", "Сущности")
   );

   public static NoPushModule method3305() {
      if (noPushModule == null) {
         noPushModule = new NoPushModule();
      }

      return noPushModule;
   }

   private NoPushModule() {
   }

   public boolean method3306(String local5397_1) {
      return this.isEnabled() && this.multiSelectSetting.matchesOption(local5397_1);
   }
}
