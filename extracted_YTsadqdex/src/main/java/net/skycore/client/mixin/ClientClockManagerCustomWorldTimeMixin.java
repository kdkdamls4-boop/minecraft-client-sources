package net.skycore.client.mixin;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.skycore.client.module.render.WorldTweaksModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerCustomWorldTimeMixin {
   @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
   private void skycore$customWorldTime(Holder<WorldClock> holder, CallbackInfoReturnable<Long> callbackInfoReturnable) {
      if (holder.is(WorldClocks.OVERWORLD)) {
         WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
         if (worldTweaksModule != null && worldTweaksModule.changesTime()) {
            callbackInfoReturnable.setReturnValue(worldTweaksModule.getCustomTime());
         }
      }
   }
}
