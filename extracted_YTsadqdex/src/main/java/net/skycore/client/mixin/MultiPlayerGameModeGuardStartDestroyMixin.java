package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeGuardStartDestroyMixin {
   @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void skycore$guardStartDestroy(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (skycore$shouldBlockBreak(blockPos)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }

   @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void skycore$guardContinueDestroy(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (skycore$shouldBlockBreak(blockPos)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }

   private static boolean skycore$shouldBlockBreak(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level == null) {
         return true;
      }

      BlockState blockState = instance.level.getBlockState(blockPos);
      if (blockState.isAir()) {
         return true;
      }

      FluidState fluidState = blockState.getFluidState();
      return !fluidState.isEmpty() && blockState.getCollisionShape(instance.level, blockPos).isEmpty();
   }
}
