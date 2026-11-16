package net.skycore.client.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineRemoveParticleMixin {
   @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
   private void skycore$removeParticle(
      ParticleOptions particleOptions, double value, double value2, double value3, double value4, double value5, double value6, CallbackInfoReturnable<Particle> callbackInfoReturnable
   ) {
      if (NoRenderModule.shouldRemoveParticle(particleOptions)) {
         callbackInfoReturnable.setReturnValue(null);
      }
   }
}
