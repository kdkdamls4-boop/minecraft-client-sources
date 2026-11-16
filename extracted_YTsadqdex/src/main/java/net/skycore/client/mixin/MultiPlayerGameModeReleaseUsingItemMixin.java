package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerUpdateEvent;
import net.skycore.client.util.BeforeMovementHelper;
import net.skycore.client.util.BeginSuppressViaPosRotHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeReleaseUsingItemMixin {
   @Inject(method = "releaseUsingItem", at = @At("HEAD"))
   private void skycore$releaseUsingItem(Player progress, CallbackInfo progress2) {
      PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent(progress);
      SkycoreClient.events().post(playerUpdateEvent);
   }

   @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
   private void skycore$viaDropPosRotBegin(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> callbackInfoReturnable) {
      if (player instanceof LocalPlayer player2 && BeforeMovementHelper.defer(player2, interactionHand)) {
         callbackInfoReturnable.setReturnValue(InteractionResult.SUCCESS);
      } else {
         BeginSuppressViaPosRotHelper.beginSuppressViaPosRot();
      }
   }

   @Inject(method = "useItem", at = @At("RETURN"))
   private void skycore$viaDropPosRotEnd(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> callbackInfoReturnable) {
      BeginSuppressViaPosRotHelper.endSuppressViaPosRot();
   }
}
