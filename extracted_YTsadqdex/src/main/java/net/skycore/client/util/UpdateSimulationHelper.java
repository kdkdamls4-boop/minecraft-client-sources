package net.skycore.client.util;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.module.movement.AirStuckModule;

public interface UpdateSimulationHelper {
   public PreventHardBendsHelper getSimulation();

   default void updateSimulation(AbstractClientPlayer player, int value) {
      PreventHardBendsHelper simulation = this.getSimulation();
      if (simulation != null) {
         boolean init = simulation.init(value);
         if (init) {
            simulation.applyMovement(new CapeVector3(1.0F, 1.0F, 0.0F));

            for (int value2 = 0; value2 < 5; value2++) {
               this.simulate(player);
            }
         }
      }
   }

   default void simulate(AbstractClientPlayer player) {
      PreventHardBendsHelper simulation = this.getSimulation();
      if (simulation != null && !simulation.empty()) {
         if (AirStuckModule.INSTANCE.isFrozen()) {
            CapeVector3 normalizeHelper3 = new CapeVector3(0.0F, -1.0F, 0.0F);
            simulation.setGravityDirection(normalizeHelper3);
            simulation.setGravity(CapeSimulationManager.config.gravity);
            simulation.applyMovement(new CapeVector3(0.0F, 0.0F, 0.0F));
            simulation.simulate();
         } else {
            double x = player.xo - player.getX();
            double z = player.zo - player.getZ();
            float y = player.yBodyRotO + player.yBodyRot - player.yBodyRotO;
            double computedValue = Mth.sin(y * (float) (Math.PI / 180.0));
            double computedValue2 = -Mth.cos(y * (float) (Math.PI / 180.0));
            float value = CapeSimulationManager.config.heightMultiplier;
            float value2 = CapeSimulationManager.config.straveMultiplier;
            if (player.isInWater()) {
               value *= 2.0F;
            }

            double y2 = Mth.clamp((player.yo - player.getY()) * 10.0, 0.0, 1.0);
            if (player.isInWater()) {
               simulation.setGravity(CapeSimulationManager.config.gravity / 10.0F);
            } else {
               simulation.setGravity(CapeSimulationManager.config.gravity);
            }

            CapeVector3 normalizeHelper32 = new CapeVector3(0.0F, -1.0F, 0.0F);
            CapeVector2 normalizeHelper2 = new CapeVector2((float)(player.getX() - player.xo), (float)(player.getZ() - player.zo));
            normalizeHelper2.rotateDegrees(-player.getYRot());
            double computedValue3 = x * computedValue + z * computedValue2 + y2 + (player.isCrouching() && !simulation.isSneaking() ? 3 : 0);
            double y3 = (player.getY() - player.yo) * value + (player.isCrouching() && !simulation.isSneaking() ? 1 : 0);
            double x2 = -normalizeHelper2.x * value2;
            simulation.setSneaking(player.isCrouching());
            CapeVector3 normalizeHelper33 = new CapeVector3((float)computedValue3, (float)y3, (float)x2);
            simulation.setGravityDirection(normalizeHelper32);
            simulation.applyMovement(normalizeHelper33);
            simulation.simulate();
         }
      }
   }
}
