package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRemoveBadEffectBlendMixin {
   @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
   private void skycore$removeBadEffectBlend(Holder<MobEffect> holder, float value, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      if ((Object)this == Minecraft.getInstance().player && NoRenderModule.shouldRemoveBadEffectsVisuals()) {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }
}
