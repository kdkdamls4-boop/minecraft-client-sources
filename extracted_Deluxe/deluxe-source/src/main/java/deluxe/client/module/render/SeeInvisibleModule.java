package deluxe.client.module.render;

import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;

@ModuleInfo(name = "SeeInvisible", description = "Показывает невидимых игроков и мобов с настраиваемой прозрачностью.", category = ModuleCategory.RENDER)
public final class SeeInvisibleModule extends Module {
   private static SeeInvisibleModule seeInvisibleModule;
   public LegacyNumberSetting legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Прозрачность", "Насколько плотно рисовать невидимых.", 0.55, 0.05, 1.0, 0.05));

   public static SeeInvisibleModule method4439() {
      if (seeInvisibleModule == null) {
         seeInvisibleModule = new SeeInvisibleModule();
      }

      return seeInvisibleModule;
   }

   private SeeInvisibleModule() {
   }
}
