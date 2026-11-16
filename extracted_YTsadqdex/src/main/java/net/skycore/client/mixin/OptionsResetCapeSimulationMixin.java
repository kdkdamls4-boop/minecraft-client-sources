package net.skycore.client.mixin;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.util.PreventHardBendsHelper;
import net.skycore.client.util.UpdateSimulationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsResetCapeSimulationMixin {
   @Inject(method = "setCameraType", at = @At("TAIL"))
   private void skycore$resetCapeSimulation(CameraType cameraType, CallbackInfo callbackInfo) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance instanceof AbstractClientPlayer && instance instanceof UpdateSimulationHelper updateSimulationHelper) {
         PreventHardBendsHelper simulation = updateSimulationHelper.getSimulation();
         if (simulation != null) {
            simulation.points.clear();
            simulation.sticks.clear();
            simulation.init(16);
         }
      }
   }
}
