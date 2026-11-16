package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.skycore.client.module.render.SwingAnimationsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCustomSwingDurationMixin {
   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void skycore$customSwingDuration(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (entity == Minecraft.getInstance().player) {
         InteractionHand interactionHand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
         Integer value = SwingAnimationsModule.INSTANCE.customSwingDuration(interactionHand);
         if (value != null) {
            callbackInfoReturnable.setReturnValue(value);
         }
      }
   }
}
