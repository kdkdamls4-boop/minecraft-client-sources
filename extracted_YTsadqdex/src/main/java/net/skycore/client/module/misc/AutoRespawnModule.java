package net.skycore.client.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class AutoRespawnModule extends ClientModule {
   public static final AutoRespawnModule INSTANCE = new AutoRespawnModule();

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (instance.gui.screen() instanceof DeathScreen) {
            if (instance.player.deathTime > 5) {
               instance.player.respawn();
               instance.gui.setScreen(null);
            }
         }
      }
   }

   private AutoRespawnModule() {
      super("Auto Respawn", "Respawns automatically after death", ModuleCategory.MISC);
   }
}
