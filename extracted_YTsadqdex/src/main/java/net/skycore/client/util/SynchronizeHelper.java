package net.skycore.client.util;

import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionStop;
import net.skycore.client.combat.AttackInputFreeze;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.combat.ServerRotationManager;
import net.skycore.client.module.ModuleManager;

public final class SynchronizeHelper {
   private static boolean lastActive;
   private static boolean initialized;

   public static void synchronize() {
      boolean active = isActive();
      if (initialized) {
         if (active == lastActive) {
            return;
         }
      }

      initialized = true;
      lastActive = active;
      if (!active) {
         resetToVanilla();
      }
   }

   private SynchronizeHelper() {
   }

   public static boolean isActive() {
      ModuleManager moduleManager = SkycoreClient.modules();
      return moduleManager.hasEnabledGameplayModules();
   }

   public static void resetToVanilla() {
      RotationManager.INSTANCE.stopRotation();
      ServerRotationManager.INSTANCE.stopRotation();
      RotationController.clear();
      AttackInputFreeze.reset();
      ActionStop.reset();
      ScheduleHelper.cancel();
   }
}
