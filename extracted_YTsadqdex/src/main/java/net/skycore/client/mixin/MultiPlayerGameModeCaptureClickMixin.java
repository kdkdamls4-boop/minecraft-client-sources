package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.BlockPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeCaptureClickMixin {
   @Unique
   private BlockPos skycore$clickedPos;
   @Unique
   private BlockState skycore$clickedState;
   @Unique
   private Direction skycore$clickedSide;

   @Inject(method = "useItemOn", at = @At("HEAD"))
   private void skycore$captureClick(LocalPlayer player, InteractionHand interactionHand, BlockHitResult skycore$clickedPos2, CallbackInfoReturnable<InteractionResult> callbackInfoReturnable) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && skycore$clickedPos2 != null) {
         this.skycore$clickedPos = skycore$clickedPos2.getBlockPos();
         this.skycore$clickedState = instance.level.getBlockState(this.skycore$clickedPos);
         this.skycore$clickedSide = skycore$clickedPos2.getDirection();
      } else {
         this.skycore$clickedState = null;
      }
   }

   @Inject(method = "useItemOn", at = @At("RETURN"))
   private void skycore$notifyPlace(LocalPlayer player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> callbackInfoReturnable) {
      InteractionResult returnValue = (InteractionResult)callbackInfoReturnable.getReturnValue();
      if (returnValue != null && returnValue.consumesAction()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && player != null && this.skycore$clickedState != null && this.skycore$clickedPos != null) {
            BlockPos blockPos = this.skycore$clickedState.canBeReplaced() ? this.skycore$clickedPos : this.skycore$clickedPos.relative(this.skycore$clickedSide);
            BlockState blockState = instance.level.getBlockState(blockPos);
            if (!blockState.isAir()) {
               SkycoreClient.events().post(new BlockPlaceEvent(blockState.getBlock(), blockPos));
            } else {
               ItemStack itemInHand = player.getItemInHand(interactionHand);
               if (itemInHand.getItem() instanceof BlockItem blockItem) {
                  Block block = blockItem.getBlock();
                  SkycoreClient.events().post(new BlockPlaceEvent(block, blockPos));
               }
            }
         }
      }
   }
}
