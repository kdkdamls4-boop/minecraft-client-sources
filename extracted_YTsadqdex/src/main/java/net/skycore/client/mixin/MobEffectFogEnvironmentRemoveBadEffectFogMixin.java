package net.skycore.client.mixin;

import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogEnvironmentRemoveBadEffectFogMixin {
   @Shadow
   public abstract Holder<MobEffect> getMobEffect();

   @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
   private void skycore$removeBadEffectFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      Holder mobEffect = this.getMobEffect();
      if (NoRenderModule.shouldRemoveBadEffectsVisuals() && (mobEffect == MobEffects.DARKNESS || mobEffect == MobEffects.BLINDNESS)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
