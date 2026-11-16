package net.skycore.client.util;

import net.skycore.client.module.movement.GuiMoveModule;

public final class RunWithBypassHelper {
   private RunWithBypassHelper() {
   }

   public static void runWithBypass(Runnable runnable) {
      GuiMoveModule iNSTANCE = GuiMoveModule.INSTANCE;
      if (iNSTANCE != null && iNSTANCE.isReallyworldMode()) {
         boolean bypassInventorySwap = iNSTANCE.bypassInventorySwap;
         iNSTANCE.bypassInventorySwap = true;

         try {
            runnable.run();
         } finally {
            iNSTANCE.bypassInventorySwap = bypassInventorySwap;
         }
      } else {
         runnable.run();
      }
   }
}
