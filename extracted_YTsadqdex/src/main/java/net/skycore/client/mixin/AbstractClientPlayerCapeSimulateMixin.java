package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.render.CapeModule;
import net.skycore.client.util.PreventHardBendsHelper;
import net.skycore.client.util.UpdateSimulationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerCapeSimulateMixin implements UpdateSimulationHelper {
   @Unique
   private final PreventHardBendsHelper skycore$capeSimulation = new PreventHardBendsHelper();

   @Override
   public PreventHardBendsHelper getSimulation() {
      return this.skycore$capeSimulation;
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void skycore$capeSimulate(CallbackInfo callbackInfo) {
      if (CapeModule.INSTANCE.isEnabled()) {
         AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
         if (player.level() != null) {
            boolean instance = player == Minecraft.getInstance().player;
            boolean gameProfile = FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
            if (instance || gameProfile) {
               CapeModule.INSTANCE.syncConfigIfNeeded();
               this.updateSimulation(player, 16);
               this.simulate(player);
            }
         }
      }
   }
}
