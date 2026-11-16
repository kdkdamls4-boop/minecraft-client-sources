package net.skycore.client.module.render;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.CapeMovementMode;
import net.skycore.client.util.CapeSimulationManager;
import net.skycore.client.util.CapeWindMode;
import net.skycore.client.util.CapeSimulationConfig;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.BooleanSetting;

public final class CapeModule extends ClientModule {
   public static final CapeModule INSTANCE = new CapeModule();
   private static final String MOVEMENT_VANILLA = "Vanilla";
   private final ModeSetting movement = this.add(new ModeSetting("Movement", "Simulation", "Simulation", "Vanilla"));
   private static final String MOVEMENT_SIM = "Simulation";
   private final NumberSetting gravity;
   private final BooleanSetting wind = this.add(new BooleanSetting("Wind", false));

   public void syncConfigIfNeeded() {
      if (this.isEnabled()) {
         this.applyConfig();
      }
   }

   public void applyConfig() {
      int intermediate = -1;
      intermediate = (byte)-1;
      CapeSimulationConfig config = CapeSimulationManager.config;
      CapeMovementMode capeMovementMode;
      if (this.movement.is("Vanilla")) {
         capeMovementMode = CapeMovementMode.VANILLA;
      } else {
         capeMovementMode = CapeMovementMode.BASIC_SIMULATION;
      }

      config.capeMovement = capeMovementMode;
      intermediate = (byte)-1;
      CapeSimulationConfig config2 = CapeSimulationManager.config;
      Boolean get = this.wind.get();
      CapeWindMode capeWindMode;
      if (get) {
         capeWindMode = CapeWindMode.WAVES;
      } else {
         capeWindMode = CapeWindMode.NONE;
      }

      config2.windMode = capeWindMode;
      CapeSimulationConfig config3 = CapeSimulationManager.config;
      double value = this.gravity.get();
      intermediate = (int)Math.round(value);
      config3.gravity = intermediate;
   }

   private CapeModule() {
      super("Cape", "Custom wavey cape behind the player", ModuleCategory.RENDER);
      this.gravity = this.add(new NumberSetting("Gravity", 25.0, 5.0, 50.0, 1.0));
      this.gravity.visibleWhen(() -> {
         return this.movement.is("Simulation");
      });
      this.applyConfig();
   }

   @Override
   protected void onEnable() {
      this.applyConfig();
   }
}
