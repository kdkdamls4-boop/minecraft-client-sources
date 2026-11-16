package net.skycore.client.util;

public class CapeSimulationManager {
   public static final String DEFAULT_CAPE_TEXTURE = "skycore:textures/cape.png";
   public static CapeSimulationManager INSTANCE;
   public static CapeSimulationConfig config = new CapeSimulationConfig();

   public CapeSimulationManager start() {
      INSTANCE = this;
      config = new CapeSimulationConfig();
      return this;
   }
}
