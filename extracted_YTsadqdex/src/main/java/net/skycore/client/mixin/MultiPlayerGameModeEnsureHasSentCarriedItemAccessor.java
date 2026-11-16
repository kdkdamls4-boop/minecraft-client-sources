package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeEnsureHasSentCarriedItemAccessor {
   @Invoker("ensureHasSentCarriedItem")
   void skycore$ensureHasSentCarriedItem();

   @Invoker("startPrediction")
   void skycore$startPrediction(ClientLevel level, PredictiveAction predictiveAction);

   @Invoker("performUseItemOn")
   InteractionResult skycore$performUseItemOn(LocalPlayer player, InteractionHand interactionHand, BlockHitResult blockHitResult);

   @Accessor("destroyProgress")
   float skycore$getDestroyProgress();

   @Accessor("destroyProgress")
   void skycore$setDestroyProgress(float value);

   @Accessor("destroyDelay")
   int skycore$getDestroyDelay();

   @Accessor("destroyDelay")
   void skycore$setDestroyDelay(int value);

   @Accessor("destroyBlockPos")
   BlockPos skycore$getDestroyBlockPos();

   @Accessor("isDestroying")
   boolean skycore$isDestroying();

   @Accessor("destroyingItem")
   void skycore$setDestroyingItem(ItemStack itemStack);
}
