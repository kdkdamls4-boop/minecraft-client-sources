package net.skycore.client.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.skycore.client.module.movement.NoPushModule;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererVisibleInBlocksMixin {
   @Inject(method = "getViewBlockingState", at = @At("RETURN"), cancellable = true)
   private static void skycore$visibleInBlocks(Player player, CallbackInfoReturnable<@Nullable BlockState> callbackInfoReturnable) {
      if (NoPushModule.allowsBlockVisibility() && callbackInfoReturnable.getReturnValue() != null) {
         callbackInfoReturnable.setReturnValue(null);
      }
   }
}
