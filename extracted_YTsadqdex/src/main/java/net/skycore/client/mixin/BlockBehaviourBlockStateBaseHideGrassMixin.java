package net.skycore.client.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.module.movement.WebIgnoreModule;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class BlockBehaviourBlockStateBaseHideGrassMixin {
   @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
   private void skycore$hideGrass(CallbackInfoReturnable<RenderShape> callbackInfoReturnable) {
      BlockState blockState = (BlockState)(Object)this;
      if (NoRenderModule.shouldHideGrass(blockState)) {
         callbackInfoReturnable.setReturnValue(RenderShape.INVISIBLE);
      }
   }

   @Inject(
      method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void skycore$webIgnoreCollision(BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext, CallbackInfoReturnable<VoxelShape> callbackInfoReturnable) {
      if (WebIgnoreModule.shouldIgnore((BlockState)(Object)this)) {
         callbackInfoReturnable.setReturnValue(Shapes.empty());
      }
   }

   @Inject(
      method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void skycore$webIgnoreOutline(BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext, CallbackInfoReturnable<VoxelShape> callbackInfoReturnable) {
      if (WebIgnoreModule.shouldIgnore((BlockState)(Object)this)) {
         callbackInfoReturnable.setReturnValue(Shapes.empty());
      }
   }

   @Inject(method = "getInteractionShape", at = @At("HEAD"), cancellable = true, require = 0)
   private void skycore$webIgnoreRaycast(BlockGetter blockGetter, BlockPos blockPos, CallbackInfoReturnable<VoxelShape> callbackInfoReturnable) {
      if (WebIgnoreModule.shouldIgnore((BlockState)(Object)this)) {
         callbackInfoReturnable.setReturnValue(Shapes.empty());
      }
   }
}
