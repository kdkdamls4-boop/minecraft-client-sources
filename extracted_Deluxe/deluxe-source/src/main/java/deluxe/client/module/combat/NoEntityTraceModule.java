package deluxe.client.module.combat;

import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;

@ModuleInfo(
   name = "NoEntityTrace",
   description = "Убирает хитбокс у всех сущностей в мире позволяя вам взаимодействовать через них.",
   category = ModuleCategory.COMBAT
)
public final class NoEntityTraceModule extends Module {
   private static NoEntityTraceModule noEntityTraceModule;

   private NoEntityTraceModule() {
   }

   public static NoEntityTraceModule method3535() {
      if (noEntityTraceModule == null) {
         noEntityTraceModule = new NoEntityTraceModule();
      }

      return noEntityTraceModule;
   }
}
