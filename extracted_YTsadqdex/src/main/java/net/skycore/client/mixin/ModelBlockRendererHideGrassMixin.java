package net.skycore.client.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererHideGrassMixin {
   @Inject(
      method = "tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void skycore$hideGrass(
      BlockQuadOutput blockQuadOutput,
      float value,
      float value2,
      float value3,
      BlockAndTintGetter blockAndTintGetter,
      BlockPos blockPos,
      BlockState blockState,
      BlockStateModel blockStateModel,
      long timestamp,
      CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.shouldHideGrass(blockState)) {
         callbackInfo.cancel();
      }
   }
}
