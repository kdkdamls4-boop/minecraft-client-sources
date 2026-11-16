package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.module.render.HandsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeHandsHitPulseMixin {
   @Inject(method = "attack", at = @At("HEAD"))
   private void skycore$handsHitPulse(Player player, Entity entity, CallbackInfo callbackInfo) {
      HandsModule.INSTANCE.onHit();
   }
}
