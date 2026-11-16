package deluxe.client.module.combat;

import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;

@ModuleInfo(
   name = "NoServerPack",
   description = "Блокирует подкачку и включение серверного ресурспака, отвечая серверу что пак загружен.",
   category = ModuleCategory.COMBAT
)
public final class NoServerPackModule extends Module {
   private static NoServerPackModule noServerPackModule;

   @Override
   protected void onEnable() {
      mc.getServerResourcePackProvider().removeAll();
   }

   public static NoServerPackModule method1257() {
      if (noServerPackModule == null) {
         noServerPackModule = new NoServerPackModule();
      }

      return noServerPackModule;
   }

   private NoServerPackModule() {
   }
}
