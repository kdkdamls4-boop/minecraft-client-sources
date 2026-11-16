package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.movement.AirStuckModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCancelTravelWhenAirStuckMixin {
   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void skycore$cancelTravelWhenAirStuck(Vec3 position, CallbackInfo callbackInfo) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (entity == Minecraft.getInstance().player) {
         if (AirStuckModule.INSTANCE.isFrozen()) {
            callbackInfo.cancel();
         }
      }
   }
}
