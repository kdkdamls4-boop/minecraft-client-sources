package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.module.movement.GrimGlideModule;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGlidingMixin {
   @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
   private Vec3 skycore$gliding(Vec3 position) {
      return (Object)this instanceof LocalPlayer && SynchronizeHelper.isActive()
         ? ElytraTargetModule.INSTANCE.onGlidingVelocity(GrimGlideModule.INSTANCE.onGlidingVelocity(position))
         : position;
   }
}
