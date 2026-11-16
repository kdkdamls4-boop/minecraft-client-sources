package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.movement.NoWebModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityIgnoreCobwebSlowdownMixin {
   @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
   private void skycore$ignoreCobwebSlowdown(BlockState blockState, Vec3 position, CallbackInfo callbackInfo) {
      if (blockState != null && blockState.is(Blocks.COBWEB)) {
         Entity entity = (Entity)(Object)this;
         if (entity == Minecraft.getInstance().player) {
            NoWebModule.markWebTouch();
            if (NoWebModule.shouldIgnoreCobwebSlowdown()) {
               callbackInfo.cancel();
            }
         }
      }
   }
}
