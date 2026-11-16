package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.skycore.client.combat.MiningStrategy;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.util.AllowsHelper;
import net.skycore.client.util.VisualSlotHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeSourceToolMixin {
   @Inject(method = "startDestroyBlock", at = @At("HEAD"))
   private void skycore$sourceTool(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (CrystalAuraModule.INSTANCE.isReallyWorldModeActive() && !MiningStrategy.INSTANCE.hasTarget()) {
         VisualSlotHelper.INSTANCE.beforeStart(blockPos);
      }
   }

   @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
   private void skycore$sourceToolContinue(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (CrystalAuraModule.INSTANCE.isReallyWorldModeActive() && !MiningStrategy.INSTANCE.hasTarget()) {
         VisualSlotHelper.INSTANCE.beforeStart(blockPos);
      }
   }

   @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void skycore$keepCrystalDig(CallbackInfo callbackInfo) {
      if (CrystalAuraModule.INSTANCE.isReallyWorldModeActive() && MiningStrategy.INSTANCE.shouldKeepDestroy()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
   private void skycore$sourceEntityGuard(Player player, Entity entity, EntityHitResult entityHitResult, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> callbackInfoReturnable) {
      if (AllowsHelper.active()) {
         callbackInfoReturnable.setReturnValue(InteractionResult.PASS);
      }
   }
}
