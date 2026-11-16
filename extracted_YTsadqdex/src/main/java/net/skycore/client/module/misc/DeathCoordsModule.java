package net.skycore.client.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SendDonationHelper;

public final class DeathCoordsModule extends ClientModule {
   public static final DeathCoordsModule INSTANCE = new DeathCoordsModule();
   private boolean sent;

   @Override
   protected void onDisable() {
      this.sent = false;
   }

   @Override
   protected void onTick() {
      int value = 1;
      int value2 = 0;
      value = (byte)1;
      value2 = (byte)0;
      value = (byte)1;
      value2 = (byte)0;
      value = (byte)1;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         this.sent = false;
      } else {
         int value3 = 1;
         if (instance.player.getHealth() > 0.0F) {
            this.sent = false;
         } else {
            value3 = (byte)-1;
            if (!this.sent) {
               value3 = (byte)1;
               value3 = (byte)1;
               if (instance.gui.screen() instanceof DeathScreen && instance.player.deathTime >= 1) {
                  value2 = (int)instance.player.getX();
                  value = (int)instance.player.getY();
                  value3 = (int)instance.player.getZ();
                  SendDonationHelper.displayInfo("Coordinates: X: " + value2 + " Y: " + value + " Z: " + value3);
                  this.sent = true;
               }
            }
         }
      }
   }

   private DeathCoordsModule() {
      super("Death Coords", "Shows death coordinates in chat", ModuleCategory.MISC);
   }
}
