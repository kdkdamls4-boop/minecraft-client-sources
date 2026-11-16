package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.module.movement.NoPushModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNoWaterPushMixin {
   @Inject(method = "isPushedByFluid", at = @At("RETURN"), cancellable = true)
   private void skycore$noWaterPush(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if ((Object)this == Minecraft.getInstance().player && NoPushModule.waterPush()) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
