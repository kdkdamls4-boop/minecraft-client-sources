package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.combat.RotationController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerHandleSilentTurnMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
   private void skycore$handleSilentTurn(LocalPlayer player, double angle, double angle2) {
      if (this.minecraft.gui.screen() != null || !RotationController.onMouseTurn(angle, angle2)) {
         player.turn(angle, angle2);
      }
   }
}
