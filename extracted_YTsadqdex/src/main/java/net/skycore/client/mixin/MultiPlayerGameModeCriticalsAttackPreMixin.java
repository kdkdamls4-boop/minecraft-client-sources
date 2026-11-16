package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.module.combat.CriticalsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeCriticalsAttackPreMixin {
   @Inject(method = "attack", at = @At("HEAD"))
   private void skycore$criticalsAttackPre(Player player, Entity entity, CallbackInfo callbackInfo) {
      CriticalsModule.INSTANCE.onAttackPre(entity);
   }

   @Inject(method = "attack", at = @At("RETURN"))
   private void skycore$criticalsAttackPost(Player player, Entity entity, CallbackInfo callbackInfo) {
      CriticalsModule.INSTANCE.onAttackPost(entity);
   }
}
