package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.command.FakeplayerCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeAttackFakePlayerMixin {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void skycore$attackFakePlayer(Player player, Entity entity, CallbackInfo callbackInfo) {
      if (FakeplayerCommand.isFake(entity) && entity instanceof FakeplayerCommand.FakePlayer fakePlayer) {
         fakePlayer.hurtClient(player.damageSources().playerAttack(player));
         callbackInfo.cancel();
      }
   }
}
