package net.skycore.client.util;

public class CapeSimulationConfig {
   public CapeMovementMode capeMovement;
   public CapeWindMode windMode = CapeWindMode.NONE;
   public CapeStyle capeStyle = CapeStyle.SMOOTH;
   public int straveMultiplier;
   public int heightMultiplier;
   public int gravity;

   public CapeSimulationConfig() {
      this.capeMovement = CapeMovementMode.BASIC_SIMULATION;
      this.gravity = 25;
      this.heightMultiplier = 6;
      this.straveMultiplier = 2;
   }
}
